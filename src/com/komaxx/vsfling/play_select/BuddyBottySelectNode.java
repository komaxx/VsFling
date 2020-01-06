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
import com.komaxx.komaxx_gl.scenegraph.interaction.ButtonInteractionInterpreter;
import com.komaxx.komaxx_gl.scenegraph.interaction.ButtonInteractionInterpreter.AButtonElement;
import com.komaxx.komaxx_gl.scenegraph.interaction.InteractionContext;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.komaxx_gl.util.InterpolatedValue;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;

/**
 * Lets the user choose whether she wants to play against
 * a human or a bot; also, when a bot, how strong it should play.
 * 
 * @author Matthias Schicker
 */
public class BuddyBottySelectNode extends Node {
	private ShortBuffer indexBuffer;

	private static final int BOTTY_STRENGTHS = 3;

	private static final float ALPHA_UNSELECTED = 0.5f;
	
	private PlaySelectView playSelectView;
	
	// ////////////////////////////////////////////////////////////
	// rendering
	private BoundTexturedQuad buddyQuad;
	private BoundTexturedQuad bottyQuad;

	private BoundTexturedQuad[] bottyStrengthQuads = new BoundTexturedQuad[BOTTY_STRENGTHS];
	private BoundTexturedQuad[] bottyStrengthSelectedQuads = new BoundTexturedQuad[BOTTY_STRENGTHS];
	
	
	// ////////////////////////////////////////////////////////////
	// interaction
	private int selectedBottyStrengthIndex = 1;

	private ButtonInteractionInterpreter buddyClickInterpreter;
	private ButtonInteractionInterpreter bottyClickInterpreter;
	private ButtonInteractionInterpreter[] bottyStrengthClickInterpreter = 
			new ButtonInteractionInterpreter[BOTTY_STRENGTHS];
	

	public BuddyBottySelectNode(PlaySelectView psv){
		this.playSelectView = psv;
		this.draws = true;
		this.transforms = false;
		this.handlesInteraction = true;

		this.blending = ACTIVATE;
		this.depthTest = DEACTIVATE;
		this.renderProgramIndex = RenderProgramStore.ALPHA_TEXTURED;

		vbo = new Vbo(
				(2 + 2*BOTTY_STRENGTHS) * TexturedQuad.VERTEX_COUNT,
				Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);

		float width = 150;
		float height = 150;
		float x = 750;
		buddyQuad = new BoundTexturedQuad();
		buddyQuad.positionXY(x, -50, x+width, -50 - height);
		buddyQuad.bindToVbo(vbo);
		buddyQuad.setAlphaAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
		
		bottyQuad = new BoundTexturedQuad();
		bottyQuad.positionXY(x, -350, x+width, -350 - height);
		bottyQuad.bindToVbo(vbo);
		bottyQuad.setAlpha(ALPHA_UNSELECTED);
		bottyQuad.setAlphaAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
		
		GlRect pos = new GlRect(710, -530, 760, -580);
		
		for (int i = 0; i < BOTTY_STRENGTHS; i++){
			bottyStrengthQuads[i] = new BoundTexturedQuad();
			bottyStrengthQuads[i].positionXY(pos);
			bottyStrengthQuads[i].bindToVbo(vbo);
			bottyStrengthQuads[i].setAlphaDirect(ALPHA_UNSELECTED);
			bottyStrengthQuads[i].setAlphaAnimationDuration(InterpolatedValue.ANIMATION_DURATION_QUICK);
			
			bottyStrengthSelectedQuads[i] = new BoundTexturedQuad();
			bottyStrengthSelectedQuads[i].positionXY(pos);
			bottyStrengthSelectedQuads[i].bindToVbo(vbo);
			bottyStrengthSelectedQuads[i].setAlphaDirect(0);
			
			pos.moveX(80);
		}

		indexBuffer = TexturedQuad.allocateQuadIndices(2 + 2*BOTTY_STRENGTHS);
		
		// interaction
		buddyClickInterpreter = new ButtonInteractionInterpreter(new AButtonElement(){
			@Override
			public boolean inBounds(float[] xy) {
				return buddyQuad.contains(xy[0], xy[1]);
			}
			@Override
			public void click(InteractionContext ic) {
				changeToBuddy();
			}
		});
		bottyClickInterpreter = new ButtonInteractionInterpreter(new AButtonElement(){
			@Override
			public boolean inBounds(float[] xy) {
				return bottyQuad.contains(xy[0], xy[1]);
			}
			@Override
			public void click(InteractionContext ic) {
				changeToBotty();
			}
		});
		for (int i = 0; i < BOTTY_STRENGTHS; i++){
			bottyStrengthClickInterpreter[i] = new ButtonInteractionInterpreter(
					new BottyStrengthClickInterpreter(i));
		}
		
		changeSelectedBottyStrength(1);
		changeToBuddy();
	}
	
	/**
	 * proxy for the clickhandler for each strength quad
	 * @author Matthias Schicker
	 */
	private class BottyStrengthClickInterpreter extends AButtonElement {
		private int index;

		public BottyStrengthClickInterpreter(int index) {
			this.index = index;
		}
		
		@Override
		public boolean inBounds(float[] xy) {
			return bottyStrengthQuads[index].contains(xy[0], xy[1]);
		}

		@Override
		public void click(InteractionContext ic) {
			changeToBotty();
			changeSelectedBottyStrength(index);
		}
	}

	protected void changeToBotty() {
		playSelectView.setVsBuddy(false);
		buddyQuad.setAlpha(ALPHA_UNSELECTED);
		bottyQuad.setAlpha(1);
		
		for (int i=0; i < BOTTY_STRENGTHS; i++){
			bottyStrengthQuads[i].setAlpha(1);
		}
		bottyStrengthSelectedQuads[selectedBottyStrengthIndex].setAlpha(1);
	}

	protected void changeToBuddy() {
		playSelectView.setVsBuddy(true);
		buddyQuad.setAlpha(1);
		bottyQuad.setAlpha(ALPHA_UNSELECTED);

		for (int i=0; i < BOTTY_STRENGTHS; i++){
			bottyStrengthQuads[i].setAlpha(ALPHA_UNSELECTED);
		}
		bottyStrengthSelectedQuads[selectedBottyStrengthIndex].setAlpha(ALPHA_UNSELECTED);
	}

	private void changeSelectedBottyStrength(int nuIndex) {
		for (int i = 0; i < BOTTY_STRENGTHS; i++){
			bottyStrengthSelectedQuads[i].setAlpha(0);
		}
		bottyStrengthSelectedQuads[nuIndex].setAlpha(1);
		
		selectedBottyStrengthIndex = nuIndex;
		playSelectView.setBottyStrength(nuIndex);
	}

	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		boolean handled = buddyClickInterpreter.onInteraction(interactionContext);
		if (!handled) handled = bottyClickInterpreter.onInteraction(interactionContext);
		for (int i = 0; i < BOTTY_STRENGTHS; i++){
			if (!handled) handled = bottyStrengthClickInterpreter[i].onInteraction(interactionContext);
		}
		return false;
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
		
		Rect[] pxRects = AtlasPainter.drawAtlas(renderContext.resources, 
				new int[]{ 
				R.drawable.buddy,
				R.drawable.botty,
				R.drawable.select_one, R.drawable.select_one_x,
				R.drawable.select_two, R.drawable.select_two_x,
				R.drawable.select_three, R.drawable.select_three_x
		}, t, 0);

		RectF[] uvRects = AtlasPainter.convertPxToUv(t, pxRects);
		
		buddyQuad.setTexCoordsUv(uvRects[0]);
		bottyQuad.setTexCoordsUv(uvRects[1]);
		
		bottyStrengthQuads[0].setTexCoordsUv(uvRects[2]);
		bottyStrengthSelectedQuads[0].setTexCoordsUv(uvRects[3]);

		bottyStrengthQuads[1].setTexCoordsUv(uvRects[4]);
		bottyStrengthSelectedQuads[1].setTexCoordsUv(uvRects[5]);

		bottyStrengthQuads[2].setTexCoordsUv(uvRects[6]);
		bottyStrengthSelectedQuads[2].setTexCoordsUv(uvRects[7]);
	}


	@Override
	public boolean onRender(RenderContext renderContext) {
		indexBuffer.position(0);
		int indexCount = 0;
		indexCount += buddyQuad.render(renderContext, indexBuffer);
		indexCount += bottyQuad.render(renderContext, indexBuffer);
		for (int i = 0; i < BOTTY_STRENGTHS; i++){
			indexCount += bottyStrengthQuads[i].render(renderContext, indexBuffer);
			indexCount += bottyStrengthSelectedQuads[i].render(renderContext, indexBuffer);
		}

		Vertex.renderTexturedTriangles(renderContext.currentRenderProgram, 0, indexCount, indexBuffer);

		return true;
	}
}
