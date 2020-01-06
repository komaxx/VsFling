package com.komaxx.physics.constraints;

import java.nio.FloatBuffer;

import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideSteppable;
import com.komaxx.util.ForceVisualizeNode;
import com.komaxx.util.IVisualizable;


/**
 * Similar to MassesDistanceConstraint but will rip (cease to exert force)
 * when a certain tension is reached.
 * 
 * @author Matthias Schicker
 *
 */
public class RipConstraint extends ACollideSteppable implements IVisualizable {
	public final Mass m1;
	public final Mass m2;

	public float naturalLength = 0;
	public float strength = 0.1f;
	
	public float maxTension = 1.2f;
	
	private IRippedHandler handler;
	private boolean ripped = false;
	

	public RipConstraint(Mass m1, Mass m2) {
		this.m1 = m1;
		this.m2 = m2;
		
		naturalLength = Vector2d.distance(m1.getPosition(), m2.getPosition());
	}

	public boolean isRipped() {
		return ripped;
	}
	
	private static Vector2d tmpVector = new Vector2d();
	@Override
	public void step(float dt) {
		if (ripped) return;
		
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
		
		if (distance / naturalLength > maxTension){
			ripped = true;
			IRippedHandler handlerCopy = handler;
			if (handlerCopy != null) handlerCopy.handleRipped(this);
		}
	}
	
	public void setRippedHandler(IRippedHandler handler) {
		this.handler = handler;
	}
	
	public IRippedHandler getRippedHandler() {
		return handler;
	}
	
	private static float[] visualizationColor = new float[]{1, 0.2f, 0.2f, 1};
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		if (ripped) return 0;
		
		ForceVisualizeNode.addVertex(lineBuffer, m1.getPosition().x, m1.getPosition().y, visualizationColor);
		ForceVisualizeNode.addVertex(lineBuffer, m2.getPosition().x, m2.getPosition().y, visualizationColor);
		return 2;
	}
	
	public void setMaxTension(float maxTension) {
		this.maxTension = maxTension;
	}
	
	/**
	 * Can be attached to a RipConstraint. Will be notified when the constraint
	 * rips.
	 * 
	 * @author Matthias Schicker
	 */
	public interface IRippedHandler {
		void handleRipped(RipConstraint c);
	}
}
