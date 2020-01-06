package com.komaxx.vsfling.objects;

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
public class ButtMeasureConstraint extends ACollideSteppable implements IVisualizable {
	public final int id;

	public final Mass m;
	public final Vector2d v = new Vector2d();

	public float naturalLength = 0;
	public float strength = 0.1f;
	
	private float maxTension = 0;
	
	private float lastLength = 0;
	
	private IButtMeasureHandler handler;
	
	/**
	 * Point is set to initial position of the mass
	 * To change the point, change the public <code>v</code>.
	 * When done, you most likely want to <code>resetNaturalLength</code> for the
	 * expected behavior (or set manually).
	 */
	public ButtMeasureConstraint(int id, Mass m) {
		this.id = id;
		this.m = m;
		this.v.set(m.getPosition());
		
		naturalLength = Vector2d.distance(m.getPosition(), v);
	}
	
	private static Vector2d tmpVector = new Vector2d();
	@Override
	public void step(float dt) {
		lastLength = Vector2d.distance(m.getPosition(), v);
		float diff = lastLength-naturalLength;

		if (lastLength == 0 || diff == 0) return;
		
		float absTension = (diff>0) ? diff : -diff;
		if (absTension > maxTension){
			if (handler!=null) handler.handleTension(this, absTension);
			maxTension = absTension;
		}
		
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

	
	private static float[] vColor1 = RenderUtil.color2floatsRGB(null, Color.BLUE);
	private static float[] vColor2 = RenderUtil.color2floatsRGB(null, 0xFF666600);
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		Vector2d position = m.getPosition();
		
		ForceVisualizeNode.addVertex(lineBuffer, v.x, v.y, vColor1);
		ForceVisualizeNode.addVertex(lineBuffer, position.x, position.y, vColor2);
		
		return 2;
	}
	
	public IButtMeasureHandler getHandler() {
		return handler;
	}

	public void setHandler(IButtMeasureHandler handler) {
		this.handler = handler;
	}

	public static interface IButtMeasureHandler {
		/**
		 * Called whenever the constraint experienced more tension than ever before.
		 * (Otherwise: NOT)
		 */
		void handleTension(ButtMeasureConstraint c, float tension);
	}
}
