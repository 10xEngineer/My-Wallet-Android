package info.blockchain.wallet.ui;

import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.Editor;
//import android.util.Log;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.android.gcm.GCMRegistrar;

import net.sourceforge.zbar.Symbol;

import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.SuccessCallback;
//import piuk.blockchain.android.ui.dialogs.RequestIdentifierDialog;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
//import piuk.blockchain.android.ui.dialogs.WelcomeDialog;
import piuk.blockchain.android.ui.*;
import piuk.blockchain.android.R;

import org.spongycastle.util.encoders.Hex;
import com.google.android.gcm.GCMRegistrar;

public class SetupActivity extends Activity		{

	private static int ZBAR_SCANNER_REQUEST = 2026;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    
	    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	    this.setContentView(R.layout.setup);

	    setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SetupActivity.this);
		Editor edit = prefs.edit();
		edit.putBoolean("virgin", true);
		edit.commit();

        Button imgCreate = ((Button)findViewById(R.id.create));
        imgCreate.setTypeface(TypefaceUtil.getInstance(this).getGravityBoldTypeface());
        imgCreate.setTextColor(0xFF1B8AC7);
        imgCreate.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
    			Intent intent = new Intent(SetupActivity.this, info.blockchain.wallet.ui.PinCreateActivity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
    			startActivity(intent);
            }
        });

        Button imgPair = ((Button)findViewById(R.id.pair));
        imgPair.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
        imgPair.setTextColor(0xFF808080);
        imgPair.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
        		Intent intent = new Intent(SetupActivity.this, ZBarScannerActivity.class);
        		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
        		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);
            }
        });

    }
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{
			String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);
        	handleQRCode(strResult);

        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == ZBAR_SCANNER_REQUEST) {
            Toast.makeText(this, "QR scan canceled", Toast.LENGTH_SHORT).show();
        }
        else {
        	;
        }

	}

	public void handleQRCode(String raw_code) {
		final WalletApplication application = (WalletApplication) getApplication();
		
		try {
			if (raw_code == null || raw_code.length() == 0) {
				throw new Exception("Invalid Pairing QR Code");
			}

			if (raw_code.charAt(0) != '1') {
				throw new Exception("Invalid Pairing Version Code " + raw_code.charAt(0));
			}

			final Handler handler = new Handler();

			{
				String[] components = raw_code.split("\\|", Pattern.LITERAL);

				if (components.length < 3) {
					throw new Exception("Invalid Pairing QR Code. Not enough components.");
				}

				final String guid = components[1];
				if (guid.length() != 36) {
					throw new Exception("Invalid Pairing QR Code. GUID wrong length.");
				}

				final String encrypted_data = components[2];

				new Thread(new Runnable() {

					@Override
					public void run() {
						
						Looper.prepare();

						try {
							String temp_password = MyRemoteWallet.getPairingEncryptionPassword(guid);

							String decrypted = MyWallet.decrypt(encrypted_data, temp_password, MyWallet.DefaultPBKDF2Iterations);

							String[] sharedKeyAndPassword = decrypted.split("\\|", Pattern.LITERAL);

							if (sharedKeyAndPassword.length < 2) {
								throw new Exception("Invalid Pairing QR Code. sharedKeyAndPassword Incorrect number of components.");
							}

							final String sharedKey = sharedKeyAndPassword[0];
							if (sharedKey.length() != 36) {
								throw new Exception("Invalid Pairing QR Code. sharedKey wrong length.");
							}

							final String password = new String(Hex.decode(sharedKeyAndPassword[1]), "UTF-8");
//							Toast.makeText(application, password, Toast.LENGTH_LONG).show();

							application.clearWallet();

//							PinEntryActivity.clearPrefValues(application);

							Editor edit = PreferenceManager.getDefaultSharedPreferences(SetupActivity.this).edit();

							edit.putString("guid", guid);
							edit.putString("sharedKey", sharedKey);

							edit.commit();

							handler.post(new Runnable() {

								@Override
								public void run() {
									application.checkIfWalletHasUpdated(password, guid, sharedKey, true, new SuccessCallback(){

										@Override
										public void onSuccess() {	
//											registerNotifications();

									        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SetupActivity.this);
											Editor edit = prefs.edit();
											edit.putBoolean("validated", true);
											edit.putBoolean("paired", true);
											edit.commit();

											try {
												final String regId = GCMRegistrar.getRegistrationId(SetupActivity.this);

												if (regId == null || regId.equals("")) {
													GCMRegistrar.register(SetupActivity.this, Constants.SENDER_ID);
												} else {
													application.registerForNotificationsIfNeeded(regId);
												}
											} catch (Exception e) {
												e.printStackTrace();
											}

								        	Intent intent = new Intent(SetupActivity.this, PinEntryActivity.class);
								        	intent.putExtra("S", "1");
											intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
								    		startActivity(intent);

//											finish();
										}

										@Override
										public void onFail() {
											finish();

											Toast.makeText(application, R.string.toast_error_syncing_wallet, Toast.LENGTH_LONG).show();
										}
									});
								}
							});

						} catch (final Exception e) {
							e.printStackTrace();

							handler.post(new Runnable() {
								public void run() {

									Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

									e.printStackTrace();

									application.writeException(e);
								}
							});
						}
						
						Looper.loop();

					}
				}).start();
			}
		} catch (Exception e) {

			Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();

			e.printStackTrace();

			application.writeException(e);
		}
		
	}

}
