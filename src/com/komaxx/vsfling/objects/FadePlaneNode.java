package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.scenegraph.interaction.InteractionContext;
import com.komaxx.komaxx_gl.util.InterpolatedValue;
import com.komaxx.komaxx_gl.util.InterpolatedValue.AnimationType;
import com.komaxx.vsfling.FadeRenderProgram;
import com.komaxx.vsfling.RenderProgramStore;

/**
 * Simple class that displays a screen-filling quad which
 * becomes increasingly opaque, for an easy fade-out effect
 * 
 * @author Matthias Schicker
 */
public class FadePlaneNode extends Node {
	private BoundTexturedQuad quad = new BoundTexturedQuad();
	private ShortBuffer indexBuffer;
	
	private InterpolatedValue phase = new InterpolatedValue(
			AnimationType.INVERSE_SQUARED, 0, 4L*InterpolatedValue.ANIMATION_DURATION_NORMAL);
	
	public FadePlaneNode(){
		draws = true;
		handlesInteraction = true;
		blending = ACTIVATE;
		
		renderProgramIndex = RenderProgramStore.FADE;
		depthTest = DEACTIVATE;
		
		visible = true;
		
		vbo = new Vbo(TexturedQuad.VERTEX_COUNT, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES);

		quad.position(-100000, 100000, 0, 100000, -100000, 0);
		quad.bindToVbo(vbo);
		indexBuffer = TexturedQuad.allocateQuadIndices(1);
	}
	
	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		return visible;
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		float nowPhase = phase.get(renderContext.frameNanoTime);
		
		if (nowPhase == 0 && phase.getTarget() == 0){
			visible = false;
			return false;
		}
		
		((FadeRenderProgram)renderContext.currentRenderProgram).setFadePhase(nowPhase);
		
		indexBuffer.position(0);
		quad.render(renderContext, indexBuffer);
		Vertex.renderTexturedTriangles(renderContext.currentRenderProgram, 0, 6, indexBuffer);
		
		return true;
	}
	
	public void fadeOut(){
		visible = true;
		phase.set(1);
	}

	public void fadeIn(){
		visible = true;
		phase.set(0);
	}
	
	public void setFadeDuration(long nuDuration){
		phase.setDuration(nuDuration);
	}
}
