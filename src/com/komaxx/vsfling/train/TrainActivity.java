package com.komaxx.vsfling.train;

import android.app.Activity;
import android.os.Bundle;
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

public class TrainActivity extends Activity {
	public static final String EXTRA_TRAINING_INDEX_KEY = "trainIndex";
	
    private BasicSceneGraphRenderView trainView;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        			WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_empty);
        
        int trainIndex = getIntent().getIntExtra(EXTRA_TRAINING_INDEX_KEY, -1);
        
        if (trainIndex == 0){
        	trainView = new TrainView1(this, buildGlConfig());
        } else if (trainIndex == 1){
        	// TODO
        } else if (trainIndex == 2){
        	// TODO
        } else {
        	KoLog.w(this, "No trainIndex given, fallback to training 0");
        	trainView = new TrainView1(this, buildGlConfig());
        }
        ((FrameLayout)findViewById(android.R.id.content)).addView(trainView);
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
       	trainView.onResume();
    }
	
	@Override
	protected void onPause() {
		trainView.onPause();
		super.onPause();
	}
    
	@Override
	protected void onDestroy() {
		trainView.onDestroy();
		super.onDestroy();
	}
}