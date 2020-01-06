package com.komaxx.physics.forcer;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;

public class Elastic {
	private final Mass m1;
	private final Mass m2;

	public float naturalLength = 0;
	public float springForce = 10;
	
	public Elastic(Mass m1, Mass m2){
		this.m1 = m1;
		this.m2 = m2;
	}
	
	private static Vector2d tmpVector = new Vector2d();
	public void applyForce() {
		float distance = Vector2d.distance(m1.getPosition(), m2.getPosition());
		float effectiveDistance = distance-naturalLength;
		
		if (effectiveDistance > 0){
			Vector2d.aToB(tmpVector, m1.getPosition(), m2.getPosition()).normalize();
			
			tmpVector.times(-1*effectiveDistance);
			tmpVector.times(springForce);
			
			m1.addForce(tmpVector);
			m2.addForce(tmpVector.times(-1));
		}
	}

}
