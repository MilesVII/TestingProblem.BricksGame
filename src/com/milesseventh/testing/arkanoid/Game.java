package com.milesseventh.testing.arkanoid;

import java.util.Random;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Game {	
	public final float PADDLE_YR = .1f, //Relative to screen height position of paddle
	                   PADDLE_WR = .1f; //Relative paddle's width
	public final float SPEED = 4f;      //Just an arbitrary adjusted factor
	public Vector ball,                 //Ball position
	              ballDirection;        //Ball direction
	public float paddleX = -1;          //Position of paddle
	public Paint p = new Paint();
	
	public Vector touch = new Vector(-1, 0);
	public void update(Canvas canvas, float dt){//deltaTime is not used
		float w = canvas.getWidth(), h = canvas.getHeight();
		
		//Initial frame
		if (paddleX < 0){
			p.setColor(Color.WHITE);          //Set drawer color to white
			paddleX = w / 2;                  //Centering the paddle
			ball = new Vector(w / 2, h / 2);  //Centering the ball
			
			//Generating initial direction
			Random r = new Random();
			float alignment;
			do {
				ballDirection = new Vector(r.nextFloat() * 4 - 2, 
				                           r.nextFloat() * 4 - 2);
				ballDirection.normalize();
				alignment = Math.abs(Vector.dot(ballDirection, Vector.getVector(1, 0)));
			} while(alignment < .2f || alignment > .8f); //Prevent generating sloping angles
		}
		
		//On input received
		if (touch.x >= 0){
			float minPaddlePosition = PADDLE_WR / 2f * w;
			float maxPaddlePosition = w - PADDLE_WR / 2f * w;
			paddleX = clamp(touch.x, minPaddlePosition, maxPaddlePosition);
			touch.x = -1;
		}
		shift(ballDirection.length() * SPEED, w, h);
		
		//Reset ball when losing the game
		if (ball.y > h)
			ball.y = 5;
		
		//Drawing ball and paddle
		canvas.drawCircle(ball.x, ball.y, 3, p);
		canvas.drawLine(paddleX - PADDLE_WR * w / 2, h * (1 - PADDLE_YR), 
		                paddleX + PADDLE_WR * w / 2, h * (1 - PADDLE_YR), p);
	}
	
	public void shift(float distance, float w, float h){
		float est = findEstimate(w, h);
		if (est > distance)
			ball.add(ballDirection.scale(distance));
		else {
			ball.add(ballDirection.scale(est));
			if (Math.abs(ball.x - w) < 1 || Math.abs(ball.x) < 1){
				//Vertical collision
				ballDirection.x *= -1;
			} else {
				//Horizontal collision
				ballDirection.y *= -1;
			}
			shift(distance - est, w, h);
		}
	}
	
	public float findEstimate(float w, float h){
		//if (Math.abs(Vector.dot(dir, Vector.getVector(0, 1))) == 1)//EXCEPTIONAL
		float minEstimate;
		
		if (ballDirection.x < 0){
			//Check left border
			minEstimate = ballDirection.length() * ball.x / 
			              Vector.project(ballDirection, Vector.getVector(-1, 0));
		} else {
			//Check right border
			minEstimate = ballDirection.length() * (w - ball.x) /
			              Vector.project(ballDirection, Vector.getVector(1, 0));
		}
		minEstimate = Math.abs(minEstimate);
		if (ballDirection.y < 0){
			//Check top border
			minEstimate = Math.min(minEstimate, 
			                       ballDirection.length() * (ball.y) 
			                       / Vector.project(ballDirection, Vector.getVector(0, -1)));
		} else {
			//Check paddle
			float tEst = ballDirection.length() * (h * (1 - PADDLE_YR) - ball.y) / Vector.project(ballDirection, Vector.getVector(0, 1));
			tEst = Math.abs(tEst);
			if (tEst < minEstimate){
				Vector collision = Vector.getVector(ball);
				collision.add(ballDirection.scale(tEst));
				if(Math.abs(collision.x - paddleX) < PADDLE_WR * w / 2){
					minEstimate = tEst;
				}
			}
		}
		
		return Math.abs(minEstimate);
	}
	
	public float clamp(float x, float min, float max){
		return Math.min(Math.max(min, x), max);
	}
}
