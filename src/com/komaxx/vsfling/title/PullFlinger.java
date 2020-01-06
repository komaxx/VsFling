package com.komaxx.vsfling.title;

import java.nio.ShortBuffer;

import android.graphics.Rect;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeMesh;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.scenegraph.interaction.DragInteractionInterpreter;
import com.komaxx.komaxx_gl.scenegraph.interaction.DragInteractionInterpreter.IDragElement;
import com.komaxx.komaxx_gl.scenegraph.interaction.InteractionContext;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.constraints.FlingerConstraint;
import com.komaxx.physics.constraints.PullConstraint;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

public class PullFlinger extends Node implements IDragElement {
	public static final int FLINGER_SEGMENTS = 18;
	public static final int PULLER_SEGMENTS = 2;
	
	private static float stringWidth = 10f;
	private static float pullerLength = 60f;
	private static float pullerSize = 60f;
	

	private boolean inverseNormals = false;
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private BoundFreeMesh flingerMesh = new BoundFreeMesh((FLINGER_SEGMENTS+1)*2, FLINGER_SEGMENTS*6);
	private BoundFreeQuad pullStringQuad = new BoundFreeQuad();
	private BoundFreeQuad pullerQuad = new BoundFreeQuad();
	

	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////
	private Mass[] stringMasses = new Mass[FLINGER_SEGMENTS + 1];
	private Mass[] pullMasses = new Mass[PULLER_SEGMENTS];
	private PullConstraint[] stringConstraints = new PullConstraint[FLINGER_SEGMENTS];
	private PullConstraint[] pullStringConstraints = new PullConstraint[PULLER_SEGMENTS];

	// //////////////////////////////////////////////////
	// interaction
	// //////////////////////////////////////////////////
	private DragInteractionInterpreter interactionInterpreter = new DragInteractionInterpreter(this);
	
	// //////////////////////////////////////////////////
	// temp stuff
	// //////////////////////////////////////////////////

	
	
	private ACollidableGroup physicalFlinger = new ACollidableGroup(){
		// nothing unusual; use default behaviour
	};

	
	public PullFlinger(TitleView tv, GlRect pos){
		draws = true;
		handlesInteraction = true;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		
		createPhysicObjects(pos);
		createRenderObjects();
		
		tv.getPhysics().addCollidable(physicalFlinger);
	}
	
	private void createPhysicObjects(GlRect pos) {
		// flinger string
		for (int i = 0; i < FLINGER_SEGMENTS+1; i++){
			Mass m = new Mass();
			m.mass = 1.4f;
			m.setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
			stringMasses[i] = m;
			physicalFlinger.addChild(m);
		}
		
		stringMasses[0].physical = false;
		stringMasses[FLINGER_SEGMENTS].physical = false;
		
		Vector2d centralPoint = new Vector2d(stringMasses[FLINGER_SEGMENTS/1].getPosition());
		centralPoint.y += 300;
		
		// pull string
		for (int i = 0; i < PULLER_SEGMENTS; i++){
			pullMasses[i] = new Mass();
			pullMasses[i].mass = 0.2f;
			pullMasses[i].friction = 0.04f;
			pullMasses[i].setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
			physicalFlinger.addChild(pullMasses[i]);
		}
		
		// pull constraints
		pullStringConstraints[0] = new PullConstraint(
				stringMasses[FLINGER_SEGMENTS/2], pullMasses[0]);
		pullStringConstraints[0].strength = 1.0f;
		pullStringConstraints[0].naturalLength = pullerLength/PULLER_SEGMENTS;
		physicalFlinger.addSteppable(pullStringConstraints[0]);
		for (int i = 1; i < PULLER_SEGMENTS; i++){
			pullStringConstraints[i] = new PullConstraint(pullMasses[i-1], pullMasses[i]);
			pullStringConstraints[i].naturalLength = pullerLength/PULLER_SEGMENTS;
			pullStringConstraints[i].strength = 1f;
			physicalFlinger.addSteppable(pullStringConstraints[i]);
		}
		
		for (int i = 0; i < FLINGER_SEGMENTS; i++){
			// keep the flinger together
			PullConstraint s = new PullConstraint(stringMasses[i], stringMasses[i+1]);
			s.strength = 1f;
			stringConstraints[i] = s;
			physicalFlinger.addSteppable(s);
			
			// exert forces to falling objects
			FlingerConstraint fc = new FlingerConstraint(stringMasses[i], stringMasses[i+1], centralPoint);
			fc.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
			fc.strength = 1.2f;
			physicalFlinger.addChild(fc);
		}
		
		placeMasses(pos);
		checkReverseNormals(centralPoint);
	}

	
	private void checkReverseNormals(Vector2d centralPoint) {
		Vector2d massToCenter = Vector2d.aToB(new Vector2d(), stringMasses[0].getPosition(), centralPoint);
		Vector2d tmpNormal = Vector2d.aToB(new Vector2d(), stringMasses[0].getPosition(), stringMasses[1].getPosition());
		inverseNormals = Vector2d.dotProduct(massToCenter, tmpNormal) < 0;
	}

	private void placeMasses(GlRect pos) {
		float y = pos.top;
		
		float width = pos.width();
		float x = pos.left;
		
		for (int i = 0; i < FLINGER_SEGMENTS+1; i++){
			stringMasses[i].setPosition(x + i*(width/FLINGER_SEGMENTS), y);
		}

		for (int i = 0; i < FLINGER_SEGMENTS; i++){
			// keep the flinger together
			stringConstraints[i].naturalLength = width/FLINGER_SEGMENTS * 0.2f;
		}

		Vector2d nowMassPos = new Vector2d(pullStringConstraints[0].m1.getPosition());
		for (int i = 0; i < PULLER_SEGMENTS; i++){
			nowMassPos.add(0, -pullerLength / PULLER_SEGMENTS);
			pullMasses[i].setPosition(nowMassPos);
		}
	}
	
	private void createRenderObjects() {
		vbo = new Vbo(
				flingerMesh.getMaxVertexCount() + pullerQuad.getMaxVertexCount() + 
				pullStringQuad.getMaxVertexCount(),
				TexturedVertex.STRIDE_BYTES
		);
		indexBuffer = Vertex.allocateIndices(
				flingerMesh.getMaxIndexCount() + pullerQuad.getMaxIndexCount() + 
				pullStringQuad.getMaxIndexCount()
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
		pullStringQuad.bindToVbo(vbo);
		pullerQuad.bindToVbo(vbo);
		
		updatePositions();
	}
	
	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		
		KoLog.i(this, interactionInterpreter.stringState(interactionContext));
		
		return interactionInterpreter.onInteraction(interactionContext);
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
			R.drawable.puller
		}, t, 1);

		RectF[] uvRects = new RectF[uvRectsPx.length];
		for (int i = 0; i < uvRects.length; i++){
			uvRects[i] = new RectF(
					(float)uvRectsPx[i].left / (float)t.getWidth(),
					(float)(uvRectsPx[i].top+2) / (float)t.getHeight(),
					(float)uvRectsPx[i].right / (float)t.getWidth(),
					((float)uvRectsPx[i].bottom-1) / (float)t.getHeight()
					);
		}
		
		float uDelta = uvRects[0].width() / FLINGER_SEGMENTS;
		for(int i = 0; i < FLINGER_SEGMENTS+1; i++){
			flingerMesh.setTexCoordsUv(i, uvRects[0].left + i*uDelta, uvRects[0].top);
			flingerMesh.setTexCoordsUv(i, uvRects[0].left + i*uDelta, uvRects[0].bottom);
		}
		
		pullerQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_LEFT, uvRects[1].left, uvRects[1].top);
		pullerQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_RIGHT, uvRects[1].right, uvRects[1].top);
		pullerQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_LEFT, uvRects[1].left, uvRects[1].bottom);
		pullerQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_RIGHT, uvRects[1].right, uvRects[1].bottom);

		pullStringQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_LEFT, uvRects[1].left, uvRects[1].top);
		pullStringQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_UPPER_RIGHT, uvRects[1].right, uvRects[1].top);
		pullStringQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_LEFT, uvRects[1].left, uvRects[1].bottom);
		pullStringQuad.setTexCoordsUv(BoundFreeQuad.VERTEX_LOWER_RIGHT, uvRects[1].right, uvRects[1].bottom);
	}

	@Override
	public boolean onRender(RenderContext rc) {
		updatePositions();
		
		int indexCount = 0;

		indexCount += flingerMesh.render(rc, indexBuffer);
		indexCount += pullStringQuad.render(rc, indexBuffer);
		indexCount += pullerQuad.render(rc, indexBuffer);
		
		Vertex.renderTexturedTriangles(rc.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}

	private static Vector2d tmpNormal1 = new Vector2d();
	private static Vector2d tmpNormal2 = new Vector2d();
	private static Vector2d tmp1 = new Vector2d();
	private static Vector2d tmp2 = new Vector2d();
	private void updatePositions() {
		updateString();
		updatePullString();
		updatePullerPosition();
	}

	private void updateString() {
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
		tmpNormal1.times(stringWidth);
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
			tmpNormal1.times(stringWidth);
			tmpNormal1.add(stringMasses[i].getPosition());
			
			flingerMesh.positionXY(i + FLINGER_SEGMENTS + 1, tmpNormal1.x, tmpNormal1.y);
		}
		
		// endpoint 2
		Vector2d.aToB(tmp1, stringMasses[FLINGER_SEGMENTS-1].getPosition(), stringMasses[FLINGER_SEGMENTS].getPosition());
		Vector2d.normal(tmpNormal1, tmp1);
		if (inverseNormals) tmpNormal1.invert();
		tmpNormal1.normalize();
		tmpNormal1.times(stringWidth);
		tmpNormal1.add(stringMasses[FLINGER_SEGMENTS].getPosition());
		
		flingerMesh.positionXY(FLINGER_SEGMENTS*2+1, tmpNormal1.x, tmpNormal1.y);
	}

	private void updatePullString() {
		Vector2d.aToB(tmp1, 
				pullStringConstraints[0].m1.getPosition(),
				pullStringConstraints[0].m2.getPosition());
		Vector2d.normal(tmpNormal1, tmp1);
		tmpNormal1.normalize();
		tmpNormal1.times(stringWidth * 0.5f);
		
		Vector2d position = pullStringConstraints[0].m1.getPosition();
		pullStringQuad.positionXY(BoundFreeQuad.VERTEX_UPPER_LEFT, 
				position.x-tmpNormal1.x, 
				position.y-tmpNormal1.y);
		pullStringQuad.positionXY(BoundFreeQuad.VERTEX_UPPER_RIGHT, 
				position.x+tmpNormal1.x, 
				position.y+tmpNormal1.y);

		tmp1.times(1.2f);
		tmp1.add(pullStringConstraints[0].m1.getPosition());
		
		pullStringQuad.positionXY(BoundFreeQuad.VERTEX_LOWER_LEFT, 
				tmp1.x-tmpNormal1.x, 
				tmp1.y-tmpNormal1.y);
		pullStringQuad.positionXY(BoundFreeQuad.VERTEX_LOWER_RIGHT, 
				tmp1.x+tmpNormal1.x, 
				tmp1.y+tmpNormal1.y);
	}

	private void updatePullerPosition() {
		// the puller
		Vector2d.aToB(tmp1, 
				pullStringConstraints[PULLER_SEGMENTS-1].m1.getPosition(),
				pullStringConstraints[PULLER_SEGMENTS-1].m2.getPosition());
		Vector2d.normal(tmpNormal1, tmp1);
		tmpNormal1.normalize();
		tmpNormal1.times(pullerSize * 0.5f);
		
		// upper left
		tmp2.set(pullStringConstraints[PULLER_SEGMENTS-1].m1.getPosition());
		pullerQuad.positionXY(BoundFreeQuad.VERTEX_UPPER_LEFT, 
				tmp2.x - tmpNormal1.x, tmp2.y - tmpNormal1.y);
		// upper right
		pullerQuad.positionXY(BoundFreeQuad.VERTEX_UPPER_RIGHT, 
				tmp2.x + tmpNormal1.x, tmp2.y + tmpNormal1.y);
		
		tmp1.normalize();
		tmp1.times(pullerSize);
		tmp1.add(pullStringConstraints[PULLER_SEGMENTS-1].m1.getPosition());
		
		// lower left
		pullerQuad.positionXY(BoundFreeQuad.VERTEX_LOWER_LEFT, 
				tmp1.x - tmpNormal1.x, tmp1.y - tmpNormal1.y);
		// upper right
		pullerQuad.positionXY(BoundFreeQuad.VERTEX_LOWER_RIGHT, 
				tmp1.x + tmpNormal1.x, tmp1.y + tmpNormal1.y);
	}

	@Override
	public boolean inBounds(float[] xy) {
		return pullerQuad.contains(xy);
	}

	@Override
	public void down(InteractionContext ic) {
		pullMasses[PULLER_SEGMENTS-1].physical = false;
	}

	@Override
	public void cancel(InteractionContext ic) {
		pullMasses[PULLER_SEGMENTS-1].physical = true;
	}

	@Override
	public void drag(InteractionContext ic, float[] nuPos) {
//		KoLog.i(this, "drag: " + Vector.toString(nuPos, 2));
		pullMasses[PULLER_SEGMENTS-1].setPosition(nuPos[0], nuPos[1]);
	}

	@Override
	public void click(InteractionContext ic) {
		pullMasses[PULLER_SEGMENTS-1].physical = true;
	}

	@Override
	public void getOffset(float[] offsetXY, float[] rayPoint) {
		Vector2d massPos = pullMasses[PULLER_SEGMENTS-1].getPosition();
		offsetXY[0] = massPos.x - rayPoint[0];
		offsetXY[1] = massPos.y - rayPoint[1];
	}
}
