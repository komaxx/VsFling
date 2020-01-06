package com.komaxx.vsfling.train;

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
public class Train1Layout {
	private Train1Layout(){}

	public static final Vector2d flingerStartPos = new Vector2d(100, -300);
	public static final Vector2d flingerPlayDirection = new Vector2d(600,flingerStartPos.y);
	public static final GlRect flingerMovementBounds = new GlRect(
			20f, 20f, 1020f, -620);   	// ~ not limited.		

	public static final Vector2d ballPos = 
			new Vector2d(flingerStartPos.x + 150, flingerStartPos.y);

	public static PathSegment[] worldPaths;
	                          

	// build the paths that define the playable area.
	static {
		float wallRimDistance = 10f;
		
		PathSegment surroundingWall = new PathSegment();
		surroundingWall.nodes = new float[][]{
				new float[]{ -2000, -wallRimDistance },
				new float[]{ 1000 - 2 * wallRimDistance, -wallRimDistance },
				new float[]{ 1000 - wallRimDistance, -2*wallRimDistance},
				new float[]{ 1000 - wallRimDistance, -600 + 2*wallRimDistance },
				new float[]{ 1000 - 2 * wallRimDistance, -600 + wallRimDistance },
				new float[]{ -2000, -600 + wallRimDistance }
		};
		surroundingWall.segmentDirection = new float[]{ 500, 500 };
		surroundingWall.width = 30;
		
		worldPaths = new PathSegment[]{ 
				surroundingWall
				};
	}
}
