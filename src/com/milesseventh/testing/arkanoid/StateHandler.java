package com.milesseventh.testing.arkanoid;

public interface StateHandler {
	public void save();
	public boolean load(); // false if no saved state available
	public void resetSave();
}
