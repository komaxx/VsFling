package com.komaxx.vsfling.train;

import java.nio.ShortBuffer;

import android.graphics.Rect;
import android.graphics.RectF;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.primitives.TexturedQuad;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.ADelayedGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.scenegraph.interaction.BoundQuadButtonInteractionInterpreter;
import com.komaxx.komaxx_gl.scenegraph.interaction.ButtonInteractionInterpreter;
import com.komaxx.komaxx_gl.scenegraph.interaction.InteractionContext;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.komaxx_gl.util.AtlasPainter;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.Ball;
import com.komaxx.vsfling.objects.CounterNode;
import com.komaxx.vsfling.objects.SingleFlinger;

public class ChallengeControlNode extends Node{
	private static final int GAME_TIME_MS = 10000;

	
	private static final byte STATE_START = 0;
	private static final byte STATE_COUNTDOWN = 1;
	private static final byte STATE_PLAYING = 2;
	private static final byte STATE_RESULT = 3;

	private final TrainView1 tv;
	private final ButtChallengeNode buttControl;
	private final SingleFlinger flinger;
	private final Ball ball;
	private final CounterNode scoreNode;
	
	private int currentScore = 0;
	private int highScore = 0;
	
	private byte state = STATE_START; 
	
	// ////////////////////////////////////////////////////////////
	// rendering
	private ShortBuffer indexBuffer;
	
	private BoundTexturedQuad playButton = new BoundTexturedQuad();
	private BoundTexturedQuad playAgainButton = new BoundTexturedQuad();
	private BoundTexturedQuad results = new BoundTexturedQuad();
	
	private BoundTexturedQuad three = new BoundTexturedQuad();
	private BoundTexturedQuad two = new BoundTexturedQuad();
	private BoundTexturedQuad one = new BoundTexturedQuad();
	private BoundTexturedQuad go = new BoundTexturedQuad();
	
	private BoundTexturedQuad[] quads = new BoundTexturedQuad[]{
			playButton, playAgainButton, results, three, two, one, go
	};
	private int[] quadDrawableIds = new int[]{
		R.drawable.buddy,
		R.drawable.toucher_left,
		R.drawable.botty,
		R.drawable.select_three,
		R.drawable.select_two,
		R.drawable.select_one,
		R.drawable.go
	};

	// ///////////////////////////////////////////////////////////
	// interaction
	private ButtonInteractionInterpreter playButtonInterpreter;
	private ButtonInteractionInterpreter playAgainButtonInterpreter;


	
	public ChallengeControlNode(TrainView1 tv, 
			ButtChallengeNode buttControl, SingleFlinger flinger, Ball ball, CounterNode scoreNode){
		this.tv = tv;
		this.buttControl = buttControl;
		this.flinger = flinger;
		this.ball = ball;
		this.scoreNode = scoreNode;
		
		draws = true;
		handlesInteraction = true;
		transforms = false;
		
		renderProgramIndex = RenderProgramStore.ALPHA_TEXTURED;
		depthTest = DEACTIVATE;
		blending = ACTIVATE;
		zLevel = TrainView1.Z_LEVEL_GAME_CONTROL;
		
		vbo = new Vbo(quads.length * TexturedQuad.VERTEX_COUNT, Vertex.TEXTURED_VERTEX_DATA_STRIDE_BYTES);
		indexBuffer = TexturedQuad.allocateQuadIndices(quads.length);
		
		highScore = tv.loadHighScore();
		
		createQuads();
		createInteractionHandler();
		
		changeState(STATE_START);
	}

	private void createInteractionHandler() {
		playButtonInterpreter = new ButtonInteractionInterpreter(
				new BoundQuadButtonInteractionInterpreter(
						new ButtonInteractionInterpreter.AButtonElement() {
					@Override
					public void click(InteractionContext ic) {
						changeState(STATE_COUNTDOWN);
					}
				}, playButton));
		playAgainButtonInterpreter = new ButtonInteractionInterpreter(
				new BoundQuadButtonInteractionInterpreter(
						new ButtonInteractionInterpreter.AButtonElement() {
					@Override
					public void click(InteractionContext ic) {
						changeState(STATE_COUNTDOWN);
					}
				}, playAgainButton));
		
	}

	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = true;
		tc.minHeight = 1028;
		tc.minWidth = 1028;
		tc.mipMapped = false;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		Rect[] uvPx = AtlasPainter.drawAtlas(renderContext.resources, quadDrawableIds, t, 1);
		RectF[] uv = AtlasPainter.convertPxToUv(t, uvPx);
		
		for (int i = 0; i < quads.length; i++){
			quads[i].setTexCoordsUv(uv[i]);
			quads[i].positionXY(0, 0, uvPx[i].width(), -uvPx[i].height());
			quads[i].setAlphaDirect(1);
		}
	}
	
	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		float midX = renderContext.surfaceHeight/2;
		float midY = renderContext.surfaceWidth/2;
		
		for(int i = 0; i < quads.length; i++){
			float width = quads[i].getPosition().width();
			float height = quads[i].getPosition().height();
			float nuX = midX - width/2;
			float nuY = midY + height/2;
			
			quads[i].positionXY(nuX, nuY, nuX+width, nuY - height);
		}
		
		scoreNode.setAnchor(
				renderContext.surfaceHeight - 20, 
				renderContext.surfaceWidth - 20);
	}
	
	private void createQuads() {
		for(int i = 0; i < quads.length; i++) quads[i].bindToVbo(vbo);
	}

	private void changeState(byte nuState) {
		switch (nuState){
		case STATE_START:
			hideAllQuads();
			buttControl.stop();
			tv.getPhysics().pause();
			playButton.setVisible(true);
			break;
		case STATE_COUNTDOWN:
			flinger.resetPosition(Train1Layout.flingerStartPos, Train1Layout.flingerPlayDirection);
			ball.resetPosition(Train1Layout.ballPos);
			buttControl.clear();
			scoreNode.updateNumber(0);
			currentScore = 0;
			hideAllQuads();
			startCountDown();
			tv.getPhysics().resume();
			break;
		case STATE_PLAYING:
			hideAllQuads();
			buttControl.start();
			queueTimeUp();
			break;
		case STATE_RESULT:
			hideAllQuads();
			results.setVisible(true);
			playAgainButton.setVisible(true);
			tv.getPhysics().pause();
			buttControl.stop();
			updateHighScore();
			break;
		}
		state = nuState;
	}
	
	private void updateHighScore() {
		if (currentScore > highScore){
			highScore = currentScore;
			tv.storeHighScore(highScore);
		}
	}

	private void queueTimeUp() {
		queueOnceInGlThread(new ADelayedGlRunnable(this, GAME_TIME_MS) {
			@Override
			protected void doRun(RenderContext rc) {
				changeState(STATE_RESULT);
			}
		});
	}

	private void hideAllQuads() {
		for (int i = 0; i < quads.length; i++) quads[i].setVisible(false);
	}

	private void startCountDown() {
		queueOnceInGlThread(new CountdownJob(this, 1000, 3));
	}
	private class CountdownJob extends ADelayedGlRunnable {
		private int nextNumber;
		public CountdownJob(Node queueNode, int delayMs, int nextNumber) {
			super(queueNode, delayMs);
			this.nextNumber = nextNumber;
		}

		@Override
		protected void doRun(RenderContext rc) {
			if (nextNumber < 0){
				changeState(STATE_PLAYING);
			} else {
				hideAllQuads();
				BoundTexturedQuad q = three;
				if (nextNumber == 2) q = two;
				else if (nextNumber == 1) q = one;
				else if (nextNumber == 0) q = go;
				
				q.setAlphaDirect(1.2f);
				q.setAlpha(0.6f);
				q.setVisible(true);
				
				queueOnceInGlThread(new CountdownJob(ChallengeControlNode.this, 1000, nextNumber-1));
			}
		}
	}

	@Override
	protected boolean onInteraction(InteractionContext interactionContext) {
		switch (state){
			case STATE_START:
				playButtonInterpreter.onInteraction(interactionContext);
				return true;
			case STATE_COUNTDOWN:
				return false;
			case STATE_PLAYING:
				return false;
			case STATE_RESULT:
				playAgainButtonInterpreter.onInteraction(interactionContext);
				return true;
		}
		return false;
	}
	
	public void addScore(ChallengeButtNode butt, int toAdd){
		currentScore += toAdd;
		buttControl.removeHitButt(butt);
		scoreNode.updateNumber(currentScore);
	}
	
	@Override
	public boolean onRender(RenderContext renderContext) {
		if (state == STATE_PLAYING) return false;
		
		indexBuffer.position(0);
		int indexCount = 0;
		for (int i = 0; i < quads.length; i++){
			indexCount += quads[i].render(renderContext, indexBuffer);
		}
		
		Vertex.renderTexturedTriangles(
				renderContext.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}
}
