package com.milesseventh.testing.arkanoid;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;

public class MainLoop extends Thread {
	public SurfaceHolder sh;
	public SharedPreferences sp;
	public boolean running = true;
	public Game game = new Game();
	public long time;;
	
	@Override
	public void run(){
		game.settings = sp;
		time = System.nanoTime();
		while (running){
			Canvas c = sh.lockCanvas();
			if (c == null){
				running = false;
				break;
			}
			c.drawColor(Color.WHITE);
			if (!game.update(c, (float)(System.nanoTime() - time) / 1000000f)){
				game = new Game();
				game.settings = sp;
			}
			//Log.d("DT", "" + (int)(System.nanoTime() - time));
			time = System.nanoTime();
			sh.unlockCanvasAndPost(c);
		}
	}
}
