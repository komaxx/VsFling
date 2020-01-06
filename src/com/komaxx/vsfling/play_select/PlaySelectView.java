package com.komaxx.vsfling.play_select;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.komaxx.komaxx_gl.BasicSceneGraphRenderView;
import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.scenegraph.IGlRunnable;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.OrthoCameraNode;
import com.komaxx.komaxx_gl.scenegraph.basic_nodes.StretchBackgroundNode;
import com.komaxx.komaxx_gl.util.InterpolatedValue;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.physics.PhysicsSim;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.forcer.Gravity;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.FadePlaneNode;
import com.komaxx.vsfling.play.PlayActivity;

public class PlaySelectView extends BasicSceneGraphRenderView implements IPhysicsView {
	public static final int Z_LEVEL_CAMERA = 1;
	public static final int Z_LEVEL_TEST = 5;			// on top of everything else
	public static final int Z_LEVEL_FADE = 25;
	public static final int Z_LEVEL_BUTT_FRAME = 50;
	public static final int Z_LEVEL_FLINGER = 100;
	public static final int Z_LEVEL_BALL = 200;
	public static final int Z_LEVEL_LEVEL = 300;
	public static final int Z_LEVEL_BACKGROUND = 1000;

	private PhysicsSim physics = new PhysicsSim();
	
	private BuddyBottySelectNode buddyBottySelect;
	private ButtFrameNode buttFrame;
	private FadePlaneNode fadeNode;
	
	private boolean vsBuddy = true;
	private byte bottyStrength = 0;
	
	public PlaySelectView(Context context, GlConfig glConfig) {
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

		StretchBackgroundNode backNode = new StretchBackgroundNode(R.drawable.play_select_back);
		backNode.zLevel = Z_LEVEL_BACKGROUND;
		sceneGraph.addNode(chromeCam, backNode);

		// //////////////////////////////////////////////////////////////////
		// world
		OrthoCameraNode worldCam = new OrthoCameraNode();
		worldCam.setFixSize(true, 1000.0f, 600.0f);
		sceneGraph.addNode(null, worldCam);
		
		buttFrame = new ButtFrameNode();
		buttFrame.zLevel = Z_LEVEL_BUTT_FRAME;
		sceneGraph.addNode(worldCam, buttFrame);

		buddyBottySelect = new BuddyBottySelectNode(this);
		buddyBottySelect.zLevel = Z_LEVEL_BUTT_FRAME;
		sceneGraph.addNode(worldCam, buddyBottySelect);
		
		// ///////////////////////////////////////////////////////////////////
		// stick spider butts
		StickSpiderButt arena1 = new StickSpiderButt(this, -94);
		sceneGraph.addNode(worldCam, arena1);
		arena1.addChildrenToSceneGraph(sceneGraph);
		arena1.setHandler(new ArenaHandler(0));

		StickSpiderButt arena2 = new StickSpiderButt(this, -288);
		sceneGraph.addNode(worldCam, arena2);
		arena2.addChildrenToSceneGraph(sceneGraph);
		arena2.setHandler(new ArenaHandler(1));
		
		StickSpiderButt arena3 = new StickSpiderButt(this, -492);
		sceneGraph.addNode(worldCam, arena3);
		arena3.addChildrenToSceneGraph(sceneGraph);
		arena3.setHandler(new ArenaHandler(2));
		
		// ///////////////////////////////////////////////////////////////////
		// fade
		fadeNode = new FadePlaneNode();
		fadeNode.setFadeDuration(InterpolatedValue.ANIMATION_DURATION_NORMAL);
		fadeNode.zLevel = Z_LEVEL_FADE;
		sceneGraph.addNode(worldCam, fadeNode);
		
		
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
		physics.addCollidable(new Gravity(new Vector2d(0, -400)));
	}
	
	private class ArenaHandler implements StickSpiderButt.ISpiderPullHandler{
		private int index;

		public ArenaHandler(int index){
			this.index = index;
		}
		
		@Override
		public void handlePulledDistance(float distance) {
			buttFrame.setAreaSelect(index, distance/100f);
		}

		@Override
		public void handleRipped() {
			buttFrame.setAreaSelect(index, 2f);
			startArena(index);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		fadeNode.fadeIn();
	}
	
	protected void startArena(int i) {
		fadeNode.fadeOut();
		postDelayed(new ArenaStarter(i), 500);
	}
	
	private class ArenaStarter implements Runnable{
		private int arenaIndex;

		public ArenaStarter(int arenaIndex) {
			this.arenaIndex = arenaIndex;
		}

		@Override
		public void run() {
			Intent i = new Intent(getContext().getApplicationContext(), PlayActivity.class);
			i.putExtra(PlayActivity.EXTRA_KEY_ARENA_INDEX, arenaIndex);
			i.putExtra(PlayActivity.EXTRA_KEY_VS_BUDDY, vsBuddy);
			i.putExtra(PlayActivity.EXTRA_KEY_BOTTY_STRENGTH, bottyStrength);
			getContext().startActivity(i);
			
			((Activity)getContext()).finish();
		}
	}

	@Override
	public PhysicsSim getPhysics() {
		return physics;
	}

	public void setVsBuddy(boolean b) {
		vsBuddy = b;
	}
	
	public void setBottyStrength(int strength){
		bottyStrength = (byte) strength;
	}
}
