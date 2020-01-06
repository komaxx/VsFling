package com.komaxx.vsfling.play_select;

import java.nio.ShortBuffer;

import android.graphics.Rect;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

/**
 * Shows the frame above the butts in the selection screen.
 * 
 * @author Matthias Schicker
 */
public class ButtFrameNode extends Node {
	private ShortBuffer indexBuffer;

	private BoundTexturedQuad frameQuad;
	private BoundTexturedQuad[] numberBacks = new BoundTexturedQuad[3]; 
	private BoundTexturedQuad[] numberFronts = new BoundTexturedQuad[3]; 

	public ButtFrameNode(){
		this.draws = true;
		this.transforms = false;
		this.handlesInteraction = false;

		this.blending = ACTIVATE;
		this.depthTest = DEACTIVATE;
		this.renderProgramIndex = RenderProgramStore.ALPHA_TEXTURED;

		vbo = new Vbo(4 * (1+3+3), Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);

		frameQuad = new BoundTexturedQuad();
		frameQuad.bindToVbo(vbo);
		frameQuad.position(500, 20, 0, 700, -620, 0);

		// numbers
		GlRect[] positions = new GlRect[]{
				new GlRect(580, -45, 650, -145),
				new GlRect(575, -240, 650, -340),
				new GlRect(570, -430, 645, -530)
		};
		for (int i = 0; i < 3; i++){
			numberBacks[i] = new BoundTexturedQuad();
			numberBacks[i].bindToVbo(vbo);
			numberBacks[i].positionXY(positions[i]);
			numberFronts[i] = new BoundTexturedQuad();
			numberFronts[i].bindToVbo(vbo);
			numberFronts[i].positionXY(positions[i]);
			numberFronts[i].setAlphaDirect(0);
		}

		indexBuffer = TexturedQuad.allocateQuadIndices(1+3+3);
	}

	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.minHeight = 512;
		tc.minWidth = 512;
		tc.mipMapped = false;

		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();

		Rect[] rects = AtlasPainter.drawAtlas(renderContext.resources, new int[]{
				R.drawable.butt_frame,
				R.drawable.select_one, R.drawable.select_one_x,
				R.drawable.select_two, R.drawable.select_two_x,
				R.drawable.select_three, R.drawable.select_three_x
		}, t, 1);
		RectF[] uv = AtlasPainter.convertPxToUv(t, rects);

		frameQuad.setTexCoordsUv(uv[0]);

		numberBacks[0].setTexCoordsUv(uv[1]);
		numberFronts[0].setTexCoordsUv(uv[2]);

		numberBacks[1].setTexCoordsUv(uv[3]);
		numberFronts[1].setTexCoordsUv(uv[4]);

		numberBacks[2].setTexCoordsUv(uv[5]);
		numberFronts[2].setTexCoordsUv(uv[6]);
	}

	@Override
	public boolean onRender(RenderContext renderContext) {
		indexBuffer.position(0);
		int indexCount = frameQuad.render(renderContext, indexBuffer);
		for (int i = 0; i < 3; i++){
			indexCount += numberBacks[i].render(renderContext, indexBuffer);
			indexCount += numberFronts[i].render(renderContext, indexBuffer);
		}

		Vertex.renderTexturedTriangles(renderContext.currentRenderProgram, 0, indexCount, indexBuffer);

		return true;
	}
	
	public void setAreaSelect(int areaIndex, float percentage){
		KoLog.i(this, areaIndex + " set to " + percentage);
		
		numberFronts[areaIndex].setAlphaDirect
		(percentage);
	}
}
