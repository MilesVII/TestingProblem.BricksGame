package com.milesseventh.testing.arkanoid;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.view.SurfaceHolder;

public class MainLoop extends Thread {
	public static class AndroidDrawingImplementation implements DrawingAdapter{
		public Canvas canvas;
		public Paint paint = new Paint();
		
		@Override
		public void line(Vector from, Vector to, int color){
			paint.setColor(color);
			canvas.drawLine(from.x, from.y, to.x, to.y, paint);
		}

		@Override
		public void rect(Vector from, Vector to, int color, boolean fill){
			paint.setColor(color);
			paint.setStyle(fill ? Style.FILL : Style.STROKE);
			canvas.drawRect(from.x, from.y, to.x, to.y, paint);
		}

		@Override
		public void circle(Vector position, float radius, int color, boolean fill){
			paint.setColor(color);
			paint.setStyle(fill ? Style.FILL : Style.STROKE);
			canvas.drawCircle(position.x, position.y, radius, paint);
		}

		@Override
		public void text(String text, Vector position, int color, boolean alignToCenter, int size){
			paint.setColor(color);
			paint.setTextAlign(alignToCenter ? Align.CENTER : Align.LEFT);
			paint.setTextSize(size);
			canvas.drawText(text, position.x, position.y, paint);
		}
	}
	public static AndroidDrawingImplementation androidgfx = new AndroidDrawingImplementation();
	
	public SurfaceHolder sh;
	public SharedPreferences sp;
	public boolean running = true;
	public Game game = new Game();
	public long time;
	
	@Override
	public void run(){
		game.settings = sp;
		game.gfx = androidgfx;
		time = System.nanoTime();
		while (running){
			Canvas c = sh.lockCanvas();
			androidgfx.canvas = c;
			if (c == null){
				running = false;
				break;
			}
			c.drawColor(Color.WHITE);
			float dt = (float)(System.nanoTime() - time) / 1000000f;
			time = System.nanoTime();
			if (!game.update(dt, c.getWidth(), c.getHeight())){
				game = new Game();
				game.settings = sp;
				game.gfx = androidgfx;
			}
			sh.unlockCanvasAndPost(c);
		}
	}
}
