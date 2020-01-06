package com.komaxx.vsfling.play_select;

import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.scenegraph.SceneGraph;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.objects.ButtNode;
import com.komaxx.vsfling.objects.ButtTextureProviderNode;

public class StickSpiderButt extends Node {
	private StickSpiderNode spider;
	private ButtNode butt;
	
	private ButtTextureProviderNode buttTextureProvider;
	
	private ISpiderPullHandler handler;
	
	public StickSpiderButt(PlaySelectView psv, float y){
		draws = false;
		handlesInteraction = false;
		transforms = false;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		depthTest = DEACTIVATE;
		blending = ACTIVATE;

		buttTextureProvider = new ButtTextureProviderNode(psv.getResources());
		
		float buttWidth = 85;
		float buttHeight = 160;
		float buttX = 480;
		
		float[][] buttBounds = new float[][]{		
			new float[]{ buttX, y+buttHeight/2 },
			new float[]{ buttX, y-buttHeight/2 },
			new float[]{ buttX+buttWidth, y-buttHeight/2 },
			new float[]{ buttX+buttWidth, y+buttHeight/2 }
		};

		butt = new ButtNode(psv, buttTextureProvider, buttBounds);
		butt.zLevel = PlaySelectView.Z_LEVEL_BALL;
		
		spider = new StickSpiderNode(psv, this, y);
		spider.zLevel = PlaySelectView.Z_LEVEL_BALL;
		spider.stickToButt(butt);
	}

	public void setHandler(ISpiderPullHandler handler) {
		this.handler = handler;
	}

	public void addChildrenToSceneGraph(SceneGraph sceneGraph) {
		sceneGraph.addNode(null, buttTextureProvider);
		sceneGraph.addNode(this, butt);
		sceneGraph.addNode(this, spider);
	}

	public static interface ISpiderPullHandler {
		void handlePulledDistance(float distance);
		void handleRipped();
	}

	public void setPulledDistance(float distance) {
		if (handler != null) handler.handlePulledDistance(distance);
	}

	public void ripped() {
		if (handler != null) handler.handleRipped();
	}
}
