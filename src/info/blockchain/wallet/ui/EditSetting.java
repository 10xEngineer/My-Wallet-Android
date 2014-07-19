package info.blockchain.wallet.ui;

import piuk.blockchain.android.R;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.text.method.NumberKeyListener;
import android.text.InputType;

public class EditSetting extends Activity {
	
    private EditText etValue = null;
    private TextView tvPrompt = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.editsetting);
        setTheme(android.R.style.Theme_Dialog);

        etValue = (EditText)findViewById(R.id.value);
        tvPrompt = (TextView)findViewById(R.id.prompt);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
            setTitle(extras.getString("prompt"));
        	tvPrompt.setText(extras.getString("existing") + ": ");
        	etValue.setText(extras.getString("value"));
        	
            if(extras.getString("prompt").contains("password") || extras.getString("prompt").contains("Password")) {
            	etValue.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }

        }

        Button button = (Button) findViewById(R.id.ok);
        button.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	String res = etValue.getText().toString();
            	
            	if(res == null) {
            		return;
            	}
            	if(res.length() < 1) {
            		return;
            	}

            	setResult(RESULT_OK,(new Intent()).setAction(res));
            	finish();
            }
       });

       Button button2 = (Button) findViewById(R.id.cancel);
       button2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
            	finish();
            }
       });
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
