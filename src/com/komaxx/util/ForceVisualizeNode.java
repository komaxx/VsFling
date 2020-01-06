package com.komaxx.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import android.opengl.GLES20;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.ARenderProgramStore;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.physics.CollideTreeRoot;
import com.komaxx.physics.PhysicsSim;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.collide.ACollideSteppable;
import com.komaxx.physics.collide.ICollidable;

/**
 * Helper class that aspires to visualize forces for easier debugging.
 * Needs to be in the same coordinate system as the physics simulation
 * to work properly.
 * 
 * @author Matthias Schicker
 */
public class ForceVisualizeNode extends Node {
	private static final int MAX_VERTEX_COUNT = 40000;
	
	private FloatBuffer lineBuffer = ByteBuffer.allocateDirect(
			MAX_VERTEX_COUNT
    ).order(ByteOrder.nativeOrder()).asFloatBuffer();

	private final PhysicsSim physicsSim;

	
	public ForceVisualizeNode(PhysicsSim physicsSim){
		this.physicsSim = physicsSim;
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		renderProgramIndex = ARenderProgramStore.SIMPLE_COLORED;
		
		// usually in top of everything else
		depthTest = DEACTIVATE;
		zLevel = 2;
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		int vertexCount = 0;

		GLES20.glLineWidth(3);
		
		lineBuffer.position(0);
		CollideTreeRoot collideTreeRoot = physicsSim.getCollideTreeRoot();
		vertexCount += addLines(collideTreeRoot);

		Vertex.renderColoredLines(renderContext.currentRenderProgram, vertexCount, lineBuffer);
		
		return true;
	}

	private int addLines(ICollidable node) {
		int ret = 0;
		
		if (node.isLeaf()){
			if (node instanceof IVisualizable){
				ret += ((IVisualizable)node).addLines(lineBuffer);
			}
		} else {
			ACollidableGroup group = ((ACollidableGroup)node);

			ArrayList<ACollideSteppable> steppables = group.getSteppables();
			if (steppables != null){
				for (int i = 0; i < steppables.size(); i++){
					ret += addLines(steppables.get(i));
				}
			}
			
			ArrayList<ICollidable> collideChildren = group.getCollideChildren();
			for (int i = 0; i < collideChildren.size(); i++){
				ret += addLines(collideChildren.get(i));
			}
		}
		
		return ret;
	}
	
	public static void addVertex(FloatBuffer buffer, float x, float y, float[] color) {
		buffer.put(x);
		buffer.put(y);
		buffer.put(0);
		buffer.put(color[0]);
		buffer.put(color[1]);
		buffer.put(color[2]);
		buffer.put(color[3]);
	}
}
