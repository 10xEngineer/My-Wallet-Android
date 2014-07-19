/*
 * Copyright 2011-2012 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package piuk.blockchain.android.ui;

import java.math.BigInteger;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.R;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
//import piuk.blockchain.android.util.ActionBarFragment;
import piuk.blockchain.android.util.WalletUtils;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.android.gcm.GCMRegistrar;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.uri.BitcoinURI;

/**
 * @author Andreas Schildbach
 */
public abstract class AbstractWalletActivity extends FragmentActivity {
	protected WalletApplication application = (WalletApplication) this.getApplication();
//	protected ActionBarFragment actionBar;
	protected final AbstractWalletActivity self = this;
	protected Handler handler = new Handler();
	protected ActivityDelegate activityDelegate;
	public static long lastDisplayedNetworkError = 0;
	private static final int ZBAR_SCANNER_REQUEST = 0;
	private static final int ZBAR_QR_SCANNER_REQUEST = 1;
	protected boolean dontCheckStatus = false;


	public abstract class QrCodeDelagate implements ActivityDelegate {

		public abstract void didReadQRCode(String data) throws Exception;

		@Override
		public void onActivityResult(final int requestCode, final int resultCode,
				final Intent intent) {

			//Zxing
			if (resultCode == RESULT_OK) {
				final String raw_code = intent.getStringExtra("SCAN_RESULT");

				try {
					if (raw_code == null || raw_code.length() == 0)
						throw new Exception("Null result returned");

					didReadQRCode(raw_code);
				} catch (Exception e) {
					e.printStackTrace();

					longToast(R.string.unknown_error);
				}
			}
		}
	}

	public void registerNotifications() {
		try {
			final String regId = GCMRegistrar.getRegistrationId(this);

			if (regId == null || regId.equals("")) {
				GCMRegistrar.register(this, Constants.SENDER_ID);
			} else {
				application.registerForNotificationsIfNeeded(regId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static interface ActivityDelegate {
		public void onActivityResult(final int requestCode, final int resultCode,
				final Intent intent);
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent intent) {

		if (activityDelegate != null)
			activityDelegate.onActivityResult(requestCode, resultCode, intent);

	}

	public void showQRReader(ActivityDelegate activityDelegate) {
		this.activityDelegate = activityDelegate;

		Intent intent = new Intent(this, ZBarScannerActivity.class);
		startActivityForResult(intent, ZBAR_QR_SCANNER_REQUEST);
	}

	static void handleCopyToClipboard(final Context context, final String address)
	{
		final ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
		clipboardManager.setText(address);

		Toast.makeText(context, R.string.wallet_address_fragment_clipboard_msg, Toast.LENGTH_SHORT).show();
	}

	public final boolean hasInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected()) {
			return true;

		} else {
			return false;
		}
	}

	public final boolean isWifiEnabled() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (info.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	public void startP2PMode() {
		final MyRemoteWallet remoteWallet = application.getRemoteWallet();

		if (remoteWallet == null)
			return;

		if (remoteWallet.isDoubleEncrypted() == false) {
			application.startBlockchainService();
		} else {
			if (remoteWallet.temporySecondPassword == null) {
				RequestPasswordDialog.show(getSupportFragmentManager(), new SuccessCallback() {

					public void onSuccess() {
						application.startBlockchainService();
					}

					public void onFail() {
						Toast.makeText(application, R.string.password_incorrect, Toast.LENGTH_LONG).show();
					}
				}, RequestPasswordDialog.PasswordTypeSecond);
			} else {
				application.startBlockchainService();
			}
		}		
	}

	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {

		@Override
		public String getDescription() {
			return getClass() + " Wallet Check Status";
		}

		@Override
		public void onWalletDidChange() {
			application.checkWalletStatus(self);
		}

		@Override
		public void onMultiAddrError() {
			if (self instanceof WalletActivity) {
				if (lastDisplayedNetworkError > System.currentTimeMillis()-Constants.NetworkErrorDisplayThreshold)
					return;

				//Don't do anything is we are already running the blockchain service
				if (application.isInP2PFallbackMode())
					return;

				lastDisplayedNetworkError = System.currentTimeMillis();

				//Only ask for P2P mode when connected to wifi 
				if (!hasInternetConnection() || !isWifiEnabled()) {
					handler.post(new Runnable() {
						public void run() {
							AlertDialog.Builder builder = new AlertDialog.Builder(self);

							builder.setTitle(R.string.network_error);

							builder.setMessage(R.string.network_error_description);

							builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.dismiss();
								}
							});

							builder.show();
						}
					});
				} else {
					handler.post(new Runnable() {
						public void run() {
							AlertDialog.Builder builder = new AlertDialog.Builder(self);

							builder.setTitle(R.string.blockchain_network_error);

							builder.setMessage(R.string.blockchain_network_error_description);

							builder.setNegativeButton(R.string.ignore, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									dialog.dismiss();
								}
							});

							builder.setPositiveButton(R.string.start_p2p_mode, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									startP2PMode();	
								}
							});

							builder.show();
						}
					});
				}
			}
		};
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		application = (WalletApplication) this.getApplication();
	}

	@Override
	protected void onResume() {
		super.onResume();

		EventListeners.addEventListener(eventListener);

		if (!dontCheckStatus) {
			application.checkWalletStatus(self);
		}

		application.connect();
	}

	@Override
	protected void onPause() {
		super.onPause();

		EventListeners.removeEventListener(eventListener);

		application.disconnectSoon();
	}

	@Override
	protected void onStart() {
		super.onStart();

		/*
		if (getActionBarFragment() != null) {
			actionBar.setIcon(Constants.APP_ICON_RESID);
			actionBar.setSecondaryTitle(Constants.TEST ? "[testnet]" : null);
		}
		*/
	}

	/*
	public ActionBarFragment getActionBarFragment() {
		if (actionBar == null)
			actionBar = (ActionBarFragment) getSupportFragmentManager()
			.findFragmentById(R.id.action_bar_fragment);

		return actionBar;
	}
	*/

	public final void toast(final String text, final Object... formatArgs) {
		toast(text, 0, Toast.LENGTH_SHORT, formatArgs);
	}

	public final void longToast(final String text, final Object... formatArgs) {
		toast(text, 0, Toast.LENGTH_LONG, formatArgs);
	}

	public final void toast(final String text, final int imageResId,
			final int duration, final Object... formatArgs) {

		if (text == null)
			return;

		final View view = getLayoutInflater().inflate(
				R.layout.transient_notification, null);
		TextView tv = (TextView) view
				.findViewById(R.id.transient_notification_text);
		tv.setText(String.format(text, formatArgs));
		tv.setCompoundDrawablesWithIntrinsicBounds(imageResId, 0, 0, 0);

		final Toast toast = new Toast(this);
		toast.setView(view);
		toast.setDuration(duration);
		toast.show();
	}

	public final void toast(final int textResId, final Object... formatArgs) {
		toast(textResId, 0, Toast.LENGTH_SHORT, formatArgs);
	}

	public final void longToast(final int textResId, final Object... formatArgs) {
		toast(textResId, 0, Toast.LENGTH_LONG, formatArgs);
	}

	public final void toast(final int textResId, final int imageResId,
			final int duration, final Object... formatArgs) {
		final View view = getLayoutInflater().inflate(
				R.layout.transient_notification, null);
		TextView tv = (TextView) view
				.findViewById(R.id.transient_notification_text);
		tv.setText(getString(textResId, formatArgs));
		tv.setCompoundDrawablesWithIntrinsicBounds(imageResId, 0, 0, 0);

		final Toast toast = new Toast(this);
		toast.setView(view);
		toast.setDuration(duration);
		toast.show();
	}

	public void errorDialog(final int title, final String message) {
		final Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setNeutralButton(R.string.button_dismiss, null);
		dialog.show();
	}

	public void errorDialog(final int title, final String message, final DialogInterface.OnClickListener dismiss) {
		final Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setNeutralButton(R.string.button_dismiss, dismiss);
		dialog.show();
	}

	protected final static String languagePrefix() {
		final String language = Locale.getDefault().getLanguage();
		if ("de".equals(language))
			return "_de";
		else if ("cs".equals(language))
			return "_cs";
		else if ("el".equals(language))
			return "_el";
		else if ("es".equals(language))
			return "_es";
		else if ("fr".equals(language))
			return "_fr";
		else if ("it".equals(language))
			return "_it";
		else if ("nl".equals(language))
			return "_nl";
		else if ("pl".equals(language))
			return "_pl";
		else if ("ru".equals(language))
			return "_ru";
		else if ("sv".equals(language))
			return "_sv";
		else if ("tr".equals(language))
			return "_tr";
		else if ("zh".equals(language))
			return "_zh";
		else
			return "";
	}

	public void showMarketPage(final String packageName) {
		final Intent marketIntent = new Intent(Intent.ACTION_VIEW,
				Uri.parse(String.format(Constants.MARKET_APP_URL, packageName)));
		if (getPackageManager().resolveActivity(marketIntent, 0) != null)
			startActivity(marketIntent);
		else
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(String
					.format(Constants.WEBMARKET_APP_URL, packageName))));
	}

	private void handleScanPrivateKeyPair(final ECKey key) throws Exception {
		final AlertDialog.Builder b = new AlertDialog.Builder(this);

		b.setPositiveButton(R.string.sweep_text, null);

		b.setNeutralButton(R.string.import_text, null);

		b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		b.setTitle("Scan Private Key");

		b.setMessage("Fetching Balance. Please Wait");

		final AlertDialog dialog = b.show();

		dialog.getButton(Dialog.BUTTON1).setEnabled(false);					

		new Thread() {
			public void run() {
				try {
					final String address;
					if (key.isCompressed()) {
						address = key.toAddressCompressed(MainNetParams.get()).toString();
					} else {
						address = key.toAddressUnCompressed(MainNetParams.get()).toString();
					}

					System.out.println("Scanned PK Address " + address);

					BigInteger balance = MyRemoteWallet.getAddressBalance(address);

					final BigInteger finalBalance = balance;

					handler.post(new Runnable() {
						@Override
						public void run() {
							final MyRemoteWallet remoteWallet = application.getRemoteWallet();

							if (remoteWallet == null)
								return;

							dialog.getButton(Dialog.BUTTON3).setEnabled(true);

							if (finalBalance.longValue() == 0) {
								dialog.setMessage("The Balance of address " + address + " is zero.");	
							} else {
								dialog.getButton(Dialog.BUTTON1).setEnabled(true);
								dialog.setMessage("The Balance of "+address+" is "+WalletUtils.formatValue(finalBalance)+" BTC. Would you like to sweep it?");
							}

							dialog.getButton(Dialog.BUTTON3).setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {

									if (remoteWallet.isDoubleEncrypted() == false) {
										reallyAddKey(dialog, key);
									} else {
										if (remoteWallet.temporySecondPassword == null) {
											RequestPasswordDialog.show(
													getSupportFragmentManager(),
													new SuccessCallback() {

														public void onSuccess() {
															reallyAddKey(dialog, key);														}

														public void onFail() {
														}
													}, RequestPasswordDialog.PasswordTypeSecond);
										} else {
											reallyAddKey(dialog, key);
										}
									}
								}
							});

							dialog.getButton(Dialog.BUTTON1).setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {

									try {
										MyRemoteWallet wallet = new MyRemoteWallet();

										wallet.addKey(key, address, null);

										Address to = application.determineSelectedAddress();

										if (to == null) {
											handler.post(new Runnable() {
												public void run() {
													dialog.dismiss();
												}
											});

											return;
										}

										BigInteger baseFee = wallet.getBaseFee();

										wallet.simpleSendCoinsAsync(to.toString(), finalBalance.subtract(baseFee), MyRemoteWallet.FeePolicy.FeeForce, baseFee, new SendProgress() {

											@Override
											public boolean onReady(
													Transaction tx,
													BigInteger fee,
													MyRemoteWallet.FeePolicy feePolicy,
													long priority) {
												return true;
											}

											@Override
											public void onSend(
													Transaction tx,
													String message) {
												handler.post(new Runnable() {
													public void run() {
														dialog.dismiss();

														longToast("Private Key Successfully Swept");
													}
												});
											}

											@Override
											public ECKey onPrivateKeyMissing(String address) {
												return null;
											}

											@Override
											public void onError(final String message) {
												handler.post(new Runnable() {
													public void run() {
														dialog.dismiss();

														longToast(message);
													}
												});															
											}

											@Override
											public void onProgress(String message) {}

											@Override
											public void onStart() {												
											}
										});

									} catch (final Exception e) {
										e.getLocalizedMessage();

										handler.post(new Runnable() {
											public void run() {
												dialog.dismiss();

												longToast(e.getLocalizedMessage());
											}
										});
									}
								}
							});
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();

					handler.post(new Runnable() {
						public void run() {
							dialog.dismiss();

							longToast(e.getLocalizedMessage());
						}
					});
				}
			}
		}.start();
	}

	private void reallyAddKey(final Dialog dialog, final ECKey key) {
		application.addKeyToWallet(key, key.toAddress(MainNetParams.get()).toString(), null, 0,
				new AddAddressCallback() {

			public void onSavedAddress(String address) {
				longToast("Private Key Successfully Imported");

				dialog.dismiss();
			}

			public void onError(String reason) {
				Toast.makeText(self, reason, Toast.LENGTH_LONG).show();

				dialog.dismiss();
			}
		});
	}


	public void handleScanPrivateKey(final String data) throws Exception {

		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					final String format = WalletUtils.detectPrivateKeyFormat(data);

					System.out.println("Scanned Private Key Format " + format);

					if (format.equals("bip38")) {
						RequestPasswordDialog.show(getSupportFragmentManager(), new SuccessCallback() {

							public void onSuccess() {
								String password = RequestPasswordDialog.getPasswordResult();

								try {
									handleScanPrivateKeyPair(WalletUtils.parsePrivateKey(format, data, password));
								} catch (Exception e) {
									longToast(e.getLocalizedMessage());
								}
							}

							public void onFail() {

							}
						}, RequestPasswordDialog.PasswordTypePrivateKey);
					} else {
						handleScanPrivateKeyPair(WalletUtils.parsePrivateKey(format, data, null));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 100);

	}

	public void handleAddWatchOnly(String data) throws Exception {

		String address;
		try {
			address = new Address(Constants.NETWORK_PARAMETERS, data).toString();
		} catch (Exception e) {
			try {
				BitcoinURI uri = new BitcoinURI(data);

				address = uri.getAddress().toString();
			} catch (Exception e1) {
				longToast(R.string.send_coins_fragment_receiving_address_error);

				return;
			}
		}

		final AlertDialog.Builder b = new AlertDialog.Builder(this);

		final String finalAddress = address;
		b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (application.getRemoteWallet() == null)
					return;

				try {
					application.getRemoteWallet().addWatchOnly(finalAddress, "android_watch_only");
					
					application.saveWallet(new SuccessCallback() {
						@Override
						public void onSuccess() {
//							EditAddressBookEntryFragment.edit(getSupportFragmentManager(), finalAddress);
						}

						@Override
						public void onFail() {
						}
					});
				} catch (Exception e) {					
					longToast(e.getLocalizedMessage());
				}
			}
		});

		b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});

		b.setTitle("Watch Only Address");

		b.setMessage("Do you wish to add the Watch Only bitcoin address " + address + " to your wallet? \n\nYou will not be able to spend any funds in this address unless you have the private key stored elsewhere. You should never add a Watch Only address that you do not have the private key for.");

		b.show();				
	}

}
