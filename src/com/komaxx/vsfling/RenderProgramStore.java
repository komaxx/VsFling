package com.komaxx.vsfling;

import com.komaxx.komaxx_gl.RenderProgram;
import com.komaxx.komaxx_gl.scenegraph.ARenderProgramStore;

public class RenderProgramStore extends ARenderProgramStore {
	public static final int SCREEN_TEXTURED = FIRST_CUSTOM_RENDER_PROGRAM;
	public static final int FADE = SCREEN_TEXTURED + 1;
	
	@Override
	protected RenderProgram buildRenderProgram(int i) {
		if (i == SCREEN_TEXTURED) return new ScreenTextureRenderProgram();
		else if (i == FADE) return new FadeRenderProgram();
		throw new RuntimeException("Invalid render program requested: " + i);
	}

	@Override
	protected int getAdditionalProgramsCount() {
		return 2;
	}

}
