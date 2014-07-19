package info.blockchain.wallet.ui;
 
import android.content.Context;
import android.graphics.Typeface;

public class TypefaceUtil {
	
    private static Typeface btc_font = null;
    private static Typeface btc_boldfont = null;
    private static Typeface gravity_font = null;
    private static Typeface gravity_boldfont = null;
    private static Typeface gravity_lightfont = null;
    private static Typeface roboto_font = null;
    private static Typeface roboto_boldfont = null;
    private static Typeface roboto_lightfont = null;
    private static TypefaceUtil instance = null;

	private TypefaceUtil() { ; }

	public static TypefaceUtil getInstance(Context ctx) {
		
		if(instance == null) {
			instance = new TypefaceUtil();
	        btc_font = Typeface.createFromAsset(ctx.getAssets(), "DejaVuSans.ttf");
	        btc_boldfont = Typeface.createFromAsset(ctx.getAssets(), "DejaVuSans-Bold.ttf");
	        gravity_font = Typeface.createFromAsset(ctx.getAssets(), "Gravity-Book.ttf");
	        gravity_boldfont = Typeface.createFromAsset(ctx.getAssets(), "Gravity-Bold.ttf");
	        gravity_lightfont = Typeface.createFromAsset(ctx.getAssets(), "Gravity-Light.ttf");
	        roboto_font = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Regular.ttf");
	        roboto_boldfont = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Bold.ttf");
	        roboto_lightfont = Typeface.createFromAsset(ctx.getAssets(), "Roboto-Light.ttf");
		}
		
		return instance;
	}

	public Typeface getBTCTypeface() {
		return btc_font;
	}

	public Typeface getBTCBoldTypeface() {
		return btc_boldfont;
	}

	public Typeface getGravityTypeface() {
		return gravity_font;
	}

	public Typeface getGravityBoldTypeface() {
		return gravity_boldfont;
	}

	public Typeface getGravityLightTypeface() {
		return gravity_lightfont;
	}

	public Typeface getRobotoTypeface() {
		return roboto_font;
	}

	public Typeface getRobotoBoldTypeface() {
		return roboto_boldfont;
	}

	public Typeface getRobotoLightTypeface() {
		return roboto_lightfont;
	}

	public int getBTCSymbol() {
		return 0x0243;
	}

}
