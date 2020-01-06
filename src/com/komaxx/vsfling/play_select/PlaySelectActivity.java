package com.komaxx.vsfling.play_select;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.komaxx.komaxx_gl.BasicSceneGraphRenderView;
import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.GlConfig.ColorDepth;
import com.komaxx.komaxx_gl.GlConfig.DepthBufferBits;
import com.komaxx.vsfling.CompatibilityUtils;
import com.komaxx.vsfling.R;

public class PlaySelectActivity extends Activity {
    private BasicSceneGraphRenderView playSelectView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_empty);
        
       	playSelectView = new PlaySelectView(this, buildGlConfig());
        ((FrameLayout)findViewById(android.R.id.content)).addView(playSelectView);
    }

    private static GlConfig buildGlConfig() {
    	GlConfig ret = new GlConfig(
    			ColorDepth.Color5650, DepthBufferBits.NO_DEPTH_BUFFER, true
    			);
    	
		return ret;
	}

	@Override
    protected void onResume() {
    	super.onResume();
    	CompatibilityUtils.setLightsOutMode(this);
       	playSelectView.onResume();
    }
	
	@Override
	protected void onPause() {
		playSelectView.onPause();
		super.onPause();
	}
    
	@Override
	protected void onDestroy() {
		playSelectView.onDestroy();
		super.onDestroy();
	}
}