package info.blockchain.api;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

/**
* This class obtains info on a Bitcoin wallet address from Blockchain.info.
* 
*/
public class Transaction extends BlockchainAPI {

    private static final String TAG = "Transaction";

    public class xPut {
    	public long value;
    	public String addr;
    	public String addr_tag;
    }

	private long height = -1L;
	private String hash = null;
	private long time = -1L;
	private long result = 0L;
	private long fee = 0L;
	private String relayed_by = null;

    private ArrayList<xPut> inputs = null;
    private ArrayList<xPut> outputs = null;
    private HashMap<String,Long> totalValues = null;
    private HashMap<String,Long> inputValues = null;
    private HashMap<String,Long> outputValues = null;

    /**
     * Constructor for this instance.
     * 
     * @param String data returned from Blockchain.info
     */
    public Transaction(String hash) {

    	strUrl = "https://blockchain.info/tx/" + hash + "?format=json";
		Log.d(TAG, strUrl);

    	inputs = new ArrayList<xPut>();
    	outputs = new ArrayList<xPut>();
    	totalValues = new HashMap<String,Long>();
    	inputValues = new HashMap<String,Long>();
    	outputValues = new HashMap<String,Long>();
    }

    public String getHash() {
    	return hash;
    }

    public long getResult() {
    	return result;
    }

    public long getFee() {
    	return fee;
    }

    public long getHeight() {
    	return height;
    }

    public long getTime() {
    	return time;
    }

    public String getRelayedBy() {
    	return relayed_by;
    }

    public ArrayList<xPut> getInputs() {
    	return inputs;
    }

    public ArrayList<xPut> getOutputs() {
    	return outputs;
    }

    public HashMap<String,Long> getTotalValues() {
    	return totalValues;
    }

    public HashMap<String,Long> getInputValues() {
    	return inputValues;
    }

    public HashMap<String,Long> getOutputValues() {
    	return outputValues;
    }

    /**
     * Parse the data supplied to this instance.
     * 
     */
    public void parse()	{
    	
        try {
    		JSONObject tx = new JSONObject(strData);
    		if(tx != null)	{
//    			Log.d(TAG, "Object OK");
    			if(tx.has("block_height"))	{
        			height = tx.getLong("block_height");
    			}
    			hash = tx.getString("hash");
    			time = tx.getLong("time");
    			relayed_by = tx.getString("relayed_by");
    			
    			long total_input = 0L;
    			long total_output = 0L;

    			if(tx.has("inputs"))	{
        			JSONArray _inputs = tx.getJSONArray("inputs");
        			for(int j = 0; j < _inputs.length(); j++)	{
            			JSONObject _input = _inputs.getJSONObject(j);
            			JSONObject prev_out = _input.getJSONObject("prev_out");
            			if(prev_out != null)	{
                			xPut input = new xPut();
                			input.addr = prev_out.getString("addr");
                			if(prev_out.has("addr_tag"))	{
                    			input.addr_tag = prev_out.getString("addr_tag");
                			}
                			if(prev_out.has("value"))	{
                    			input.value = prev_out.getLong("value");
                    			total_input += input.value;
                    			
                    			if(totalValues.get(input.addr) != null)	{
                    				totalValues.put(input.addr, totalValues.get(input.addr) - input.value);
                    			}
                    			else	{
                    				totalValues.put(input.addr, 0L - input.value);
                    			}
                    			
                    			if(inputValues.get(input.addr) != null)	{
                    				inputValues.put(input.addr, inputValues.get(input.addr) - input.value);
                    			}
                    			else	{
                    				inputValues.put(input.addr, 0L - input.value);
                    			}

                			}
                			inputs.add(input);
            			}
        			}
    			}

    			if(tx.has("out"))	{
        			JSONArray _outputs = tx.getJSONArray("out");
        			for(int j = 0; j < _outputs.length(); j++)	{
            			JSONObject _output = _outputs.getJSONObject(j);
            			if(_output != null)	{
                			xPut output = new xPut();
                			output.addr = _output.getString("addr");
                			if(_output.has("addr_tag"))	{
                    			output.addr_tag = _output.getString("addr_tag");
                			}
                			if(_output.has("value"))	{
                    			output.value = _output.getLong("value");
                    			total_output += output.value;
                    			
                    			if(totalValues.get(output.addr) != null)	{
                    				totalValues.put(output.addr, totalValues.get(output.addr) + output.value);
                    			}
                    			else	{
                    				totalValues.put(output.addr, output.value);
                    			}
                    			
                    			if(outputValues.get(output.addr) != null)	{
                    				outputValues.put(output.addr, outputValues.get(output.addr) - output.value);
                    			}
                    			else	{
                    				outputValues.put(output.addr, 0L - output.value);
                    			}

                			}
                			outputs.add(output);
            			}
        			}
    			}
    			
    			fee = Math.abs(total_input - total_output);
    			if(tx.has("result"))	{
        			result = tx.getLong("result");
    			}
    			else	{
    				for(xPut out : outputs)	{
    					result += out.value;
    				}
    			}
    		}
    	} catch (JSONException je) {
    		je.printStackTrace();
    	}

    }

}
