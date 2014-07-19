package info.blockchain.wallet.ui;
 
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

//import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import piuk.blockchain.android.util.WalletUtils;

public class ExchangeRateUtil {
	
    private static ExchangeRateUtil instance = null;
    private static double USD = 452.0;

	private ExchangeRateUtil() { ; }

	private static SharedPreferences prefs = null;
    private static SharedPreferences.Editor editor = null;
    
    private static long ts = 0L;

	public static ExchangeRateUtil getInstance(Context ctx) {
		
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        editor = prefs.edit();

		if(instance == null) {
			instance = new ExchangeRateUtil();
		}
		
		getTicker();
		
		return instance;
	}

	private static String getTicker() {

		String fx = null;
		
        try {
        	if(System.currentTimeMillis() - ts > (15 * 60 * 1000)) {
//                get("USD", IOUtils.toString(new URL("http://blockchain.info/ticker"), "UTF-8"));
                get("USD", WalletUtils.getURL("http://blockchain.info/ticker"));
        	}
        }
        catch(MalformedURLException mue) {
        	mue.printStackTrace();
        }
        catch(IOException ioe) {
        	ioe.printStackTrace();
        }
        catch(Exception e) {
        	e.printStackTrace();
        }
        
        return fx;
	}

	public double getUSD() {
		
		if(USD > 0.0) {
			return USD;
		}
		else {
			String s = prefs.getString("USD", "0.1");
			double usd = Double.parseDouble(s);
			return usd;
		}
	}

    private static void get(String currency, String data)	 {
        try {
    		JSONObject jsonObject = new JSONObject(data);
    		if(jsonObject != null)	{
    			JSONObject jsonCurr = jsonObject.getJSONObject(currency);
        		if(jsonCurr != null)	{
        			USD = jsonCurr.getDouble("last");
        			editor.putString("USD", Double.toString(USD));
        			editor.commit();
        			ts = System.currentTimeMillis();
        			Log.d("Blockchain/Bitstamp USD", "" + USD);
        		}
    		}
    	} catch (JSONException je) {
    		;
    	}
    }

}
