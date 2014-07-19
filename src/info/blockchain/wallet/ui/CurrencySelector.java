package info.blockchain.wallet.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.Toast;
//import android.util.Log;

import piuk.blockchain.android.R;

public class CurrencySelector extends Activity	{

	private SelectedSpinner spCurrencies = null;
	private String[] currencies = null;
	private Button bOK = null;
	private Button bCancel = null;
    private ArrayAdapter<CharSequence> spAdapter = null;
	
	private SharedPreferences prefs = null;
    private SharedPreferences.Editor editor = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_currency);
	    
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = prefs.edit();
        
        spCurrencies = (SelectedSpinner)findViewById(R.id.receive_coins_default_currency);
        spAdapter = ArrayAdapter.createFromResource(this, R.array.currencies, android.R.layout.simple_spinner_item);
    	spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); 
    	spCurrencies.setAdapter(spAdapter);

        bOK = (Button)findViewById(R.id.confirm);
        bOK.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	int sel = spCurrencies.getSelectedItemPosition();
            	if(sel != 0) {
            		editor.putString("ccurrency", currencies[sel].substring(currencies[sel].length() - 3));
            		editor.commit();
            		BlockchainUtil.updateRate(CurrencySelector.this, currencies[sel].substring(currencies[sel].length() - 3));
                	finish();
            	}
            }
        });

        bCancel = (Button)findViewById(R.id.cancel);
        bCancel.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	finish();
            }
        });

        initValues();

    }

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        else	{
        	;
        }

        return false;
    }

    private void initValues() {
    	currencies = getResources().getStringArray(R.array.currencies);
    	String strCurrency = prefs.getString("ccurrency", "USD");
    	int sel = -1;
    	for(int i = 0; i < currencies.length; i++) {
    		if(currencies[i].endsWith(strCurrency)) {
    	        spCurrencies.setSelection(i);
    	        sel = i;
    	        break;
    		}
    	}
    	if(sel == -1) {
	        spCurrencies.setSelection(currencies.length - 1);
    	}

    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
	    Rect dialogBounds = new Rect();
	    getWindow().getDecorView().getHitRect(dialogBounds);

	    if(!dialogBounds.contains((int) event.getX(), (int) event.getY()) && event.getAction() == MotionEvent.ACTION_DOWN) {
	    	return false;
	    }
	    else {
		    return super.dispatchTouchEvent(event);
	    }
	}

}
