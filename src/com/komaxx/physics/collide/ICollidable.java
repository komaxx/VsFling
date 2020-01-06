package com.komaxx.physics.collide;

import java.util.ArrayList;

import com.komaxx.komaxx_gl.math.GlRect;

/**
 * The generic interface that needs to be implemented by all objects that
 * are to be part of the physics-graph (i.e., tree).
 * 
 * @author Matthias Schicker
 */
public interface ICollidable {
	GlRect getCollideBounds();
	
	/**
	 * Whether or not this IColldidable has any children. ICollidables may
	 * either have children <i>or</i> be leafs.
	 */
	boolean isLeaf();
	
	ACollidableGroup getParent();
	
	ArrayList<ICollidable> getCollideChildren();

	void recomputeBounds();
	
	void exertForce(ICollidable target);
	
	/**
	 * Called after all collisions where processed. Should all nodes make
	 * commit the made changes.
	 */
	void step(float dt);
	
	/**
	 * Delivers a binary mask that defines on which class of objects this
	 * collidable exerts forces.
	 */
	int getExertsForceMask();
	
	/**
	 * Delivers a binary mask that defines from which class of objects this
	 * collidable accepts forces.
	 */
	int getAcceptsForceMask();

	/**
	 * Called after the tree was changed. Branch nodes (as opposed to leaf nodes)
	 * are supposed to re-combine the force-masks of their children.
	 */
	void rebuildCollisionMasks();

	void setParent(ACollidableGroup parent);
}
