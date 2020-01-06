package com.komaxx.physics.constraints;

import java.nio.FloatBuffer;

import android.graphics.Color;

import com.komaxx.komaxx_gl.util.RenderUtil;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideSteppable;
import com.komaxx.util.ForceVisualizeNode;
import com.komaxx.util.IVisualizable;

/**
 * Tries to keep a mass in a defined distance from a defined point.
 * 
 * @author Matthias Schicker
 *
 */
public class PointToMassDistanceConstraint extends ACollideSteppable implements IVisualizable {
	public final Mass m;
	public final Vector2d v = new Vector2d();

	public float naturalLength = 0;
	public float strength = 0.1f;
	
	private float lastLength = 0;

	/**
	 * Don't forget to set the point! For that, change the public <code>v</code>.
	 * When done, you most likely want to <code>resetNaturalLength</code> for the
	 * expected behavior.
	 */
	public PointToMassDistanceConstraint(Mass m) {
		this.m = m;
		naturalLength = Vector2d.distance(m.getPosition(), v);
	}

	
	public PointToMassDistanceConstraint(Mass m, Vector2d v) {
		this.m = m;
		this.v.set(v);
		
		naturalLength = Vector2d.distance(m.getPosition(), v);
	}
	
	private static Vector2d tmpVector = new Vector2d();
	@Override
	public void step(float dt) {
		lastLength = Vector2d.distance(m.getPosition(), v);
		float diff = lastLength-naturalLength;

		if (lastLength == 0 || diff == 0) return;
		
		Vector2d.aToB(tmpVector, v, m.getPosition());
		tmpVector.times((1f/lastLength) * -diff * strength);

		m.move(tmpVector);
	}
	
	/**
	 * Sets the current distance between the mass and the set point as
	 * natural length.
	 */
	public void resetNaturalLength() {
		naturalLength = Vector2d.distance(m.getPosition(), v);
	}

	
	private static float[] vColor1 = RenderUtil.color2floatsRGB(null, Color.BLACK);
	private static float[] vColor2 = RenderUtil.color2floatsRGB(null, 0xFF666600);
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		Vector2d position = m.getPosition();
		
		ForceVisualizeNode.addVertex(lineBuffer, v.x, v.y, vColor1);
		ForceVisualizeNode.addVertex(lineBuffer, position.x, position.y, vColor2);
		
		return 2;
	}
}
