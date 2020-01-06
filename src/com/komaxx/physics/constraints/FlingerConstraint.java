package com.komaxx.physics.constraints;

import java.nio.FloatBuffer;

import android.graphics.Color;

import com.komaxx.komaxx_gl.util.RenderUtil;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.collide.ICollidable;
import com.komaxx.util.ForceVisualizeNode;
import com.komaxx.util.IVisualizable;

/**
 * Constraint that transmits force from a flinger to a mass (usually the ball)
 * 
 * @author Matthias Schicker
 */
public class FlingerConstraint extends ACollideLeaf implements IVisualizable {
	private Mass m1;
	private Mass m2;

	private boolean invertNormal = false;
	
	public final Vector2d normal = new Vector2d();
	private final Vector2d p1ToP2 = new Vector2d();
	private float p1toP2Distance;

	public float strength = 1.0f;
	public float friction = 0.3f;

	private static Vector2d tmpVector = new Vector2d();

	public FlingerConstraint(Mass m1, Mass m2, Vector2d forceTargetPoint) {
		this.m1 = m1;
		this.m2 = m2;

		recomputeNormal();

		Vector2d.aToB(tmpVector, m1.getPosition(), forceTargetPoint);
		if (Vector2d.dotProduct(tmpVector, normal) < 0){
			invertNormal = true;
			normal.invert();
		}
	}

	private void recomputeNormal() {
		Vector2d.aToB(p1ToP2, m1.getPosition(), m2.getPosition());
		p1toP2Distance = p1ToP2.length();
		p1ToP2.normalize();
		Vector2d.normal(normal, p1ToP2);
		normal.normalize();
		if (invertNormal) normal.invert();
	}

	private static Vector2d tmpSpeedVector = new Vector2d();
	private static Vector2d tmpSpeedVector2 = new Vector2d();
	@Override
	public void exertForce(ICollidable target) {
		Mass mass = (Mass) target;
		
		recomputeNormal();
		
		Vector2d.aToB(tmpVector, m1.getPosition(), mass.getPosition());

		float preDistance = Vector2d.dotProduct(normal, Vector2d.aToB(tmpVector, m1.getPosition(), mass.getLastPosition()));
		float postDistance = Vector2d.dotProduct(normal, Vector2d.aToB(tmpVector, m1.getPosition(), mass.getPosition()));
		
		
		// compute collision point
		float lambda = Vector2d.dotProduct(tmpVector, p1ToP2);

		float minLambda = -0.1f;
		float maxLambda = p1toP2Distance * (1 - minLambda);
		
		if (	postDistance < 0 &&
				(Math.signum(preDistance) != Math.signum(postDistance)
				|| postDistance > -15) 
				&& 
				lambda > minLambda && 
				lambda < maxLambda 
		){
			float massRatio = mass.mass / (mass.mass + m1.mass + m2.mass);
			
			// ///////////////////////////////////////////////
			// compute mass replacement
			float amount = postDistance * strength * (1-massRatio);
			tmpVector.set(normal);
			tmpVector.times(-amount);
			mass.move(tmpVector);
			
			// ///////////////////////////////////////////////
			// compute friction
			mass.getVelocity(tmpSpeedVector);
			float massSpeed = Vector2d.dotProduct(tmpSpeedVector, p1ToP2);

			// compute average flinger speed
			m1.getVelocity(tmpSpeedVector);
			m2.getVelocity(tmpSpeedVector2);
			tmpSpeedVector.add(tmpSpeedVector2);
			tmpSpeedVector.times(0.5f);
			
			float flingerSpeed = Vector2d.dotProduct(tmpSpeedVector, p1ToP2);
			
			float relativeSpeed = massSpeed - flingerSpeed;
			// friction-move mass
			tmpVector.set(p1ToP2);
			tmpVector.times(-relativeSpeed * friction * (1-massRatio));
			mass.move(tmpVector);
			// friction move flinger
			tmpVector.set(p1ToP2);
			tmpVector.times(relativeSpeed * friction * massRatio);
			m1.move(tmpVector);
			m2.move(tmpVector);
			
			// /////////////////////////////////////////////////
			// compute bumper replacement
			// TODO compute influence according to distance to either flinger mass
			amount = postDistance * strength * massRatio;
			tmpVector.set(normal);
			tmpVector.times(amount);
			m1.move(tmpVector);
			m2.move(tmpVector);
		}
	}

	@Override
	public void step(float dt) {
		// nothing to do
	}

	@Override
	public void recomputeBounds() {
		collideBounds.set(m1.getCollideBounds());
		collideBounds.union(m2.getCollideBounds());
	}

	float[] visualizationColor = RenderUtil.color2floatsRGBA(null, Color.RED);
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		Vector2d pos = m1.getPosition();
		ForceVisualizeNode.addVertex(lineBuffer, pos.x, pos.y, visualizationColor);
		pos = m2.getPosition();
		ForceVisualizeNode.addVertex(lineBuffer, pos.x, pos.y, visualizationColor);
		return 2;
	}
}
