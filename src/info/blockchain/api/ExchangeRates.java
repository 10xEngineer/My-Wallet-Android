package info.blockchain.api;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

/**
* This class obtains info on the currencies communicated via http://blockchain.info/ticker.
* 
*/
public class ExchangeRates extends BlockchainAPI {
	
    private static final String TAG = "ExchangeRates";
    
    private static HashMap<String,Double> fxRates = null;
    private static HashMap<String,String> fxSymbols = null;

    /**
    * Currencies handles by http://blockchain.info/ticker
    *
    */
    private static String[] currencies = {
    	   "AUD", "BRL", "CAD", "CHF", "CLP", "CNY", "DKK", "EUR", "GBP", "HKD",
    	   "ISK", "JPY", "KRW", "NZD", "PLN", "RUB", "SEK", "SGD", "THB", "TWD", "USD"
	};
    
    /**
     * Constructor for this instance.
     * 
     */
    public ExchangeRates()	 {
    	
    	strUrl = "https://blockchain.info/ticker";
    	
    	fxRates = new HashMap<String,Double>();
    	fxSymbols = new HashMap<String,String>();
    }

    /**
     * Returns exchange rate for provided currency code.
     * 
     * @param String currency code ISO
     * 
     * @return double last price
     */
    public double getLastPrice(String currency)	 {
    	if(fxRates.get(currency) != null)	 {
    		return fxRates.get(currency);
    	}
    	else	 {
    		return 0.0;
    	}
    }

    /**
     * Returns currency symbol for provided currency code.
     * 
     * @param String currency code ISO
     * 
     * @return String currency symbol
     */
    public String getSymbol(String currency)	 {
    	if(fxSymbols.get(currency) != null)	 {
    		return fxSymbols.get(currency);
    	}
    	else	 {
    		return null;
    	}
    }

    /**
     * Returns String array of currency code
     * 
     * @return String[] array of currency codes
     */
    public String[] getCurrencies()	 {
    	return currencies;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	 {
    	for(int i = 0; i < currencies.length; i++)	 {
        	get(currencies[i]);
    	}
    }

    private void get(String currency)	 {
        try {
    		JSONObject jsonObject = new JSONObject(strData);
    		if(jsonObject != null)	{
    			JSONObject jsonCurr = jsonObject.getJSONObject(currency);
        		if(jsonCurr != null)	{
        			double last_price = jsonCurr.getDouble("last");
        			fxRates.put(currency, Double.valueOf(last_price));
        			String symbol = jsonCurr.getString("symbol");
        			fxSymbols.put(currency, symbol);
        		}
    		}
    	} catch (JSONException je) {
			fxRates.put(currency, Double.valueOf(-1.0));
			fxSymbols.put(currency, null);
    	}
    }

}
