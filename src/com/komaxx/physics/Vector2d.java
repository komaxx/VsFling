package com.komaxx.physics;

import android.annotation.SuppressLint;
import android.util.FloatMath;

// FloatMath was in my tests somewhat slower than Math -> use Math and ignore warnings
@SuppressLint("FloatMath") 
public class Vector2d {
	public float x;
	public float y;

	public Vector2d(){
		x = 0;
		y = 0;
	}
	
	public Vector2d(Vector2d v){
		x = v.x;
		y = v.y;
	}
	
	public Vector2d(float[] v){
		x = v[0];
		y = v[1];
	}

	public Vector2d(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public void set(Vector2d v) {
		x = v.x;
		y = v.y;
	}


	public void set(float[] f) {
		x = f[0];
		y = f[1];
	}
	
	public void add(Vector2d v) {
		x += v.x;
		y += v.y;
	}

	public void set(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public static float distance(Vector2d a, float bx, float by) {
		return (float) Math.sqrt(sq(a.x - bx) + sq(a.y - by));
	}

	
	public static float distance(Vector2d a, Vector2d b) {
		return (float) Math.sqrt(sq(a.x - b.x) + sq(a.y - b.y));
	}

	private static float sq(float f) {
		return f*f;
	}

	/**
	 * Skalar-multiplies the vector. Returns itself for convenience
	 */
	public Vector2d times(float s) {
		x *= s;
		y *= s;
		return this;
	}

	public static Vector2d aToB(Vector2d result, Vector2d a, Vector2d b) {
		result.x = b.x - a.x;
		result.y = b.y - a.y;
		
		return result;
	}
	
	public static float dotProduct(Vector2d v1, Vector2d v2){
		return v1.x*v2.x + v1.y*v2.y;
	}
	
	public void normalize() {
		float length = length();
		if (length != 0) times(1f/length);
	}

	public float length() {
		return FloatMath.sqrt(x*x + y*y);
	}
	
	@Override
	public String toString() {
		return "("+x+","+y+")";
	}

	public static void normal(Vector2d normal, Vector2d v) {
		normal.x = -v.y;
		normal.y = v.x;
	}

	public void add(float x2, float y2) {
		x += x2;
		y += y2;
	}

	public static float squareLength(Vector2d v1, Vector2d v2) {
		return sq(v1.x-v2.x) + sq(v1.y-v2.y);
	}

	public static float squareLength(Vector2d v) {
		return v.x*v.x + v.y*v.y;
	}

	public float[] toArray(float[] vector) {
		vector[0] = x;
		vector[1] = y;
		return vector;
	}

	public void invert() {
		x = -x;
		y = -y;
	}
}
