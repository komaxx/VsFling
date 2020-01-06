package com.komaxx.physics;

import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.collide.ICollidable;

public class PhysicsSim {
	public final static int STEP_MS = 1000/180;
	public final static float STEP_DT = (float)STEP_MS / 1000f;
	
	public final static int FALLBACK_THRESHOLD = 20;
	public final static int FALLBACK_STEPS = 5;
	
	protected CollideTreeRoot collideTreeRoot = new CollideTreeRoot();
	
	private boolean paused = false;
	
	// caches, tmps
	private long lastFrameTime = -1;

	public void doPhysics() {
		if (paused) return;
		
//		long startTime = System.currentTimeMillis();

		if (lastFrameTime < 0) lastFrameTime = System.currentTimeMillis();
		
		long nowTime = System.currentTimeMillis();
		int frameTime = (int) (nowTime - lastFrameTime);
		if (frameTime < 1) frameTime = 1;
		
		int steps = frameTime/STEP_MS;
		
		if (steps > FALLBACK_THRESHOLD){
			KoLog.w(this, "Uhoh, more than "+FALLBACK_THRESHOLD+" steps per frame -> fast-forward");
			steps = FALLBACK_STEPS;
			lastFrameTime = nowTime - FALLBACK_STEPS*STEP_MS;
		}

		float dt = STEP_MS / 1000f;

		for (int i = 0; i < steps; i++){
			collideTreeRoot.collide();
			collideTreeRoot.step(dt);
			lastFrameTime += STEP_MS;
		}

//		KoLog.i(this, steps + " steps, " + (System.currentTimeMillis() - startTime) + " ms");
	}

	public void addCollidable(ICollidable physicalFlinger) {
		collideTreeRoot.addChild(physicalFlinger);
	}
	

	public void removeCollidable(ACollidableGroup toRemove) {
		ACollidableGroup parent = toRemove.getParent();
		if (parent != null) parent.removeChild(toRemove);
	}

	public CollideTreeRoot getCollideTreeRoot() {
		return collideTreeRoot;
	}
	
	public void pause(){
		paused = true;
	}
	
	public void resume(){
		paused = false;
	}
	
	public boolean isPaused() {
		return paused;
	}
}
