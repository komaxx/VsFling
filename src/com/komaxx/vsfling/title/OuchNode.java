package com.komaxx.vsfling.title;

import java.nio.ShortBuffer;

import android.graphics.Bitmap;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.texturing.TextureStore;
import com.komaxx.vsfling.RenderProgramStore;

public class OuchNode extends Node {
	private final int resourceId;

	private BoundTexturedQuad quad = new BoundTexturedQuad();
	private ShortBuffer indexBuffer;
	
	public OuchNode(int resourceId){
		this.resourceId = resourceId;
		
		draws = true;
		transforms = false;
		handlesInteraction = false;
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;

		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		vbo = new Vbo(quad.getMaxVertexCount(), TexturedVertex.STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(1);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		Bitmap bitmap = TextureStore.getBitmap(renderContext, resourceId);
		
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.minHeight = bitmap.getWidth();
		tc.minWidth = bitmap.getHeight();
		tc.mipMapped = false;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		t.update(bitmap, 0, 0);
		
		quad.positionXY(-bitmap.getWidth()/2, 0, bitmap.getWidth()/2, -bitmap.getHeight());
		quad.setTexCoordsUv(0, 0, 
				(float)bitmap.getWidth()/(float)t.getWidth(), 
				(float)bitmap.getHeight()/(float)t.getHeight(), false);
		
		quad.bindToVbo(vbo);
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		indexBuffer.position(0);
		int indexCount = quad.render(renderContext, indexBuffer);
		
		Vertex.renderTexturedTriangles(renderContext.currentRenderProgram, 0, indexCount, indexBuffer);

		return true;
	}
}
