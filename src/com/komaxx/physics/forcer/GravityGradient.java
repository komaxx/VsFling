package com.komaxx.physics.forcer;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.collide.ICollidable;
import com.komaxx.vsfling.ForceMasks;

public class GravityGradient extends ACollideLeaf  {
	public Vector2d force = new Vector2d();
	private Vector2d tmpForce = new Vector2d();
	
	public GravityGradient(Vector2d force){
		this.force.set(force);
		collideBounds.set(-100000, 100000, 100000, -100000);
		
		exertMask = ForceMasks.GRAVITY_FORCE;
	}
	
	@Override
	public void recomputeBounds() {
		// no
	}

	@Override
	public void exertForce(ICollidable target) {
		Mass m = (Mass) target;

		tmpForce.set(force);
		tmpForce.times(m.mass);
		m.addForce(tmpForce);
	}

	@Override
	public void step(float dt) {
		// nothing
	}
}
