package com.milesseventh.testing.arkanoid.android;

import com.milesseventh.testing.arkanoid.DrawingAdapter;
import com.milesseventh.testing.arkanoid.Game;
import com.milesseventh.testing.arkanoid.StateHandler;
import com.milesseventh.testing.arkanoid.Vector;
import com.milesseventh.testing.arkanoid.Game.Ball;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.view.SurfaceHolder;

public class MainLoop extends Thread {
	public class AndroidStateHandler implements StateHandler{
		SharedPreferences settings;
		
		public void save(){
			Editor e = settings.edit();
			e.clear();
			e.putInt("LVL", game.level);
			for (int i = 0; i < game.blocks.length; ++i)
				e.putInt("BLOCK " + i, game.blocks[i]);
			e.putInt("B_COUNT", game.balls.size());
			e.putInt("R_COUNT", game.reservedBalls);
			e.putInt("SCORE", game.score);
			e.putBoolean("HIGHLIGHT", game.isHighlightEnabled);
			for (int i = 0; i < game.balls.size(); ++i){
				e.putFloat("B " + i + "XP", game.balls.get(i).position.x);
				e.putFloat("B " + i + "YP", game.balls.get(i).position.y);
				
				e.putFloat("B " + i + "XD", game.balls.get(i).direction.x);
				e.putFloat("B " + i + "YD", game.balls.get(i).direction.y);
			}
			e.commit();
		}
		
		public boolean load(){
			int l = settings.getInt("LVL", -1);
			
			if (l == -1)
				return false;
			else
				game.level = l;
			for (int i = 0; i < game.blocks.length; ++i)
				game.blocks[i] = settings.getInt("BLOCK " + i, game.blocks[i]);
			int bcount = settings.getInt("B_COUNT", 0);
			game.reservedBalls = settings.getInt("R_COUNT", 0);
			game.score = settings.getInt("SCORE", 0);
			game.isHighlightEnabled = settings.getBoolean("HIGHLIGHT", false);
			for (int i = 0; i < bcount; ++i){
				Ball b = game.new Ball();
				b.position  = new Vector(settings.getFloat("B " + i + "XP", 0), settings.getFloat("B " + i + "YP", 0));
				b.direction = new Vector(settings.getFloat("B " + i + "XD", 0), settings.getFloat("B " + i + "YD", 0));
				game.balls.add(b);
			}
			
			game.isPaused = true;
			return true;
		}
		
		public void resetSave(){
			Editor e = settings.edit();
			e.clear();
			e.commit();
		}
	};
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
	public AndroidStateHandler ash = new AndroidStateHandler();
	
	public SurfaceHolder sh;
	public SharedPreferences sp;
	public boolean running = true;
	public Game game;
	public long time;
	
	@Override
	public void run(){
		ash.settings = sp;
		game = new Game(androidgfx, ash);
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
				game = new Game(androidgfx, ash);
			}
			sh.unlockCanvasAndPost(c);
		}
	}
}
