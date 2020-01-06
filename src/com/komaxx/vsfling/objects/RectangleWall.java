package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.BoundTexturedQuad;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.constraints.BumperConstraint;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.title.TitleView;

public class RectangleWall extends Node {
	private GlRect rawBounds = new GlRect();
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private BoundTexturedQuad wallQuad = new BoundTexturedQuad();
	
	
	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////
	private BumperConstraint topWall;
	private BumperConstraint rightWall;
	private BumperConstraint bottomWall;
	private BumperConstraint leftWall;

	// //////////////////////////////////////////////////
	// temp stuff
	// //////////////////////////////////////////////////

	private ACollidableGroup physicalWall = new ACollidableGroup() {
		@Override
		public void step(float dt) {
			// nothing
		}
	};

	
	public RectangleWall(TitleView tv, float left, float top, float right, float bottom){
		rawBounds.set(left, top, right, bottom);
		
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		blending = DEACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SIMPLE_TEXTURED;
		
		createPhysicObjects();
		createRenderObjects();
		
		tv.getPhysics().addCollidable(physicalWall);
	}
	
	private void createPhysicObjects() {
		buildConstraints();
	}
	
	
	private void buildConstraints() {
		topWall = new BumperConstraint(new Vector2d(0,0), new Vector2d(10f,0), new Vector2d(0f, 100000f));
		rightWall = new BumperConstraint(new Vector2d(10f,0), new Vector2d(10f,-1000f), new Vector2d(100000f, 0));
		bottomWall = new BumperConstraint(new Vector2d(0,-1000f), new Vector2d(10f,-1000f), new Vector2d(0f, -100000f));
		leftWall = new BumperConstraint(new Vector2d(0,0), new Vector2d(0f,-1000f), new Vector2d(-100000f, 0));
		
		topWall.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		rightWall.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		bottomWall.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
		leftWall.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);

		physicalWall.addChild(topWall);
		physicalWall.addChild(rightWall);
		physicalWall.addChild(bottomWall);
		physicalWall.addChild(leftWall);
	}

	
	private void createRenderObjects() {
		vbo = new Vbo(
				wallQuad.getMaxVertexCount(),
				TexturedVertex.STRIDE_BYTES
		);
		indexBuffer = Vertex.allocateIndices(
				wallQuad.getMaxIndexCount()
		);

		wallQuad.bindToVbo(vbo);
		wallQuad.setTexCoordsUv(0, 0, 1, 1, false);
	}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = false;
		tc.basicColor = Color.GREEN;
		tc.minHeight = 64;
		tc.minWidth = 64;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		Bitmap bmp = Bitmap.createBitmap(64, 64, t.getBitmapConfig());
		Drawable drawable = renderContext.resources.getDrawable(android.R.drawable.gallery_thumb);
		drawable.setBounds(0, 0, 64, 64);
		Canvas c = new Canvas(bmp);
		drawable.draw(c);
		t.update(bmp, 0, 0);
	}
	
	@Override
	public void onSurfaceChanged(RenderContext renderContext) {
		float left = rawBounds.left * renderContext.surfaceWidth;
		float top = rawBounds.top * renderContext.surfaceHeight;
		float right = rawBounds.right * renderContext.surfaceWidth;
		float bottom = rawBounds.bottom * renderContext.surfaceHeight;

		wallQuad.positionXY(left, top, right, bottom);

		topWall.setPosition(left, top, right, top);
		rightWall.setPosition(right, top, right, bottom);
		bottomWall.setPosition(left, bottom, right, bottom);
		leftWall.setPosition(left, top, left, bottom);
	}

	@Override
	public boolean onRender(RenderContext rc) {
		int indexCount = 0;

		indexCount += wallQuad.render(rc, indexBuffer);
		
		Vertex.renderTexturedTriangles(rc.currentRenderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}
}
