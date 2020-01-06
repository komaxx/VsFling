package com.komaxx.physics.constraints;

import java.nio.FloatBuffer;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideSteppable;
import com.komaxx.util.ForceVisualizeNode;
import com.komaxx.util.IVisualizable;

/**
 * Almost the same as distance constraint. However, only exerts pulling force.
 * Good for strings and cloth.
 * 
 * @author Matthias Schicker
 */
public class PullConstraint extends ACollideSteppable implements IVisualizable {
	public final Mass m1;
	public final Mass m2;

	public float naturalLength = 0;
	public float strength = 0.1f;

	public PullConstraint(Mass m1, Mass m2) {
		this.m1 = m1;
		this.m2 = m2;
		
		naturalLength = Vector2d.distance(m1.getPosition(), m2.getPosition());
	}

	private static Vector2d tmpVector = new Vector2d();
	@Override
	public void step(float dt) {
		float distance = Vector2d.distance(m1.getPosition(), m2.getPosition());
		float diff = distance-naturalLength;

		if (diff > 0){
			Vector2d.aToB(tmpVector, m1.getPosition(), m2.getPosition());
			tmpVector.times((1/distance) * diff * 0.5f 
					* strength
					);
	
			m1.move(tmpVector);
			m2.move(tmpVector.times(-1));
		}
	}
	
	private static float[] visualizationColor = new float[]{0, 0.0f, 0.4f, 1};
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		ForceVisualizeNode.addVertex(lineBuffer, m1.getPosition().x, m1.getPosition().y, visualizationColor);
		ForceVisualizeNode.addVertex(lineBuffer, m2.getPosition().x, m2.getPosition().y, visualizationColor);
		return 2;
	}

}
