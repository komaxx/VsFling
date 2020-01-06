package com.komaxx.vsfling.play_select;

import java.nio.ShortBuffer;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.primitives.ColoredVertex;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.IGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.scenegraph.interaction.DragInteractionInterpreter;
import com.komaxx.komaxx_gl.scenegraph.interaction.DragInteractionInterpreter.IDragElement;
import com.komaxx.komaxx_gl.scenegraph.interaction.InteractionContext;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.komaxx_gl.util.ObjectsStore;
import com.komaxx.komaxx_gl.util.RenderUtil;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.constraints.MassesDistanceConstraint;
import com.komaxx.physics.constraints.PullConstraint;
import com.komaxx.physics.constraints.RipConstraint;
import com.komaxx.physics.constraints.RipConstraint.IRippedHandler;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.ButtNode;

public class StickSpiderNode extends Node implements IDragElement, IRippedHandler {
	private static final int LEGS = 6;
	
	private static final int KNEE_DISTANCE = 50;
	
	private static final float bodyWidth = 26;
	private static final float legWidth = 6;
	private static final float pullStringWidth = 5;
	
	private StickSpiderButt stickButt;
	
	// ////////////////////////////////////////////////////////////
	// rendering
	private ShortBuffer indexBuffer;
	private BoundFreeQuad body;
	private BoundFreeQuad puller;
	private BoundFreeQuad pullerLine;
	private BoundFreeQuad[] thighs;
	private BoundFreeQuad[] calves;

	// ////////////////////////////////////////////////////////////
	// physics
	private Mass pullerMass;
	private Mass bodyTailMass;
	private Mass bodyCenterMass;
	private Mass[] kneeMasses;
	private Mass[] footMasses;
	
	private PullConstraint pullerConstraint;
	private MassesDistanceConstraint[] legDistanceConstraints;
	
	// ////////////////////////////////////////////////////////////
	// interaction
	private DragInteractionInterpreter dragInteractionInterpreter = 
		new DragInteractionInterpreter(this);

	private GlRect movementBounds = new GlRect();
	private Vector2d initialPullerPosition = new Vector2d();
	private int rippedConstraints = 0;
	
	// ////////////////////////////////////////////////////////////
	// tmps
	private static Vector2d tmpNormal1 = new Vector2d();
	private static Vector2d tmp1 = new Vector2d();
	private static Vector2d tmp2 = new Vector2d();

	
	private ACollidableGroup physicalSpider = new ACollidableGroup() {
	};

	
	public StickSpiderNode(IPhysicsView view, StickSpiderButt stickButt, float y) {
		this.stickButt = stickButt;
		draws = true;
		handlesInteraction = true;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		
		vbo = new Vbo(
				2*4 +		// body, puller
				4 +			// puller string
				LEGS*2*4		// legs
				, ColoredVertex.STRIDE_BYTES);
		indexBuffer = ColoredVertex.allocateIndices(
				2*TexturedQuad.INDICES_COUNT +									// body, puller
				TexturedQuad.INDICES_COUNT +									// puller string
				LEGS*2 *TexturedQuad.INDICES_COUNT);							// legs
		
		createPhysicObjects(y);
		createRenderObjects();
		
		view.getPhysics().addCollidable(physicalSpider);
		
		queueOnceInGlThread(bouncyJob);
	}
	
	private IGlRunnable bouncyJob = new IGlRunnable() {
		private int period = 200 + (int)(Math.random()*200.0);
		@Override
		public void run(RenderContext rc) {
			float v = RenderUtil.sinStep(rc.frameNanoTime, period, 0.3f, 0.6f);
			for(int i = 0; i < LEGS; i++){
				legDistanceConstraints[i].strength = v;
			}
			queueOnceInGlThread(this);
		}
		
		@Override
		public void abort() { /* don't care */ }
	};

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
			R.drawable.puller,
			R.drawable.puller,				// TODO replace with body texture
			R.drawable.spider_thigh,
			R.drawable.spider_thigh,		// TODO replace with calve
			R.drawable.pull_string,			// TODO replace with puller_string
		}, t, 1);

		RectF[] uvRects = new RectF[uvRectsPx.length];
		for (int i = 0; i < uvRects.length; i++){
			uvRects[i] = t.getUvCoords(new RectF(), uvRectsPx[i]);
		}
		
		puller.setTexCoordsUv(uvRects[0]);
		body.setTexCoordsUv(uvRects[1]);
		pullerLine.setTexCoordsUv(uvRects[4]);

		for (int i = 0; i < LEGS; i++){
			thighs[i].setTexCoordsUv(uvRects[2]);
			calves[i].setTexCoordsUv(uvRects[3]);
		}
	}
	
	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		dragInteractionInterpreter.onInteraction(interactionContext);
		
		stickButt.setPulledDistance(
				Vector2d.distance(pullerMass.getPosition(), initialPullerPosition));
		
		return false;
	}
	
	private void createPhysicObjects(float y) {
		createMasses(y);
		createConstraints();
	}

	@SuppressLint("FloatMath") 
	private void createMasses(float y) {
		initialPullerPosition.set(230, y);
		
		pullerMass = new Mass();
		pullerMass.setPosition(initialPullerPosition);
		pullerMass.physical = false;
		physicalSpider.addChild(pullerMass);
		
		float tailCenterX = initialPullerPosition.x + 150;
		bodyTailMass = new Mass();
		bodyTailMass.setPosition(tailCenterX, y);
		bodyTailMass.setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
		physicalSpider.addChild(bodyTailMass);

		float bodyCenterX = tailCenterX + 40;
		bodyCenterMass = new Mass();
		bodyCenterMass.setPosition(bodyCenterX, y);
		bodyCenterMass.setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
		physicalSpider.addChild(bodyCenterMass);

		kneeMasses = new Mass[LEGS];
		// prepare
		for (int i = 0; i < LEGS; i++){
			kneeMasses[i] = new Mass();
			kneeMasses[i].setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
			kneeMasses[i].mass = 3;
			kneeMasses[i].friction *= 3;
			physicalSpider.addChild(kneeMasses[i]);
		}
		// position
		float angle = (float) Math.toRadians(-20);
		for (int i = 0; i < LEGS/2; i++){
			float xOffset = (float) Math.sin(angle) * KNEE_DISTANCE;
			float yOffset = (float) Math.cos(angle) * KNEE_DISTANCE;
			
			kneeMasses[i*2    ].setPosition(tailCenterX + xOffset, y + yOffset);
			kneeMasses[i*2 + 1].setPosition(tailCenterX + xOffset, y - yOffset);
			
			angle += Math.toRadians(220f / LEGS);
		}
		
		footMasses = new Mass[LEGS];
		for (int i = 0; i < LEGS; i++){
			footMasses[i] = new Mass();
			footMasses[i].setAcceptForceMask(ForceMasks.GRAVITY_FORCE);
			footMasses[i].mass = 0.5f;
			footMasses[i].friction *= 3;
			physicalSpider.addChild(footMasses[i]);
		}
		// footMasses will be positioned during the "stick" call
		
		// set the valid movement area
		movementBounds.set(
				initialPullerPosition.x - 130, y + 20, 
				initialPullerPosition.x + 20, y - 20);
	}
	
	private void createConstraints() {
		pullerConstraint = new PullConstraint(pullerMass, bodyTailMass);
		pullerConstraint.strength = 1;
		physicalSpider.addSteppable(pullerConstraint);
		
		MassesDistanceConstraint bodyConstraint = 
				new MassesDistanceConstraint(bodyTailMass, bodyCenterMass);
		bodyConstraint.strength = 1;
		physicalSpider.addSteppable(bodyConstraint);
		
		legDistanceConstraints = new MassesDistanceConstraint[LEGS];
		for (int i = 0; i < LEGS; i++){
			legDistanceConstraints[i]
					= new MassesDistanceConstraint(bodyTailMass, kneeMasses[i]);
			legDistanceConstraints[i].strength = 0.5f;
			physicalSpider.addSteppable(legDistanceConstraints[i]);

			MassesDistanceConstraint thighConstraint = new MassesDistanceConstraint(bodyCenterMass, kneeMasses[i]);
			thighConstraint.strength = 0.5f;
			physicalSpider.addSteppable(thighConstraint);
		}
		
		// constraints to keep the knees apart
		for (int i = 0; i < LEGS/2; i++){
			MassesDistanceConstraint distaneConstraint
					= new MassesDistanceConstraint(kneeMasses[i*2+1], kneeMasses[i*2]);
			distaneConstraint.strength = 0.05f;
			physicalSpider.addSteppable(distaneConstraint);
		}
		
		// add some pre-tension for effect
		pullerConstraint.naturalLength *= 0.9f;
	}


	public void stickToButt(ButtNode butt) {
		Mass[] buttMasses = butt.getMasses();
		
		int outOffset = 1;
		
		for (int i = 0; i < LEGS/2; i++){			
			Mass closestMass = buttMasses[outOffset + i];
			footMasses[i*2].setPosition(closestMass.getPosition().x-4, closestMass.getPosition().y);
			addRipConstraint(closestMass, footMasses[i*2]);

			closestMass = buttMasses[buttMasses.length-1 - outOffset - i];
			footMasses[i*2+1].setPosition(closestMass.getPosition().x-4, closestMass.getPosition().y);			
			addRipConstraint(closestMass, footMasses[i*2+1]);
		}
		
		for (int i = 0; i < LEGS; i++){
			MassesDistanceConstraint c = new MassesDistanceConstraint(kneeMasses[i], footMasses[i]);
			c.strength = 0.6f;
			physicalSpider.addSteppable(c);
		}
		
		// constraints to keep the feet apart
		for (int i = 0; i < LEGS/2; i++){
			MassesDistanceConstraint distaneConstraint
					= new MassesDistanceConstraint(footMasses[i*2+1], footMasses[i*2]);
			distaneConstraint.strength = 0.05f;
			physicalSpider.addSteppable(distaneConstraint);
		}
	}

	private void addRipConstraint(Mass m1, Mass m2) {
		RipConstraint c = new RipConstraint(m1, m2);
		c.strength = 0.95f;
		c.setMaxTension(3);
		physicalSpider.addSteppable(c);
		
		c.setRippedHandler(this);
	}


	// ////////////////////////////////////////////////////
	// interaction
	
	@Override
	public boolean inBounds(float[] xy) {
		return puller.contains(xy);
	}

	@Override
	public void down(InteractionContext ic) {}

	@Override
	public void cancel(InteractionContext ic) {}

	@Override
	public void drag(InteractionContext ic, float[] nuPos) {
		movementBounds.clampInside(nuPos);
		pullerMass.setPosition(nuPos[0], nuPos[1]);
	}

	@Override
	public void getOffset(float[] offsetXY, float[] rayPoint) {
		offsetXY[0] = pullerMass.getPosition().x - rayPoint[0];
		offsetXY[1] = pullerMass.getPosition().y - rayPoint[1];
	}
	
	@Override
	public void click(InteractionContext ic) {}
	
	// ////////////////////////////////////////////////////
	// rendering
	private void createRenderObjects() {
		puller = new BoundFreeQuad();
		puller.bindToVbo(vbo);
		
		body = new BoundFreeQuad();
		body.bindToVbo(vbo);
		
		pullerLine = new BoundFreeQuad();
		pullerLine.bindToVbo(vbo);
		
		thighs = new BoundFreeQuad[LEGS];
		calves = new BoundFreeQuad[LEGS];
		for (int i = 0; i < LEGS; i++){
			thighs[i] = new BoundFreeQuad();
			calves[i] = new BoundFreeQuad();
			
			thighs[i].bindToVbo(vbo);
			calves[i].bindToVbo(vbo);
		}
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		updatePositions();
		
		indexBuffer.position(0);
		int indicesCount = 0;
		indicesCount += pullerLine.render(renderContext, indexBuffer);
		for (int i = 0; i < LEGS; i++){
			indicesCount += thighs[i].render(renderContext, indexBuffer);
			indicesCount += calves[i].render(renderContext, indexBuffer);
		}
		indicesCount += puller.render(renderContext, indexBuffer);
		indicesCount += body.render(renderContext, indexBuffer);
		
		Vertex.renderTexturedTriangles(renderContext.currentRenderProgram, 0, indicesCount, indexBuffer);
		
		return true;
	}

	private void updatePositions() {
		updatePullerPosition();
		updateBodyPosition();
		updateLegPositions();
	}

	private void updateLegPositions() {
		position(pullerLine, pullerMass, bodyTailMass, pullStringWidth);
		
		for (int i = 0; i < LEGS; i++){
			position(thighs[i], bodyCenterMass, kneeMasses[i], legWidth);
		}

		// the calves are trickier: We need to prolong them to actually touch the butt!
		for (int i = 0; i < LEGS; i++){
			Vector2d.aToB(tmp1, kneeMasses[i].getPosition(), footMasses[i].getPosition());
			tmp1.times(1.1f);
			tmp1.add(kneeMasses[i].getPosition());
			
			calves[i].positionAlong2(
					kneeMasses[i].getPosition().toArray(ObjectsStore.tmpVector1), 
					tmp1.toArray(ObjectsStore.tmpVector2),
					legWidth);
		}
	}

	private static void position(BoundFreeQuad q, Mass m1, Mass m2, float width) {
		q.positionAlong2(
				m1.getPosition().toArray(ObjectsStore.tmpVector1), 
				m2.getPosition().toArray(ObjectsStore.tmpVector2),
				width);
	}

	private static Vector2d facePoint = new Vector2d();
	private static Vector2d tailPoint = new Vector2d();
	private void updateBodyPosition() {
		float tailOffset = -20;
		float faceOffset = 10;

		Vector2d.aToB(tmp1, bodyTailMass.getPosition(), bodyCenterMass.getPosition());
		tmp1.normalize();
		tmp1.times(faceOffset);
		
		facePoint.set(bodyCenterMass.getPosition());
		facePoint.add(tmp1);
		
		tmp1.times(tailOffset / faceOffset);
		tailPoint.set(bodyTailMass.getPosition());
		tailPoint.add(tmp1);
		
		body.positionAlong2(
				facePoint.toArray(ObjectsStore.tmpVector1), 
				tailPoint.toArray(ObjectsStore.tmpVector2), bodyWidth);
	}

	private void updatePullerPosition() {
		float pullerDiagonalHalf = 46;
		
		Vector2d.aToB(tmp1, pullerMass.getPosition(), bodyTailMass.getPosition());
		tmp1.normalize();
		Vector2d.normal(tmpNormal1, tmp1);
		
		tmp1.add(tmpNormal1);
		tmp1.normalize();
		tmp1.times(pullerDiagonalHalf);
		
		// upper right
		tmp2.set(pullerMass.getPosition());
		puller.positionXY(BoundFreeQuad.VERTEX_UPPER_RIGHT, 
				tmp2.x + tmp1.x, tmp2.y + tmp1.y);
		// lower left
		tmp2.set(pullerMass.getPosition());
		puller.positionXY(BoundFreeQuad.VERTEX_LOWER_LEFT, 
				tmp2.x - tmp1.x, tmp2.y - tmp1.y);

		Vector2d.normal(tmpNormal1, tmp1);
		tmp1.set(tmpNormal1);
		// upper left
		tmp2.set(pullerMass.getPosition());
		puller.positionXY(BoundFreeQuad.VERTEX_UPPER_LEFT, 
				tmp2.x + tmp1.x, tmp2.y + tmp1.y);
		// lower right
		tmp2.set(pullerMass.getPosition());
		puller.positionXY(BoundFreeQuad.VERTEX_LOWER_RIGHT, 
				tmp2.x - tmp1.x, tmp2.y - tmp1.y);
	}

	@Override
	public void handleRipped(RipConstraint c) {
		rippedConstraints++;
		
		if (rippedConstraints >= LEGS){
			stickButt.ripped();
		}
	}
}
