package com.milesseventh.testing.arkanoid.desktop;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.event.MouseInputAdapter;

import com.milesseventh.testing.arkanoid.DrawingAdapter;
import com.milesseventh.testing.arkanoid.Game;
import com.milesseventh.testing.arkanoid.StateHandler;
import com.milesseventh.testing.arkanoid.Vector;

// because horsey doesn't know how to do cross-platform code
class DummyStateHandler implements StateHandler {
	@Override
	public void save() {
	}

	@Override
	public boolean load() {
		return false;
	}

	@Override
	public void resetSave() {
	}
}

class DrawingAPI implements DrawingAdapter {
	private Graphics graphics;

	public void beginGraphics(Graphics graphics) {
		this.graphics = graphics;

		this.graphics.setColor(Color.WHITE);
		this.graphics.fillRect(0, 0, Window.width, Window.height);
	}

	public void endGraphics() {
		this.graphics.dispose();
	}

	public static Color toAWTColor(int color) {
		return new Color(Game.unpackR(color), Game.unpackG(color), Game.unpackB(color));
	}

	@Override
	public void line(Vector from, Vector to, int color) {
		graphics.setColor(toAWTColor(color));
		graphics.drawLine((int) from.x, (int) from.y, (int) to.x, (int) to.y);
	}

	@Override
	public void rect(Vector from, Vector to, int color, boolean fill) {
		float width = (float) Math.ceil(to.x - from.x);
		float height = (float) Math.ceil(to.y - from.y);

		graphics.setColor(toAWTColor(color));

		if (fill)
			graphics.fillRect((int) from.x, (int) from.y, (int) width, (int) height);
		else
			graphics.drawRect((int) from.x, (int) from.y, (int) width, (int) height);
	}

	@Override
	public void circle(Vector position, float radius, int color, boolean fill) {
		graphics.setColor(toAWTColor(color));
		graphics.fillOval(
				(int) (position.x - radius), 
				(int) (position.y - radius),
				
				(int) (radius * 2), 
				(int) (radius * 2)
		);
	}

	@Override
	public void text(String text, Vector position, int color, boolean alignToCenter, int size) {
		Font font = new Font("TimesRoman", Font.PLAIN, size);

		graphics.setColor(toAWTColor(color));
		graphics.setFont(font);

		if (alignToCenter) {
			FontMetrics metrics = graphics.getFontMetrics();
			float x = position.x - metrics.stringWidth(text) * 0.5f;

			graphics.drawString(text, (int) x, (int) position.y);
		} else {
			graphics.drawString(text, (int) position.x, (int) position.y);
		}
	}
}

class InputHandler extends MouseInputAdapter {
	private Game game;

	InputHandler(Game game) {
		this.game = game;
	}

	// thanks, horsey
	public synchronized void setGame(Game game) {
		this.game = game;
	}

	public synchronized void mousePressed(MouseEvent e) {
		this.game.clickEvent(e.getX(), true);
	}

	public synchronized void mouseDragged(MouseEvent e) {
		this.game.clickEvent(e.getX(), false);
	}
}

public class Window {
	static int width = 640;
	static int height = 480;

	private Canvas canvas;
	private JFrame frame;

	private DrawingAPI drawingAPI;
	private Game game;
	private InputHandler inputHandler;

	Window() {
		canvas = new Canvas();
		canvas.setSize(new Dimension(width, height));

		frame = new JFrame("Seventh Block Kuzushi (ported by Liquor Pone)");

		frame.getContentPane().add(canvas);
		frame.setResizable(false);
		frame.pack();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		canvas.createBufferStrategy(2);

		drawingAPI = new DrawingAPI();
		game = new Game(drawingAPI, new DummyStateHandler());
		game.accentColor = Game.packColor(0, 153, 0);
		inputHandler = new InputHandler(game);

		canvas.addMouseListener(inputHandler);
		canvas.addMouseMotionListener(inputHandler);
	}

	public void updateGame(float dt) {
		BufferStrategy bs = canvas.getBufferStrategy();
		drawingAPI.beginGraphics(bs.getDrawGraphics());

		if (!game.update(dt, width, height)) {
			// horsey is very silly and thinks that this is a reasonable way to
			// restart a game
			game = new Game(drawingAPI, new DummyStateHandler());
			game.accentColor = Game.packColor(0, 153, 0);
			inputHandler.setGame(game);
		}

		drawingAPI.endGraphics();
		bs.show();
	}

	public static void main(String[] args) {
		Window window = new Window();
		
		// because horsey likes to use milliseconds instead of seconds
		float dtPerFrameInMilliseconds = 1000.0f / 60.0f;
		long dtPerFrameInNanoseconds = 1000000000 / 60;
		
		long startTime = System.nanoTime();
		long timeInGame = 0;

		while (true) {
			long currentTime = System.nanoTime() - startTime;
			
			while (timeInGame < currentTime) {
				window.updateGame(dtPerFrameInMilliseconds);
				timeInGame += dtPerFrameInNanoseconds;
			}
			
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
