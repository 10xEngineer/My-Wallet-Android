package info.blockchain.api;

import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

/**
* This class obtains info on the latest block from Blockchain.info.
* 
*/
public class LatestBlock extends BlockchainAPI	{
	
    private static final String TAG = "LatestBlockReader";

    private long latest_block = -1L;

    /**
     * Constructor for this instance.
     * 
     */
    public LatestBlock() {
    	
    	strUrl = "https://blockchain.info/latestblock";
    }

    /**
     * This method returns the height of the latest block or -1L upon error.
     * 
     * @return long latest block
     */
    public long getLatestBlock()	{
    	return latest_block;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{

        try {
    		JSONObject jsonObject = new JSONObject(strData);
    		if(jsonObject != null)	{
    			latest_block = jsonObject.getLong("height");
    		}
    	} catch (JSONException je) {
    		;
    	}

    }

}
