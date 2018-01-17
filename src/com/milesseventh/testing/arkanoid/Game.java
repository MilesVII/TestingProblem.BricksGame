package com.milesseventh.testing.arkanoid;

import java.util.ArrayList;
import java.util.Random;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;

public class Game {	
	class Ball {
		Vector position, direction;
	}
	
	class CastResult {
		float distance; 
		boolean isHorizontal, isPaddle = false;
		int block = NO_BLOCK;
	}
	
	public static final float PADDLE_YR        = .1f, 
	                          PADDLE_WR_MAX    = .24f, 
	                          BLOCKS_FIELD_HR  = .45f,
	                          SPEED_MAX        = .5f;
	public static final int BLOCKS_IN_ROW      = 23, 
	                        BLOCKS_IN_COLUMN   = 10, 
	                        LEVELS_MAX         = 17,
	                        RESERVED_BALLS_MAX = 7,
	                        NO_BLOCK = -1;
	public float paddleX = -1;
	public ArrayList<Ball> balls = new ArrayList<Ball>(), 
	                       toBeRemoved = new ArrayList<Ball>();
	public float[] blocks = new float[BLOCKS_IN_ROW * BLOCKS_IN_COLUMN];
	public Paint pain      = new Paint(), 
	             titlePain = new Paint();
	public int w, h;
	public float maxCollisionDistance, BLOCK_W, BLOCK_H;
	public Vector touch = new Vector(-1, 0);
	public boolean justTouched = false;
	public Random r = new Random();
	
	public int level = 0, score = 0, reservedBalls = RESERVED_BALLS_MAX / 2;
	public float PADDLE_WR, SPEED, BALL_SPAWN_PROB;
	
	///////////////////////////////////////////////////////////////////////////////////
	//Game events
	
	public boolean isGameStarted  = false,
	               levelIsRunning = false,
	               isPaused       = true,
	               isTitleShowing = true,
	               keepRunning    = true;
	public void onGameStart(Canvas canvas){
			w = canvas.getWidth(); 
			h = canvas.getHeight();
			maxCollisionDistance = Vector.getVector(w, h).length();
			BLOCK_W = w / BLOCKS_IN_ROW;
			BLOCK_H = h * BLOCKS_FIELD_HR / BLOCKS_IN_COLUMN;
			
			pain.setColor(Color.BLACK);
			pain.setTextAlign(Align.LEFT);
			pain.setTextSize(16);
			
			titlePain.setColor(Color.rgb(218, 64, 0));
			titlePain.setTextAlign(Align.CENTER);
			titlePain.setTextSize(21);
			
			paddleX = w / 2;
			isGameStarted = true;
			
			if (load()){
				levelIsRunning = true;
				isTitleShowing = false;
				loadLevelParameters();
			}
	}
	
	public void onLevelStart(){
		++level;
		reservedBalls = Math.min(++reservedBalls, RESERVED_BALLS_MAX);
		loadLevelParameters();
		generateBlocks();
		
		balls.clear();
		
		Ball ball = new Ball();
		ball.position = new Vector(w / 2, h / 2);
		ball.direction = generateDirection();
		balls.add(ball);
		
		levelIsRunning = true;
		isPaused = true;
		
		if (level != 1)
			save();
	}
	
	public void loadLevelParameters(){
		float x = Math.min(1, (float) level /  LEVELS_MAX);
		PADDLE_WR = lerp(x, PADDLE_WR_MAX, PADDLE_WR_MAX * .4f);
		SPEED = lerp(x, SPEED_MAX * .7f , SPEED_MAX);
		BALL_SPAWN_PROB = lerp(x, .2f, .01f);
	}
	
	public float getCurve(float x){
		x = clamp(x, 0, 1);
		return (float)Math.sin(x * Math.PI - Math.PI / 2);
	}

	public boolean update(Canvas canvas, float dt){
		if (!isGameStarted)
			onGameStart(canvas);
		if (!levelIsRunning)
			onLevelStart();

		if (justTouched)
			if (isGameOver())
				keepRunning = false;
			else
				isPaused = false;
		justTouched = false;
		
		if (touch.x >= 0){
			isTitleShowing = false;
			float minPaddlePosition = PADDLE_WR / 2f * w;
			float maxPaddlePosition = w - PADDLE_WR / 2f * w;
			paddleX = clamp(touch.x, minPaddlePosition, maxPaddlePosition);
			touch.x = -1;
		}

		//Balls processing
		if (!isPaused){
				for (int i = 0; i < balls.size(); ++i){
					shift(balls.get(i), dt * SPEED);
					if (balls.get(i).position.y > h)
						toBeRemoved.add(balls.get(i));
					canvas.drawCircle(balls.get(i).position.x, balls.get(i).position.y, 3, pain);
				}
			for (Ball b: toBeRemoved)
				balls.remove(b);
			toBeRemoved.clear();
			if (balls.isEmpty()){
				if (reservedBalls == 0){
					//game is over
					isPaused = true;
					resetSave();
				} else {
					--reservedBalls;
					Ball ball = new Ball();
					ball.position = new Vector(w / 2, h / 2);
					ball.direction = generateDirection();
					balls.add(ball);
				}
			}
	
			//Paddle rendering
			canvas.drawLine(paddleX - PADDLE_WR * w / 2, h * (1 - PADDLE_YR), 
			                paddleX + PADDLE_WR * w / 2, h * (1 - PADDLE_YR), pain);
		}
		//Blocks rendering
		if (!isTitleShowing)
			for (int i = 0; i < BLOCKS_IN_ROW; ++i)
				for (int j = 0; j < BLOCKS_IN_COLUMN; ++j)
					if (blocks[i + j * BLOCKS_IN_ROW] > 0)
						canvas.drawRect(i * BLOCK_W, j * BLOCK_H, 
						                (i + 1) * BLOCK_W, (j + 1) * BLOCK_H, pain);
		
		//Status bar
		if (isPaused){
			canvas.drawText(isTitleShowing ? "Seventh Block Kuzushi by Miles Seventh" :
			                   (isGameOver() ? ("Game is over. Your score: " + score) : "PAUSED"), 
			                w / 2, h / 2, titlePain);
			canvas.drawText("Tap to play", 5, h - 5, pain);
		} else
			canvas.drawText(String.format("Level %3d | Balls available %1d | Score %4d | ",
			//                            + "Balls on level %3d | Speed %.2f | PaddleWR %.2f | Prob %.1f",
			                              level, reservedBalls, score/*, balls.size(), SPEED, 
			                              PADDLE_WR, BALL_SPAWN_PROB*/), 5, h - 5, pain);
		
		return keepRunning;
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//Continuous collision
	
	public void shift(Ball ball, float distance){
		CastResult cr = findCollisionDistance(ball);
		if (cr.distance >= distance)
			ball.position.add(ball.direction.scale(distance));
		else {
			ball.position.add(ball.direction.scale(cr.distance));
			//ON COLLISION
			if (cr.isHorizontal){
				if (cr.isPaddle){
					ball.direction.x = ball.position.x - paddleX;
					if (Math.abs(ball.direction.x) < 1)
						ball.direction.x += 2;
					ball.direction.y = -PADDLE_WR * w / 2.2f;
					ball.direction.normalize();
				} else
					ball.direction.y *= -1;
			} else {
				ball.direction.x *= -1;
			}
			if (cr.block != NO_BLOCK){
				blocks[cr.block] = 0;
				if (r.nextFloat() < BALL_SPAWN_PROB){
					Ball b = new Ball();
					b.direction = generateDirection();
					b.position = getBlockPosition(cr.block, true);
					balls.add(b);
				}
				++score;
				
				if (!isAnyBlocksLeft())
					levelIsRunning = false;
			}
			shift(ball, distance - cr.distance);
		}
	}
	
	public CastResult findCollisionDistance(Ball ball){
		CastResult borderCast = EDToBorders(ball);
		CastResult brickCast = EDToBricks(ball);
		if (borderCast.distance < brickCast.distance){
			return borderCast;
		}
		return brickCast;
	}
	
	public CastResult EDToBorders(Ball ball){
		CastResult r = new CastResult();
		float min = maxCollisionDistance, xray;
		boolean isH = true;
		
		if (ball.direction.x < 0){
			xray = EDtoVerticalWall(ball, 0); //Left
			if (xray < min){
				isH = false;
				min = xray;
			}
		} else {
			xray = EDtoVerticalWall(ball, w); //Right
			if (xray < min){
				isH = false;
				min = xray;
			}
		}
		
		if (ball.direction.y < 0){
			xray = EDtoHorizontalWall(ball, 0); //Top
			if (xray < min){
				isH = true;
				min = xray;
			}
		} else {
			xray = EDtoHorizontalWall(ball, h * (1 - PADDLE_YR)); //Bottom
			if (xray < min && Math.abs(offsetByED(ball, xray).x - paddleX) < PADDLE_WR * w / 2){
				isH = true;
				r.isPaddle = true;
				min = xray;
			}
		}
		r.distance = min;
		r.isHorizontal = isH;
		return r;
	}
	
	public CastResult EDToBricks(Ball ball){
		CastResult min = new CastResult();
		min.distance = maxCollisionDistance;
		for (int i = 0; i < BLOCKS_IN_ROW; ++i)
			for (int j = 0; j < BLOCKS_IN_COLUMN; ++j)
				if (blocks[i + j * BLOCKS_IN_ROW] > 0){
					CastResult xray = EDToBrick(ball, i, j);
					if (xray.distance < min.distance){
						min = xray;
						min.block = i + j * BLOCKS_IN_ROW;
					}
				}
		return min;
	}
	
	public CastResult EDToBrick(Ball ball, int i, int j){
		float min = maxCollisionDistance, xray, off;
		boolean isH = true;
		xray = EDtoVerticalWall(ball, i * BLOCK_W); //Left
		off = offsetByED(ball, xray).y;
		if (xray < min && off > j * BLOCK_H && off < (j + 1) * BLOCK_H){
			isH = false;
			min = xray;
		}
		
		xray = EDtoVerticalWall(ball, (i + 1) * BLOCK_W);  //Right
		off = offsetByED(ball, xray).y;
		if (xray < min && off > j * BLOCK_H && off < (j + 1) * BLOCK_H){
			isH = false;
			min = xray;
		}
		
		xray = EDtoHorizontalWall(ball, j * BLOCK_H);      //Top
		off = offsetByED(ball, xray).x;
		if (xray < min && off > i * BLOCK_W && off < (i + 1) * BLOCK_W){
			isH = true;
			min = xray;
		}
		
		xray = EDtoHorizontalWall(ball, (j + 1) * BLOCK_H);//Bottom
		off = offsetByED(ball, xray).x;
		if (xray < min && off > i * BLOCK_W && off < (i + 1) * BLOCK_W){
			isH = true;
			min = xray;
		}
		CastResult r = new CastResult();
		r.distance = min;
		r.isHorizontal = isH;
		return r;
	}
	
	public float EDtoVerticalWall(Ball ball, float wallX){
		if (Math.signum(wallX - ball.position.x) == Math.signum(ball.direction.x)){
			return /*ball.direction.length() * */(wallX - ball.position.x) / 
			       ball.direction.x;
		} else
			return maxCollisionDistance;
	}
	
	public float EDtoHorizontalWall(Ball ball, float wallY){
		if (Math.signum(wallY - ball.position.y) == Math.signum(ball.direction.y)){
			return /*ball.direction.length() * */(wallY - ball.position.y) / 
			       ball.direction.y;
		} else
			return maxCollisionDistance;
	}
	
	public Vector offsetByED(Ball ball, float ed){
		return Vector.getVector(ball.position).add(ball.direction.scale(ed));
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//Utils
	
	public Vector generateDirection(){
		Vector t = new Vector();
		float alignment;
		do {
			t = new Vector(r.nextFloat() * 4 - 2, 
			                           r.nextFloat() * 4 - 2);
			t.normalize();
			alignment = Math.abs(Vector.dot(t, Vector.getVector(1, 0)));
		} while(alignment < .2f || alignment > .8f);
		if (t.y < 0)
			t.y *= -1;
		return t;
	}
	
	SimplexNoise sn = new SimplexNoise();
	public void generateBlocks(){
		int seed = r.nextInt();
		for (int i = 0; i < blocks.length; ++i){
			Vector v = getBlockPosition(i, true);
			blocks[i] = Math.round(sn.eval(v.x / w * 10, v.y / h * 10, seed) + .3f);
		}
	}
	
	public boolean isAnyBlocksLeft(){
		for (float b: blocks)
			if (b > 0)
				return true;
		return false;
	}
	
	public Vector getBlockPosition(int id, boolean centered){
		Vector v = new Vector();
		v.x = id % BLOCKS_IN_ROW;
		v.y = (id - v.x) / BLOCKS_IN_ROW;
		if (centered){
			v.x += .5f;
			v.y += .5f;
		}
		v.x *= BLOCK_W;
		v.y *= BLOCK_H;
		return v;
	}
	
	public boolean isGameOver(){
		return reservedBalls == 0 && balls.isEmpty();
	}
	
	public float clamp(float x, float min, float max){
		return Math.min(Math.max(min, x), max);
	}
	
	public float lerp(float percent, float a, float b){
		return a + (b - a) * percent;
	}
	
	/////
	//
	
	SharedPreferences settings;
	
	public void save(){
		Editor e = settings.edit();
		e.clear();
		e.putInt("LVL", level);
		for (int i = 0; i < blocks.length; ++i)
			e.putFloat("BLOCK " + i, blocks[i]);
		e.putInt("B_COUNT", balls.size());
		e.putInt("R_COUNT", reservedBalls);
		e.putInt("SCORE", score);
		for (int i = 0; i < balls.size(); ++i){
			e.putFloat("B " + i + "XP", balls.get(i).position.x);
			e.putFloat("B " + i + "YP", balls.get(i).position.y);
			
			e.putFloat("B " + i + "XD", balls.get(i).direction.x);
			e.putFloat("B " + i + "YD", balls.get(i).direction.y);
		}
		e.commit();
	}
	
	public boolean load(){
		int l = settings.getInt("LVL", -1);
		
		if (l == -1)
			return false;
		else
			level = l;
		for (int i = 0; i < blocks.length; ++i)
			blocks[i] = settings.getFloat("BLOCK " + i, blocks[i]);
		int bcount = settings.getInt("B_COUNT", 0);
		reservedBalls = settings.getInt("R_COUNT", 0);
		score = settings.getInt("SCORE", 0);
		for (int i = 0; i < bcount; ++i){
			Ball b = new Ball();
			b.position  = new Vector(settings.getFloat("B " + i + "XP", 0), settings.getFloat("B " + i + "YP", 0));
			b.direction = new Vector(settings.getFloat("B " + i + "XD", 0), settings.getFloat("B " + i + "YD", 0));
			balls.add(b);
		}
		
		isPaused = true;
		return true;
	}
	
	public void resetSave(){
		Editor e = settings.edit();
		e.clear();
		e.commit();
	}
}
