package com.komaxx.vsfling.objects;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.texturing.TextureStore;
import com.komaxx.komaxx_gl.util.RenderUtil;
import com.komaxx.physics.Vector2d;
import com.komaxx.vsfling.R;

/**
 * Given to each ButtNode to provide them with (shared) textures. Does
 * not otherwise interact with the user (no drawing, no interaction).
 * 
 * @author Matthias Schicker
 */
public class ButtTextureProviderNode extends Node {
	private static final int ALPHA_THRESHOLD = 15;

	private static final int BIG_STEPWIDTH = 8;
	
	
	private RectF buttBoundsUvCoords = new RectF();
	private Texture texture;
	private Bitmap buttBitmap;
	private Vector2d[] buttLine;
	
	public ButtTextureProviderNode(Resources res){
		draws = false;
		transforms = false;
		handlesInteraction = false;
		
		depthTest = DONT_CARE;
		blending = DONT_CARE;
		
		zLevel = 2;

		buttBitmap = TextureStore.getBitmap(res, R.drawable.butt1);
	}
	
	public int getTextureHandle(){
		return textureHandle;
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.basicColor = Color.TRANSPARENT;
		tc.minWidth = buttBitmap.getWidth();
		tc.minHeight = buttBitmap.getHeight();
		tc.mipMapped = false;
		
		texture = new Texture(tc);
		texture.create(renderContext);
		textureHandle = texture.getHandle();

		renderContext.bindTexture(texture.getHandle());
		texture.update(buttBitmap, 0, 0);
		
		buttBoundsUvCoords.set(0, 0, 
				(float)buttBitmap.getWidth()/(float)texture.getWidth(), 
				(float)buttBitmap.getHeight()/(float)texture.getHeight());
	}

	public RectF getButtTextureUvCoords() {
		return buttBoundsUvCoords;
	}
	
	/**
	 * Delivers the buttLine (the outline of the cheeks) in butt coords,
	 * i.e., in coords relative to the lower left (0|0) to upper right (1|1)
	 * of the butt.
	 */
	public Vector2d[] getButtLine(){
		if (buttLine == null){
			recomputeButtLine();
		}
		return buttLine;
	}

	private void recomputeButtLine() {
		buttLine = new Vector2d[ButtNode.BUTT_MASSES];
		
		float xPadding = 7;
		
		float bitmapWidth = (float)buttBitmap.getWidth();
		float bitmapHeight = (float)buttBitmap.getHeight();
		float pxDelta = 
				(bitmapWidth - 2*xPadding)
				/ 
				(ButtNode.BUTT_MASSES-1);
		
		float x = xPadding;
		for (int i = 0; i < ButtNode.BUTT_MASSES; i++){
			float y = findFirstOpaquePixel((int) x);

			// convert to butt coords!
			buttLine[i] = new Vector2d(
					(float)x/bitmapWidth,
					(bitmapHeight-y)/bitmapHeight);
			
			x += pxDelta;
		}

	}

	private float findFirstOpaquePixel(int x) {
		int l = buttBitmap.getHeight();
		int ret = l-1;
		// first: look in big steps 
		for (; ret >= 0; ret -= BIG_STEPWIDTH){
			if (Color.alpha(buttBitmap.getPixel(x, ret)) > ALPHA_THRESHOLD){
				break;
			}
		}
		// now trace back in smaller steps
		for (; ret < l; ret++){
			if (Color.alpha(buttBitmap.getPixel(x, ret)) < ALPHA_THRESHOLD){
				break;
			}
		}
		
		// one pixel offset to not cut off the texture too much
		ret = RenderUtil.clamp(ret+2, ret, l);
		
		return ret;
	}
}
