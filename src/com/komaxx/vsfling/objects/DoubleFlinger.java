package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import android.graphics.Rect;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeMesh;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeQuad;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.scenegraph.interaction.DragInteractionInterpreter;
import com.komaxx.komaxx_gl.scenegraph.interaction.DragInteractionInterpreter.ADragElement;
import com.komaxx.komaxx_gl.scenegraph.interaction.InteractionContext;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.constraints.FlingerConstraint;
import com.komaxx.physics.constraints.MinDistanceConstraint;
import com.komaxx.physics.constraints.PullConstraint;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

public class DoubleFlinger extends Node {
	public static final int FLINGER_SEGMENTS = 18;

	public static final float MAX_STRING_LENGTH = 200;
	
	/**
	 * TODO: This might better be device independent...
	 */
	private static final float TOUCHER_SIZE = 60;
	/**
	 * on the ends of the strings, there are circles which repel the ball masses. They have this radius
	 */
	private static final float STRING_END_RADIUS = 15;
	/**
	 * Defines the distance between the touch-plates and the string. Set high enough
	 * to ensure that the string is completely visible.
	 */
	private static final float PILLAR_HEIGHT = 100;
	/**
	 * Render thickness. No influence on physics.
	 */
	private static float STRING_THICKNESS = 10f;
	/**
	 * Render thickness.
	 */
	private static float PILLAR_THICKNESS = 5f;
	
	
	private GlRect movementLimits = new GlRect(-100000, 100000, 100000, -100000);
	private boolean inverseNormals = false;
	
	private float pillarOffset;
	
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private BoundFreeMesh flingerMesh = new BoundFreeMesh((FLINGER_SEGMENTS+1)*2, FLINGER_SEGMENTS*6);

	// The quads that are actually touched by the user to play
	private BoundTexturedQuad toucherLeft = new BoundTexturedQuad();
	private BoundTexturedQuad toucherRight = new BoundTexturedQuad();
	// string ends with little knots 
	private BoundTexturedQuad stringEndLeft = new BoundTexturedQuad();
	private BoundTexturedQuad stringEndRight = new BoundTexturedQuad();
	// pillars connect the toucher with the string. Will rotate when the flinger is longer stretched than allowed.
	private BoundFreeQuad pillarLeft = new BoundFreeQuad();
	private BoundFreeQuad pillarRight = new BoundFreeQuad();
	

	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////
	private Mass[] stringMasses = new Mass[FLINGER_SEGMENTS + 1];
	private PullConstraint[] stringConstraints = new PullConstraint[FLINGER_SEGMENTS];
	private MinDistanceConstraint stringConstraintEndLeft;
	private MinDistanceConstraint stringConstraintEndRight;

	// //////////////////////////////////////////////////
	// interaction
	// //////////////////////////////////////////////////
	private DragInteractionInterpreter leftInterpreter;
	private DragInteractionInterpreter rightInterpreter;
	
	
	// //////////////////////////////////////////////////
	// temp stuff
	// //////////////////////////////////////////////////
	private static float[] tmpV1 = new float[2];
	private static float[] tmpV2 = new float[2];
	
	
	private ACollidableGroup physicalFlinger = new ACollidableGroup() {
		boolean oddFrame = false;
		@Override
		public void step(float dt) {
			super.step(dt);
			stringConstraintEndLeft.setPosition(stringMasses[0].getPosition());
			stringConstraintEndRight.setPosition(stringMasses[FLINGER_SEGMENTS].getPosition());
			oddFrame = !oddFrame;
		}
	};

	
	public DoubleFlinger(IPhysicsView tv, GlRect pos, Vector2d playDirection){
		draws = true;
		handlesInteraction = true;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;

		pillarOffset = (pos.centerY() < playDirection.y) ? pillarOffset=PILLAR_HEIGHT : -PILLAR_HEIGHT;
		
		createPhysicObjects(pos, playDirection);
		createRenderObjects();
		prePositionTouchers();
		
		tv.getPhysics().addCollidable(physicalFlinger);
		
		leftInterpreter = new DragInteractionInterpreter(new ADragElement() {
			@Override
			public boolean inBounds(float[] xy) {
				return toucherLeft.contains(xy[0], xy[1]);
			}
			
			@Override
			public void drag(InteractionContext ic, float[] nuPos) {
				positionToucher(false, nuPos);
				repositionPillars();
			}
		});
		
		rightInterpreter = new DragInteractionInterpreter(new ADragElement() {
			@Override
			public boolean inBounds(float[] xy) {
				return toucherRight.contains(xy[0], xy[1]);
			}
			
			@Override
			public void drag(InteractionContext ic, float[] nuPos) {
				positionToucher(true, nuPos);
				repositionPillars();
			}
		});
	}

	private void prePositionTouchers() {
		float halfSize = TOUCHER_SIZE/2f; 

		Vector2d massPos = stringMasses[0].getPosition();
		toucherLeft.positionXY(massPos.x - halfSize, massPos.y - pillarOffset + halfSize,
				massPos.x + halfSize, massPos.y - pillarOffset - halfSize);

		massPos = stringMasses[FLINGER_SEGMENTS].getPosition();
		toucherRight.positionXY(massPos.x - halfSize, massPos.y - pillarOffset + halfSize,
				massPos.x + halfSize, massPos.y - pillarOffset - halfSize);
		
		repositionPillars();
	}

	/**
	 * This will set the position of the pillars and, consequently, of the 
	 * ends of the strings. Will take into account the max allowed length
	 * of the string.
	 */
	protected void repositionPillars() {
		float leftX = toucherLeft.getPosition().centerX();
		float rightX = toucherRight.getPosition().centerX();
		
		// first: place the end-points of the string
		
		if (rightX - leftX <= MAX_STRING_LENGTH){
			// easy case: below max length, let the pillars just stand upright
			stringMasses[0].setPosition(leftX, toucherLeft.getPosition().centerY() + pillarOffset);
			stringMasses[FLINGER_SEGMENTS].setPosition(rightX, toucherRight.getPosition().centerY() + pillarOffset);
		} else {
			// simple start: just center it, no rotation
			float offset = (rightX-leftX - MAX_STRING_LENGTH) /2;
			
			stringMasses[0].setPosition(leftX + offset, toucherLeft.getPosition().centerY() + pillarOffset);
			stringMasses[FLINGER_SEGMENTS].setPosition(rightX - offset, toucherRight.getPosition().centerY() + pillarOffset);
			

			// more complicated version (for later?)
			/*
			// the string would be more stretched than allowed when the pillars 
			// are just upright. Rotate accordingly!
			
			if (rightX-leftX > MAX_STRING_LENGTH + 2*PILLAR_HEIGHT){
				// easy case: Pillars are horizontal
			} else {
				// find angle of the pillars.
			}
			//*/
		}
		
		// place knots
		Vector2d pos = stringMasses[0].getPosition();
		stringEndLeft.positionXY(pos.x-STRING_END_RADIUS, pos.y + STRING_END_RADIUS, pos.x+STRING_END_RADIUS, pos.y-STRING_END_RADIUS);
		pos = stringMasses[FLINGER_SEGMENTS].getPosition();
		stringEndRight.positionXY(pos.x-STRING_END_RADIUS, pos.y + STRING_END_RADIUS, pos.x+STRING_END_RADIUS, pos.y-STRING_END_RADIUS);
		
		// place pillars renderings
		toucherLeft.getPosition().getCenter2(tmpV1);
		stringEndLeft.getPosition().getCenter2(tmpV2);
		pillarLeft.positionAlong2(tmpV1, tmpV2, PILLAR_THICKNESS);
		
		toucherRight.getPosition().getCenter2(tmpV1);
		stringEndRight.getPosition().getCenter2(tmpV2);
		pillarRight.positionAlong2(tmpV1, tmpV2, PILLAR_THICKNESS);
	}

	private static GlRect tmpPosRect = new GlRect();
	protected void positionToucher(boolean rightToucher, float[] nuPos) {
		float halfSize = TOUCHER_SIZE/2f; 
		// position
		tmpPosRect.set(nuPos[0] - halfSize, nuPos[1] + halfSize, nuPos[0] + halfSize, nuPos[1] - halfSize);
		// limit to valid area
		tmpPosRect.moveInside(movementLimits);
		
		BoundTexturedQuad toucher = rightToucher ? toucherRight : toucherLeft;
		
		float deltaX = tmpPosRect.centerX() - toucher.getPosition().centerX();
		float deltaY = tmpPosRect.centerY() - toucher.getPosition().centerY();
		
		toucher.positionXY(tmpPosRect);
		
		// only one down point? Then move the other, too!
		if ((rightToucher && !leftInterpreter.isBound()) || (!rightToucher && !rightInterpreter.isBound())){
			toucher = rightToucher ? toucherLeft : toucherRight;
			float x = toucher.getPosition().centerX() + deltaX;
			float y = toucher.getPosition().centerY() + deltaY;
			
			tmpPosRect.set(x - halfSize, y + halfSize, x + halfSize, y - halfSize);
			tmpPosRect.moveInside(movementLimits);
			toucher.positionXY(tmpPosRect);
		}
	}

	private void createPhysicObjects(GlRect pos, Vector2d playDirection) {
		float y = pos.top;
		
		float width = pos.width();
		float x = pos.left;
		
		// flinger string
		for (int i = 0; i < FLINGER_SEGMENTS+1; i++){
			Mass m = new Mass();
			m.setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
			m.mass = 4;
			m.setPosition(x + i*(width/FLINGER_SEGMENTS), y);
			stringMasses[i] = m;
			physicalFlinger.addChild(m);
		}
		
		stringMasses[0].physical = false;
		stringMasses[FLINGER_SEGMENTS].physical = false;
		
		for (int i = 0; i < FLINGER_SEGMENTS; i++){
			// keep the flinger together
			PullConstraint s = new PullConstraint(stringMasses[i], stringMasses[i+1]);
			s.strength = 1f;
			stringConstraints[i] = s;
			physicalFlinger.addSteppable(s);

			// exert forces to falling objects
			FlingerConstraint fc = new FlingerConstraint(stringMasses[i], stringMasses[i+1], playDirection);
			stringConstraints[i].naturalLength = (MAX_STRING_LENGTH / FLINGER_SEGMENTS) * 0.66f;
			fc.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
			fc.strength = 1.2f;
			physicalFlinger.addChild(fc);
		}
		
		stringConstraintEndLeft = new MinDistanceConstraint(stringMasses[0].getPosition(), STRING_END_RADIUS);
		stringConstraintEndLeft.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		
		stringConstraintEndRight = new MinDistanceConstraint(stringMasses[FLINGER_SEGMENTS].getPosition(), STRING_END_RADIUS);
		stringConstraintEndRight.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		
		physicalFlinger.addChild(stringConstraintEndLeft);
		physicalFlinger.addChild(stringConstraintEndRight);
		
		checkReverseNormals(playDirection);
	}

	
	private void checkReverseNormals(Vector2d centralPoint) {
		Vector2d massToCenter = Vector2d.aToB(new Vector2d(), stringMasses[0].getPosition(), centralPoint);
		Vector2d tmpNormal = Vector2d.aToB(new Vector2d(), stringMasses[0].getPosition(), stringMasses[1].getPosition());
		inverseNormals = Vector2d.dotProduct(massToCenter, tmpNormal) < 0;
	}
	
	private void createRenderObjects() {
		vbo = new Vbo(
				flingerMesh.getMaxVertexCount() + 2*3*TexturedQuad.VERTEX_COUNT,
				TexturedVertex.STRIDE_BYTES
		);
		indexBuffer = Vertex.allocateIndices(
				flingerMesh.getMaxIndexCount() + 2*3*TexturedQuad.INDICES_COUNT
		);

		// create the indices for the flingerMesh
		short[] flingerIndices = new short[flingerMesh.getMaxIndexCount()];
		int indexIndex = 0;
		for (int i = 0; i < FLINGER_SEGMENTS; i++){
			flingerIndices[indexIndex+0] = (short)i;
			flingerIndices[indexIndex+1] = (short)(i + FLINGER_SEGMENTS + 1);
			flingerIndices[indexIndex+2] = (short)(i + 1);

			flingerIndices[indexIndex+3] = (short)(i + 1);
			flingerIndices[indexIndex+4] = (short)(i + FLINGER_SEGMENTS + 1);
			flingerIndices[indexIndex+5] = (short)(i + FLINGER_SEGMENTS + 2);

			indexIndex += 6;
		}
		flingerMesh.setIndices(flingerIndices);
		
		flingerMesh.bindToVbo(vbo);
		
		toucherLeft.bindToVbo(vbo);
		toucherRight.bindToVbo(vbo);
		stringEndLeft.bindToVbo(vbo);
		stringEndRight.bindToVbo(vbo);
		pillarLeft.bindToVbo(vbo);
		pillarRight.bindToVbo(vbo);
		
		updateRenderingFromPhysics();
	}
	
	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		if (!rightInterpreter.onInteraction(interactionContext))
			leftInterpreter.onInteraction(interactionContext);
		return false;
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.edgeBehavior = TextureConfig.EDGE_MIRROR_REPEAT;
		tc.minWidth = 256;
		tc.minHeight = 256;
		tc.mipMapped = false;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		Rect[] uvRectsPx = AtlasPainter.drawAtlas(renderContext.resources, new int[]{
			R.drawable.pull_string,
			R.drawable.string_end_left,
			R.drawable.string_end_right,
			R.drawable.toucher_left,
			R.drawable.toucher_right,
		}, t, 1);

		RectF[] uvRects = new RectF[uvRectsPx.length];
		for (int i = 0; i < uvRects.length; i++){
			uvRects[i] = t.getUvCoords(uvRectsPx[i]);
		}
		
		float uDelta = uvRects[0].width() / FLINGER_SEGMENTS;
		for(int i = 0; i < FLINGER_SEGMENTS+1; i++){
			flingerMesh.setTexCoordsUv(i, uvRects[0].left + i*uDelta, uvRects[0].top);
			flingerMesh.setTexCoordsUv(i, uvRects[0].left + i*uDelta, uvRects[0].bottom);
		}
		
		stringEndLeft.setTexCoordsUv(uvRects[1]);
		stringEndRight.setTexCoordsUv(uvRects[2]);
		
		toucherLeft.setTexCoordsUv(uvRects[3]);
		toucherRight.setTexCoordsUv(uvRects[4]);
		
		RectF r = uvRects[0];
		pillarLeft.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_LEFT, r.left, r.top);
		pillarLeft.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_RIGHT, r.right, r.top);
		pillarLeft.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_LEFT, r.left, r.bottom);
		pillarLeft.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_RIGHT, r.right, r.bottom);

		pillarRight.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_LEFT, r.left, r.top);
		pillarRight.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_RIGHT, r.right, r.top);
		pillarRight.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_LEFT, r.left, r.bottom);
		pillarRight.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_RIGHT, r.right, r.bottom);

	}
	

	@Override
	public boolean onRender(RenderContext rc) {
		updateRenderingFromPhysics();
		
		int indexCount = 0;

		indexCount += flingerMesh.render(rc, indexBuffer);
		indexCount += pillarLeft.render(rc, indexBuffer);
		indexCount += pillarRight.render(rc, indexBuffer);
		indexCount += stringEndLeft.render(rc, indexBuffer);
		indexCount += stringEndRight.render(rc, indexBuffer);
		indexCount += toucherLeft.render(rc, indexBuffer);
		indexCount += toucherRight.render(rc, indexBuffer);
		
		Vertex.renderTexturedTriangles(rc.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}

	private static Vector2d tmpNormal1 = new Vector2d();
	private static Vector2d tmpNormal2 = new Vector2d();
	private static Vector2d tmp1 = new Vector2d();
	/**
	 * Called during physics cycle. Supposed to adapt the rendering to
	 * the current state of the physics simulation.
	 */
	private void updateRenderingFromPhysics() {
		// first: position vertices on mass points
		int l = FLINGER_SEGMENTS + 1;
		for (int i = 0; i < l; i++){
			Vector2d position = stringMasses[i].getPosition();
			flingerMesh.positionXY(i, position.x, position.y);
		}
		
		// now: place second row according to mixed normals
		
		// endpoint 0
		Vector2d.aToB(tmp1, stringMasses[0].getPosition(), stringMasses[1].getPosition());
		Vector2d.normal(tmpNormal1, tmp1);
		if (inverseNormals) tmpNormal1.invert();
		tmpNormal1.normalize();
		tmpNormal1.times(STRING_THICKNESS);
		tmpNormal1.add(stringMasses[0].getPosition());
		
		flingerMesh.positionXY(FLINGER_SEGMENTS+1, tmpNormal1.x, tmpNormal1.y);
		
		// points in between
		for (int i = 1; i < FLINGER_SEGMENTS; i++){
			Vector2d.aToB(tmp1, stringMasses[i-1].getPosition(), stringMasses[i].getPosition());
			Vector2d.normal(tmpNormal1, tmp1);
			if (inverseNormals) tmpNormal1.invert();
			
			Vector2d.aToB(tmp1, stringMasses[i].getPosition(), stringMasses[i+1].getPosition());
			Vector2d.normal(tmpNormal2, tmp1);
			if (inverseNormals) tmpNormal2.invert();
			
			tmpNormal1.add(tmpNormal2);
			tmpNormal1.normalize();
			tmpNormal1.times(STRING_THICKNESS);
			tmpNormal1.add(stringMasses[i].getPosition());
			
			flingerMesh.positionXY(i + FLINGER_SEGMENTS + 1, tmpNormal1.x, tmpNormal1.y);
		}
		
		// endpoint 2
		Vector2d.aToB(tmp1, stringMasses[FLINGER_SEGMENTS-1].getPosition(), stringMasses[FLINGER_SEGMENTS].getPosition());
		Vector2d.normal(tmpNormal1, tmp1);
		if (inverseNormals) tmpNormal1.invert();
		tmpNormal1.normalize();
		tmpNormal1.times(STRING_THICKNESS);
		tmpNormal1.add(stringMasses[FLINGER_SEGMENTS].getPosition());
		
		flingerMesh.positionXY(FLINGER_SEGMENTS*2+1, tmpNormal1.x, tmpNormal1.y);
	}

	public GlRect getMovementLimits() {
		return movementLimits;
	}

	public void setMovementLimits(GlRect movementLimits) {
		this.movementLimits = movementLimits;
	}
}
