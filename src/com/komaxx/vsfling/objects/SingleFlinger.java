package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import android.annotation.SuppressLint;
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
import com.komaxx.physics.constraints.PointToMassDistanceConstraint;
import com.komaxx.physics.constraints.PullConstraint;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

public class SingleFlinger extends Node {
	public static final int STRING_SEGMENTS = 18;
	
	public static final int PILLAR_CONSTRAINTS = 6;				// more == more stable
	public static final float PILLAR_CONSTRAIN_STRENGTH = 0.8f;
	public static final float PILLAR_CENTER_RADIUS = 50;		// larger==more stable
	
	public static final float STRING_ELEMENT_MASS = 2;
	
	/**
	 * In ]0;1]. The smaller, the higher the tension in the string.
	 */
	public static final float PRE_TENSION = 0.55f;
	
	/**
	 * The width of the string in world units.
	 */
	public static final float WIDTH = 200;
	
	public static final float TOUCHER_SIZE = 60;
	
	private static final float STRING_END_RADIUS = 15;
	/**
	 * Defines the distance between the touch-plates and the string. Set high enough
	 * to ensure that the string is completely visible.
	 */
	private static final float TOUCHER_STRING_DISTANCE = 70;
	/**
	 * Render thickness. No influence on physics.
	 */
	private static float STRING_THICKNESS = 10f;
	
	
	/**
	 * The touchable area of the flinger can only move inside of this area.
	 */
	private GlRect movementLimits = new GlRect(-100000, 100000, 100000, -100000);
	private boolean inverseNormals = false;
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private BoundFreeMesh flingerMesh = new BoundFreeMesh((STRING_SEGMENTS+1)*2, STRING_SEGMENTS*6);

	// The quad that is actually touched by the user to move the flinger
	private BoundTexturedQuad toucher = new BoundTexturedQuad();
	// string ends with little knots 
	private BoundTexturedQuad stringEndLeft = new BoundTexturedQuad();
	private BoundTexturedQuad stringEndRight = new BoundTexturedQuad();
	// pillars connect the toucher with the string. Will rotate when the flinger is longer stretched than allowed.
	private BoundFreeQuad pillarLeft = new BoundFreeQuad();
	private BoundFreeQuad pillarRight = new BoundFreeQuad();
	

	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////
	private Mass[] stringMasses = new Mass[STRING_SEGMENTS + 1];
	private PullConstraint[] stringConstraints = new PullConstraint[STRING_SEGMENTS];
	private PointToMassDistanceConstraint[] pillarConstraints = new PointToMassDistanceConstraint[PILLAR_CONSTRAINTS];
	private MinDistanceConstraint stringConstraintEndLeft;
	private MinDistanceConstraint stringConstraintEndRight;

	/**
	 * Stores the relative position from the center of the toucher to the anchor points
	 */
	private Vector2d[] relativeAnchorPositions = new Vector2d[PILLAR_CONSTRAINTS];
	
	// //////////////////////////////////////////////////
	// interaction
	// //////////////////////////////////////////////////
	private DragInteractionInterpreter dragInterpreter;
	
	// //////////////////////////////////////////////////
	// temp stuff
	// //////////////////////////////////////////////////
	
	
	private ACollidableGroup physicalFlinger = new ACollidableGroup() {
		boolean oddFrame = false;
		@Override
		public void step(float dt) {
			super.step(dt);
			stringConstraintEndLeft.setPosition(stringMasses[0].getPosition());
			stringConstraintEndRight.setPosition(stringMasses[STRING_SEGMENTS].getPosition());
			oddFrame = !oddFrame;
		}
	};

	
	public SingleFlinger(IPhysicsView tv, Vector2d toucherPos, Vector2d playDirection){
		draws = true;
		handlesInteraction = true;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;

		createPhysicObjects(toucherPos, playDirection);
		createRenderObjects();
		preplaceToucher(toucherPos);
		
		tv.getPhysics().addCollidable(physicalFlinger);
		
		dragInterpreter = new DragInteractionInterpreter(new ADragElement() {
			@Override
			public boolean inBounds(float[] xy) {
				return toucher.contains(xy[0], xy[1]);
			}
			
			@Override
			public void drag(InteractionContext ic, float[] nuPos) {
				movementLimits.clampInside(nuPos);
				moveToucher(nuPos[0], nuPos[1]);
			}
		});
	}
	
	private void moveToucher(float x, float y){
		// reposition toucher
		float halfSize = TOUCHER_SIZE/2f; 
		toucher.positionXY(x - halfSize, y + halfSize,
				x + halfSize, y - halfSize);
		
		// reposition attached physic points
		Vector2d relPoint;
		for (int i = 0; i < PILLAR_CONSTRAINTS; i++){
			relPoint = relativeAnchorPositions[i];
			pillarConstraints[i].v.set(x + relPoint.x, y + relPoint.y);
		}
	}

	/**
	 * for startup time. Place the toucher beneath
	 */
	private void preplaceToucher(Vector2d toucherPos) {
		float halfSize = TOUCHER_SIZE/2f; 
		toucher.positionXY(toucherPos.x - halfSize, toucherPos.y + halfSize,
				toucherPos.x + halfSize, toucherPos.y - halfSize);
	}

	private void createPhysicObjects(Vector2d toucherPos, Vector2d playDirection) {
		createStringMasses(toucherPos, playDirection);
		createStringConstraints(playDirection);
		createPillarConstraints(toucherPos, playDirection);
		
		stringConstraintEndLeft = new MinDistanceConstraint(stringMasses[0].getPosition(), STRING_END_RADIUS);
		stringConstraintEndLeft.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		
		stringConstraintEndRight = new MinDistanceConstraint(stringMasses[STRING_SEGMENTS].getPosition(), STRING_END_RADIUS);
		stringConstraintEndRight.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		
		physicalFlinger.addChild(stringConstraintEndLeft);
		physicalFlinger.addChild(stringConstraintEndRight);
		
		checkReverseNormals(playDirection);
	}

	private void createStringConstraints(Vector2d playDirection) {
		// connect all segments of the string with constraints.
		for (int i = 0; i < STRING_SEGMENTS; i++){
			// keep the flinger together
			PullConstraint s = new PullConstraint(stringMasses[i], stringMasses[i+1]);
			s.strength = 1f;
			stringConstraints[i] = s;
			physicalFlinger.addSteppable(s);

			// exert forces to falling objects
			FlingerConstraint fc = new FlingerConstraint(stringMasses[i], stringMasses[i+1], playDirection);
			stringConstraints[i].naturalLength = (WIDTH / STRING_SEGMENTS) * PRE_TENSION;
			fc.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
			fc.strength = 1.2f;
			physicalFlinger.addChild(fc);
		}
	}

	private void createStringMasses(Vector2d toucherPos, Vector2d playDirection) {
		// string center from touch point
		Vector2d.aToB(tmp1, toucherPos, playDirection);
		tmp1.normalize();
		Vector2d.normal(tmpNormal1, tmp1);
		tmp1.times(TOUCHER_STRING_DISTANCE);
		tmp1.add(toucherPos);
		
		tmpNormal1.times(WIDTH/2);
		tmp1.add(tmpNormal1);
		
		tmpNormal1.normalize();
		tmpNormal1.times(-WIDTH/STRING_SEGMENTS);
		
		// flinger string
		for (int i = 0; i < STRING_SEGMENTS+1; i++){
			Mass m = new Mass();
			m.setAcceptForceMask(ForceMasks.GRAVITY_FORCE | ForceMasks.BALL_PUSH_FORCE);
			m.mass = 4;
			m.setPosition(tmp1);
			stringMasses[i] = m;
			physicalFlinger.addChild(m);
			
			tmp1.add(tmpNormal1);
		}
		
		stringMasses[0].physical = true;
		stringMasses[STRING_SEGMENTS].physical = true;
	}

	public void resetPosition(Vector2d toucherPos, Vector2d playDirection) {
		// string center from touch point
		Vector2d.aToB(tmp1, toucherPos, playDirection);
		tmp1.normalize();
		Vector2d.normal(tmpNormal1, tmp1);
		tmp1.times(TOUCHER_STRING_DISTANCE);
		tmp1.add(toucherPos);
		
		// first mass position
		tmpNormal1.times(WIDTH/2);
		tmp1.add(tmpNormal1);
		
		// delta between string masses
		tmpNormal1.normalize();
		tmpNormal1.times(-WIDTH/STRING_SEGMENTS);
		
		// flinger string
		for (int i = 0; i < STRING_SEGMENTS+1; i++){
			stringMasses[i].setPosition(tmp1);
			tmp1.add(tmpNormal1);
		}

		// reset toucher position
		moveToucher(toucherPos.x, toucherPos.y);
	}
	
	@SuppressLint("FloatMath") 
	private void createPillarConstraints(Vector2d toucherPos, Vector2d playDirection) {
		Vector2d.aToB(tmp1, toucherPos, playDirection);
		tmp1.normalize();
		Vector2d.normal(tmpNormal1, tmp1);

		for (int i = 0; i < PILLAR_CONSTRAINTS/2; i++){
			float angle = (float) (((float)i / (float)(PILLAR_CONSTRAINTS-1)) * Math.PI);
			
			float xOffset = (float) Math.sin(angle) * PILLAR_CENTER_RADIUS;
			float yOffset = (float) Math.cos(angle) * PILLAR_CENTER_RADIUS;
			
			Vector2d offset = new Vector2d(tmp1);
			offset.times(yOffset);
			
			tmpNormal2.set(tmpNormal1);
			tmpNormal2.times(-xOffset);
			offset.add(tmpNormal2);
			relativeAnchorPositions[i*2] = offset;
			
			offset = new Vector2d(offset);
			tmpNormal2.times(-2);
			offset.add(tmpNormal2);
			relativeAnchorPositions[i*2 + 1] = offset;
		}
		
		// almost start from the center to avoid flipping over
		relativeAnchorPositions[0] = new Vector2d(0, 0);
		relativeAnchorPositions[1] = new Vector2d(0, 0);
				
		// now, create the constraints
		for (int i = 0; i < PILLAR_CONSTRAINTS/2; i++){
			PointToMassDistanceConstraint c = new PointToMassDistanceConstraint(stringMasses[STRING_SEGMENTS]);
			tmp1.set(toucherPos);
			tmp1.add(relativeAnchorPositions[i*2]);
			c.v.set(tmp1);
			c.strength = PILLAR_CONSTRAIN_STRENGTH;
			c.resetNaturalLength();
			pillarConstraints[i*2] = c;
			physicalFlinger.addSteppable(c);
			
			c = new PointToMassDistanceConstraint(stringMasses[0]);
			tmp1.set(toucherPos);
			tmp1.add(relativeAnchorPositions[i*2+1]);
			c.v.set(tmp1);
			c.strength = PILLAR_CONSTRAIN_STRENGTH;
			c.resetNaturalLength();
			pillarConstraints[i*2+1] = c;
			physicalFlinger.addSteppable(c);
		}
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
		for (int i = 0; i < STRING_SEGMENTS; i++){
			flingerIndices[indexIndex+0] = (short)i;
			flingerIndices[indexIndex+1] = (short)(i + STRING_SEGMENTS + 1);
			flingerIndices[indexIndex+2] = (short)(i + 1);

			flingerIndices[indexIndex+3] = (short)(i + 1);
			flingerIndices[indexIndex+4] = (short)(i + STRING_SEGMENTS + 1);
			flingerIndices[indexIndex+5] = (short)(i + STRING_SEGMENTS + 2);

			indexIndex += 6;
		}
		flingerMesh.setIndices(flingerIndices);
		
		flingerMesh.bindToVbo(vbo);
		
		toucher.bindToVbo(vbo);
		stringEndLeft.bindToVbo(vbo);
		stringEndRight.bindToVbo(vbo);
		pillarLeft.bindToVbo(vbo);
		pillarRight.bindToVbo(vbo);
		
		updateRenderingFromPhysics();
	}
	
	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		dragInterpreter.onInteraction(interactionContext);
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
			R.drawable.toucher_left
		}, t, 1);

		RectF[] uvRects = new RectF[uvRectsPx.length];
		for (int i = 0; i < uvRects.length; i++){
			uvRects[i] = t.getUvCoords(uvRectsPx[i]);
		}
		
		float uDelta = uvRects[0].width() / STRING_SEGMENTS;
		for(int i = 0; i < STRING_SEGMENTS+1; i++){
			flingerMesh.setTexCoordsUv(i, uvRects[0].left + i*uDelta, uvRects[0].top);
			flingerMesh.setTexCoordsUv(i, uvRects[0].left + i*uDelta, uvRects[0].bottom);
		}
		
		stringEndLeft.setTexCoordsUv(uvRects[1]);
		stringEndRight.setTexCoordsUv(uvRects[2]);
		
		toucher.setTexCoordsUv(uvRects[3]);
		
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
		indexCount += toucher.render(rc, indexBuffer);
		
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
		int l = STRING_SEGMENTS + 1;
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
		
		flingerMesh.positionXY(STRING_SEGMENTS+1, tmpNormal1.x, tmpNormal1.y);
		
		// points in between
		for (int i = 1; i < STRING_SEGMENTS; i++){
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
			
			flingerMesh.positionXY(i + STRING_SEGMENTS + 1, tmpNormal1.x, tmpNormal1.y);
		}
		
		// endpoint 2
		Vector2d.aToB(tmp1, stringMasses[STRING_SEGMENTS-1].getPosition(), stringMasses[STRING_SEGMENTS].getPosition());
		Vector2d.normal(tmpNormal1, tmp1);
		if (inverseNormals) tmpNormal1.invert();
		tmpNormal1.normalize();
		tmpNormal1.times(STRING_THICKNESS);
		tmpNormal1.add(stringMasses[STRING_SEGMENTS].getPosition());
		
		flingerMesh.positionXY(STRING_SEGMENTS*2+1, tmpNormal1.x, tmpNormal1.y);
	}

	public GlRect getMovementLimits() {
		return movementLimits;
	}

	public void setMovementLimits(GlRect movementLimits) {
		this.movementLimits = movementLimits;
	}
}
