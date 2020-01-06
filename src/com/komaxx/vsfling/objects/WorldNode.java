package com.komaxx.vsfling.objects;

import java.nio.ShortBuffer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.komaxx.komaxx_gl.RenderContext;
import com.komaxx.komaxx_gl.bound_meshes.Bound2DPathStrip;
import com.komaxx.komaxx_gl.bound_meshes.Vbo;
import com.komaxx.komaxx_gl.math.Vector;
import com.komaxx.komaxx_gl.primitives.TexturedVertex;
import com.komaxx.komaxx_gl.primitives.Vertex;
import com.komaxx.komaxx_gl.scenegraph.Node;
import com.komaxx.komaxx_gl.texturing.Texture;
import com.komaxx.komaxx_gl.texturing.TextureConfig;
import com.komaxx.physics.Vector2d;
import com.komaxx.physics.collide.ACollidableGroup;
import com.komaxx.physics.constraints.BumperConstraint;
import com.komaxx.vsfling.ForceMasks;
import com.komaxx.vsfling.IPhysicsView;
import com.komaxx.vsfling.PathSegment;
import com.komaxx.vsfling.R;
import com.komaxx.vsfling.RenderProgramStore;
import com.komaxx.vsfling.ScreenTextureRenderProgram;

public class WorldNode extends Node {
	private static final int textureSize = 256;
	
	// //////////////////////////////////////////////////
	// rendering
	// //////////////////////////////////////////////////
	private ShortBuffer indexBuffer;
	private Bound2DPathStrip[] strips;
	
	
	// //////////////////////////////////////////////////
	// physics
	// //////////////////////////////////////////////////

	// //////////////////////////////////////////////////
	// temp stuff
	// //////////////////////////////////////////////////

	private ACollidableGroup physicalWorld = new ACollidableGroup() {
		@Override
		public void step(float dt) {
			// nothing
		}
	};

	
	public WorldNode(IPhysicsView tv, PathSegment[] worldPaths){
		draws = true;
		handlesInteraction = false;
		transforms = false;
		
		blending = ACTIVATE;
		depthTest = DEACTIVATE;
		
		renderProgramIndex = RenderProgramStore.SCREEN_TEXTURED;

		createPhysicObjects(worldPaths);
		createRenderObjects(worldPaths);
		
		tv.getPhysics().addCollidable(physicalWorld);
	}
	
	private void createPhysicObjects(PathSegment[] worldPaths) {
		Vector2d v1 = new Vector2d();
		Vector2d v2 = new Vector2d();
		
		Vector2d forceTargetPoint = new Vector2d();
		float[] tmp = new float[2];
		float[] tmp2 = new float[2];
		float[] tmp3 = new float[2];
		
		for (PathSegment segment : worldPaths){
			Vector.aToB2(tmp, segment.nodes[0], segment.segmentDirection);
			Vector.aToB2(tmp2, segment.nodes[0], segment.nodes[1]);
			Vector.normal2(tmp3, tmp2);
			
			boolean reverseNormal = Vector.dotProduct2(tmp, tmp3) > 0;
			
			int l = segment.nodes.length;
			for (int i = 1; i < l; i++){
				Vector.aToB2(tmp, segment.nodes[i-1], segment.nodes[i]);
				Vector.normal2(tmp2, tmp);
				if (reverseNormal) Vector.invert2(tmp2);

				Vector.addBtoA2(tmp2, segment.nodes[i-1]);
				
				v1.set(segment.nodes[i-1][0], segment.nodes[i-1][1]);
				v2.set(segment.nodes[i][0], segment.nodes[i][1]);
				forceTargetPoint.set(tmp2);
				BumperConstraint bc = new BumperConstraint(v1, v2, forceTargetPoint);
				bc.strength = segment.bounceStrength;
				bc.setExertForceMask(ForceMasks.BALL_PUSH_FORCE);
				physicalWorld.addChild(bc);
			}
		}
	}
	
	private void createRenderObjects(PathSegment[] worldPaths) {
		int vertexCount = 0;
		int indexCount = 0;
		
		int  stripCount = worldPaths.length;
		strips = new Bound2DPathStrip[stripCount];
		for (int i = 0; i < stripCount; i++){
			PathSegment nowSegment = worldPaths[i];
			strips[i] = new Bound2DPathStrip(nowSegment.nodes, nowSegment.segmentDirection, nowSegment.width);
			
			vertexCount += strips[i].getMaxVertexCount();
			indexCount += strips[i].getMaxIndexCount();
		}
		
		vbo = new Vbo(vertexCount, TexturedVertex.STRIDE_BYTES);
		indexBuffer = Vertex.allocateIndices(indexCount);

		for (int i = 0; i < stripCount; i++) strips[i].bindToVbo(vbo);
}
	
	@Override
	protected void onSurfaceCreated(RenderContext renderContext) {
		TextureConfig tc = new TextureConfig();
		tc.alphaChannel = false;
		tc.basicColor = Color.GREEN;
		tc.minHeight = textureSize;
		tc.minWidth = textureSize;
		tc.edgeBehavior = TextureConfig.EDGE_REPEAT;
		tc.nearestMapping = true;
		
		Texture t = new Texture(tc);
		t.create(renderContext);
		textureHandle = t.getHandle();
		
		Bitmap bmp = Bitmap.createBitmap(t.getWidth(), t.getHeight(), t.getBitmapConfig());
		Drawable drawable = renderContext.resources.getDrawable(R.drawable.pencil_scratches);
		drawable.setBounds(0, 0, t.getWidth(), t.getHeight());
		Canvas c = new Canvas(bmp);
		drawable.draw(c);
		t.update(bmp, 0, 0);
	}
	
	@Override
	public boolean onRender(RenderContext rc) {
		int indexCount = 0;

		ScreenTextureRenderProgram renderProgram = (ScreenTextureRenderProgram) rc.currentRenderProgram;
		renderProgram.setScaleFactors(1f / textureSize, 1f / textureSize);
		
		int stripCount = strips.length;
		for (int i = 0; i < stripCount; i++) indexCount += strips[i].render(rc, indexBuffer);
		
		Vertex.renderTexturedTriangles(renderProgram, 0, indexCount, indexBuffer);
		
		return true;
	}
}
