package com.milesseventh.testing.arkanoid;

import java.util.ArrayList;
import java.util.Random;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Game {	
	class Ball {
		Vector position, direction;
	}
	
	class CastResult {
		float distance; 
		boolean isHorizontal, isPaddle = false;
		int block = NO_BLOCK;
	}
	
	public static final boolean ANDROID_STATE_HANDLING_OVERRIDE = true;
	public static final float PADDLE_YR        = .1f, 
	                          PADDLE_WR_MAX    = .24f, 
	                          BLOCKS_FIELD_HR  = .45f,
	                          SPEED_MAX        = .5f;
	public static final int BLOCKS_IN_ROW      = 23, 
	                        BLOCKS_IN_COLUMN   = 10, 
	                        LEVELS_MAX         = 17,
	                        RESERVED_BALLS_MAX = 7,
	                        BLOCK_HEALTH_MAX = 4,
	                        NO_BLOCK = -1,
	                        COLOR_BLACK = 0xFF << 24;
	public float paddleX = -1;
	public ArrayList<Ball> balls = new ArrayList<Ball>(), 
	                       toBeRemoved = new ArrayList<Ball>();
	public int[] blocks = new int[BLOCKS_IN_ROW * BLOCKS_IN_COLUMN];
	public int w, h;
	public float maxCollisionDistance;
	public Vector touch = new Vector(-1, 0), BLOCK_SIZE = new Vector();
	public boolean justTouched = false;
	public Random r = new Random();
	public DrawingAdapter gfx;
	public int level = 18, score = 0, reservedBalls = RESERVED_BALLS_MAX / 2, accentColor = packColor(218, 64, 0);
	public float PADDLE_WR, SPEED, BALL_SPAWN_PROB;
	
	///////////////////////////////////////////////////////////////////////////////////
	//Game events
	
	public boolean isGameStarted  = false,
	               levelIsRunning = false,
	               isPaused       = true,
	               isTitleShowing = true,
	               keepRunning    = true,
	               isHighlightEnabled = false,
	               isAutoplayEnabled = false;
	public void onGameStart(int width, int height){
			w = width; 
			h = height;
			maxCollisionDistance = Vector.getVector(w, h).length();
			BLOCK_SIZE.x = w / (float) BLOCKS_IN_ROW;
			BLOCK_SIZE.y = h * BLOCKS_FIELD_HR / (float) BLOCKS_IN_COLUMN;
			
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

	public boolean update(float dt, int w, int h){
		if (!isGameStarted)
			onGameStart(w, h);
		if (!levelIsRunning)
			onLevelStart();

		if (justTouched){
			if (isGameOver())
				keepRunning = false;
			else
				isPaused = false;
		}
		justTouched = false;

		float minPaddlePosition = PADDLE_WR / 2f * w;
		float maxPaddlePosition = w - PADDLE_WR / 2f * w;
		if (touch.x >= 0){
			isTitleShowing = false;
			paddleX = clamp(touch.x, minPaddlePosition, maxPaddlePosition);
			touch.x = -1;
		}
		if (isAutoplayEnabled && !isPaused){
			//Override paddle position
			float opp = findOptimalPaddlePosition();
			if (!Float.isNaN(opp))
				paddleX = clamp(opp, minPaddlePosition, maxPaddlePosition);
		}
		
		if (!isTitleShowing){
			//Blocks rendering
			for (int i = 0; i < BLOCKS_IN_ROW; ++i)
				for (int j = 0; j < BLOCKS_IN_COLUMN; ++j)
					if (blocks[i + j * BLOCKS_IN_ROW] > 0){
						gfx.rect(Vector.getVector(BLOCK_SIZE).multiply(i, j), 
						         Vector.getVector(BLOCK_SIZE).multiply(i + 1, j + 1), 
						         getColorByBlockHealth(blocks[i + j * BLOCKS_IN_ROW]), true);
					}
		}
		
		//Balls processing
		if (!isPaused){
			for (int i = 0; i < balls.size(); ++i){
				Ball dot = balls.get(i);
				CastResult cr = shift(dot, dt * SPEED);
				if (dot.position.y > h)
					toBeRemoved.add(balls.get(i));
				
				//Render kickback ray
				if (cr.isPaddle && (isHighlightEnabled || blocksLeft() <= 17))
					renderKickoff(dot, cr);
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
					isPaused = true;
					save();
				}
			}
		}
	
		if (!isTitleShowing){
			//Balls rendering
			for (int i = 0; i < balls.size(); ++i)
				gfx.circle(balls.get(i).position, 3, COLOR_BLACK, true);
			//Paddle rendering
			gfx.line(Vector.getVector(paddleX - PADDLE_WR * w / 2, h * (1 - PADDLE_YR)), 
			         Vector.getVector(paddleX + PADDLE_WR * w / 2, h * (1 - PADDLE_YR)), 
			         COLOR_BLACK);
		}
		
		//Status bar
		if (isPaused){
			gfx.text(isTitleShowing ? "Seventh Block Kuzushi by Miles Seventh" :
			            (isGameOver() ? ("Game is over. Your score: " + score) : "PAUSED"), 
			         Vector.getVector(w / 2, h / 2), accentColor, true, 21);
			gfx.text("Tap to play", Vector.getVector(5, h - 5), COLOR_BLACK, false, 16);
		} else
			gfx.text(String.format("Level %3d | Balls available %1d | Score %4d | ", level, reservedBalls, score), 
			         Vector.getVector(5, h - 5), COLOR_BLACK, false, 16);
		
		return keepRunning;
	}

	static final float AP_SIDEKICK = .7f; //0 for kickoff from center of the paddle, 1 for it's tip
	static final long AP_PERIOD_MS = 200;
	public float findOptimalPaddlePosition(){
		float minDist = Float.POSITIVE_INFINITY;
		float hit = Float.NaN;
		for (Ball dot: balls)
			if (dot.direction.y > 0){
				CastResult cr = findCollisionDistance(dot, w / 2f, w);
				if (cr.isPaddle && cr.distance < minDist){
					minDist = cr.distance;
					hit = dot.direction.scaled(cr.distance).add(dot.position).x;
				}
			}
		if (Float.isNaN(hit))
			return Float.NaN;
		else {
			float time = System.currentTimeMillis() % (AP_PERIOD_MS * 4);
			if (time < AP_PERIOD_MS * 2)
				return hit + ((PADDLE_WR * (float)w) / 2f) * ((time - AP_PERIOD_MS) / (float)AP_PERIOD_MS * AP_SIDEKICK);
			else
				return hit + ((PADDLE_WR * (float)w) / 2f) * ((time - AP_PERIOD_MS * 3) / (float)AP_PERIOD_MS * AP_SIDEKICK) * -1;
		}
	}

	private Ball temp = new Ball();
	private Vector paddleCollisionPoint = new Vector();
	public void renderKickoff(Ball ball, CastResult cr){
		Vector from = paddleCollisionPoint;
		from.set(Vector.add(ball.position, ball.direction.scaled(cr.distance)));
		temp.position = from;
		temp.direction = Vector.getVector(ball.direction);
		kickoff(temp);
		CastResult kbr = cast(temp);
		
		Vector to = Vector.add(from, Vector.scale(temp.direction, kbr.distance));

		gfx.line(ball.position, Vector.getVector(from.x, h * (1 - PADDLE_YR)), accentColor);
		gfx.line(           to, Vector.getVector(from.x, h * (1 - PADDLE_YR)), accentColor);
		gfx.circle(Vector.getVector(from.x, h * (1 - PADDLE_YR)), 
		           3, accentColor, true);
		
		if (kbr.block != NO_BLOCK){
			Vector bp = this.getBlockPosition(kbr.block, false);
			gfx.rect(bp, Vector.getVector(bp).add(BLOCK_SIZE), packColor(0, 255, 0), false);
		}
	}
	
	public CastResult shift(Ball ball, float distance){
		CastResult cr = cast(ball);
		if (cr.distance >= distance)
			ball.position.add(ball.direction.scaled(distance));
		else {
			ball.position.add(ball.direction.scaled(cr.distance));
			//ON COLLISION
			if (cr.isHorizontal){
				if (cr.isPaddle){
					kickoff(ball);
				} else
					ball.direction.y *= -1;
			} else {
				ball.direction.x *= -1;
			}
			if (cr.block != NO_BLOCK){
				if (--blocks[cr.block] == 0 && r.nextFloat() < BALL_SPAWN_PROB){
					Ball b = new Ball();
					b.direction = generateDirection();
					b.position = new Vector(getBlockPosition(cr.block, true));
					balls.add(b);
				}
				++score;
				
				if (!isAnyBlocksLeft())
					levelIsRunning = false;
			}
			shift(ball, distance - cr.distance);
		}
		return cr;
	}
	
	public Vector kickoff(Ball ball){
		ball.direction.x = ball.position.x - paddleX;
		if (Math.abs(ball.direction.x) < 1)
			ball.direction.x += 2;
		ball.direction.y = -PADDLE_WR * w / 2.2f;
		ball.direction.normalize();
		return ball.direction;
	}
	
	///////////////////////////////////////////////////////////////////////////////////
	//Continuous collision

	public CastResult cast(Ball ball){
		CastResult borderCast = EDToBorders(ball, paddleX, PADDLE_WR * w);
		CastResult brickCast = EDToBricks(ball);
		if (borderCast.distance < brickCast.distance){
			return borderCast;
		}
		return brickCast;
	}
	public CastResult findCollisionDistance(Ball ball, float paddleX, float paddleW){
		CastResult borderCast = EDToBorders(ball, paddleX, paddleW);
		CastResult brickCast = EDToBricks(ball);
		if (borderCast.distance < brickCast.distance){
			return borderCast;
		}
		return brickCast;
	}
	
	public CastResult EDToBorders(Ball ball, float paddleX, float paddleW){
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
			if (xray < min && Math.abs(offsetByED(ball, xray).x - paddleX) < paddleW / 2){
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
		xray = EDtoVerticalWall(ball, i * BLOCK_SIZE.x); //Left
		off = offsetByED(ball, xray).y;
		if (xray < min && off > j * BLOCK_SIZE.y && off < (j + 1) * BLOCK_SIZE.y){
			isH = false;
			min = xray;
		}
		
		xray = EDtoVerticalWall(ball, (i + 1) * BLOCK_SIZE.x);  //Right
		off = offsetByED(ball, xray).y;
		if (xray < min && off > j * BLOCK_SIZE.y && off < (j + 1) * BLOCK_SIZE.y){
			isH = false;
			min = xray;
		}
		
		xray = EDtoHorizontalWall(ball, j * BLOCK_SIZE.y);      //Top
		off = offsetByED(ball, xray).x;
		if (xray < min && off > i * BLOCK_SIZE.x && off < (i + 1) * BLOCK_SIZE.x){
			isH = true;
			min = xray;
		}
		
		xray = EDtoHorizontalWall(ball, (j + 1) * BLOCK_SIZE.y);//Bottom
		off = offsetByED(ball, xray).x;
		if (xray < min && off > i * BLOCK_SIZE.x && off < (i + 1) * BLOCK_SIZE.x){
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
		return Vector.getVector(ball.position).add(ball.direction.scaled(ed));
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

	public void generateBlocks(){
		int seed = r.nextInt();
		float noise;
		for (int i = 0; i < blocks.length; ++i){
			Vector v = getBlockPosition(i, true);
			noise = getNoise(v, seed);
			blocks[i] = (int)Math.round(noise);
			if (blocks[i] > 0){
				blocks[i] = Math.round(
				            	lerp((getCurve(level / (float)LEVELS_MAX) + 1f) / 2f * (noise - .5f) * 2.3f, 
				            	     1, BLOCK_HEALTH_MAX)
				            );
			}
		}
	}

	SimplexNoise sn = new SimplexNoise();
	public float getNoise(Vector v, float seed){
		return remap((float)sn.eval(v.x / w * 6, v.y / h * 6, seed), -1f, 1f, 0f, 1f);
	}
	
	public boolean isAnyBlocksLeft(){
		for (float b: blocks)
			if (b > 0)
				return true;
		return false;
	}
	
	public int blocksLeft(){
		int r = 0;
		for (float b: blocks)
			if (b > 0)
				++r;
		return r;
	}
	
	public Vector getBlockPosition(int id, boolean centered){
		Vector v = Vector.getVector();
		v.x = id % BLOCKS_IN_ROW;
		v.y = (id - v.x) / BLOCKS_IN_ROW;
		if (centered){
			v.x += .5f;
			v.y += .5f;
		}
		v.multiply(BLOCK_SIZE);
		return v;
	}
	
	public int getColorByBlockHealth(int health){
		float percent = (health - 1) / (float)(BLOCK_HEALTH_MAX - 1);
		return packColor(Math.round(lerp(percent, 0, unpackR(accentColor))), 
		                 Math.round(lerp(percent, 0, unpackG(accentColor))), 
		                 Math.round(lerp(percent, 0, unpackB(accentColor))));
	}
	
	public static int packColor(int r, int g, int b){
		r = r & 0xFF;
		g = g & 0xFF;
		b = b & 0xFF;
		return (0xFF << 24) | (r << 16) | (g << 8) | b; 
	}
	
	public static int unpackR(int c){
		return (c >> 16) & 0xFF;
	}
	public static int unpackG(int c){
		return (c >> 8) & 0xFF;
	}
	public static int unpackB(int c){
		return c & 0xFF;
	}
	
	public boolean isGameOver(){
		return reservedBalls == 0 && balls.isEmpty();
	}

	public float clamp(float x, float min, float max){
		return Math.min(Math.max(min, x), max);
	}
	
	public int clamp(int x, int min, int max){
		return Math.min(Math.max(min, x), max);
	}
	
	public float lerp(float percent, float a, float b){
		return a + (b - a) * percent;
	}
	
	public float remap(float in, float a, float b, float m, float n){
		return m + (in - a) / (b - a) * (n - m);
	}
	/////////////////////////
	//State handling
	
	SharedPreferences settings;
	
	public void save(){
		if (!ANDROID_STATE_HANDLING_OVERRIDE)
			return;
		Editor e = settings.edit();
		e.clear();
		e.putInt("LVL", level);
		for (int i = 0; i < blocks.length; ++i)
			e.putInt("BLOCK " + i, blocks[i]);
		e.putInt("B_COUNT", balls.size());
		e.putInt("R_COUNT", reservedBalls);
		e.putInt("SCORE", score);
		e.putBoolean("HIGHLIGHT", isHighlightEnabled);
		for (int i = 0; i < balls.size(); ++i){
			e.putFloat("B " + i + "XP", balls.get(i).position.x);
			e.putFloat("B " + i + "YP", balls.get(i).position.y);
			
			e.putFloat("B " + i + "XD", balls.get(i).direction.x);
			e.putFloat("B " + i + "YD", balls.get(i).direction.y);
		}
		e.commit();
	}
	
	public boolean load(){
		if (!ANDROID_STATE_HANDLING_OVERRIDE)
			return false;
		int l = settings.getInt("LVL", -1);
		
		if (l == -1)
			return false;
		else
			level = l;
		for (int i = 0; i < blocks.length; ++i)
			blocks[i] = settings.getInt("BLOCK " + i, blocks[i]);
		int bcount = settings.getInt("B_COUNT", 0);
		reservedBalls = settings.getInt("R_COUNT", 0);
		score = settings.getInt("SCORE", 0);
		isHighlightEnabled = settings.getBoolean("HIGHLIGHT", false);
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
		if (!ANDROID_STATE_HANDLING_OVERRIDE)
			return;
		Editor e = settings.edit();
		e.clear();
		e.commit();
	}
}
