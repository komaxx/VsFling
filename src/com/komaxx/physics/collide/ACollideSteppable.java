package com.komaxx.physics.collide;

import java.util.ArrayList;

import com.komaxx.komaxx_gl.math.GlRect;

/**
 * Super-class for all objects that do not engage in collisions but
 * are to be stepped in each physics loop. Typical example: Constraints
 * that hold an object together.
 * 
 * @author Matthias Schicker
 */
public abstract class ACollideSteppable implements ICollidable {
	private static GlRect noBounds = new GlRect(0,0,0,0);

	protected ACollidableGroup parent;
	
	
	@Override
	public GlRect getCollideBounds() {
		return noBounds;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public ArrayList<ICollidable> getCollideChildren() {
		// never has any children. To create branch nodes in the physics
		// tree, use ACollidableGroups
		return null;
	}

	@Override
	public void recomputeBounds() {
		// no. No bounds.
	}

	@Override
	public void exertForce(ICollidable target) {
		// no.
	}

	@Override
	public int getExertsForceMask() {
		return 0;
	}

	@Override
	public int getAcceptsForceMask() {
		return 0;
	}

	@Override
	public void rebuildCollisionMasks() {
		// no. no collisions.
	}

	@Override
	public void setParent(ACollidableGroup parent) {
		this.parent = parent;
	}
	
	@Override
	public ACollidableGroup getParent() {
		return parent;
	}
}
