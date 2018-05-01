package com.milesseventh.testing.arkanoid;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class GameActivity extends Activity{
	class HarvesterOfEyes extends SurfaceView implements SurfaceHolder.Callback{
		MainLoop ml;
		
		public HarvesterOfEyes(Context context) {
			super(context);
			setFocusable(true);
			getHolder().addCallback(this);
			
			//Input handling
			setOnTouchListener(new OnTouchListener(){
				@SuppressWarnings("deprecation")
				@Override
				public boolean onTouch(View v, MotionEvent me){
					if (ml != null && ml.game != null){
						if (!ml.game.isPaused)
							ml.game.touch.x = me.getX();
						if (me.getAction() == MotionEvent.ACTION_DOWN){
							ml.game.justTouched = true;
						}
						if (me.getAction() == MotionEvent.ACTION_POINTER_2_UP)
							ml.game.isHighlightEnabled = !ml.game.isHighlightEnabled;
					}
					return true;
				}
				
			});
		}

		@Override
		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {}

		@Override
		public void surfaceCreated(SurfaceHolder sh){
			//Start game when drawing surface is available
			ml = new MainLoop();
			ml.sh = sh;
			ml.sp = PreferenceManager.getDefaultSharedPreferences(getContext());
			ml.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0){
			if (ml != null){
				ml.running = false;
				/*if (ml.game != null)
					ml.game.save();*/
			}
		}
	}
	
	public HarvesterOfEyes eyeless;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		eyeless = new HarvesterOfEyes(this);
		setContentView(eyeless);
	}
	
	@Override
	public void onBackPressed(){
		eyeless.ml.game.isPaused = true;
		eyeless.ml.game.save();
	}
}
