package com.komaxx.physics.collide;

import java.util.ArrayList;

import com.komaxx.komaxx_gl.math.GlRect;

/**
 * Super-class for generic objects in the physics-tree. Objects of this kind
 * may be part of collisions, exert forces on each other, or just receive them.
 * 
 * @author Matthias Schicker
 */
public abstract class ACollideLeaf implements ICollidable {
	protected GlRect collideBounds = new GlRect();
	protected GlRect lastCollideBounds = new GlRect();

	protected int exertMask = 0x0;
	protected int acceptMask = 0x0;
	
	
	protected ACollidableGroup parent;
	
	@Override
	public GlRect getCollideBounds() {
		return collideBounds;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public ArrayList<ICollidable> getCollideChildren() {
		return null;
	}

	@Override
	public int getAcceptsForceMask() {
		return acceptMask;
	}
	
	@Override
	public int getExertsForceMask() {
		return exertMask;
	}
	
	@Override
	public void rebuildCollisionMasks() {
		// nothing to do in generic case, collision masks are considered static
	}
	
	/**
	 * Should be called before adding the leaf to the collision tree. Otherwise,
	 * the change in the mask may not be propagated to upper layers in the tree.
	 */
	public void setAcceptForceMask(int acceptMask) {
		this.acceptMask = acceptMask;
	}
	
	/**
	 * Should be called before adding the leaf to the collision tree. Otherwise,
	 * the change in the mask may not be propagated to upper layers in the tree.
	 */
	public void setExertForceMask(int exertMask) {
		this.exertMask = exertMask;
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
