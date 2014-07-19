package info.blockchain.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
//import android.util.Log;

/**
* This class obtains info on the API status of Blockchain.info.
* 
*/
public class Status extends BlockchainAPI	{
	
    private static final String TAG = "Status";

    private boolean isMaintenance = false;
    private int nbServers = 0;

    /**
     * Constructor for this instance.
     * 
     */
    public Status() {
    	
    	strUrl = "https://blockchain.info/status_check";
    }

    /**
     * This method returns true if the API in maintenance mode.
     * 
     * @return boolean
     */
    public boolean isMaintenance()	{
    	return isMaintenance;
    }

    /**
     * This method returns the number of active bitcoind servers.
     * 
     * @return int number of servers
     */
    public int nbServers()	{
    	return nbServers;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{

        try {
    		JSONObject jsonObject = new JSONObject(strData);
    		if(jsonObject != null)	{
    			isMaintenance = jsonObject.getBoolean("maintenance");
    		}
    		JSONArray servers = jsonObject.getJSONArray("bitcoind_servers");
    		if(servers != null)	{
    			nbServers = servers.length();
    		}
    	} catch (JSONException je) {
    		;
    	}

    }

}
