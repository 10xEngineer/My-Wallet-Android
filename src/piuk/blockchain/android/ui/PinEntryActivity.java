package piuk.blockchain.android.ui;

/*Client ID: 	381130279932.apps.googleusercontent.com
Redirect URIs: 	urn:ietf:wg:oauth:2.0:oob http://localhost
Application type: 	Android
Package name: 	com.ultimasquare.pinview
Certificate fingerprint (SHA1): 	86:F2:4D:FD:34:98:BF:0C:47:94:34:D4:8C:68:A3:84:B7:D7:B2:0F
Deep Linking: 	Disabled*/


import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.encoders.Hex;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.WalletApplication;
//import piuk.blockchain.android.ui.dialogs.RekeyWalletDialog;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
//import piuk.blockchain.android.ui.dialogs.WelcomeDialog;
import piuk.blockchain.android.util.WalletUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PinEntryActivity extends AbstractWalletActivity {
	public static boolean active = false;
	public static final int UNKNOWN = 1;
	public static final int BEGIN_SETUP = 1;
	public static final int CONFIRM_PIN_SETUP = 2;
	public static final int BEGIN_CHECK_PIN = 4;
	public static final int FINISHING_SETUP = 3;
	public static final int VALIDATING_PIN = 5;
	public static final int PBKDF2Iterations = 2000;
	private static List<WeakReference<PinEntryActivity>> fragmentRefs = new ArrayList<WeakReference<PinEntryActivity>>();

	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Pinentry Listener";
		}

		@Override
		public void onWalletDidChange() {		
			begin();
		}
	};

	public static void beginAll() {
		for (WeakReference<PinEntryActivity> fragmentRef : fragmentRefs) {
			if (fragmentRef != null && fragmentRef.get() != null) {
				try {
					((PinEntryActivity)fragmentRef.get()).begin();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static final String WebROOT = "https://"+Constants.BLOCKCHAIN_DOMAIN+"/pin-store";

	int stage = UNKNOWN;

	String previousEntered;
	String userEntered;
	String userPin="8888";

	final int PIN_LENGTH = 4;
	boolean keyPadLockedFlag = false;
	Context appContext;

	TextView titleView;

	TextView pinBox0;
	TextView pinBox1;
	TextView pinBox2;
	TextView pinBox3;

	TextView [] pinBoxArray;

	TextView statusView;

	Button button0;
	Button button1;
	Button button2;
	Button button3;
	Button button4;
	Button button5;
	Button button6;
	Button button7;
	Button button8;
	Button button9;
	Button button10;
	Button buttonDelete;
	Button buttonBlank;

	public static JSONObject apiGetValue(String key, String pin) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("key=" + key);
		args.append("&pin="+ pin);
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
		args.append("&pin="+pin);
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
	
	/* ***************
	public static void clearPrefValues(WalletApplication application) throws Exception {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(application).edit();

		editor.remove("pin_kookup_key");
		editor.remove("encrypted_password");

		if (!editor.commit()) {
			throw new Exception("Error Saving Preferences");
		}
	}
	*/

	private void disableKeyPad(boolean enabled) {
		button0.setEnabled(!enabled);
		button1.setEnabled(!enabled);
		button2.setEnabled(!enabled);
		button3.setEnabled(!enabled);
		button4.setEnabled(!enabled);
		button5.setEnabled(!enabled);
		button6.setEnabled(!enabled);
		button7.setEnabled(!enabled);
		button8.setEnabled(!enabled);
		button9.setEnabled(!enabled);
		buttonDelete.setEnabled(!enabled);
		buttonBlank.setEnabled(!enabled);
	}

	public void validatePIN(final String PIN) {
		disableKeyPad(true);

		statusView.setText("Validating PIN");

		final Activity activity = this;

		new Thread(new Runnable() {
			public void run() {

				String pin_lookup_key = PreferenceManager.getDefaultSharedPreferences(application).getString("pin_kookup_key", null);
				String encrypted_password = PreferenceManager.getDefaultSharedPreferences(application).getString("encrypted_password", null);

				try {
					final JSONObject response = apiGetValue(pin_lookup_key, PIN);

					String decryptionKey = (String) response.get("success");
					if (decryptionKey != null) {	
						application.didEncounterFatalPINServerError = false;

						String password = MyWallet.decrypt(encrypted_password, decryptionKey, PBKDF2Iterations);

						application.checkIfWalletHasUpdatedAndFetchTransactions(password, new SuccessCallback() {
							@Override
							public void onSuccess() {
								handler.post(new Runnable() {
									public void run() {															
										Toast.makeText(application, "PIN Verified", Toast.LENGTH_SHORT).show();	

										disableKeyPad(false);
										
										/*
										if (application.needsWalletRekey()) {
											RekeyWalletDialog.show(getSupportFragmentManager(), application, new SuccessCallback() {
												@Override
												public void onSuccess() {													
													finish();
												}

												@Override
												public void onFail() {													
													finish();
												}
											});
										} else {
											finish();
										}
										*/
										finish();
									}
								});
							}

							@Override
							public void onFail() {
								handler.post(new Runnable() {
									public void run() {
										disableKeyPad(false);

										Toast.makeText(application,
												R.string.toast_wallet_decryption_failed, Toast.LENGTH_LONG)
												.show();	

										/* *******************
										try {
											clearPrefValues(application);
										} catch (Exception e) {
											e.printStackTrace();
										}
										*/

										begin();
									}
								});
							}
						});
					} else if (response.get("error") != null) {

						//Even though we received an error it is a valid response
						//So no fatal
						application.didEncounterFatalPINServerError = false;

						//"code" == 2 means the PIN is incorrect
						if (!response.containsKey("code") || ((Number)response.get("code")).intValue() != 2) {
							/* **********************
							clearPrefValues(application);
							*/
						}

						handler.post(new Runnable() {
							public void run() {
								disableKeyPad(false);

								Toast.makeText(application, (String)response.get("error"), Toast.LENGTH_SHORT).show();	

								begin();
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
								disableKeyPad(false);

								AlertDialog.Builder builder = new AlertDialog.Builder(activity);

								builder.setCancelable(false);

								builder.setMessage(R.string.pin_server_error_description);

								builder.setTitle(R.string.pin_server_error);

								builder.setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										validatePIN(PIN);

										dialog.dismiss();

										begin();
									}
								});
								builder.setNegativeButton(R.string.pin_server_error_enter_password_manually, new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.dismiss();

										RequestPasswordDialog.show(
												getSupportFragmentManager(),
												new SuccessCallback() {  
													public void onSuccess() {
														finish();
													}
													public void onFail() {	
														Toast.makeText(application, R.string.password_incorrect, Toast.LENGTH_LONG).show();

														begin();
													}
												}, RequestPasswordDialog.PasswordTypeMain);

									}
								});

								AlertDialog dialog = builder.create();

								dialog.show();

								begin();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		}).start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		fragmentRefs.add(new WeakReference<PinEntryActivity>(this));

		EventListeners.addEventListener(eventListener);

		appContext = this;
		userEntered = "";

		setContentView(R.layout.activity_pin_entry_view);

		buttonDelete = (Button) findViewById(R.id.buttonDeleteBack);
		buttonDelete.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (keyPadLockedFlag == true)
				{
					return;
				}

				if (userEntered.length()>0)
				{
					userEntered = userEntered.substring(0,userEntered.length()-1);
					pinBoxArray[userEntered.length()].setText("");
				}
			}
		}
				);

		titleView = (TextView)findViewById(R.id.titleBox);

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

		View.OnClickListener pinButtonHandler = new View.OnClickListener() {
			public void onClick(View v) {

				if (keyPadLockedFlag == true)
				{
					return;
				}

				Button pressedButton = (Button)v;

				if (userEntered.length() < PIN_LENGTH)
				{ 
					final String PIN = userEntered + pressedButton.getText();

					userEntered = PIN;

					//Update pin boxes
					pinBoxArray[userEntered.length()-1].setText("8");

					if (userEntered.length() == PIN_LENGTH)
					{
						if (stage == BEGIN_CHECK_PIN) {
							stage = VALIDATING_PIN;

							validatePIN(PIN);
						} if (stage == BEGIN_SETUP) {
							previousEntered = userEntered;

							clear();

							titleView.setText("Confirm PIN");	
							statusView.setText("Please renter the PIN code");

							stage = CONFIRM_PIN_SETUP;
						} else if (stage == CONFIRM_PIN_SETUP) {
							if (previousEntered.equals(userEntered)) {
								stage = FINISHING_SETUP;

								statusView.setText("Saving PIN. Please Wait.");

								disableKeyPad(true);

								new Thread(new Runnable() {
									public void run() {
										try {
											byte[] bytes = new byte[16];

											SecureRandom random = new SecureRandom();

											random.nextBytes(bytes);

											final String key = new String(Hex.encode(bytes), "UTF-8");

											random.nextBytes(bytes);

											final String value = new String(Hex.encode(bytes), "UTF-8");

											final JSONObject response = apiStoreKey(key, value, PIN);

											if (response.get("success") != null) {

												application.didEncounterFatalPINServerError = false;

												handler.post(new Runnable() {
													public void run() {
														try {
															Editor editor = PreferenceManager.getDefaultSharedPreferences(application).edit();

															editor.putString("pin_kookup_key", key);
															editor.putString("encrypted_password", MyWallet.encrypt(application.getRemoteWallet().getTemporyPassword(), value, PBKDF2Iterations));

															if (!editor.commit()) {
																throw new Exception("Error Saving Preferences");
															}

															Toast.makeText(application,
																	R.string.toast_pin_saved, Toast.LENGTH_SHORT)
																	.show();	

															finish();

															disableKeyPad(false);
														} catch (Exception e) {
															e.printStackTrace();

															Toast.makeText(application,
																	e.getLocalizedMessage(), Toast.LENGTH_LONG)
																	.show();	

															begin();

															disableKeyPad(false);
														}
													}
												});
											} else if (response.get("error") != null) {
												application.didEncounterFatalPINServerError = false;

												handler.post(new Runnable() {
													public void run() {
														Toast.makeText(application, (String) response.get("error"), Toast.LENGTH_LONG)
														.show();	

														disableKeyPad(false);

														begin();
													}
												});
											} else {
												throw new Exception("Unknown Error");
											}
										} catch (final Exception e) {
											application.didEncounterFatalPINServerError = true;

											e.printStackTrace();

											finish();
										}
									}
								}).start();
							} else {
								statusView.setText("PIN does not match");

								begin();
							}
						}
					}	
				} 
				else
				{
					//Roll over
					pinBoxArray[0].setText("");
					pinBoxArray[1].setText("");
					pinBoxArray[2].setText("");
					pinBoxArray[3].setText("");

					userEntered = "";

					statusView.setText("");

					userEntered = userEntered + pressedButton.getText();

					//Update pin boxes
					pinBoxArray[userEntered.length()-1].setText("8");
				}
			}
		};


		button0 = (Button)findViewById(R.id.button0);
		button0.setOnClickListener(pinButtonHandler);

		button1 = (Button)findViewById(R.id.button1);
		button1.setOnClickListener(pinButtonHandler);

		button2 = (Button)findViewById(R.id.button2);
		button2.setOnClickListener(pinButtonHandler);

		button3 = (Button)findViewById(R.id.button3);
		button3.setOnClickListener(pinButtonHandler);

		button4 = (Button)findViewById(R.id.button4);
		button4.setOnClickListener(pinButtonHandler);

		button5 = (Button)findViewById(R.id.button5);
		button5.setOnClickListener(pinButtonHandler);

		button6 = (Button)findViewById(R.id.button6);
		button6.setOnClickListener(pinButtonHandler);

		button7 = (Button)findViewById(R.id.button7);
		button7.setOnClickListener(pinButtonHandler);

		button8 = (Button)findViewById(R.id.button8);
		button8.setOnClickListener(pinButtonHandler);

		button9 = (Button)findViewById(R.id.button9);
		button9.setOnClickListener(pinButtonHandler);

		buttonDelete = (Button)findViewById(R.id.buttonDeleteBack);

		buttonBlank = (Button)findViewById(R.id.buttonBlank); 
	}


	public void begin() {

		final Activity activity = this;

		disableKeyPad(false);

		clear();

		RequestPasswordDialog.hide();
//		WelcomeDialog.hide();

		String pin_lookup_key = PreferenceManager.getDefaultSharedPreferences(this).getString("pin_kookup_key", null);
		String encrypted_password = PreferenceManager.getDefaultSharedPreferences(this).getString("encrypted_password", null);

		if (pin_lookup_key == null || encrypted_password == null || application.decryptionErrors > 0) {
			titleView.setText("Create PIN");	

			statusView.setText("Please create a new PIN code");

			stage = BEGIN_SETUP;

			if (application.getRemoteWallet() == null || application.decryptionErrors > 0) {
				if (application.decryptionErrors <= 1 && application.getGUID() != null && application.getSharedKey() != null) {

					RequestPasswordDialog.show(
							getSupportFragmentManager(),
							new SuccessCallback() {  
								public void onSuccess() {
									statusView.setText("Password Ok. Please create a PIN.");
								}
								public void onFail() {							
//									WelcomeDialog.show(getSupportFragmentManager(), activity, (WalletApplication)getApplication());
								}
							}, RequestPasswordDialog.PasswordTypeMain);
				} else {
//					WelcomeDialog.show(getSupportFragmentManager(), activity, (WalletApplication)getApplication());
				}
			} 
		} else {
			titleView.setText("Enter PIN");	

			stage = BEGIN_CHECK_PIN;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		begin();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		EventListeners.removeEventListener(eventListener);
	}

	@Override
	public void onStart() {
		super.onStart();
		active = true;
		begin();
	} 

	@Override
	public void onStop() {
		super.onStop();
		active = false;
	}


	public void clear() {
		statusView.setText("");

		//Roll over
		pinBoxArray[0].setText("");
		pinBoxArray[1].setText("");
		pinBoxArray[2].setText("");
		pinBoxArray[3].setText("");

		userEntered = "";
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub

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
}
