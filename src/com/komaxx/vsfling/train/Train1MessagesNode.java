package com.komaxx.vsfling.train;

import android.graphics.Color;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.vsfling.RenderProgramStore;

/**
 * Displays the tutorial messages for the first train screen.
 * 
 * @author Matthias Schicker
 */
public class Train1MessagesNode extends Node {
	// /////////////////////////////////////////////////////////
	// rendering
	private BoundTexturedQuad messageQuad = new BoundTexturedQuad();
	private Texture texture;
//	private ShortBuffer indexBuffer;
	
	
	public Train1MessagesNode(){
		draws = true;
		transforms = false;
		handlesInteraction = true;

		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		useVboPainting = true;
		
		vbo = new Vbo(messageQuad.getMaxVertexCount(), TexturedVertex.STRIDE_BYTES);
//		indexBuffer = TexturedQuad.allocateQuadIndices(1);
		
		messageQuad.bindToVbo(vbo);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.basicColor = Color.TRANSPARENT;
		tc.minHeight = 512;
		tc.minWidth = 512;
		
		texture = new Texture(tc);
		texture.create(renderContext);
		textureHandle = texture.getHandle();
	}
}	
