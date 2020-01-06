package com.komaxx.vsfling;

/**
 * Simple storage class, mainly used to define walls and
 * obstacles.
 * 
 * @author Matthias Schicker
 */
public class PathSegment {
	public float[][] nodes;
	public float[] segmentDirection;
	public float width;
	
	public float bounceStrength = 1.0f;
}
