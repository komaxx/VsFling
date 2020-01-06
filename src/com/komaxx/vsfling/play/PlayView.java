package com.komaxx.vsfling.play;

import android.content.Context;
import android.graphics.Color;

import com.komaxx.komaxx_gl.BasicSceneGraphRenderView;
import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.scenegraph.IGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.OrthoCameraNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.StretchBackgroundNode;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.Mass;
import com.komaxx.physics.PhysicsSim;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollideLeaf;
import com.komaxx.physics.constraints.IBumpTrigger;
import com.komaxx.physics.constraints.TriggerConstraint;
import com.komaxx.physics.forcer.Gravity;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.Ball;
import com.komaxx.vsfling.objects.SingleFlinger;
import com.komaxx.vsfling.objects.WorldNode;

public class PlayView extends BasicSceneGraphRenderView implements IPhysicsView {
	public static final int Z_LEVEL_CAMERA = 1;
	public static final int Z_LEVEL_TEST = 5;			// on top of everything else
	public static final int Z_LEVEL_FLINGER = 100;
	public static final int Z_LEVEL_BALL = 200;
	public static final int Z_LEVEL_LEVEL = 300;
	public static final int Z_LEVEL_BACKGROUND = 1000;

	private PhysicsSim physics = new PhysicsSim();
	
	private SingleFlinger p1Flinger;
	private SingleFlinger p2Flinger;
	
	
	public PlayView(Context context, GlConfig glConfig) {
		super(context, glConfig, new RenderProgramStore());

		this.setFocusable(true);
		this.setClickable(true);

		sceneGraph.setBackgroundColor(Color.RED);
		sceneGraph.toggleColorBufferCleaning(false);
		sceneGraph.toggleDepthBufferCleaning(false);

		buildGraph();
		
		sceneGraph.getRoot().queueOnceInGlThread(physicsJob);
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
		// player flinger
		Vector2d screenCenter = new Vector2d(500, -300);
		/*
		p1Flinger = new DoubleFlinger(this, PlayLayout.p1FlingerStartPos, screenCenter);
		p1Flinger.setMovementLimits(PlayLayout.p1FlingerMovementBounds);
		p1Flinger.setName("p1Flinger");
		p1Flinger.zLevel = Z_LEVEL_FLINGER;
		sceneGraph.addNode(worldCam, p1Flinger);

		p2Flinger = new DoubleFlinger(this, PlayLayout.p2FlingerStartPos, screenCenter);
		p2Flinger.setMovementLimits(PlayLayout.p2FlingerMovementBounds);
		p2Flinger.setName("p2Flinger");
		p2Flinger.zLevel = Z_LEVEL_FLINGER;
		sceneGraph.addNode(worldCam, p2Flinger);
		//*/
		p1Flinger = new SingleFlinger(this, Play1Layout.p1FlingerStartPos, screenCenter);
		p1Flinger.setMovementLimits(Play1Layout.p1FlingerMovementBounds);
		p1Flinger.setName("p1Flinger");
		p1Flinger.zLevel = Z_LEVEL_FLINGER;
		sceneGraph.addNode(worldCam, p1Flinger);

		p2Flinger = new SingleFlinger(this, Play1Layout.p2FlingerStartPos, screenCenter);
		p2Flinger.setMovementLimits(Play1Layout.p2FlingerMovementBounds);
		p2Flinger.setName("p2Flinger");
		p2Flinger.zLevel = Z_LEVEL_FLINGER;
		sceneGraph.addNode(worldCam, p2Flinger);

		
		Ball ball = new Ball(this, Play1Layout.p1StartBallPos);
		ball.setName("playBall");
		ball.zLevel = Z_LEVEL_BALL;
		sceneGraph.addNode(worldCam, ball);
		
		// ///////////////////////////////////////////////////////////////////
		// world
		WorldNode worldNode = new WorldNode(this, Play1Layout.worldPaths);
		worldNode.setName("playWorld");
		worldNode.zLevel = Z_LEVEL_LEVEL;
		sceneGraph.addNode(worldCam, worldNode);
		
		
		// ///////////////////////////////////////////////////////////////////
		// test
//		InteractionTestNode itn = new InteractionTestNode(60, -5);
//		itn.zLevel = Z_LEVEL_OUCH;
//		sceneGraph.addNode(chromeCam, itn);
//		ForceVisualizeNode debugVisualizer = new ForceVisualizeNode(physics);
//		sceneGraph.addNode(worldCam, debugVisualizer);
		
		// ///////////////////////////////////////////////////////////////////
		// some more physics :)
		
		// gravity
		physics.addCollidable(new Gravity(new Vector2d(-1400, 0), new GlRect(0, 0, 500, -600)));
		physics.addCollidable(new Gravity(new Vector2d(1400, 0), new GlRect(500, 0, 1000, -600)));
		
		// lose trigger
		{
			TriggerConstraint p1LostTrigger = new TriggerConstraint(
					new Vector2d(-100, 50), new Vector2d(700, 50), screenCenter);
			p1LostTrigger.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
			p1LostTrigger.setTrigger(new IBumpTrigger() {
				@Override
				public void trigger(ACollideLeaf bc, Mass m) {
					KoLog.e(this, "TRIGGERED 1111");
				}
				@Override
				public float getMinTriggerDistance() { return 3; }
			});
			physics.addCollidable(p1LostTrigger);
		}

		{
			TriggerConstraint p2LostTrigger = new TriggerConstraint(
					new Vector2d(-100, 1050), new Vector2d(700, 1050), screenCenter);
			p2LostTrigger.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
			p2LostTrigger.setTrigger(new IBumpTrigger() {
				@Override
				public void trigger(ACollideLeaf bc, Mass m) {
					KoLog.e(this, "TRIGGERED 2222");
				}
				@Override
				public float getMinTriggerDistance() { return 3; }
			});
			physics.addCollidable(p2LostTrigger);
		}
	}
	
	@Override
	public PhysicsSim getPhysics() {
		return physics;
	}
}
