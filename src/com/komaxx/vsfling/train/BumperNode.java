package com.komaxx.vsfling.train;

import java.nio.ShortBuffer;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundFreeQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.util.AnimatedValue;
import com.komaxx.komaxx_gl.util.StepInterpolator;
import com.komaxx.physics.Mass;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.constraints.BumperConstraint;
import com.komaxx.physics.constraints.IBumpTrigger;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.RenderProgramStore;

public class BumperNode extends Node implements IBumpTrigger {
	private BoundFreeQuad quad = new BoundFreeQuad();
	private ShortBuffer indicesBuffer = TexturedQuad.allocateQuadIndices(1);
	
	private BumperConstraint bumperConstraint;
	private Vector2d v1 = new Vector2d();
	private Vector2d v2 = new Vector2d();
	
	private Vector2d forceDirection = new Vector2d();
	
	private AnimatedValue animatedValue;
	
	public BumperNode(TrainView1 tv, Vector2d pos1, Vector2d pos2, Vector2d forceDirection) {
		v1.set(pos1);
		v2.set(pos2);
		this.forceDirection.set(forceDirection);
		this.forceDirection.normalize();
		
		draws = true;
		transforms = false;
		handlesInteraction = true;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.DEPPEN_SHADER;
		
		vbo = new Vbo(quad.getMaxVertexCount(), TexturedVertex.STRIDE_BYTES);
		quad.bindToVbo(vbo);

		animatedValue = new AnimatedValue(
				StepInterpolator.BUMPER_STEPS, 0, 50, AnimatedValue.ANIMATION_DURATION_NORMAL);
		
		bumperConstraint = new BumperConstraint(v1, v2, forceDirection);
		bumperConstraint.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		bumperConstraint.setTrigger(this);
		tv.getPhysics().addCollidable(bumperConstraint);
	}

	@Override
	public boolean onRender(RenderContext renderContext) {
		updatePosition(renderContext);
		
		indicesBuffer.position(0);
		int indexCount = quad.render(renderContext, indicesBuffer);
		
		Vertex.renderTexturedTriangles(renderContext.currentRenderProgram, 0, indexCount, indicesBuffer);
		
		return true;
	}

	private static Vector2d tmp1 = new Vector2d();
	private static Vector2d tmp2 = new Vector2d();
	private static Vector2d tmpOffset = new Vector2d();
	private void updatePosition(RenderContext renderContext) {
		float offset = animatedValue.get(renderContext.frameNanoTime);
		tmpOffset.set(forceDirection);
		tmpOffset.times(offset);
		
		tmp1.set(v1);
		tmp2.set(v2);
		
		tmp1.add(tmpOffset);
		tmp2.add(tmpOffset);
		
		bumperConstraint.setPosition(tmp1, tmp2);
		
		// now for the rendering
		quad.positionXY(BoundFreeQuad.VERTEX_UPPER_LEFT, tmp1.x, tmp1.y);
		quad.positionXY(BoundFreeQuad.VERTEX_UPPER_RIGHT, tmp2.x, tmp2.y);
		
		tmpOffset.set(forceDirection);
		tmpOffset.times(-30);
		tmp1.add(tmpOffset);
		tmp2.add(tmpOffset);
		
		quad.positionXY(BoundFreeQuad.VERTEX_LOWER_LEFT, tmp1.x, tmp1.y);
		quad.positionXY(BoundFreeQuad.VERTEX_LOWER_RIGHT, tmp2.x, tmp2.y);
	}

	@Override
	public float getMinTriggerDistance() {
		return 1;
	}

	@Override
	public void trigger(ACollideLeaf bc, Mass m) {
		animatedValue.start();
	}
}

