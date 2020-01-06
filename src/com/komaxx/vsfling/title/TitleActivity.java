package com.komaxx.vsfling.title;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.komaxx.komaxx_gl.GlConfig;
import com.komaxx.komaxx_gl.GlConfig.ColorDepth;
import com.komaxx.komaxx_gl.GlConfig.DepthBufferBits;
import com.komaxx.vsfling.R;

public class TitleActivity extends Activity {
    private TitleView titleView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        			WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_title);
        
        titleView = new TitleView(this, buildGlConfig());
        ((FrameLayout)findViewById(R.id.content)).addView(titleView);
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
    	titleView.onResume();
    }
	
	@Override
	protected void onPause() {
		titleView.onPause();
		super.onPause();
	}
    
	@Override
	protected void onDestroy() {
		titleView.onDestroy();
		super.onDestroy();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_vs_fling, menu);
        return true;
    }
}