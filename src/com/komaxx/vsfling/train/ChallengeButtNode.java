package com.komaxx.vsfling.train;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.util.AnimatedValue;
import com.komaxx.komaxx_gl.util.InterpolatedValue;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.Vector2d;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.objects.ButtMeasureConstraint;
import com.komaxx.vsfling.objects.ButtMeasureConstraint.IButtMeasureHandler;
import com.komaxx.vsfling.objects.ButtNode;
import com.komaxx.vsfling.objects.ButtTextureProviderNode;

public class ChallengeButtNode extends ButtNode implements IButtMeasureHandler {
	private long attachTime = 0;
	
	private float maxScore = 1;
	
	private ChallengeControlNode challengeNode; 
	
	private GlRect bounds = new GlRect();
	
	/**
	 * In local (butt) coord system
	 */
	private Vector2d[] measureFixOffsets = new Vector2d[3];

	private AnimatedValue leftFixMovementX;
	private AnimatedValue leftFixMovementY;
	private AnimatedValue rightFixMovementX;
	private AnimatedValue rightFixMovementY;
	
	
	private InterpolatedValue scoreMultiplier = new InterpolatedValue(
			InterpolatedValue.AnimationType.LINEAR, 1);
	
	// //////////////////////////////////////////////////
	// temps
	private static Vector2d buttX = new Vector2d(); 
	private static Vector2d buttY = new Vector2d();

	
	public ChallengeButtNode(IPhysicsView tv,
			ChallengeControlNode challengeNode,
			ButtTextureProviderNode textureProvider, float[][] bounds) {
		super(tv, textureProvider, bounds);
		this.challengeNode = challengeNode;
		
		computeBounds(bounds);
		computeMeasureFixOffsets(bounds);
	}
	
	public void setScoreDecay(float startScore, float targetScore, int decayMs){
		scoreMultiplier.setDirect(startScore);
		scoreMultiplier.setDuration(InterpolatedValue.msToNs(decayMs));
		scoreMultiplier.set(targetScore);
	}

	private void computeBounds(float[][] setBounds) {
		bounds.set(setBounds[0][0], setBounds[0][1], setBounds[0][0], setBounds[0][1]);
		for (int i = 1; i < setBounds.length; i++){
			bounds.enlarge(setBounds[i]);
		}
	}

	private void computeMeasureFixOffsets(float[][] bounds) {
		Vector2d.aToB(buttX, new Vector2d(bounds[0]), new Vector2d(bounds[1]));
		Vector2d.aToB(buttY, new Vector2d(bounds[0]), new Vector2d(bounds[3]));
		
		buttX.normalize();
		buttY.normalize();
		
		Vector2d topLeft = new Vector2d(bounds[3]);		// top left corner
		Vector2d offset = new Vector2d();
		for (int i = 0; i < 3; i++){
			Vector2d.aToB(offset, topLeft, measureConstraints[i].v);
			
			float x = Vector2d.dotProduct(buttX, offset);
			float y = Vector2d.dotProduct(buttY, offset);
			
			measureFixOffsets[i] = new Vector2d(x, y);
		}
	}

	@Override
	public boolean onRender(RenderContext rc) {
		scoreMultiplier.get(rc.frameNanoTime);
		
		if (leftFixMovementX != null){
			executeAnimation(rc);
		}
		
		return super.onRender(rc);
	}
	
	private static Vector2d tmpPos = new Vector2d();
	private static Vector2d tmpX = new Vector2d();
	private static Vector2d tmpY = new Vector2d();
	private void executeAnimation(RenderContext rc) {
		leftTopMass.setPosition(
				leftFixMovementX.get(rc.frameNanoTime), 
				leftFixMovementY.get(rc.frameNanoTime));
		
		rightTopMass.setPosition(
				rightFixMovementX.get(rc.frameNanoTime), 
				rightFixMovementY.get(rc.frameNanoTime));
		
		// reposition fix points for measureConstraints
		Vector2d.aToB(buttX, leftTopMass.getPosition(), rightTopMass.getPosition());
		buttX.normalize();
		Vector2d.normal(buttY, buttX);
		
		for (int i = 0; i < 3; i++){
			tmpX.set(buttX);
			tmpX.times(measureFixOffsets[i].x);
			
			tmpY.set(buttY);
			tmpY.times(measureFixOffsets[i].y);
			
			tmpPos.set(leftTopMass.getPosition());
			tmpPos.add(tmpX);
			tmpPos.add(tmpY);
			
			measureConstraints[i].v.set(tmpPos);
		}
	}

	@Override
	public void onAttached() {
		super.onAttached();
		attachTime = System.currentTimeMillis();
	}
	
	public long getAgeMs(){
		return System.currentTimeMillis() - attachTime;
	}
	
	@Override
	protected void createPhysicObjects(float[][] bounds) {
		super.createPhysicObjects(bounds);
		
		for (int i = 0; i < measureConstraints.length; i++){
			measureConstraints[i].setHandler(this);
		}
	}
	
	@Override
	protected void createRenderObjects(float[][] bounds) {
		super.createRenderObjects(bounds);
	}

	/**
	 * GLThread only!
	 */
	public void removeYourself() {
		if (sceneGraph != null){
			sceneGraph.removeNode(this);
		}
		
		tv.getPhysics().removeCollidable(physicalButt);
	}

	@Override
	public void handleTension(ButtMeasureConstraint bms, float tension) {
		int score = computeScore(tension);
		if (score > maxScore){
			maxScore = score;
			KoLog.i(this, "Score : " + score + ": " + tension + " * " + scoreMultiplier.getLast());
			challengeNode.addScore(this, score);
		}
	}

	private int computeScore(float tension) {
		return (int)(tension * scoreMultiplier.getLast());
	}

	public void setAnimation(AnimatedValue leftX, AnimatedValue leftY,
			AnimatedValue rightX, AnimatedValue rightY) {
		leftFixMovementX = leftX;
		leftFixMovementY = leftY;
		rightFixMovementX = rightX;
		rightFixMovementY = rightY;
		
		leftFixMovementX.start();
		leftFixMovementY.start();
		rightFixMovementX.start();
		rightFixMovementY.start();
	}

	public GlRect getBounds() {
		return bounds;
	}
}
