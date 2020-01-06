package com.komaxx.vsfling;

import android.opengl.GLES20;

import com.komaxx.komaxx_gl.RenderProgram;

public class FadeRenderProgram extends RenderProgram {
	public int fadePhaseUniformHandle = -1;
	
	@Override
	protected void findHandles() {
		vertexXyzHandle = getAttributeHandle("aPosition");
		matrixMVPHandle = getUniformHandle("uMVPMatrix");
		fadePhaseUniformHandle = getUniformHandle("uFadePhase");
	}

	/**
	 * Call this directly before rendering. The RenderProgram needs to be 
	 * already bound.
	 */
	public void setFadePhase(float phase){
		GLES20.glUniform1f(fadePhaseUniformHandle, phase);
	}
	
	@Override
	protected String getVertexShader() {
		return fadeVertexShader;
	}

	@Override
	protected String getFragmentShader() {
		return fadeFragmentShader;
	}

	private final String fadeVertexShader = 
			  "uniform mat4 uMVPMatrix;\n"
			
			+ "attribute vec4 aPosition;\n"
			
			+ "varying float vAlpha;\n"

			+ "void main() {\n"
			+ "  gl_Position = uMVPMatrix * aPosition;\n"
			+ "}\n";

	private final String fadeFragmentShader = 
			  "precision highp float;\n"
			
			+ "uniform float uFadePhase;\n"

			
			+ "void main() {\n"
			+ "  gl_FragColor = vec4(0.0, 0.0, 0.0, uFadePhase);\n"
			+ "}\n";
}

