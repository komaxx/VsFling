package com.komaxx.vsfling.train;

import java.util.ArrayList;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.math.GlTrapezoid;
import com.komaxx.komaxx_gl.math.Vector;
import com.komaxx.komaxx_gl.scenegraph.ADelayedGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.util.AnimatedValue;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.komaxx_gl.util.RenderUtil;
import com.komaxx.komaxx_gl.util.StepInterpolator;
import com.komaxx.vsfling.objects.ButtTextureProviderNode;

/**
 * Creates random butts placed somewhere on the screen.
 * Organizes them to appear at some point and vanish after a given time.
 *  
 * @author Matthias Schicker
 */
public class ButtChallengeNode extends Node {
	private static final int MAX_CONCURRENT_BUTTS = 3;
	
	private static final int MIN_NEXTBUTT_DELAY_MS = 200;
	private static final int MAX_NEXTBUTT_DELAY_MS = 600;

	private static final int MIN_NEXTSPECIAL_DELAY_MS = 5000;
	private static final int MAX_NEXTSPECIAL_DELAY_MS = 8000;
	
	private static final int MIN_BUTTREMOVAL_DELAY_MS = 3000;
	private static final int MAX_BUTTREMOVAL_DELAY_MS = 5000;
	
	private static final int HIT_REMOVAL_DELAY_MS = 300;
	
	private static final float BUTT_START_SCORE_MULTIPLIER = 500;
	private static final float BUTT_STOP_SCORE_MULTIPLIER = 100;
	private static final float SPECIAL_BUTT_SCORE_MULTIPLIER = 1000;
	
	/**
	 * how long a special butt takes to cross the screen
	 */
	private static final long SPECIAL_BUTT_DURATION_MS = 3000;
	private static final long SPECIAL_BUTT_DURATION_NS = SPECIAL_BUTT_DURATION_MS * 1000000L;
	
	private ChallengeControlNode challengeControlNode;
	private final TrainView1 physicsView;
	private ButtTextureProviderNode buttTextureProvider;
	
	private GlRect buttArea = new GlRect(450, -20, 950, -580);
	
	private ArrayList<ChallengeButtNode> activeButts = new ArrayList<ChallengeButtNode>();
	private ChallengeButtNode activeSpecialButt;
	private int gameId = 1;

	
	public ButtChallengeNode(TrainView1 physicsView){
		this.physicsView = physicsView;
		draws = true;
		transforms = false;
		handlesInteraction = false;
		
		depthTest = DONT_CARE;
		blending = DONT_CARE;
	}
	
	/**
	 * MUST be called before using the node for the first time!
	 */
	public void setChallengeControlNode(ChallengeControlNode ccn){
		this.challengeControlNode = ccn;
	}
	
	@Override
	public void onAttached() {
		if (buttTextureProvider == null){
			buttTextureProvider = new ButtTextureProviderNode(physicsView.getResources());
			sceneGraph.addNode(this, buttTextureProvider);
		}
	}
	
	public void start(){
		queueInGlThread(new AddButtJob(this, 50, gameId));
		for (int i = 0; i < MAX_CONCURRENT_BUTTS; i++){
			scheduleNextButt(gameId);
		}
		
		int specialDelay = (int) RenderUtil.getRandom(MIN_NEXTSPECIAL_DELAY_MS, MAX_NEXTSPECIAL_DELAY_MS);
		queueInGlThread(new SpecialButtJob(this, specialDelay, gameId));
	}
	
	public void stop(){
		abortAllAddJobs();
	}
	
	private void abortAllAddJobs() {
		gameId++;
	}

	public void clear(){
		for (int i = activeButts.size()-1; i>= 0; --i){
			removeButt(activeButts.get(i), -1);
		}
	}
	
	private void removeButt(ChallengeButtNode butt, int buttGameId) {
		butt.removeYourself();
		activeButts.remove(butt);
		
		scheduleNextButt(buttGameId);
	}

	public ChallengeButtNode addRandomButt(RenderContext rc, int ageMs) {
		if (sceneGraph == null) return null;
		
		float[][] bounds = findButtSpot();
		
		ChallengeButtNode nuButtNode = new ChallengeButtNode(
				physicsView, challengeControlNode, buttTextureProvider, bounds);
		nuButtNode.setConstraintStrengths(0.5f, 0.25f);
		nuButtNode.setScoreDecay(
				BUTT_START_SCORE_MULTIPLIER,
				BUTT_STOP_SCORE_MULTIPLIER,
				ageMs);
		nuButtNode.zLevel = TrainView1.Z_LEVEL_BALL;
		sceneGraph.addNode(this, nuButtNode);
		
		activeButts.add(nuButtNode);
		
		return nuButtNode;
	}
	
	public void addSpecialButt(RenderContext rc) {
		if (sceneGraph == null) return;
		
		if (activeSpecialButt != null){
			KoLog.e(this, "added special butt although previous still present");
		}
		
		float specialWidth = 50;
		float specialHeight = 100;
		
		float x = 920;
		float[][] bounds = new float[][]{
			new float[]{x, specialHeight},						// ll
			new float[]{x, 0},									// lr
			new float[]{x+specialWidth, 0},						// ur
			new float[]{x+specialWidth, specialHeight},			// ul
			
		};
		
		activeSpecialButt = new ChallengeButtNode(physicsView, 
				challengeControlNode, buttTextureProvider, bounds);
		activeSpecialButt.setScoreDecay(
				SPECIAL_BUTT_SCORE_MULTIPLIER, 
				SPECIAL_BUTT_SCORE_MULTIPLIER, 1);
		activeSpecialButt.setConstraintStrengths(0.5f, 0.25f);
		activeSpecialButt.zLevel = TrainView1.Z_LEVEL_BALL;
		sceneGraph.addNode(this, activeSpecialButt);
		
		long speed = SPECIAL_BUTT_DURATION_NS;
		AnimatedValue leftX = new AnimatedValue(
				StepInterpolator.QUATTRO_SINUS_STEPS, bounds[3][0], -30, speed);
		AnimatedValue leftY = new AnimatedValue(
				StepInterpolator.FLAT_STEPS, bounds[3][1], -600 - 2*specialHeight, speed);
		AnimatedValue rightX = new AnimatedValue(
				StepInterpolator.QUATTRO_SINUS_STEPS, bounds[2][0], -30, speed);
		AnimatedValue rightY = new AnimatedValue(
				StepInterpolator.FLAT_STEPS, bounds[2][1], -600 - 2*specialHeight, speed);
		activeSpecialButt.setAnimation(leftX, leftY, rightX, rightY);
	}

	private GlTrapezoid tmpBounds = new GlTrapezoid(2);
	private float[][] findButtSpot() {
		boolean found = false;
		while (!found){
			generateButtBounds();
			moveTmpBoundsInButtArea();
			if (!intersectsWithOtherButt()) found = true;
		}
		
		float[][] ret = new float[][]{		// TODO: Maybe clone the coords?
			tmpBounds.ur, tmpBounds.ul,
			tmpBounds.ll, tmpBounds.lr,
		};
		
		return ret;
	}

	private boolean intersectsWithOtherButt() {
		// the butt bounds in spe are stored in the Trapezoid tmpBounds
		tmpBounds.getBoundingBox(tmpBoundingBox);
		
		for (int i = 0; i < activeButts.size(); i++){
			if (activeButts.get(i).getBounds().intersects(tmpBoundingBox)){
				return true;
			}
		}
		
		return false;
	}

	private GlRect tmpBoundingBox = new GlRect();
	private void moveTmpBoundsInButtArea() {
		tmpBounds.getBoundingBox(tmpBoundingBox);
		tmpBoundingBox.sort();
		tmpBoundingBox.getMoveInsideDelta(tmp1, buttArea);
		if (tmp1[0] != 0 || tmp1[1] != 0) tmpBounds.move2(tmp1);
	}


	private float[] tmp1 = new float[2];
	private float[] tmpButtDirection = new float[2];
	private float[] tmpButtParallel = new float[2];
	private void generateButtBounds() {
		float buttHeight = RenderUtil.getRandom(100, 160);
		float buttWidth = buttHeight/2f;
		
		// random "left" butt-mass
		tmp1[0] = RenderUtil.getRandom(buttArea.left, buttArea.right);
		tmp1[1] = RenderUtil.getRandom(buttArea.bottom, buttArea.top);
		// random direction
		tmpButtDirection[0] = RenderUtil.getRandom(1, 0.1f);	// TODO: make the butt-direction a parameter (?)
		tmpButtDirection[1] = RenderUtil.getRandom(-1, 1);		// TODO: make the butt-direction a parameter (?)
		Vector.normalize2(tmpButtDirection);
		Vector.normal2(tmpButtParallel, tmpButtDirection);
		Vector.scalarMultiply2(tmpButtParallel, buttHeight);
		Vector.scalarMultiply2(tmpButtDirection, -buttWidth);
		
		Vector.set2(tmpBounds.ll, tmp1);
		Vector.aPlusB2(tmpBounds.lr, tmp1, tmpButtParallel);
		Vector.aPlusB2(tmpBounds.ul, tmpBounds.ll, tmpButtDirection);
		Vector.aPlusB2(tmpBounds.ur, tmpBounds.lr, tmpButtDirection);
		tmpBounds.setDirty();
	}

	private class AddButtJob extends ADelayedGlRunnable {
		private int issuedGameId;

		public AddButtJob(Node queueNode, int delayMs, int gameId) {
			super(queueNode, delayMs);
			issuedGameId = gameId;
		}

		@Override
		protected void doRun(RenderContext rc) {
			if (gameId != issuedGameId){
				// this was of the last game. ignore.
				return;
			}
			
			if (activeButts.size() < MAX_CONCURRENT_BUTTS){
				int ageMs = MIN_BUTTREMOVAL_DELAY_MS + 
						(int)(Math.random()*(MAX_BUTTREMOVAL_DELAY_MS-MIN_BUTTREMOVAL_DELAY_MS));
				ChallengeButtNode nuButt = addRandomButt(rc, ageMs);
				if (nuButt != null){
					scheduleButtRemoval(nuButt, ageMs);
				}
			}
		}

		private void scheduleButtRemoval(ChallengeButtNode nuButt, int delayMs) {
			queueInGlThread(new AutomaticButtRemovalJob(ButtChallengeNode.this, delayMs, nuButt, issuedGameId));
		}
	}
	
	/**
	 * To be called after a butt was hit. Schedules removal of the butt after a short delay. 
	 */
	public void removeHitButt(ChallengeButtNode butt) {
		queueInGlThread(
				new HitButtRemovalJob(
						ButtChallengeNode.this, (int) HIT_REMOVAL_DELAY_MS, butt, gameId
						)
				);
	}
	
	private class SpecialButtJob extends ADelayedGlRunnable {
		private int issuedGameId;

		public SpecialButtJob(Node queueNode, int delayMs, int gameId) {
			super(queueNode, delayMs);
			issuedGameId = gameId;
		}

		@Override
		protected void doRun(RenderContext rc) {
			if (gameId != issuedGameId){
				// this was of the last game. ignore.
				return;
			}

			addSpecialButt(rc);
			scheduleNextSpecialButt();
			scheduleSpecialRemoval();
		}

		private void scheduleNextSpecialButt() {
			int specialDelay = (int) RenderUtil.getRandom(MIN_NEXTSPECIAL_DELAY_MS, MAX_NEXTSPECIAL_DELAY_MS);
			specialDelay += SPECIAL_BUTT_DURATION_MS;
			queueInGlThread(new SpecialButtJob(ButtChallengeNode.this, specialDelay, gameId));
		}

		private void scheduleSpecialRemoval() {
			queueInGlThread(
					new AutomaticButtRemovalJob(
							ButtChallengeNode.this, (int) SPECIAL_BUTT_DURATION_MS, activeSpecialButt,
							issuedGameId
							)
					);
		}
	}
	

	protected void scheduleNextButt(int buttGameId) {
		int delay = MIN_NEXTBUTT_DELAY_MS + (int)(Math.random()*(MAX_NEXTBUTT_DELAY_MS-MIN_NEXTBUTT_DELAY_MS));
		queueInGlThread(new AddButtJob(ButtChallengeNode.this, delay, buttGameId));
	}
	
	
	private class AutomaticButtRemovalJob extends ADelayedGlRunnable {
		private ChallengeButtNode toRemove;
		private int buttGameId;

		public AutomaticButtRemovalJob(
				Node queueNode, int delayMs, ChallengeButtNode toRemove, int buttGameId) {
			super(queueNode, delayMs);
			this.toRemove = toRemove;
			this.buttGameId = buttGameId;
		}

		@Override
		protected void doRun(RenderContext rc) {
			if (activeButts.contains(toRemove)){
				removeButt(toRemove, buttGameId);
			} else if (toRemove == activeSpecialButt){
				activeSpecialButt.removeYourself();
				activeSpecialButt = null;
			} else {
				KoLog.i(this, "Could not automatically remove butt, already removed");
			}
		}
	}
	
	/**
	 * Scheduled when a butt was hit.
	 */
	private class HitButtRemovalJob extends ADelayedGlRunnable {
		private ChallengeButtNode toRemove;
		private int buttGameId;

		public HitButtRemovalJob(
				Node queueNode, int delayMs, ChallengeButtNode toRemove, int buttGameId) {
			super(queueNode, delayMs);
			this.toRemove = toRemove;
			this.buttGameId = buttGameId;
		}

		@Override
		protected void doRun(RenderContext rc) {
			removeButt(toRemove, buttGameId);
		}
	}
}
