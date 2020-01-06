package com.komaxx.vsfling.play;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.komaxx.komaxx_gl.BasicSceneGraphRenderView;
import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.GlConfig.ColorDepth;
import com.komaxx.komaxx_gl.GlConfig.DepthBufferBits;
import com.komaxx.komaxx_gl.util.KoLog;
import com.komaxx.vsfling.CompatibilityUtils;
import com.komaxx.vsfling.R;

public class PlayActivity extends Activity {
	/**
	 * Back/home key presses will not be accepted for this time after the last touch-event.
	 * This shall ensure that players don't leave the arena on accident.
	 */
	private static final long BACKHOME_COOLDOWN_TIME_MS = 600;

	public static final String EXTRA_KEY_ARENA_INDEX = "arena";
	public static final String EXTRA_KEY_VS_BUDDY = "vsBuddy";
	public static final String EXTRA_KEY_BOTTY_STRENGTH = "bottyStrength";
	
	private BasicSceneGraphRenderView playView;
	
	private long lastInteractionEventTime = System.currentTimeMillis();

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_empty);
        
       	playView = new PlayView(this, buildGlConfig());
        ((FrameLayout)findViewById(android.R.id.content)).addView(playView);
    }

    private static GlConfig buildGlConfig() {
    	GlConfig ret = new GlConfig(
    			ColorDepth.Color5650, DepthBufferBits.NO_DEPTH_BUFFER, true
    			);
    	
		return ret;
	}

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
    	lastInteractionEventTime = System.currentTimeMillis();
    	return super.dispatchTouchEvent(ev);
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean ret = (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) 
				&& System.currentTimeMillis()-lastInteractionEventTime < BACKHOME_COOLDOWN_TIME_MS;
		
		if (ret){
			KoLog.i(this, "keyDown PRE_HANDLED!");
	    	CompatibilityUtils.setLightsOutMode(this);
			return true;
		}
		
		return super.onKeyDown(keyCode, event);
	}
    
	@Override
    protected void onResume() {
    	super.onResume();
    	CompatibilityUtils.setLightsOutMode(this);
       	playView.onResume();
    }
	
	@Override
	protected void onPause() {
		playView.onPause();
		super.onPause();
	}
    
	@Override
	protected void onDestroy() {
		playView.onDestroy();
		super.onDestroy();
	}
}