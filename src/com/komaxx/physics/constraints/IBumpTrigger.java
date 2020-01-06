package com.komaxx.physics.constraints;

import com.komaxx.physics.Mass;
import com.komaxx.physics.collide.ACollideLeaf;

/**
 * Can be attached to some constraints. Will be triggered when a certain minimum
 * of force is exerted onto a mass, i.e., when the object is penetrated far enough.
 */
public interface IBumpTrigger {
	/**
	 * Use this value to set how much force needs to be exerted to
	 * trigger this bumper. The value actually sets the necessary penetration
	 * depth, so set a value appropriate to the used coordinate system!
	 * Not used by all trigger constraints.
	 */
	float getMinTriggerDistance();
	
	/**
	 * Called when enough force is exerted on a mass / a mass penetrated an
	 * area enough.
	 */
	void trigger(ACollideLeaf bc, Mass m);
}