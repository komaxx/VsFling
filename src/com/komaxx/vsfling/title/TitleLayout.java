package com.komaxx.vsfling.title;

import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.physics.Vector2d;
import com.komaxx.vsfling.PathSegment;

/**
 * Contains constants that define the layout of the Title
 * Screen.
 * 
 * @author Matthias Schicker
 *
 */
public class TitleLayout {
	private TitleLayout(){}

	public static final GlRect flingerBounds = new GlRect(
			375f, -380f,
			625f, -380f);

	public static final Vector2d ballPos = new Vector2d(flingerBounds.centerX(), flingerBounds.top + 60);

	public static PathSegment[] worldPaths;
	                          
	public static float[][] playButt = new float[][]{
		new float[]{ 197f, -101f },
		new float[]{ 336.5f, -34 },
		new float[]{ 294f, 22f },
		new float[]{ 160f, -46.5f },
	};
	
	public static float[][] trainButt = new float[][]{
		new float[]{ 654.5f, -54f },
		new float[]{ 786f, -126 },
		new float[]{ 823.5f, -64f },
		new float[]{ 700f, 3.5f },
	};

	// build the paths that define the playable area.
	static {
		float wallWidth = 50f;
		
		// left wall, play shaft
		PathSegment leftWallSegment = new PathSegment();
		leftWallSegment.nodes = new float[][] { 
				new float[]{ flingerBounds.left, flingerBounds.top },
				new float[]{ 161f, -46.5f },
				};
		leftWallSegment.segmentDirection = new float[]{ 0f, 0f };
		leftWallSegment.width = wallWidth;
		
		// center "V"
		PathSegment vSegment = new PathSegment();
		vSegment.nodes = new float[][] { 
				new float[]{ 316f, -7.5f },
				new float[]{ 478f, -220f },
				new float[]{ 502f, -234f },
				new float[]{ 516f, -222f },
				new float[]{ 696.5f, -2f },
		};
		vSegment.segmentDirection = new float[]{ 500f, 0f };
		vSegment.width = wallWidth;

		// right wall, train shaft
		float[] rightWallStart = new float[]{ flingerBounds.right, flingerBounds.top };
		float[] rightWallStop = new float[]{ 823f, -65f };
		PathSegment rightWallSegment = new PathSegment();
		rightWallSegment.nodes = new float[][] { rightWallStart, rightWallStop };
		rightWallSegment.segmentDirection = new float[]{ 1000f, -1000f };
		rightWallSegment.width = wallWidth;
		
		worldPaths = new PathSegment[]{ 
				leftWallSegment, 
				vSegment, 
				rightWallSegment };
	}
}
