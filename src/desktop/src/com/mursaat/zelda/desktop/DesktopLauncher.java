package com.mursaat.zelda.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.mursaat.zelda.Zelda;

public class DesktopLauncher 
{
	public static void main (String[] arg)
	{
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		config.setTitle("Zelda");
		config.setWindowedMode(864, 480);
		config.setForegroundFPS(60);
		new Lwjgl3Application(new Zelda(), config);
	}
}
