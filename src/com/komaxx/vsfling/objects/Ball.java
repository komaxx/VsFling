package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.FloatMath;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeMesh;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.collide.ACollideSteppable;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

public class Ball extends Node {
	public static final int RIM_MASSES = 12;
	
	private static float ballRadius = 30f;
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private BoundFreeMesh ballMesh = new BoundFreeMesh(RIM_MASSES, (RIM_MASSES-2)*3);
	
	
	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////
	private Mass[] masses = new Mass[RIM_MASSES];
	
	
	private ACollidableGroup physicalBall = new ACollidableGroup() {
	};

	// //////////////////////////////////////////////////
	// temp stuff
	// //////////////////////////////////////////////////

	
	public Ball(IPhysicsView tv, Vector2d pos){
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		
		createPhysicObjects(pos);
		createRenderObjects();
		
		tv.getPhysics().addCollidable(physicalBall);
	}
	
	private void createPhysicObjects(Vector2d pos) {
		buildMasses();
		buildConstraints();
		
		placeMasses(pos); 
	}
	
	private void buildMasses() {
		for (int i = 0; i < RIM_MASSES; i++){
			Mass m = new Mass();
			m.mass = 1.5f;
			m.friction *= 0.25f;
			m.setAcceptForceMask(
					ForceMasks.GRAVITY_FORCE | ForceMasks.BALL_PUSH_FORCE
					);
			masses[i] = m;
			physicalBall.addChild(m);
		}
		
		masses[masses.length-1].mass *= 2;
		masses[masses.length-2].mass *= 2;
	}
	
	public void resetPosition(Vector2d ballPos) {
		placeMasses(ballPos);
	}
	
	private void placeMasses(Vector2d pos) {
		float x = pos.x;
		float y = pos.y;
		
		for (int i = 0; i < RIM_MASSES; i++){
			float angle = (float) Math.toRadians(i* 360f/RIM_MASSES);
			
			masses[i].setPosition(
					x + FloatMath.cos(angle)*ballRadius, 
					y + FloatMath.sin(angle)*ballRadius);
		}

		// we need to reset the natural length of the constraints now!
		ArrayList<ACollideSteppable> steppables = physicalBall.getSteppables();
		int l = steppables.size();
		for (int i = 0; i < l; i++){
			if (steppables.get(i) instanceof BallConstraint){
				((BallConstraint)steppables.get(i)).resetNaturalLength();
			}
		}
	}

	private int[] steps = new int[]{ 1, 2, 3, 5 };
	private void buildConstraints() {
		for (int i = 0; i < RIM_MASSES; i++){
			for (int j = 0; j < steps.length; j++){
				BallConstraint s = null;

				if (i%2==0){
					s = new BallConstraint(
							masses[i], 
							masses[(i+steps[j]) % RIM_MASSES]);
				} else {
					s = new BallConstraint(
							masses[(i+steps[j]) % RIM_MASSES],
							masses[i]
					);
				}
				s.strength = 0.09f;
//				s.strength = 0.04f;
				physicalBall.addSteppable(s);
			}
		}
	}

	
	private void createRenderObjects() {
		vbo = new Vbo(
				ballMesh.getMaxVertexCount(),
				TexturedVertex.STRIDE_BYTES
		);
		indexBuffer = Vertex.allocateIndices(
				ballMesh.getMaxIndexCount()
		);

		// create the indices for the flingerMesh
		short[] ballIndices = new short[ballMesh.getMaxIndexCount()];
		int indexIndex = 0;
		for (int i = 1; i < RIM_MASSES-1; i++){
			ballIndices[indexIndex+0] = (short)i;
			ballIndices[indexIndex+1] = (short)(0);
			ballIndices[indexIndex+2] = (short)(i + 1);

			indexIndex += 3;
		}
		ballMesh.setIndices(ballIndices);
		
		// set texture coords
		for (int i = 0; i < RIM_MASSES; i++){
			float angle = (float) Math.toRadians(i* 360f/RIM_MASSES);

			ballMesh.setAlpha(i, 1);
			
			ballMesh.setTexCoordsUv(i,
					0.5f + FloatMath.cos(angle)*0.5f, 
					0.5f + FloatMath.sin(angle)*0.5f);
		}
		
		ballMesh.bindToVbo(vbo);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		int textureSize = 128;
		
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.basicColor = Color.GREEN;
		tc.minHeight = textureSize;
		tc.minWidth = textureSize;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		Bitmap bmp = Bitmap.createBitmap(textureSize, textureSize, t.getBitmapConfig());
		bmp.eraseColor(Color.TRANSPARENT);
		Canvas c = new Canvas(bmp);

		Drawable drawable = renderContext.resources.getDrawable(R.drawable.ball);
		drawable.setBounds(0, 0, textureSize, textureSize);
		drawable.draw(c);
		
		t.update(bmp, 0, 0);
	}
	
	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
//		rePlace(renderContext);
	}
	
	@Override
	public boolean onRender(RenderContext rc) {
		updatePositions();
		
		int indexCount = 0;

		indexCount += ballMesh.render(rc, indexBuffer);
		
		Vertex.renderTexturedTriangles(rc.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}

	private void updatePositions() {
		for (int i = 0; i < RIM_MASSES; i++){
			Vector2d position = masses[i].getPosition();
			ballMesh.positionXY(i, position.x, position.y);
		}
	}
	
	/**
	 * A special asymmetrical constraint (pulls stronger than it pushed) 
	 * specifically for the ball.
	 * 
	 * @author Matthias Schicker
	 */
	public static class BallConstraint extends ACollideSteppable {
		public final Mass m1;
		public final Mass m2;

		public float naturalLength = 0;
		public float strength = 0.1f;

		public BallConstraint(Mass m1, Mass m2) {
			this.m1 = m1;
			this.m2 = m2;
			
			resetNaturalLength();
		}

		private static Vector2d tmpVector = new Vector2d();
		@Override
		public void step(float dt) {
			float distance = Vector2d.distance(m1.getPosition(), m2.getPosition());
			float diff = distance-naturalLength;

			if (diff > 0) diff = diff * 0.6f;
			
			Vector2d.aToB(tmpVector, m1.getPosition(), m2.getPosition());
			tmpVector.times((1/distance) * diff * 0.5f 
					* strength
					);

			m1.move(tmpVector);
			m2.move(tmpVector.times(-1));
		}
		
		/**
		 * sets the natural length of this constraint to the given distance
		 * between the masses.
		 */
		public void resetNaturalLength() {
			naturalLength = Vector2d.distance(m1.getPosition(), m2.getPosition());
		}
	}
}
