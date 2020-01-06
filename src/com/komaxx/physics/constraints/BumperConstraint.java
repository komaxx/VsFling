package com.komaxx.physics.constraints;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.collide.ICollidable;

/**
 * Used for walls and all sorts of boundaries.
 * 
 * @author Matthias Schicker
 */
public class BumperConstraint extends ACollideLeaf {
	private Vector2d p1 = new Vector2d();
	private Vector2d p2 = new Vector2d();
	private Vector2d forceTargetPoint = new Vector2d();

	public final Vector2d normal = new Vector2d();
	private final Vector2d p1ToP2 = new Vector2d();
	private float p1toP2Distance;

	public float strength = 1.5f;
	public float friction = 0.3f;
	
	private IBumpTrigger trigger = null;

	// //////////////////////////////////////////////////////
	// temps
	private static Vector2d tmpVector = new Vector2d();


	public BumperConstraint(Vector2d v1, Vector2d v2, Vector2d forceTargetPoint) {
		this.p1.set(v1);
		this.p2.set(v2);
		this.forceTargetPoint.set(forceTargetPoint);

		recomputeAuxVectors();
		resetCollideBounds();
	}

	private void resetCollideBounds() {
		collideBounds.set(p1.x, p1.y, p2.x, p2.y);
		collideBounds.sort();
		
		tmpVector.set(normal);
		tmpVector.times(-50);
		tmpVector.add(p1);

		collideBounds.enlarge(tmpVector.x, tmpVector.y);
		
		tmpVector.set(normal);
		tmpVector.times(-50);
		tmpVector.add(p2);
		collideBounds.enlarge(tmpVector.x, tmpVector.y);
		
		collideBounds.sort();
	}

	public void setPosition(float p1X, float p1Y, float p2X, float p2Y){
		p1.set(p1X, p1Y);
		p2.set(p2X, p2Y);
		
		recomputeAuxVectors();
		resetCollideBounds();
	}

	public void setPosition(Vector2d v1, Vector2d v2){
		p1.set(v1);
		p2.set(v2);
		
		recomputeAuxVectors();
		resetCollideBounds();
	}

	
	private void recomputeAuxVectors() {
		// compute normal
		Vector2d.aToB(p1ToP2, p1, p2);
		p1toP2Distance = p1ToP2.length();
		p1ToP2.normalize();
		Vector2d.normal(normal, p1ToP2);
//		normal.normalize();		// no need to normalize! The turned p1ToP2 is already normalized

		Vector2d.aToB(tmpVector, p1, forceTargetPoint);
		if (Vector2d.dotProduct(tmpVector, normal) < 0){
			normal.invert();
		}
	}

	public boolean exertsForce() {
		return true;
	}

	public boolean acceptsForce() {
		return false;
	}

	@Override
	public void step(float dt) {
		// nothing to do
	}

	@Override
	public void recomputeBounds() {
		// nothing to do
	}
	
	private static Vector2d tmpSpeedVector = new Vector2d();
	@Override
	public void exertForce(ICollidable target) {
		Mass mass = (Mass) target;

		float preDistance = Vector2d.dotProduct(normal, Vector2d.aToB(tmpVector, p1, mass.getLastPosition()));
		float distance = Vector2d.dotProduct(normal, Vector2d.aToB(tmpVector, p1, mass.getPosition()));
		// compute collision point
		float lambda = Vector2d.dotProduct(tmpVector, p1ToP2);
		
		
		if (distance < 0
				&& (distance > -30 || Math.signum(preDistance)!=Math.signum(distance)) 		// do not let this force work for unlimited distances
				&& lambda > 0  
				&& lambda < p1toP2Distance 
		){
			float force = distance * strength ;
			
			tmpVector.set(normal);
			tmpVector.times(-force);
			// transfer forces to mass
			mass.move(tmpVector);
			
			// compute friction
			mass.getVelocity(tmpSpeedVector);
			float massSpeed = Vector2d.dotProduct(tmpSpeedVector, p1ToP2);
			// simple first trial: procentual speed change along surface
			tmpVector.set(p1ToP2);
			mass.move(tmpVector.times(-massSpeed * friction));
			
			if (trigger!=null && -distance > trigger.getMinTriggerDistance()){
				trigger.trigger(this, mass);
			}
		}
	}
	
	public IBumpTrigger getTrigger() {
		return trigger;
	}
	
	public void setTrigger(IBumpTrigger trigger) {
		this.trigger = trigger;
	}
}
