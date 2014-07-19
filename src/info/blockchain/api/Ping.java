package info.blockchain.api;

import android.util.Log;

/**
* This class obtains info on the API status of Blockchain.info.
* 
*/
public class Ping extends BlockchainAPI	{
	
    private static final String TAG = "Ping";

    private boolean isUp = false;

    /**
     * Constructor for this instance.
     * 
     */
    public Ping() {
    	super();
    	
    	strUrl = "https://blockchain.info/ping";
    }

    /**
     * This method returns true if the API has been successfully pinged.
     * 
     * @return boolean
     */
    public boolean isUp()	{
    	return isUp;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{
    	
    	if(strData.contains("OK"))	{
    		isUp = true;
    	}
    	else	{
    		isUp = false;
    	}

    }

}
