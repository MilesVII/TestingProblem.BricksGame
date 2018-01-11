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
			c.drawColor(Color.DKGRAY);    //Clear drawing surface
			game.update(c, dt);           //Updating game state
			dt = System.nanoTime() - dt;
			sh.unlockCanvasAndPost(c);
		}
	}
}
