package com.komaxx.vsfling.train;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;

import com.komaxx.komaxx_gl.BasicSceneGraphRenderView;
import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.scenegraph.IGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.OrthoCameraNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.RotationNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.StretchBackgroundNode;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.PhysicsSim;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.forcer.Gravity;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.Ball;
import com.komaxx.vsfling.objects.CounterNode;
import com.komaxx.vsfling.objects.SingleFlinger;
import com.komaxx.vsfling.objects.WorldNode;

public class TrainView1 extends BasicSceneGraphRenderView implements IPhysicsView {
	private static final String SHARED_PREFERENCES_NAME = "sharePrefs";
	private static final String HIGHSCORE_STORE_KEY = "highScare";
	
	public static final int Z_LEVEL_CAMERA = 1;
	public static final int Z_LEVEL_TEST = 5;			// on top of everything else
	public static final int Z_LEVEL_GAME_CONTROL = 10;
	public static final int Z_LEVEL_FLINGER = 100;
	public static final int Z_LEVEL_BALL = 200;
	public static final int Z_LEVEL_LEVEL = 300;
	public static final int Z_LEVEL_BACKGROUND_OBJECTS = 900;
	public static final int Z_LEVEL_BACKGROUND = 1000;

	private PhysicsSim physics = new PhysicsSim();
	
	private SingleFlinger flinger;
	
	public TrainView1(Context context, GlConfig glConfig) {
		super(context, glConfig, new RenderProgramStore());

		this.setFocusable(true);
		this.setClickable(true);

		sceneGraph.setBackgroundColor(Color.RED);
		sceneGraph.toggleColorBufferCleaning(false);
		sceneGraph.toggleDepthBufferCleaning(false);
		
		loadHighScore();

		buildGraph();
		
		sceneGraph.getRoot().queueOnceInGlThread(physicsJob);
	}
	
	public int loadHighScore() {
		SharedPreferences preferences = 
				getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		return preferences.getInt(HIGHSCORE_STORE_KEY, 0);
	}

	public void storeHighScore(int nuHighScore){
		SharedPreferences preferences = 
				getContext().getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putInt(HIGHSCORE_STORE_KEY, nuHighScore);
		editor.apply();
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

		StretchBackgroundNode backNode = new StretchBackgroundNode(R.drawable.background);
		backNode.zLevel = Z_LEVEL_BACKGROUND;
		sceneGraph.addNode(chromeCam, backNode);
		
		// //////////////////////////////////////////////////////////////////
		// world
		OrthoCameraNode worldCam = new OrthoCameraNode();
		worldCam.setFixSize(true, 1000.0f, 600.0f);
		sceneGraph.addNode(null, worldCam);

		// ///////////////////////////////////////////////////////////////////
		// flinger
		flinger = new SingleFlinger(this, Train1Layout.flingerStartPos, Train1Layout.flingerPlayDirection);
		flinger.setMovementLimits(Train1Layout.flingerMovementBounds);
		flinger.setName("titleFlinger");
		flinger.zLevel = Z_LEVEL_FLINGER;
		sceneGraph.addNode(worldCam, flinger);
		
		Ball ball = new Ball(this, Train1Layout.ballPos);
		ball.setName("titleBall");
		ball.zLevel = Z_LEVEL_BALL;
		sceneGraph.addNode(worldCam, ball);
		
		// ///////////////////////////////////////////////////////////////////
		// world
		WorldNode worldNode = new WorldNode(this, Train1Layout.worldPaths);
		worldNode.setName("titleWorld");
		worldNode.zLevel = Z_LEVEL_LEVEL;
		sceneGraph.addNode(worldCam, worldNode);
		
		BumperNode bumperNode = new BumperNode(this, 
				new Vector2d(20, 20), new Vector2d(20,-620), 
				new Vector2d(500,-300));
		worldNode.zLevel = Z_LEVEL_BALL;
		sceneGraph.addNode(worldCam, bumperNode);
		
		
		// ///////////////////////////////////////////////////////////////////
		// butts challenge
		ButtChallengeNode buttChallenge = new ButtChallengeNode(this);
		sceneGraph.addNode(worldCam, buttChallenge);
		buttChallenge.start();
		
		
		// ///////////////////////////////////////////////////////////////////
		// game control
		RotationNode rn = new RotationNode();
		rn.setRotation(-90, 0, 0, 1);
		sceneGraph.addNode(chromeCam, rn);
		
		CounterNode scoreNode = new CounterNode();
		scoreNode.updateNumber(123456789);
		scoreNode.zLevel = Z_LEVEL_BACKGROUND_OBJECTS;
		scoreNode.setAnchor(1, 1);
		sceneGraph.addNode(rn, scoreNode);
		
		ChallengeControlNode ccn = new ChallengeControlNode(
				this, buttChallenge, flinger, ball, scoreNode);
		buttChallenge.setChallengeControlNode(ccn);
		sceneGraph.addNode(rn, ccn);

		
		// ///////////////////////////////////////////////////////////////////
		// debug
//		sceneGraph.addNode(worldCam, new ForceVisualizeNode(physics));
		
		// and add gravity
		physics.addCollidable(new Gravity(new Vector2d(-1400, 0)));
	}
	
	@Override
	public PhysicsSim getPhysics() {
		return physics;
	}
}
