package com.komaxx.util;

import java.nio.FloatBuffer;

/**
 * May be implemented by ICollidables (leafes only). In that case, they can
 * be visualized by a ForceVisualizerNode for easier debugging.
 * 
 * @author Matthias Schicker
 */
public interface IVisualizable {
	/**
	 * Implementing classes are supposed to add colored vertices here.
	 * These will be painted as separate lines when the physics tree was
	 * completely scraped. </br>
	 * 
	 * You may use the public static functions in <code>ForceVisualizeNode</code>
	 * to easily add vertices!
	 * 
	 * @return	The number of added vertices (MUST be a multiple of 2).
	 */
	int addLines(FloatBuffer lineBuffer);
}
