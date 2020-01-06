package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeMesh;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.constraints.FlingerConstraint;
import com.komaxx.physics.constraints.IBumpTrigger;
import com.komaxx.physics.constraints.MassesDistanceConstraint;
import com.komaxx.physics.constraints.TriggerConstraint;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.title.TitleView;

public class ButtNode extends Node implements IBumpTrigger {
	public static final float DEFAULT_FIXED_CONSTRAINTS_STRENGTH = 0.1f;
	public static final float DEFAULT_SURFACE_CONSTRAINTS_STRENGTH = 0.4f;

	public static int BUTT_MASSES = 9;
	
	protected IPhysicsView tv;
	private ButtTextureProviderNode buttTextureProvider;
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private BoundFreeMesh buttMesh = new BoundFreeMesh(BUTT_MASSES + 2, BUTT_MASSES * 3);
	

	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////
	protected Mass[] buttMasses = new Mass[BUTT_MASSES];
	protected Mass leftTopMass;
	protected Mass rightTopMass;
	protected ButtMeasureConstraint[] measureConstraints = new ButtMeasureConstraint[3];
	protected MassesDistanceConstraint[] surfaceConstraints;
	protected MassesDistanceConstraint[] buttConstraints = new MassesDistanceConstraint[
        BUTT_MASSES-1+		// constraints between bottom masses
        2*BUTT_MASSES			// constraints from buttMass to central point
    ];

	// //////////////////////////////////////////////////
	// tmp stuff
	private Vector2d tmp1 = new Vector2d();

	
	protected ACollidableGroup physicalButt = new ACollidableGroup() {
	};

	
	public ButtNode(IPhysicsView tv, ButtTextureProviderNode textureProvider, float[][] bounds){
		this.tv = tv;
		this.buttTextureProvider = textureProvider;
		
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		clusterIndex = TitleView.CLUSTER_BUTT;
		
		createPhysicObjects(bounds);
		createRenderObjects(bounds);
		
		tv.getPhysics().addCollidable(physicalButt);
	}
	
	protected void createPhysicObjects(float[][] bounds) {
		Vector2d bottomLeft = new Vector2d(bounds[0]);
		Vector2d buttX = Vector2d.aToB(new Vector2d(), 
				new Vector2d(bounds[0]), new Vector2d(bounds[1]));
		Vector2d buttY = Vector2d.aToB(new Vector2d(), 
				new Vector2d(bounds[0]), new Vector2d(bounds[3]));
		
		Vector2d[] buttLine = buttTextureProvider.getButtLine();
		Vector2d tmpX = new Vector2d();
		Vector2d tmpY = new Vector2d();
		
		for (int i = 0; i < BUTT_MASSES; i++){
			tmpX.set(buttX);
			tmpX.times(buttLine[i].x);

			tmpY.set(buttY);
			tmpY.times(buttLine[i].y);
			
			tmpX.add(tmpY);
			tmpX.add(bottomLeft);

			
			buttMasses[i] = new Mass();
			buttMasses[i].friction *= -0.5f;
			buttMasses[i].mass = 2;
			buttMasses[i].setPosition(tmpX);
			
			physicalButt.addChild(buttMasses[i]);
		}
		
		// fixed masses
		leftTopMass = new Mass();
		leftTopMass.physical = false;
		leftTopMass.setPosition(bounds[3][0], bounds[3][1]);
		
		rightTopMass = new Mass();
		rightTopMass.physical = false;
		rightTopMass.setPosition(bounds[2][0], bounds[2][1]);
		
		// forces inside the butt
		int constraintIndex = 0;
		// butt-mass fixed masses
		for (int i = 0; i < BUTT_MASSES; i++){
			MassesDistanceConstraint lc = new MassesDistanceConstraint(leftTopMass, buttMasses[i]);
			lc.setTag("f");
			lc.strength = DEFAULT_FIXED_CONSTRAINTS_STRENGTH;
			buttConstraints[constraintIndex] = lc;
			physicalButt.addSteppable(lc);
			constraintIndex++;
			
			MassesDistanceConstraint rc = new MassesDistanceConstraint(rightTopMass, buttMasses[i]);
			rc.setTag("f");
			rc.strength = DEFAULT_FIXED_CONSTRAINTS_STRENGTH;
			buttConstraints[constraintIndex] = rc;
			physicalButt.addSteppable(rc);
			constraintIndex++;
		}
		// inter-butt-mass constraints
		for (int i = 0; i < BUTT_MASSES-1; i++){
			MassesDistanceConstraint dc = new MassesDistanceConstraint(buttMasses[i], buttMasses[i + 1]);
			dc.strength = DEFAULT_SURFACE_CONSTRAINTS_STRENGTH;
			buttConstraints[constraintIndex] = dc;
			physicalButt.addSteppable(dc);
			
			constraintIndex++;
		}
		
		// central constraints and measure things!
		// compute central point
		measureConstraints[0] = new ButtMeasureConstraint(0, buttMasses[BUTT_MASSES/2]);
		measureConstraints[1] = new ButtMeasureConstraint(1, buttMasses[BUTT_MASSES/3]);
		measureConstraints[2] = new ButtMeasureConstraint(2, buttMasses[BUTT_MASSES*2/3]);
		for (int i = 0; i < 3; i++){
			measureConstraints[i].strength = DEFAULT_FIXED_CONSTRAINTS_STRENGTH/2;
			physicalButt.addSteppable(measureConstraints[i]);
		}
		
		createBallPushConstraints(leftTopMass, rightTopMass, buttY);
		
		TriggerConstraint tc = new TriggerConstraint(
				new Vector2d(bounds[0][0], bounds[0][1]), 
				new Vector2d(bounds[1][0], bounds[1][1]), 
				new Vector2d(500f, -1000f));
		tc.setTrigger(this);
		tc.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		physicalButt.addChild(tc);
	}
	
	public void setConstraintStrengths(float surface, float internal){
		for (MassesDistanceConstraint c : buttConstraints){
			if (c.getTag() == null) c.strength = surface;
			else c.strength = internal;
		}
	}
	
	private void createBallPushConstraints(Mass leftTopMass, Mass rightTopMass, Vector2d buttY) {
		Vector2d forceTarget = new Vector2d(buttMasses[BUTT_MASSES/2].getPosition());
		buttY.times(-1000);
		forceTarget.add(buttY);
		
		for (int i = 0; i < BUTT_MASSES-1; i++){
			FlingerConstraint fc = new FlingerConstraint(buttMasses[i], buttMasses[i + 1], forceTarget);
			fc.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
			fc.strength = 1f;
			
			physicalButt.addChild(fc);
		}
		
		//*
		// left wall
		Vector2d.aToB(tmp1, rightTopMass.getPosition(), leftTopMass.getPosition());
		tmp1.add(leftTopMass.getPosition());
		FlingerConstraint left = new FlingerConstraint(leftTopMass, buttMasses[0], tmp1);
		left.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		left.strength = 0.4f;
		physicalButt.addChild(left);
		
		// right wall
		Vector2d.aToB(tmp1, leftTopMass.getPosition(), rightTopMass.getPosition());
		tmp1.add(rightTopMass.getPosition());
		FlingerConstraint right = new FlingerConstraint(
				rightTopMass, buttMasses[BUTT_MASSES-1], tmp1);
		right.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		right.strength = 0.4f;
		physicalButt.addChild(right);
		
		/*
		// top wall
		Vector2d.aToB(tmp1, buttMasses[0].getPosition(), leftTopMass.getPosition());
		tmp1.add(leftTopMass.getPosition());
		BumperConstraint top = new BumperConstraint(
				leftTopMass.getPosition(), rightTopMass.getPosition(), tmp1);
		top.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		top.strength = 0.4f;
		physicalButt.addChild(top);
		//*/
	}

	protected void createRenderObjects(float[][] bounds) {
		vbo = new Vbo(
				buttMesh.getMaxVertexCount(),
				TexturedVertex.STRIDE_BYTES
		);
		indexBuffer = Vertex.allocateIndices(buttMesh.getMaxIndexCount());

		
		int i = 0;
		Vector2d tmpPos;
		for (i = 0; i < BUTT_MASSES; i++){
			tmpPos = buttMasses[i].getPosition();
			buttMesh.positionXY(i, tmpPos.x, tmpPos.y);
		}
		
		buttMesh.positionXY(i, bounds[2][0], bounds[2][1]);
		i++;
		buttMesh.positionXY(i, bounds[3][0], bounds[3][1]);
		
		// build indices
		short[] indices = new short[buttMesh.getMaxIndexCount()];
		int indicesIndex = 0;
		int half = BUTT_MASSES/2;
		// from left fixed edge
		for (i = 0; i < half; i++){
			indices[indicesIndex] = (short)i;
			indices[indicesIndex + 1] = (short)(i + 1);
			indices[indicesIndex + 2] = (short)(BUTT_MASSES + 1);
			
			indicesIndex +=3;
		}
		// from right fixed edge
		for (i = 0; i < half; i++){
			indices[indicesIndex] = (short)(half + i);
			indices[indicesIndex + 1] = (short)(half + i + 1);
			indices[indicesIndex + 2] = (short)(BUTT_MASSES);
			
			indicesIndex +=3;
		}
		// center triangle
		indices[indicesIndex] = (short)(BUTT_MASSES + 1);
		indices[indicesIndex + 1] = (short)(half);
		indices[indicesIndex + 2] = (short)(BUTT_MASSES);
		
		
		buttMesh.setIndices(indices);
		
		buttMesh.bindToVbo(vbo);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		textureHandle = buttTextureProvider.getTextureHandle();
		
		RectF uvCoords = buttTextureProvider.getButtTextureUvCoords();
		Vector2d[] buttLine = buttTextureProvider.getButtLine();
		
		int i = 0;
		Vector2d tmpUv = new Vector2d();
		for (i = 0; i < BUTT_MASSES; i++){
			tmpUv.set(buttLine[i]);
			tmpUv.x *= uvCoords.right-uvCoords.left;
			tmpUv.y = (1f-buttLine[i].y) * (uvCoords.bottom-uvCoords.top);
			
			buttMesh.setTexCoordsUv(i, tmpUv.x, tmpUv.y);
		}

		// top right
		buttMesh.setTexCoordsUv(i, uvCoords.right, uvCoords.top);
		i++;
		buttMesh.setTexCoordsUv(i, uvCoords.left, uvCoords.top);
	}

	@Override
	public boolean onRender(RenderContext rc) {
		updatePositions();
		
		int indexCount = 0;
		indexCount += buttMesh.render(rc, indexBuffer);
		
		Vertex.renderTexturedTriangles(rc.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}

	private void updatePositions() {
		Vector2d tmpPos;
		int i = 0;
		for (; i < BUTT_MASSES; i++){
			tmpPos = buttMasses[i].getPosition();
			buttMesh.positionXY(i, tmpPos.x, tmpPos.y);
		}
		tmpPos = rightTopMass.getPosition();
		buttMesh.positionXY(i, tmpPos.x, tmpPos.y);
		i++;
		tmpPos = leftTopMass.getPosition();
		buttMesh.positionXY(i, tmpPos.x, tmpPos.y);
	}

	@Override
	public float getMinTriggerDistance() {
		return 1;
	}

	@Override
	public void trigger(ACollideLeaf l, Mass m) {
		if (tv instanceof TitleView){
			((TitleView)tv).buttActivated(this);
		}
	}

	public Mass[] getMasses() {
		return buttMasses;
	}
}
