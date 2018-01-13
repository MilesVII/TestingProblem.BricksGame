package com.milesseventh.testing.arkanoid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
				@Override
				public boolean onTouch(View v, MotionEvent me){
					if (ml != null){
						ml.game.touch.x = me.getX();
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
			ml.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0){
			ml.running = false;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(new HarvesterOfEyes(this));
	}
}
