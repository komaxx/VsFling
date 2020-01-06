package com.komaxx.physics.constraints;

import android.annotation.SuppressLint;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.collide.ICollidable;

/**
 * Pushes everything inside of a certain radius away and out of
 * this perimeter.
 * 
 * @author Matthias Schicker
 */
public class MinDistanceConstraint extends ACollideLeaf {
	private Vector2d center = new Vector2d();
	private float radius = 30f;
	private float radiusSquared = radius*radius;

	// //////////////////////////////////////////////////////
	// temps
	private static Vector2d tmpVector = new Vector2d();


	public MinDistanceConstraint(Vector2d center, float radius) {
		this.center.set(center);
		this.radius = radius;
		this.radiusSquared = radius*radius;

		resetCollideBounds();
	}

	private void resetCollideBounds() {
		collideBounds.set(
				center.x - radius, center.y + radius, 
				center.x + radius, center.y - radius);
	}

	public void setRadius(float nuRadius){
		this.radius = nuRadius;
		this.radiusSquared = radius*radius;
		resetCollideBounds();
	}
	
	public void setPosition(Vector2d pos) {
		setPosition(pos.x, pos.y);
	}
	
	public void setPosition(float centerX, float centerY){
		center.set(centerX, centerY);
		resetCollideBounds();
	}
	
	@Override
	public void step(float dt) {
		// nothing to do
	}

	@Override
	public void recomputeBounds() {
		// nothing to do
	}

	@SuppressLint("FloatMath")
	@Override
	public void exertForce(ICollidable target) {
		Mass mass = (Mass) target;

		Vector2d.aToB(tmpVector, center, mass.getPosition());
		
		float distanceSquared = Vector2d.squareLength(tmpVector);
		// compute collision point
		
		if (distanceSquared < radiusSquared){
			float distance = (float) Math.sqrt(distanceSquared);
			tmpVector.times(1f/distance);		// normalize
			tmpVector.times(radius-distance);	// now set to constraint distance
			mass.move(tmpVector);
		}
	}
}
