package com.milesseventh.testing.arkanoid;

public interface DrawingAdapter {
	public void line(Vector from, Vector to, int color);
	public void rect(Vector from, Vector to, int color, boolean fill);
	public void circle(Vector position, float radius, int color, boolean fill);
	public void text(String text, Vector position, int color, boolean alignToCenter, int size);
}
