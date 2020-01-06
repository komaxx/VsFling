package com.komaxx.physics.forcer;

import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.collide.ICollidable;
import com.komaxx.vsfling.ForceMasks;

public class Gravity extends ACollideLeaf  {
	public Vector2d force = new Vector2d();
	private Vector2d tmpForce = new Vector2d();
	
	public Gravity(Vector2d force){
		this(force, new GlRect(-100000, 100000, 100000, -100000));
	}
	
	public Gravity(Vector2d force2, GlRect bounds) {
		this.force.set(force2);
		collideBounds.set(bounds);
		
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
