package com.komaxx.physics;

import java.nio.FloatBuffer;

import android.graphics.Color;

import com.komaxx.komaxx_gl.util.RenderUtil;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.collide.ICollidable;
import com.komaxx.util.ForceVisualizeNode;
import com.komaxx.util.IVisualizable;

public class Mass extends ACollideLeaf implements IVisualizable {
	private static int instance = 0;
	protected static Vector2d tmpVector = new Vector2d();

	private int ID = instance++;
	
	public boolean physical = true;
	
	public float mass = 1;
	public float friction = 0.005f;
	
	protected Vector2d lastPosition = new Vector2d();
	protected Vector2d position = new Vector2d();

	protected Vector2d force = new Vector2d();
	
	
	@Override
	public void step(float dt) {
		if (!physical) return;

		tmpVector.set(position);
		
		position.x = position.x*(2f-friction) - lastPosition.x*(1-friction) + (force.x / mass) * dt*dt;
		position.y = position.y*(2f-friction) - lastPosition.y*(1-friction) + (force.y / mass) * dt*dt;
		
		lastPosition.set(tmpVector);
		
		force.set(0, 0);
	}

	public void addForce(Vector2d toApply) {
		force.add(toApply);
	}

	public void addForce(float x, float y) {
		force.add(x, y);
	}
	
	public void setPosition(float x, float y) {
		position.x = x;
		position.y = y;
		
		lastPosition.set(position);
		recomputeBounds();
	}
	
	public void getVelocity(Vector2d tmpSpeedVector) {
		tmpSpeedVector.x = position.x - lastPosition.x;
		tmpSpeedVector.y = position.y - lastPosition.y;
	}
	
	public Vector2d getPosition() {
		return position;
	}

	public void move(Vector2d v) {
		if (physical){
			position.add(v);
		}
	}

	public void move(float x, float y) {
		if (physical){
			position.add(x, y);
		}
	}
	
	public Vector2d getLastPosition() {
		return lastPosition;
	}

	public void offset(Vector2d v) {
		lastPosition.add(v);
		position.add(v);
	}

	public void setPosition(Vector2d nuPos) {
		position.set(nuPos);
		lastPosition.set(nuPos);
	}

	public Vector2d getForce() {
		return force;
	}

	@Override
	public void recomputeBounds() {
		lastCollideBounds.set(collideBounds);
		
		int margin = 8;
		collideBounds.set(
				position.x - margin, 
				position.y + margin, 
				position.x + margin, 
				position.y - margin);
	}

	@Override
	public void exertForce(ICollidable target) {
		// no
	}
	
	@Override
	public String toString() {
		return ID + position.toString();
	}

	// /////////////////////////////////////////////////////////////////////
	// Visualization / Debugging
	private static float[] visualizationColor = RenderUtil.color2floatsRGBA(null, Color.GRAY);
	@Override
	public int addLines(FloatBuffer lineBuffer) {
		int halfSize = 3;
		ForceVisualizeNode.addVertex(lineBuffer, position.x-halfSize, position.y+halfSize, visualizationColor);
		ForceVisualizeNode.addVertex(lineBuffer, position.x+halfSize, position.y-halfSize, visualizationColor);
		ForceVisualizeNode.addVertex(lineBuffer, position.x-halfSize, position.y-halfSize, visualizationColor);
		ForceVisualizeNode.addVertex(lineBuffer, position.x+halfSize, position.y+halfSize, visualizationColor);

		return 4;
	}
}
