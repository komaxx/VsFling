package com.komaxx.vsfling.play;

import com.komaxx.komaxx_gl.math.GlRect;
import com.komaxx.physics.Vector2d;
import com.komaxx.vsfling.PathSegment;

public class Play1Layout {

	public static Vector2d p1FlingerStartPos = new Vector2d(100, -300);
	public static Vector2d p2FlingerStartPos = new Vector2d(900, -300);

	public static GlRect p1FlingerMovementBounds = new GlRect(0,0,300,-600);
	public static GlRect p2FlingerMovementBounds = new GlRect(700,0,1000,-600);

	
	public static Vector2d p1StartBallPos = new Vector2d(p1FlingerStartPos.x+100, -300);
	public static Vector2d p2StartBallPos = new Vector2d(p2FlingerStartPos.x-100, -300);

	public static PathSegment[] worldPaths;
	
	// build the paths that define the playable area.
	static {
		float wallRimDistance = 6f;
		
		PathSegment topWall = new PathSegment();
		topWall.nodes = new float[][]{
				new float[]{ -2000, -wallRimDistance },
				new float[]{ 2000, -wallRimDistance  }
		};
		topWall.segmentDirection = new float[]{ 500, 1000 };
		topWall.bounceStrength = 1;
		topWall.width = 30;

		PathSegment bottomWall = new PathSegment();
		bottomWall.nodes = new float[][]{
				new float[]{ -2000, -600 + wallRimDistance },
				new float[]{ 2000, -600 + wallRimDistance  }
		};
		bottomWall.segmentDirection = new float[]{ 500, -2000 };
		bottomWall.bounceStrength = 1;
		bottomWall.width = 30;
		
		
		worldPaths = new PathSegment[]{ 
				topWall,
				bottomWall
				};
	}

}
