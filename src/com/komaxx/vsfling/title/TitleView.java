package com.komaxx.vsfling.title;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.komaxx.komaxx_gl.BasicSceneGraphRenderView;
import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.scenegraph.ADelayedGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.IGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.AnimatedScaleNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.OrthoCameraNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.StretchBackgroundNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.TranslationNode;
import com.komaxx.komaxx_gl.util.InterpolatedValue;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.PhysicsSim;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.forcer.Gravity;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.Ball;
import com.komaxx.vsfling.objects.ButtNode;
import com.komaxx.vsfling.objects.ButtTextureProviderNode;
import com.komaxx.vsfling.objects.FadePlaneNode;
import com.komaxx.vsfling.objects.WorldNode;
import com.komaxx.vsfling.play_select.PlaySelectActivity;
import com.komaxx.vsfling.train.TrainActivity;

public class TitleView extends BasicSceneGraphRenderView implements IPhysicsView {
	private static int OUCH_HIDE_DELAY_MS = 500;
	
	public static final int Z_LEVEL_CAMERA = 1;
	public static final int Z_LEVEL_TEST = 5;			// on top of everything else
	public static final int Z_LEVEL_OUCH = 50;
	public static final int Z_LEVEL_FADE = 70;
	public static final int Z_LEVEL_FLINGER = 100;
	public static final int Z_LEVEL_BALL = 200;
	public static final int Z_LEVEL_LEVEL = 300;
	public static final int Z_LEVEL_BACKGROUND = 1000;

	public static final int CLUSTER_BUTT = 1;
	
	private PhysicsSim physics = new PhysicsSim();
	
	private PullFlinger flinger;
	
	private ButtNode playButt;
	private ButtNode trainButt;
	private FadePlaneNode fadeNode;
	
	private AnimatedScaleNode playOuchScaleNode;
	private AnimatedScaleNode trainOuchScaleNode;

	
	public TitleView(Context context, GlConfig glConfig) {
		super(context, glConfig, new RenderProgramStore());

		this.setFocusable(true);
		this.setClickable(true);

		sceneGraph.setBackgroundColor(Color.RED);
		sceneGraph.toggleColorBufferCleaning(false);
		sceneGraph.toggleDepthBufferCleaning(false);

		buildGraph();
		
		sceneGraph.getRoot().queueOnceInGlThread(physicsJob);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		fadeNode.fadeIn();
	}
	
	private IGlRunnable physicsJob = new IGlRunnable() {
		@Override
		public void run(RenderContext rc) {
			physics.doPhysics();
			sceneGraph.getRoot().queueOnceInGlThread(this);
		}
		
		@Override
		public void abort() {
			KoLog.i(this, "Physics job aborted!");
		}
	};

	private void buildGraph() {
		// //////////////////////////////////////////////////////////////////
		// chrome
		OrthoCameraNode chromeCam = new OrthoCameraNode();
		sceneGraph.addNode(null, chromeCam);

		StretchBackgroundNode backNode = new StretchBackgroundNode(R.drawable.title_background);
		backNode.zLevel = Z_LEVEL_BACKGROUND;
		sceneGraph.addNode(chromeCam, backNode);

		// //////////////////////////////////////////////////////////////////
		// world
		OrthoCameraNode worldCam = new OrthoCameraNode();
		worldCam.setFixSize(true, 1000.0f, 600.0f);
		sceneGraph.addNode(null, worldCam);

		
		// ///////////////////////////////////////////////////////////////////
		// flinger
		flinger = new PullFlinger(this, TitleLayout.flingerBounds);
		flinger.setName("titleFlinger");
		flinger.zLevel = Z_LEVEL_FLINGER;
		sceneGraph.addNode(worldCam, flinger);
		
		Ball ball = new Ball(this, TitleLayout.ballPos);
		ball.setName("titleBall");
		ball.zLevel = Z_LEVEL_BALL;
		sceneGraph.addNode(worldCam, ball);
		
		// ///////////////////////////////////////////////////////////////////
		// world
		WorldNode worldNode = new WorldNode(this, TitleLayout.worldPaths);
		worldNode.setName("titleWorld");
		worldNode.zLevel = Z_LEVEL_LEVEL;
		sceneGraph.addNode(worldCam, worldNode);
		
		// ///////////////////////////////////////////////////////////////////
		// butts
		ButtTextureProviderNode buttTextureProvider = new ButtTextureProviderNode(getResources());
		sceneGraph.addNode(null, buttTextureProvider);
		
		playButt = new ButtNode(this, buttTextureProvider, TitleLayout.playButt);
		playButt.setName("playButt");
		playButt.zLevel = Z_LEVEL_BALL;
		sceneGraph.addNode(worldCam, playButt);

		trainButt = new ButtNode(this, buttTextureProvider, TitleLayout.trainButt);
		trainButt.setName("trainButt");
		trainButt.setConstraintStrengths(0.4f, 0.3f);
		trainButt.zLevel = Z_LEVEL_BALL;
		sceneGraph.addNode(worldCam, trainButt);

		// ///////////////////////////////////////////////////////////////////
		// ouch
		TranslationNode playTranslate = new TranslationNode();
		playTranslate.setTranslation(getResources().getDimension(R.dimen.play_ouch_translate_x), 0, 0);
		sceneGraph.addNode(chromeCam, playTranslate);
		
		playOuchScaleNode = new AnimatedScaleNode();
		playOuchScaleNode.setAnimationSpeed(InterpolatedValue.ANIMATION_DURATION_QUICK);
		playOuchScaleNode.setScaleDirect(0f);
		sceneGraph.addNode(playTranslate, playOuchScaleNode);
		
		OuchNode playOuch = new OuchNode(R.drawable.left_ouch);
		playOuch.zLevel = Z_LEVEL_OUCH;
		sceneGraph.addNode(playOuchScaleNode, playOuch);

		
		TranslationNode trainTranslate = new TranslationNode();
		trainTranslate.setTranslation(getResources().getDimension(R.dimen.train_ouch_translate_x), 0, 0);
		sceneGraph.addNode(chromeCam, trainTranslate);
		
		trainOuchScaleNode = new AnimatedScaleNode();
		trainOuchScaleNode.setAnimationSpeed(InterpolatedValue.ANIMATION_DURATION_QUICK);
		trainOuchScaleNode.setScaleDirect(0f);
		sceneGraph.addNode(trainTranslate, trainOuchScaleNode);
		
		OuchNode trainOuch = new OuchNode(R.drawable.left_ouch);
		trainOuch.zLevel = Z_LEVEL_OUCH;
		sceneGraph.addNode(trainOuchScaleNode, trainOuch);

		// ///////////////////////////////////////////////////////////////////
		// fade
		fadeNode = new FadePlaneNode();
		fadeNode.zLevel = Z_LEVEL_FADE;
		sceneGraph.addNode(worldCam, fadeNode);
		
		// ///////////////////////////////////////////////////////////////////
		// test
//		InteractionTestNode itn = new InteractionTestNode(60, -5);
//		itn.zLevel = Z_LEVEL_OUCH;
//		sceneGraph.addNode(chromeCam, itn);
		
		// ///////////////////////////////////////////////////////////////////
		// debug
//		sceneGraph.addNode(worldCam, new ForceVisualizeNode(physics));
		
		// and add gravity
		physics.addCollidable(new Gravity(new Vector2d(0, -1400)));
	}
	
	@Override
	public PhysicsSim getPhysics() {
		return physics;
	}

	public void buttActivated(ButtNode buttNode) {
		//*
		if (buttNode == playButt) {
			playOuchScaleNode.setAnimationSpeed(InterpolatedValue.ANIMATION_DURATION_QUICK);
			playOuchScaleNode.setScale(1.0f);
			playActivator.postponeTrigger();
			sceneGraph.getRoot().queueOnceInGlThread(playActivator);
		} else if (buttNode == trainButt){
			trainOuchScaleNode.setAnimationSpeed(InterpolatedValue.ANIMATION_DURATION_QUICK);
			trainOuchScaleNode.setScale(1.0f);
			trainActivator.postponeTrigger();
			sceneGraph.getRoot().queueOnceInGlThread(trainActivator);
		}
		fadeNode.fadeOut();
		//*/
	}
	
	private ADelayedGlRunnable playActivator 
	  = new ADelayedGlRunnable(sceneGraph.getRoot(), OUCH_HIDE_DELAY_MS) {
		@Override
		protected void doRun(RenderContext rc) {
			playOuchScaleNode.setAnimationSpeed(InterpolatedValue.ANIMATION_DURATION_SLOW);
			playOuchScaleNode.setScale(0);

			postDelayed(new Runnable(){
				@Override
				public void run(){
					Intent i = new Intent(getContext().getApplicationContext(), PlaySelectActivity.class);
					getContext().startActivity(i);
				}
			}, 400);
		}
	};

	private ADelayedGlRunnable trainActivator 
	  = new ADelayedGlRunnable(sceneGraph.getRoot(), OUCH_HIDE_DELAY_MS) {
		@Override
		protected void doRun(RenderContext rc) {
			trainOuchScaleNode.setAnimationSpeed(InterpolatedValue.ANIMATION_DURATION_SLOW);
			trainOuchScaleNode.setScale(0);
			
			postDelayed(new Runnable(){
				@Override
				public void run(){
					Intent i = new Intent(getContext().getApplicationContext(), TrainActivity.class);
					getContext().startActivity(i);
				}
			}, 400);
		}
	};
}
