package com.komaxx.physics.collide;

import java.util.ArrayList;

import com.komaxx.komaxx_gl.math.GlRect;

public abstract class ACollidableGroup implements ICollidable {
	protected GlRect lastBounds = new GlRect();
	protected GlRect bounds = new GlRect();
	
	protected ArrayList<ICollidable> children = new ArrayList<ICollidable>();
	protected ArrayList<ACollideSteppable> steppables;

	protected ACollidableGroup parent;
	
	protected int combinedAcceptMask = 0;
	protected int combinedExertsMask = 0;
	
	@Override
	public GlRect getCollideBounds() {
		return bounds;
	}

	@Override
	public void setParent(ACollidableGroup parent) {
		this.parent = parent;
	}
	
	@Override
	public ACollidableGroup getParent() {
		return parent;
	}
	
	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public ArrayList<ICollidable> getCollideChildren() {
		return children;
	}

	public ArrayList<ACollideSteppable> getSteppables() {
		return steppables;
	}
	
	/**
	 * Called exactly once per physics frame. All physical objects should recompute
	 * (if necessary) their bounds and store their old bounds for object 
	 * trajectory computations.
	 */
	@Override
	public void recomputeBounds() {
		int l = children.size();
		if (l < 1) return;
		
		lastBounds.set(bounds);
		
		ICollidable favoriteChild = children.get(0);		// Yes. Not all are loved the same.
		favoriteChild.recomputeBounds();
		bounds.set(favoriteChild.getCollideBounds());
		
		for (int i = 1; i < l; i++){
			ICollidable child = children.get(i);
			child.recomputeBounds();
			
			bounds.union(child.getCollideBounds());
		}
	}
	
	public void addChild(ICollidable child){
		children.add(child);
		child.setParent(this);

		recomputeCollisionMasks();
	}
	
	
	public void removeChild(ICollidable child){
		children.remove(child);
		child.setParent(null);
		
		recomputeCollisionMasks();
	}
	
	/**
	 * Use this to add special ICollidables that are not, in fact, collidable
	 * but only need to be called once a physics-frame to do their "step"-work.
	 */
	public void addSteppable(ACollideSteppable toAdd){
		if (steppables == null) steppables = new ArrayList<ACollideSteppable>(10);
		steppables.add(toAdd);
	}
	
	public void removeSteppable(ACollideSteppable toRemove){
		if (steppables == null) return;
		steppables.remove(toRemove);
	}

	private void recomputeCollisionMasks() {
		if (parent != null){
			parent.recomputeCollisionMasks();
		} else {
			rebuildCollisionMasks();
		}
	}

	@Override
	public void rebuildCollisionMasks() {
		combinedAcceptMask = 0;
		combinedExertsMask = 0;
		
		for (int i = 0; i < children.size(); i++){
			children.get(i).rebuildCollisionMasks();

			combinedAcceptMask = combinedAcceptMask | children.get(i).getAcceptsForceMask();
			combinedExertsMask = combinedExertsMask | children.get(i).getExertsForceMask();
		}
	}

	@Override
	public void exertForce(ICollidable target) {
		// no!
	}
	
	@Override
	public void step(float dt) {
		int l = children.size();
		for (int i = 0; i < l; i++){
			children.get(i).step(dt);
		}
		l = steppables==null ? 0 : steppables.size();
		for (int i = 0; i < l; i++){
			steppables.get(i).step(dt);
		}
	}

	@Override
	public int getAcceptsForceMask() {
		return combinedAcceptMask;
	}
	
	@Override
	public int getExertsForceMask() {
		return combinedExertsMask;
	}
}
