package org.metawatch.manager.apps;

import android.content.Context;
import android.graphics.Bitmap;

public interface InternalApp {
	
	public final static int BUTTON_NOT_USED = 0;
	public final static int BUTTON_USED = 1;
	public final static int BUTTON_USED_DONT_UPDATE = 2;
	
	public class AppData {
		public String id;
		public String name;
		
		boolean supportsDigital = false;
		boolean supportsAnalog = false;
	}
	
	public AppData getInfo();
	
	// An app should do any required construction on the first call of activate or update
	public void activate(int watchType);
	public void deactivate(int watchType);
	
	public Bitmap update(Context context, int watchType);
	
	// Returns one of BUTTON_UNUSED, BUTTON_USED or BUTTON_USED_DONT_UPDATE
	public int buttonPressed(Context context, int id);

}
