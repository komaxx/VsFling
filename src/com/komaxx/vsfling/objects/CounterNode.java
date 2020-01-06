package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import android.graphics.Rect;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.Vector;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

/**
 * Displays a (integer) number on the screen.
 *  
 * @author Matthias Schicker
 */
public class CounterNode extends Node {
	private static final int MAX_DIGITS = 12;

	public static final byte ANCHOR_LEFT = 0;
	public static final byte ANCHOR_RIGHT = 1;			// default
	public static final byte ANCHOR_CENTER = 2;

	private byte anchoring = ANCHOR_RIGHT;
	private int number = 0;
	private float[] anchor = new float[]{0,0};

	
	private BoundTexturedQuad[] quads = new BoundTexturedQuad[MAX_DIGITS];
	private ShortBuffer indexBuffer;
	
	private Rect[] digitPXs;
	private RectF[] digitUVs;
	
	private boolean numberDirty = true;
	
	
	private int[] digitIds = new int[]{
		R.drawable.zero,
		R.drawable.one, R.drawable.two, R.drawable.three,
		R.drawable.four, R.drawable.five, R.drawable.six,
		R.drawable.seven, R.drawable.eight, R.drawable.nine
	};
			
	
	public CounterNode(){
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		vbo = new Vbo(TexturedQuad.VERTEX_COUNT * MAX_DIGITS, Vertex.COLOR_VERTEX_DATA_STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(MAX_DIGITS);
		
		for (int i = 0; i < MAX_DIGITS; i++){
			quads[i] = new BoundTexturedQuad();
			quads[i].bindToVbo(vbo); 
		}
	}
	
	/**
	 * GlThread
	 */
	public void updateNumber(int nuNumber){
		number = nuNumber;
		numberDirty = true;
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.minWidth = 512;
		tc.minHeight = 512;
		tc.mipMapped = false;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		digitPXs = AtlasPainter.drawAtlas(renderContext.resources, digitIds, t, 1);
		digitUVs = AtlasPainter.convertPxToUv(t, digitPXs);
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		if (numberDirty){
			resetNumberQuads(renderContext);
			numberDirty = false;
		}
		
		int indexCount = 0;
		indexBuffer.position(0);
		for (int i = 0; i < MAX_DIGITS; i++){
			indexCount += quads[i].render(renderContext, indexBuffer);
		}
		
		Vertex.renderTexturedTriangles(
				renderContext.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}
	
	private static float[] tmpAnchor = new float[2];
	private void resetNumberQuads(RenderContext renderContext) {
		int tmpNumber = number;
		Vector.set2(tmpAnchor, anchor);

		int digit;
		int i = 0;
		BoundTexturedQuad quad;
		Rect digitPx;
		do {
			digit = tmpNumber % 10;
			quad = quads[i];
			
			quad.setVisible(true);
			quad.setTexCoordsUv(digitUVs[digit]);
			
			// TODO: adapt for other anchorings!
			digitPx = digitPXs[digit];
			quad.positionXY(tmpAnchor[0]-digitPx.width(), tmpAnchor[1], tmpAnchor[0], tmpAnchor[1]-digitPx.height());
			
			tmpAnchor[0] -= digitPx.width();

			tmpNumber = tmpNumber / 10;
			i++;
		} while(i < MAX_DIGITS &&tmpNumber > 0);

		
		for (; i < MAX_DIGITS; i++){
			quads[i].setVisible(false);
		}
	}
	
	public void setAnchor(float x, float y){
		anchor[0] = x;
		anchor[1] = y;
		numberDirty = true;
	}
}
