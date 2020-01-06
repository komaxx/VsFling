package com.komaxx.vsfling;

import android.opengl.GLES20;

import com.komaxx.komaxx_gl.RenderProgram;

/**
 * Uses gl_FragCoord as texture coords (instead of explicit ones). 
 * For this to work, you *MUST* set the uniforms xFactor and yFactor! The
 * frag coords are multiplied by these. So, if you use "1" for both, the 
 * texture is scaled to exactly the size of one pixel and you'll only
 * get a monochrome shading.
 * 
 * @author Matthias Schicker
 */
public class ScreenTextureRenderProgram extends RenderProgram {
	public int scaleFactorsUniformHandle = -1;
	
	@Override
	protected void findHandles() {
		vertexXyzHandle = getAttributeHandle("aPosition");
		matrixMVPHandle = getUniformHandle("uMVPMatrix");
		scaleFactorsUniformHandle = getUniformHandle("uScaleFactors");
        vertexAlphaHandle = getAttributeHandle("aAlpha");
	}

	/**
	 * Call this directly before rendering. The RenderProgram needs to be 
	 * already bound.
	 */
	public void setScaleFactors(float x, float y){
		GLES20.glUniform2f(scaleFactorsUniformHandle, x, y);
	}
	
	@Override
	protected String getVertexShader() {
		return alphaTextureVertexShader;
	}

	@Override
	protected String getFragmentShader() {
		return alphaTextureFragmentShader;
	}

	private final String alphaTextureVertexShader = 
			  "uniform mat4 uMVPMatrix;\n"
			
			+ "attribute vec4 aPosition;\n"
			+ "attribute float aAlpha;\n"
			
			+ "varying float vAlpha;\n"

			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "  vAlpha = aAlpha;\n"
			+ "}\n";

	private final String alphaTextureFragmentShader = 
			  "precision highp float;\n"
			
			+ "uniform sampler2D sTexture;\n"
			+ "uniform vec2 uScaleFactors;\n"

			+ "varying float vAlpha;\n"
			
			+ "void main() {\n"
			+ "  vec2 texCoord = vec2(gl_FragCoord.x * uScaleFactors.x,  gl_FragCoord.y * uScaleFactors.y);\n"
			+ "  gl_FragColor = texture2D(sTexture, texCoord);\n"
			+ "  gl_FragColor.a = vAlpha;\n"
			+ "}\n";
}

