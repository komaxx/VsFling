package com.komaxx.physics.constraints;

import java.nio.FloatBuffer;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideSteppable;
import com.komaxx.util.ForceVisualizeNode;
import com.komaxx.util.IVisualizable;

/**
 * Tries to keep two masses in a defined distance from each other by moving
 * both.
 * 
 * @author Matthias Schicker
 *
 */
public class MassesDistanceConstraint extends ACollideSteppable implements IVisualizable{
	public final Mass m1;
	public final Mass m2;

	public float naturalLength = 0;
	public float strength = 0.1f;
	
	private Object tag;
	public boolean visualize = true;

	public MassesDistanceConstraint(Mass m1, Mass m2) {
		this.m1 = m1;
		this.m2 = m2;
		
		naturalLength = Vector2d.distance(m1.getPosition(), m2.getPosition());
	}

	private static Vector2d tmpVector = new Vector2d();
	@Override
	public void step(float dt) {
		float distance = Vector2d.distance(m1.getPosition(), m2.getPosition());
		float diff = distance-naturalLength;

		if (distance == 0 || diff == 0) return;
		
//		if (diff > 0) diff = diff * 0.2f;
		
		Vector2d.aToB(tmpVector, m1.getPosition(), m2.getPosition());
		tmpVector.times((1/distance) * diff * 0.5f 
				* strength
				);

		m1.move(tmpVector);
		m2.move(tmpVector.times(-1));
	}
	
	public void setTag(Object tag) {
		this.tag = tag;
	}
	
	public Object getTag() {
		return tag;
	}
	
	private static float[] visualizationColor = new float[]{0, 0.3f, 0, 1};
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		if (!visualize) return 0;
		ForceVisualizeNode.addVertex(lineBuffer, m1.getPosition().x, m1.getPosition().y, visualizationColor);
		ForceVisualizeNode.addVertex(lineBuffer, m2.getPosition().x, m2.getPosition().y, visualizationColor);
		return 2;
	}
	
}
