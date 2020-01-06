package com.komaxx.physics.forcer;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;

public class Spring {
	public final Mass m1;
	public final Mass m2;

	public float naturalLength = 0;
	public float springForce = 10;

	
	public Spring(Mass m1, Mass m2){
		this.m1 = m1;
		this.m2 = m2;

		naturalLength = Vector2d.distance(m1.getPosition(), m2.getPosition());
	}

	private static Vector2d tmpVector = new Vector2d();
	public void applyForce() {
		float distance = Vector2d.distance(m1.getPosition(), m2.getPosition());
		float effectiveDistance = distance-naturalLength;
		if (distance == 0) return;
		
		Vector2d.aToB(tmpVector, m1.getPosition(), m2.getPosition());
		tmpVector.times(1/distance);		// normalizing using the already known length

		float force = -effectiveDistance * springForce ;
		
		tmpVector.times(force);

		m1.addForce(tmpVector);
		m2.addForce(tmpVector.times(-1));
	}
}
