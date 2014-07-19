package info.blockchain.wallet.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.Display;
import android.graphics.Point;
//import android.util.Log;

public class DeviceUtil {
	
	private static DeviceUtil instance = null;
	
	private static Context context = null;
	
	private static float REG_RES = 2.0f;

	private static float scale = 0.0f;

	private DeviceUtil() { ; }

	public static DeviceUtil getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
			Resources resources = context.getResources();
			scale = resources.getDisplayMetrics().density;
			instance = new DeviceUtil();
		}
		
		return instance;
	}

	public float getScale() {
		return scale;
	}

	public boolean isHiRes() {
		return (scale > REG_RES);
	}
	
	public boolean isSmallScreen() {
		 Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
		 Point size = new Point();
		 display.getSize(size);
//		 int width = size.x;
		 int height = size.y;
		 
		 if(height <= 800) {
			 return true;
		 }
		 else {
			 return false;
		 }
	}

}
