package com.milesseventh.testing.arkanoid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.view.SurfaceHolder;

public class MainLoop extends Thread {
	public SurfaceHolder sh;
	public boolean running = true;
	public Game game = new Game();
	public float dt = 0;
	
	@Override
	public void run(){
		while (running){
			Canvas c = sh.lockCanvas();
			if (c == null){
				running = false;
				break;
			}
			c.drawColor(Color.WHITE);
			if (!game.update(c, dt))
				game = new Game();
			dt = System.nanoTime() - dt;
			sh.unlockCanvasAndPost(c);
		}
	}
}
