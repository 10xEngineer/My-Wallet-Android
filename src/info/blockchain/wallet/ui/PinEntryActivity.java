package info.blockchain.wallet.ui;

import java.security.SecureRandom;

import piuk.blockchain.android.Constants;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.R;
//import piuk.blockchain.android.ui.dialogs.RekeyWalletDialog;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
import piuk.blockchain.android.util.WalletUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.NumberKeyListener;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import info.blockchain.api.ExchangeRates;
import net.sourceforge.zbar.Symbol;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.encoders.Hex;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;

public class PinEntryActivity extends FragmentActivity {

	String userEntered = "";
	
	final int PIN_LENGTH = 4;
	boolean keyPadLockedFlag = false;
	Context context = null;
	
	TextView titleView = null;
	
	TextView pinBox0 = null;
	TextView pinBox1 = null;
	TextView pinBox2 = null;
	TextView pinBox3 = null;
	
	TextView[] pinBoxArray = null;
	
	TextView statusView = null;
	
	Button button0 = null;
	Button button1 = null;
	Button button2 = null;
	Button button3 = null;
	Button button4 = null;
	Button button5 = null;
	Button button6 = null;
	Button button7 = null;
	Button button8 = null;
	Button button9 = null;
	Button buttonForgot = null;
	Button buttonDeleteBack = null;
	
	private boolean validating = true;
	private boolean creating = false;
	private String userInput = null;

	private static final String WebROOT = "https://" + Constants.BLOCKCHAIN_DOMAIN + "/pin-store";
	public static final int PBKDF2Iterations = 2000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		context = this;
		userEntered = "";

		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN);

	    if(!DeviceUtil.getInstance(this).isSmallScreen()) {
			setContentView(R.layout.activity_pin_entry);
	    }
	    else {
			setContentView(R.layout.activity_pin_entry_small);
	    }
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
            if(extras.getString("N") != null && extras.getString("N").equals("1"))	{
            	validating = false;
            	creating = true;
            }
            else if(extras.getString("N") != null && extras.getString("N").length() == 4)	{
            	validating = false;
            	creating = true;
            	userInput = extras.getString("N");
            }
            else if(extras.getString("S") != null && extras.getString("S").equals("1"))	{
            	validating = false;
            }
            else if(extras.getString("S") != null && extras.getString("S").length() == 4)	{
            	validating = false;
            	userInput = extras.getString("S");
            }
            else	{
            	validating = true;
            }
        }
        
		Typeface typeface = Typeface.createFromAsset(getAssets(), "Roboto-Regular.ttf");  
		
		buttonForgot = (Button) findViewById(R.id.buttonForgot);
		buttonForgot.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	;
		    }
		});
		buttonForgot.setTypeface(typeface);

		buttonDeleteBack = (Button) findViewById(R.id.buttonDeleteBack);
		buttonDeleteBack.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View v) {
		    	if(keyPadLockedFlag == true)	{
		    		return;
		    	}
		    	
			    if(userEntered.length() > 0)	{
			    	for (int i = 0; i < pinBoxArray.length; i++)
			    		pinBoxArray[i].setText("");
			    	userEntered = "";
		    	}
		    }
		});
		
		titleView = (TextView)findViewById(R.id.titleBox);
		titleView.setTypeface(typeface);
		
		pinBox0 = (TextView)findViewById(R.id.pinBox0);
		pinBox1 = (TextView)findViewById(R.id.pinBox1);
		pinBox2 = (TextView)findViewById(R.id.pinBox2);
		pinBox3 = (TextView)findViewById(R.id.pinBox3);
		
		pinBoxArray = new TextView[PIN_LENGTH];
		pinBoxArray[0] = pinBox0;
		pinBoxArray[1] = pinBox1;
		pinBoxArray[2] = pinBox2;
		pinBoxArray[3] = pinBox3;

		statusView = (TextView) findViewById(R.id.statusMessage);
		statusView.setTypeface(typeface);

		View.OnClickListener pinButtonHandler = new View.OnClickListener() {
		    public void onClick(View v) {
		    	
			    if(keyPadLockedFlag == true)	{
		    		return;
		    	}
		    	
		    	Button pressedButton = (Button)v;

			    if(userEntered.length() < PIN_LENGTH)	{
		    		userEntered = userEntered + pressedButton.getText().toString().substring(0, 1);
//		    		Log.v("PinView", "User entered=" + userEntered);
		    		
		    		// Update pin boxes
		    		pinBoxArray[userEntered.length() - 1].setText("8");
		    		
		    		if(userEntered.length() == PIN_LENGTH)	{

		    			if(validating)	{
				    		validatePIN(userEntered);
		    			}
		    			else	{
		    				if(userInput != null)	{
		    					if(userInput.equals(userEntered))	{
		    						
									new Thread(new Runnable(){
									    @Override
									    public void run() {
									    	
											Looper.prepare();

											final WalletApplication application = (WalletApplication) getApplication();
											Editor edit = PreferenceManager.getDefaultSharedPreferences(PinEntryActivity.this).edit();

											//
											// Save PIN
											//
									        try {
												byte[] bytes = new byte[16];
												SecureRandom random = new SecureRandom();
												random.nextBytes(bytes);
												final String key = new String(Hex.encode(bytes), "UTF-8");
												random.nextBytes(bytes);
												final String value = new String(Hex.encode(bytes), "UTF-8");
//												final JSONObject response = piuk.blockchain.android.ui.PinEntryActivity.apiStoreKey(key, value, userInput);
												final JSONObject response = apiStoreKey(key, value, userInput);
												if (response.get("success") != null) {
													
													edit.putString("pin_kookup_key", key);
													edit.putString("encrypted_password", MyWallet.encrypt(application.getRemoteWallet().getTemporyPassword(), value, PBKDF2Iterations));

													if (!edit.commit()) {
														throw new Exception("Error Saving Preferences");
													}
													else {
														TimeOutUtil.getInstance().updatePin();

														Toast.makeText(PinEntryActivity.this, "PIN saved", Toast.LENGTH_SHORT).show();	
											        	Intent intent = new Intent(PinEntryActivity.this, MainActivity.class);
														intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
											    		startActivity(intent);
													}

												}
												else {

													Toast.makeText(application, response.toString(), Toast.LENGTH_LONG).show();

													Toast.makeText(PinEntryActivity.this, "PIN saved", Toast.LENGTH_SHORT).show();	
										        	Intent intent = new Intent(PinEntryActivity.this, MainActivity.class);
													String navigateTo = getIntent().getStringExtra("navigateTo");
													intent.putExtra("navigateTo", navigateTo);   
													intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
										    		startActivity(intent);

												}
									        } catch (Exception e) {
												Toast.makeText(application, e.toString(), Toast.LENGTH_LONG).show();
									            e.printStackTrace();
									        }
											//
											//
											//
									        
											Looper.loop();

									    }
									}).start();

		    						
		    					}
		    					else	{
		    						Toast.makeText(PinEntryActivity.this, "Start over", Toast.LENGTH_SHORT).show();	

		    						Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
						        	intent.putExtra("S", userEntered);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
						    		startActivity(intent);
		    					}
		    				}
		    				else	{
		    					if(creating)	{
						        	Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
						        	intent.putExtra("N", userEntered);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
						    		startActivity(intent);
		    					}
		    					else	{
						        	Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
						        	intent.putExtra("S", userEntered);
									intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
						    		startActivity(intent);
		    					}
		    				}
		    			}

		    		}	
		    	}
			    else	{
		    		//Roll over
		    		pinBoxArray[0].setText("");
		    		pinBoxArray[1].setText("");
		    		pinBoxArray[2].setText("");
		    		pinBoxArray[3].setText("");
		    		
		    		userEntered = "";
		    		
		    		statusView.setText("");
		    		
		    		userEntered = userEntered + pressedButton.getText().toString().substring(0, 1);
//		    		Log.v("PinView", "User entered=" + userEntered);
		    		
		    		//Update pin boxes
		    		pinBoxArray[userEntered.length() - 1].setText("8");
		    		
		    		validatePIN(userEntered);

		    	}
		    }
		};

		button0 = (Button)findViewById(R.id.button0);
		button0.setTypeface(typeface);
		button0.setOnClickListener(pinButtonHandler);
		
		button1 = (Button)findViewById(R.id.button1);
		button1.setTypeface(typeface);
		button1.setOnClickListener(pinButtonHandler);

        SpannableStringBuilder cs = null;
        float sz = 0.6f;

		button2 = (Button)findViewById(R.id.button2);
		button2.setTypeface(typeface);
		button2.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("2 ABC");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button2.setText(cs);

		button3 = (Button)findViewById(R.id.button3);
		button3.setTypeface(typeface);
		button3.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("3 DEF");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button3.setText(cs);
		
		button4 = (Button)findViewById(R.id.button4);
		button4.setTypeface(typeface);
		button4.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("4 GHI");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button4.setText(cs);
		
		button5 = (Button)findViewById(R.id.button5);
		button5.setTypeface(typeface);
		button5.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("5 JKL");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button5.setText(cs);
		
		button6 = (Button)findViewById(R.id.button6);
		button6.setTypeface(typeface);
		button6.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("6 MNO");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button6.setText(cs);
		
		button7 = (Button)findViewById(R.id.button7);
		button7.setTypeface(typeface);
		button7.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("7 PQRS");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button7.setText(cs);
		
		button8 = (Button)findViewById(R.id.button8);
		button8.setTypeface(typeface);
		button8.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("8 TUV");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button8.setText(cs);
		
		button9 = (Button)findViewById(R.id.button9);
		button9.setTypeface(typeface);
		button9.setOnClickListener(pinButtonHandler);
        cs = new SpannableStringBuilder("9 WXYZ");
        cs.setSpan(new RelativeSizeSpan(sz), 2, cs.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		button9.setText(cs);

		buttonDeleteBack = (Button)findViewById(R.id.buttonDeleteBack);
		buttonDeleteBack.setTypeface(typeface);

		final int colorOff = 0xff333333;
		final int colorOn = 0xff1a87c6;

		button0.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button0.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button0.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button1.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button1.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button1.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button2.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button2.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button2.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button3.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button3.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button3.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button4.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button4.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button4.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button5.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button5.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button5.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button6.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button6.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button6.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button7.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button7.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button7.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button8.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button8.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button8.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		button9.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		button9.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		button9.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		buttonDeleteBack.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		buttonDeleteBack.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		buttonDeleteBack.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });

		buttonForgot.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction())	{
                	case android.view.MotionEvent.ACTION_DOWN:
                	case android.view.MotionEvent.ACTION_MOVE:
                		buttonForgot.setBackgroundColor(colorOn);
                		break;
                	case android.view.MotionEvent.ACTION_UP:
                	case android.view.MotionEvent.ACTION_CANCEL:
                		buttonForgot.setBackgroundColor(colorOff);
                		break;
                	}

                return false;
            }
        });
		
		ExchangeRates fxRates = new ExchangeRates();
        DownloadFXRatesTask task = new DownloadFXRatesTask(context, fxRates);
        task.execute(new String[] { fxRates.getUrl() });

		final WalletApplication application = (WalletApplication)PinEntryActivity.this.getApplication();
		if (application.getGUID() == null && !creating) {
        	Intent intent = new Intent(PinEntryActivity.this, SetupActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    		startActivity(intent);
		}

	}

	@Override
	public void onBackPressed() {
		//App not allowed to go back to Parent activity until correct pin entered.
		return;
		//super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_pin_entry_view, menu);
		return true;
	}

	private class LockKeyPadOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
              for(int i = 0; i < 2; i++) {
                  try {
                      Thread.sleep(1000);
                  } catch (InterruptedException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
                  }
              }

              return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            statusView.setText("");

            //Roll over
            pinBoxArray[0].setText("");
            pinBoxArray[1].setText("");
            pinBoxArray[2].setText("");
            pinBoxArray[3].setText("");

            userEntered = "";

            keyPadLockedFlag = false;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
	}

	public void validatePIN(final String PIN) {
		
		final WalletApplication application = (WalletApplication)PinEntryActivity.this.getApplication();

		final Handler handler = new Handler();

		final Activity activity = this;

		ProgressUtil.getInstance(PinEntryActivity.this).show();

		new Thread(new Runnable() {

			public void run() {
				
				Looper.prepare();

				String pin_lookup_key = PreferenceManager.getDefaultSharedPreferences(application).getString("pin_kookup_key", null);
				String encrypted_password = PreferenceManager.getDefaultSharedPreferences(application).getString("encrypted_password", null);

				try {
//					final JSONObject response = piuk.blockchain.android.ui.PinEntryActivity.apiGetValue(pin_lookup_key, PIN);
					final JSONObject response = apiGetValue(pin_lookup_key, PIN);

					String decryptionKey = (String) response.get("success");
					if (decryptionKey != null) {	
						application.setTemporyPIN(PIN);
						application.didEncounterFatalPINServerError = false;

						String password = MyWallet.decrypt(encrypted_password, decryptionKey, piuk.blockchain.android.ui.PinEntryActivity.PBKDF2Iterations);
//						Toast.makeText(PinEntryActivity.this, password, Toast.LENGTH_SHORT).show();	

						application.checkIfWalletHasUpdatedAndFetchTransactions(password, new SuccessCallback() {
							@Override
							public void onSuccess() {
								handler.post(new Runnable() {
									public void run() {
										
										TimeOutUtil.getInstance().updatePin();

										Editor edit = PreferenceManager.getDefaultSharedPreferences(PinEntryActivity.this).edit();
										edit.putBoolean("verified", true);
										edit.commit();

										ProgressUtil.getInstance(PinEntryActivity.this).close();

							        	Intent intent = new Intent(PinEntryActivity.this, MainActivity.class);
										String navigateTo = getIntent().getStringExtra("navigateTo");
										intent.putExtra("navigateTo", navigateTo);   
										intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							        	intent.putExtra("verified", true);
							    		startActivity(intent);

									}
								});
							}

							@Override
							public void onFail() {
								handler.post(new Runnable() {
									public void run() {

										Toast.makeText(PinEntryActivity.this, piuk.blockchain.android.R.string.toast_wallet_decryption_failed, Toast.LENGTH_LONG).show();	

									}
								});
							}
						});
					} else if (response.get("error") != null) {

						Toast.makeText(PinEntryActivity.this, response.toString(), Toast.LENGTH_SHORT).show();	

						application.didEncounterFatalPINServerError = false;

						handler.post(new Runnable() {
							public void run() {

								Toast.makeText(PinEntryActivity.this, (String)response.get("error"), Toast.LENGTH_SHORT).show();	

							}
						});
					} else {
						throw new Exception("Unknown Error");
					}
				} catch (final Exception e) {
					e.printStackTrace();

					application.didEncounterFatalPINServerError = true;

					handler.post(new Runnable() {
						public void run() {
							

							try {

								AlertDialog.Builder builder = new AlertDialog.Builder(activity);

								builder.setCancelable(false);

								builder.setMessage(piuk.blockchain.android.R.string.pin_server_error_description);

								builder.setTitle(piuk.blockchain.android.R.string.pin_server_error);

								builder.setPositiveButton(piuk.blockchain.android.R.string.try_again, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {

										Toast.makeText(PinEntryActivity.this, "Starting over", Toast.LENGTH_SHORT).show();	

										Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
										intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
							    		startActivity(intent);

									}
								});
								builder.setNegativeButton(piuk.blockchain.android.R.string.pin_server_error_enter_password_manually, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.dismiss();

										RequestPasswordDialog.show(getSupportFragmentManager(),
												new SuccessCallback() {  
													public void onSuccess() {
														Toast.makeText(PinEntryActivity.this, "Password correct", Toast.LENGTH_LONG).show();

														Intent intent = new Intent(PinEntryActivity.this, MainActivity.class);
														String navigateTo = getIntent().getStringExtra("navigateTo");
														intent.putExtra("navigateTo", navigateTo);
														intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
											    		startActivity(intent);

													}
													public void onFail() {	
														Toast.makeText(PinEntryActivity.this, piuk.blockchain.android.R.string.password_incorrect, Toast.LENGTH_LONG).show();

														Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
														intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
											    		startActivity(intent);

													}
												}, RequestPasswordDialog.PasswordTypeMain);

									}
								});

								AlertDialog dialog = builder.create();

								ProgressUtil.getInstance(PinEntryActivity.this).close();

								dialog.show();

//								begin();
							} catch (Exception e) {
								e.printStackTrace();
							}
							

						}
					});
				}
				
				Looper.loop();

			}
		}).start();

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		final WalletApplication application = (WalletApplication) getApplication();
		application.setIsPassedPinScreen(false);
	}

	public static JSONObject apiGetValue(String key, String pin) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("key=" + key);
		args.append("&pin=" + pin);
		args.append("&method=get");

		String response = WalletUtils.postURL(WebROOT, args.toString());

		if (response == null || response.length() == 0)
			throw new Exception("Invalid Server Response");
		
		try {
			return (JSONObject) new JSONParser().parse(response);
		} catch (ParseException e) {
			throw new Exception("Invalid Server Response");
		}		
	}

	public static JSONObject apiStoreKey(String key, String value, String pin) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("key=" + key);
		args.append("&value=" + value);
		args.append("&pin=" + pin);
		args.append("&method=put");
		
		String response = WalletUtils.postURL(WebROOT, args.toString());

		if (response == null || response.length() == 0)
			throw new Exception("Invalid Server Response");

		try {
			return (JSONObject) new JSONParser().parse(response);
		} catch (ParseException e) {
			throw new Exception("Invalid Server Response");
		}		
	}

}
