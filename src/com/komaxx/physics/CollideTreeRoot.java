package com.komaxx.physics;

import java.util.ArrayList;

import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.collide.ICollidable;

public class CollideTreeRoot extends ACollidableGroup {
	private boolean oddFrame = false;
	
	public void collide() {
		recomputeBounds();
		
		int l = children.size();
		
//		if (oddFrame){
			for (int i = l-2; i > -1; --i){
				ICollidable a = children.get(i);
				GlRect aBounds = a.getCollideBounds();
				for (int j = l-1; j > i; --j){
					ICollidable b = children.get(j);
					if (GlRect.intersects(aBounds, b.getCollideBounds()) && collides(a, b)) collide(a, b);
				}
			}
//		} else {
//			int lMinusOne = l-1;
//			for (int i = 0; i < lMinusOne; i++){
//				ICollidable a = children.get(i);
//				GlRect aBounds = a.getCollideBounds();
//				for (int j = i+1; j < l; j++){
//					ICollidable b = children.get(j);
//					if (GlRect.intersects(aBounds, b.getCollideBounds()) && collides(a, b)) collide(a, b);
//				}
//			}
//		}
//		
		
		oddFrame = !oddFrame;
	}

	private void collide(ICollidable a, ICollidable b) {
		if (!a.isLeaf()){
			GlRect bBounds = b.getCollideBounds();
			
			ArrayList<ICollidable> aChilds = a.getCollideChildren();
			int l = aChilds.size();
			for (int i = l-1; i >= 0; i--){
				ICollidable aChild = aChilds.get(i);
				GlRect aChildBounds = aChild.getCollideBounds();
				if (GlRect.intersects(aChildBounds, bBounds) && collides(aChild, b)){
					collide(aChild, b);
				}
			}
		} else if (!b.isLeaf()){
			GlRect aBounds = a.getCollideBounds();

			ArrayList<ICollidable> bChilds = b.getCollideChildren();
			int l = bChilds.size();
			for (int i = l-1; i >= 0; --i){
				ICollidable bChild = bChilds.get(i);
				GlRect bChildBounds = bChild.getCollideBounds();
				if (GlRect.intersects(aBounds, bChildBounds)) collide(a, bChild);
			}
		} else {
			if ((a.getExertsForceMask() & b.getAcceptsForceMask()) != 0){
				a.exertForce(b);
			}
			
			if ((b.getExertsForceMask() & a.getAcceptsForceMask()) != 0){
				b.exertForce(a);
			}
		}
	}

	private static boolean collides(ICollidable a, ICollidable b) {
		return 	(a.getAcceptsForceMask() & b.getExertsForceMask()) != 0 ||
			   	(a.getExertsForceMask() & b.getAcceptsForceMask()) != 0;
	}
}
