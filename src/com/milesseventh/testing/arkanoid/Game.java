package com.milesseventh.testing.arkanoid;

import java.util.ArrayList;
import java.util.Random;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;

public class Game {	
	class Ball {
		public static final int NO_BLOCK = -1;
		Vector position, direction;
		int block = NO_BLOCK;
	}
	
	class CastResult {
		float distance; 
		boolean isHorizontal, isPaddle = false;
	}
	
	public static final float PADDLE_YR        = .1f, 
	                          PADDLE_WR_MAX    = .24f, 
	                          BLOCKS_FIELD_HR  = .45f,
	                          SPEED_MAX        = 10f;
	public static final int BLOCKS_IN_ROW      = 27, 
	                        BLOCKS_IN_COLUMN   = 12, 
	                        LEVELS_MAX         = 17,
	                        RESERVED_BALLS_MAX = 7;
	public float paddleX = -1;
	public ArrayList<Ball> balls = new ArrayList<Ball>(), 
	                       toBeRemoved = new ArrayList<Ball>();
	public float[] blocks = new float[BLOCKS_IN_ROW * BLOCKS_IN_COLUMN];
	public Paint p = new Paint();
	public int w, h;
	public float maxCollisionDistance, BLOCK_W, BLOCK_H;
	public Vector touch = new Vector(-1, 0);
	public Random r = new Random();
	
	public int level = 0, score = 0, reservedBalls = RESERVED_BALLS_MAX / 2;
	public float PADDLE_WR, SPEED, BALL_SPAWN_PROB;
	
	public float getCurve(float x){
		x = clamp(x, 0, 1);
		return (float)Math.sin(x * Math.PI - Math.PI / 2);
	}
	
	public void onLevelStart(){
		++level;
		reservedBalls = Math.min(++reservedBalls, RESERVED_BALLS_MAX);
		float x = Math.min(1, (float) level /  LEVELS_MAX);
		PADDLE_WR = lerp(x, PADDLE_WR_MAX, PADDLE_WR_MAX * .4f);//PADDLE_WR_MAX * (1 - getCurve(x)) * .6f + .4f;
		SPEED = lerp(x, SPEED_MAX * .5f , SPEED_MAX);//SPEED_MAX * getCurve(x) * .6f + .4f;
		//SPEED = 7;
		BALL_SPAWN_PROB = lerp(x, .2f, .01f);//(1 - getCurve(x)) * .4f + .5f;
		generateBlocks();
		
		balls.clear();
		
		Ball ball = new Ball();
		ball.position = new Vector(w / 2, h / 2);
		ball.direction = generateDirection();
		balls.add(ball);
		
		levelIsRunning = true;
	}
	
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
	
	public void generateBlocks(){
		SimplexNoise sn = new SimplexNoise();
		for (int i = 0; i < blocks.length; ++i){
			Vector v = getBlockPosition(i, true);
			blocks[i] = Math.round(sn.eval(v.x / w * 10, v.y / h * 10) + .3f);
		}
	}
	
	public boolean isAnyBlocksLeft(){
		for (float b: blocks)
			if (b > 0)
				return true;
		return false;
	}
	
	public boolean isGameStarted = false,
	               levelIsRunning = false;
	public void onGameStart(Canvas canvas){
			w = canvas.getWidth(); 
			h = canvas.getHeight();
			maxCollisionDistance = Vector.getVector(w, h).length();
			BLOCK_W = w / BLOCKS_IN_ROW;
			BLOCK_H = h * BLOCKS_FIELD_HR / BLOCKS_IN_COLUMN;
			p.setColor(Color.WHITE);
			p.setTextAlign(Align.LEFT);
			p.setTextSize(16);
			paddleX = w / 2;
			isGameStarted = true;
	}

	public void update(Canvas canvas, float dt){
		if (!isGameStarted)
			onGameStart(canvas);
		if (!levelIsRunning)
			onLevelStart();

		if (touch.x >= 0){
			float minPaddlePosition = PADDLE_WR / 2f * w;
			float maxPaddlePosition = w - PADDLE_WR / 2f * w;
			paddleX = clamp(touch.x, minPaddlePosition, maxPaddlePosition);
			touch.x = -1;
		}

		//Balls processing
		for (int i = 0; i < balls.size(); ++i){
			shift(balls.get(i), SPEED);
			if (balls.get(i).position.y > h)
				toBeRemoved.add(balls.get(i));
			canvas.drawCircle(balls.get(i).position.x, balls.get(i).position.y, 3, p);
		}
		for (Ball b: toBeRemoved)
			balls.remove(b);
		toBeRemoved.clear();
		if (balls.isEmpty()){
			if (reservedBalls == 0){
				//game is over
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
		                paddleX + PADDLE_WR * w / 2, h * (1 - PADDLE_YR), p);

		//Blocks rendering
		for (int i = 0; i < BLOCKS_IN_ROW; ++i)
			for (int j = 0; j < BLOCKS_IN_COLUMN; ++j)
				if (blocks[i + j * BLOCKS_IN_ROW] > 0)
					canvas.drawRect(i * BLOCK_W, j * BLOCK_H, 
					                (i + 1) * BLOCK_W, (j + 1) * BLOCK_H, p);
		
		//Status bar
		canvas.drawText(String.format("Level %3d | Balls available %1d | Score %4d | Balls on level %3d | Speed %.2f | PaddleWR %.2f | Prob %.1f",
		                              level, reservedBalls, score, balls.size(), SPEED, PADDLE_WR, BALL_SPAWN_PROB), 5, h, p);
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
					ball.direction.y = -PADDLE_WR * w / 2;
					ball.direction.normalize();
				} else
					ball.direction.y *= -1;
			} else {
				ball.direction.x *= -1;
			}
			if (ball.block != Ball.NO_BLOCK){
				blocks[ball.block] = 0;
				if (r.nextFloat() < BALL_SPAWN_PROB){
					Ball b = new Ball();
					b.direction = generateDirection();
					b.position = getBlockPosition(ball.block, true);
					balls.add(b);
				}
				++score;
				ball.block = Ball.NO_BLOCK;
				
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
						ball.block = i + j * BLOCKS_IN_ROW;
					}
				}
		if (min.distance == maxCollisionDistance)
			ball.block = Ball.NO_BLOCK;
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
	
	public float clamp(float x, float min, float max){
		return Math.min(Math.max(min, x), max);
	}
	
	public float lerp(float percent, float a, float b){
		return a + (b - a) * percent;
	}
}
