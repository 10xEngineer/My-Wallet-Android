package info.blockchain.wallet.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.zbar.Symbol;
import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyRemoteWallet.FeePolicy;
import piuk.blockchain.android.MyRemoteWallet.SendProgress;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
//import piuk.blockchain.android.service.BlockchainServiceImpl;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.ui.dialogs.RequestPasswordDialog;
import piuk.blockchain.android.util.WalletUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView.OnEditorActionListener;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextThemeWrapper;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
//import android.util.Log;
import android.util.Pair;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.params.MainNetParams;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class SendFragment extends Fragment   {
	private static final String SendTypeQuickSend = "Quick Send";
	private static final String SendTypeCustomSend = "Custom Send";
	private static final String SendTypeSharedCoin = "Shared Coin";

	private static final String NEW_ADDRESS = "New address";
	private static final String SELECT_ADDRESS = "Select address";

	private static int SIMPLE_SEND = 1;
	private static int CUSTOM_SEND = 2;
	private static int SHARED_SEND = 3;

    private static int SCAN_PRIVATE_KEY_FOR_SENDING = 1;
	private static int PICK_CONTACT = 10;
	private static int SELECT_INTL_PREFIX = 11;
	private static int ZBAR_SCANNER_REQUEST = 2026;

	private static int CURRENT_SEND = SIMPLE_SEND;

	private LinearLayout lastSendingAddress = null;

	private boolean addressesOn = false;
	private boolean contactsOn = true;
	private boolean phoneContactsOn = false;

	private View rootView = null;

    private EditText edAmount1 = null;
    private TextView tvAmount2 = null;
    private EditText edAddress = null;
    private TextView tvCurrency = null;
    private LinearLayout summary2 = null;
    private LinearLayout summary3 = null;
    
    private TextView tvAmount = null;
    private TextView tvAmountBis = null;
    private TextView tvArrow = null;
    private TextView tvAddress = null;
    private TextView tvAddressBis = null;
	private String strCurrentFiatSymbol = "$";
	private String strCurrentFiatCode = "USD";
    
    private LinearLayout simple_spend = null;
    private LinearLayout custom_spend = null;

	private boolean isMagic = false;
    private View oldView = null;
    private LinearLayout parent = null;
    private LinearLayout magic = null;
    private int children = 0;
    private View childIcons = null;
    private View childList = null;
    private ListView magicList = null;
    
    private Switch sendMode = null;

    private LinearLayout layoutAddresses = null;
    private LinearLayout layoutContacts = null;
    private LinearLayout layoutPhoneContacts = null;
    private TextView tvAddresses = null;
    private TextView tvContacts = null;
    private TextView tvPhoneContacts = null;

    private LinearLayout icon_row = null;
    private LinearLayout magic_contacts = null;
    private LinearLayout magic_qr = null;
    private LinearLayout magic_keyboard = null;

    private Button btSend = null;
    private TextView tvSentPrompt = null;

    private ImageView ivClearInput = null;
    private boolean isKeyboard = false;

	private boolean isBTC = true;

    private List<HashMap<String,String>> magicData = null;
    private List<HashMap<String,String>> filteredDisplayList = null;
	private MagicAdapter adapter = null;
	private String currentSelectedAddress = null;

	private WalletApplication application;
	private final Handler handler = new Handler();
	private Runnable sentRunnable;
	private String sendType;
	//private BlockchainServiceImpl service;

	private List<String> activeAddresses;
	private Map<String,String> labels;
	private List<Map<String, Object>> addressBookMapList;
	private String emailOrNumber;
	private boolean sendViaEmail;
	private boolean sentViaSMS;

	private CustomSend cs = null;
	private SendProgress csProgress = null;
	private Button btConfirm = null;
	public static final String ACTION_INTENT = "info.blockchain.wallet.ui.SendFragment.BTC_ADDRESS_SCAN";

	private ProgressDialog sendingProgressDialog = null;

	private static final Map<String, ECKey> temporaryPrivateKeys = new HashMap<String, ECKey>();
	private static String scanPrivateKeyAddress = null;
	
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_INTENT.equals(intent.getAction())) {

                String address = intent.getStringExtra("BTC_ADDRESS");

//        		Toast.makeText(getActivity(), "In SendFragment:" + address, Toast.LENGTH_SHORT).show();

    			doScanInput(address);

            }
        }
    };

	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Send Fragment Listener";
		}

		@Override
		public void onCoinsSent(final Transaction tx, final long result) {
			btSend.setVisibility(View.GONE);
	        summary3.setVisibility(View.VISIBLE);
	        tvSentPrompt.setVisibility(View.VISIBLE);

	        if(sendingProgressDialog != null) {
		        sendingProgressDialog.dismiss();
	        }

	        clearSend();
		};

		@Override
		public void onCoinsReceived(final Transaction tx, final long result) {
		};

		@Override
		public void onTransactionsChanged() {
		};

		@Override
		public void onWalletDidChange() {
		}

		@Override
		public void onCurrencyChanged() {
		};
	};
	private final ServiceConnection serviceConnection = new ServiceConnection()
	{
		public void onServiceConnected(final ComponentName name, final IBinder binder)
		{
			//service = (BlockchainServiceImpl) ((BlockchainServiceImpl.LocalBinder) binder).getService();
		}

		public void onServiceDisconnected(final ComponentName name)
		{
			//service = null;
		}
	};

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

		final MainActivity activity = (MainActivity) getActivity();
		application = (WalletApplication) activity.getApplication();
		//activity.bindService(new Intent(activity, BlockchainServiceImpl.class), serviceConnection, Context.BIND_AUTO_CREATE);
    	sendViaEmail = false;
    	sentViaSMS = false;
    	
        rootView = inflater.inflate(R.layout.fragment_send, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");
        strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

    	simple_spend = (LinearLayout)rootView.findViewById(R.id.send_container);
    	custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);
    	custom_spend.setVisibility(View.GONE);
    	
    	CURRENT_SEND = SIMPLE_SEND;
    	sendType = SendTypeQuickSend;

        tvAmount = (TextView)rootView.findViewById(R.id.amount);
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis = (TextView)rootView.findViewById(R.id.amount_bis);
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow = (TextView)rootView.findViewById(R.id.arrow);
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress = (TextView)rootView.findViewById(R.id.sending_address);
        tvAddress.setVisibility(View.INVISIBLE);
        tvAddressBis = (TextView)rootView.findViewById(R.id.sending_address_bis);
        tvAddressBis.setVisibility(View.INVISIBLE);

        summary2 = (LinearLayout)rootView.findViewById(R.id.summary2);
        summary2.setVisibility(View.INVISIBLE);
        summary3 = (LinearLayout)rootView.findViewById(R.id.summary3);
        summary3.setVisibility(View.GONE);

        btSend = (Button)rootView.findViewById(R.id.send);
        tvSentPrompt = (TextView)rootView.findViewById(R.id.sent_prompt);
        tvSentPrompt.setVisibility(View.GONE);
        
        tvCurrency = (TextView)rootView.findViewById(R.id.currency);
        tvCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(isBTC) {
            		tvCurrency.setText(strCurrentFiatSymbol);
            		String tmp = edAmount1.getText().toString();
            		if(tmp.length() < 1) {
            			tmp = "0.0000";
            		}
            		String tmp2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
            		try {
            			double d = Double.parseDouble(tmp2);
            			if(0.0 == d) {
            				tmp2 = "";
            			}
            		}
            		catch(Exception e) {
            			tmp2 = "";
            		}
            		edAmount1.setText(tmp2);
                    tvAmount2.setText(tmp + " BTC");
            	}
            	else {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            		String tmp = edAmount1.getText().toString(); 
            		if(tmp.length() < 1) {
            			tmp = "0.00";
            		}
            		String tmp2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
            		try {
            			double d = Double.parseDouble(tmp2);
            			if(0.0 == d) {
            				tmp2 = "";
            			}
            		}
            		catch(Exception e) {
            			tmp2 = "";
            		}
                    edAmount1.setText(tmp2);
                    tvAmount2.setText(tmp + " " + strCurrentFiatCode);
            	}
            	isBTC = isBTC ? false : true;
            }
        });

        ivClearInput = (ImageView)rootView.findViewById(R.id.input_toggle);

    	LinearLayout divider1 = (LinearLayout)rootView.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
    	LinearLayout divider2 = (LinearLayout)rootView.findViewById(R.id.divider2);
    	divider2.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
    	LinearLayout divider3 = (LinearLayout)rootView.findViewById(R.id.divider3);
    	divider3.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
    	LinearLayout divider4 = (LinearLayout)rootView.findViewById(R.id.divider4);
    	divider4.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);

        ((ImageView)rootView.findViewById(R.id.direction)).setImageResource(R.drawable.red_arrow);
        ((TextView)rootView.findViewById(R.id.currency)).setText(strCurrentFiatSymbol);
        ((TextView)rootView.findViewById(R.id.currency)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());

        initAddressBookList();

        btSend = ((Button)rootView.findViewById(R.id.send));
        btSend.setVisibility(View.INVISIBLE);
        btSend.setOnClickListener(new Button.OnClickListener() {

			final SendProgress progress = new SendProgress() {
				public void onSend(final Transaction tx, final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);

					        if(sendingProgressDialog != null) {
						        sendingProgressDialog.dismiss();
					        }
				            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

							Intent intent = activity.getIntent();
							intent.putExtra("tx", tx.getHash());
							activity.setResult(Activity.RESULT_OK, intent);
						}
					});

					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					application.doMultiAddr(true);
				}

				public void onError(final String message) {
					handler.post(new Runnable() {
						public void run() {

							System.out.println("On Error");

					        if(sendingProgressDialog != null) {
						        sendingProgressDialog.dismiss();
					        }
							if (message != null)
								Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

							application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);
						}
					});
				}

				public void onProgress(final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);
						}
					});
				}

				public boolean onReady(Transaction tx, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy, long priority) {

					boolean containsOutputLessThanThreshold = false;
					for (TransactionOutput output : tx.getOutputs()) {
						if (output.getValue().compareTo(Constants.FEE_THRESHOLD_MIN) < 0) {
							containsOutputLessThanThreshold = true;
							break;
						}
					}

					if (feePolicy != MyRemoteWallet.FeePolicy.FeeNever && fee.compareTo(BigInteger.ZERO) == 0) {
						if (tx.bitcoinSerialize().length > 1000 || containsOutputLessThanThreshold) {
							makeTransaction(MyRemoteWallet.FeePolicy.FeeForce);
							return false;
						} else if (priority < 97600000L) {
							handler.post(new Runnable() {
								public void run() {
	    					        if(sendingProgressDialog != null) {
	    						        sendingProgressDialog.dismiss();
	    					        }
									AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
									builder.setMessage(R.string.ask_for_fee)
									.setCancelable(false);

									AlertDialog alert = builder.create();

									alert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.continue_without_fee), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											makeTransaction(MyRemoteWallet.FeePolicy.FeeNever);
											dialog.dismiss();
										} }); 

									alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.add_fee), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											makeTransaction(MyRemoteWallet.FeePolicy.FeeForce);

											dialog.dismiss();
										}}); 

									alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int id) {
											dialog.dismiss();
										}});

									alert.show();
								}
							});

							handler.post(new Runnable() {
								public void run() {
									application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);
								}
							});
							return false;
						}
					}

					return true;
				}

				public ECKey onPrivateKeyMissing(final String address) {

					if (SendFragment.temporaryPrivateKeys.containsKey(address)) {
						return SendFragment.temporaryPrivateKeys.get(address);
					}

					handler.post(new Runnable() {
						public void run() {
					        if(sendingProgressDialog != null) {
						        sendingProgressDialog.dismiss();
					        }
							AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
							builder.setMessage(getString(R.string.ask_for_private_key, address))
							.setCancelable(false)
							.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									SendFragment.scanPrivateKeyAddress = address;

									Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
									intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
					        		startActivityForResult(intent, SCAN_PRIVATE_KEY_FOR_SENDING);	    		
								}
							})
							.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {

									synchronized (SendFragment.temporaryPrivateKeys) {
										SendFragment.temporaryPrivateKeys.notify();
									}

									dialog.cancel();
								}
							});

							AlertDialog alert = builder.create();

							alert.show();
						}
					});

					try {
						synchronized (SendFragment.temporaryPrivateKeys) {
							SendFragment.temporaryPrivateKeys.wait();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					return SendFragment.temporaryPrivateKeys.get(address);
				}

				@Override
				public void onStart() {
					handler.post(new Runnable() {
						public void run() {
							if (SendFragment.this.sendingProgressDialog == null || (SendFragment.this.sendingProgressDialog != null
									&& !SendFragment.this.sendingProgressDialog.isShowing())) {
						    	SendFragment.this.sendingProgressDialog = new ProgressDialog(getActivity());
						    	SendFragment.this.sendingProgressDialog.setCancelable(true);
						    	SendFragment.this.sendingProgressDialog.setIndeterminate(true);
						    	SendFragment.this.sendingProgressDialog.setTitle("Sending...");
						    	SendFragment.this.sendingProgressDialog.setMessage("Please wait");
						    	SendFragment.this.sendingProgressDialog.show();
							}
						}
					});					
				}
			};
        	
			final SendProgress progressEmailSMS = new SendProgress() {
				public void onSend(final Transaction tx, final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);

					        if(sendingProgressDialog != null) {
						        sendingProgressDialog.dismiss();
					        }
							Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

							Intent intent = activity.getIntent();
							intent.putExtra("tx", tx.getHash());
							activity.setResult(Activity.RESULT_OK, intent);
						}
					});

					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					application.doMultiAddr(true);
				}

				public void onError(final String message) {
					handler.post(new Runnable() {
						public void run() {

							System.out.println("On Error");

					        if(sendingProgressDialog != null) {
						        sendingProgressDialog.dismiss();
					        }
							if (message != null)
								Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

							application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);
						}
					});
				}

				public void onProgress(final String message) {
					handler.post(new Runnable() {
						public void run() {
							application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);
						}
					});
				}

				@Override
				public boolean onReady(Transaction tx, BigInteger fee,
						MyRemoteWallet.FeePolicy feePolicy, long priority) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public ECKey onPrivateKeyMissing(String address) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void onStart() {
					handler.post(new Runnable() {
						public void run() {
							if (SendFragment.this.sendingProgressDialog == null || (SendFragment.this.sendingProgressDialog != null
									&& !SendFragment.this.sendingProgressDialog.isShowing())) {
						    	SendFragment.this.sendingProgressDialog = new ProgressDialog(getActivity());
						    	SendFragment.this.sendingProgressDialog.setCancelable(true);
						    	SendFragment.this.sendingProgressDialog.setIndeterminate(true);
						    	SendFragment.this.sendingProgressDialog.setTitle("Sending...");
						    	SendFragment.this.sendingProgressDialog.setMessage("Please wait");
						    	SendFragment.this.sendingProgressDialog.show();
							}
						}
					});	
				}
			};

			public void makeTransaction(MyRemoteWallet.FeePolicy feePolicy) {
				if (application.getRemoteWallet() == null)
					return;

				try {
					MyRemoteWallet wallet = application.getRemoteWallet();

					BigInteger baseFee = wallet.getBaseFee();

					BigInteger fee = null;

					if (sendType != null && sendType.equals(SendTypeCustomSend))
						btConfirm.performClick();

					if (feePolicy == MyRemoteWallet.FeePolicy.FeeNever) {
						fee = BigInteger.ZERO;
					} else if (feePolicy == MyRemoteWallet.FeePolicy.FeeForce) {
						fee = baseFee;
					} else if (sendType != null && sendType.equals(SendTypeCustomSend)) {
						feePolicy = MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded;
						fee = cs.getFee();
					} else {
						fee = (wallet.getFeePolicy() == 1) ? baseFee : BigInteger.ZERO;
					}

					if (sendType != null && sendType.equals(SendTypeSharedCoin)) {

					} else {

						String addressString = application.getRemoteWallet().getToAddress(edAddress.getText().toString());

						Address receivingAddress = new Address(Constants.NETWORK_PARAMETERS, addressString);

						if (sendType != null && sendType == SendTypeQuickSend) {
							quickSend(receivingAddress, fee, feePolicy);
						} else if (sendType != null && sendType == SendTypeCustomSend) {
							if (isCustomSendInputsCorrect()) {
								customSend(receivingAddress, fee, feePolicy);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public void quickSend(Address receivingAddress, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy) throws Exception {
				if (application.getRemoteWallet() == null)
					return;

				final BigInteger amount = getBTCEnteredOutputValue(edAmount1.getText().toString());
				final WalletApplication application = (WalletApplication) getActivity().getApplication();
				if (application.isInP2PFallbackMode()) {
					throw new Exception("P2PFallbackMode disabled");
					/*
					final long blockchainLag = System.currentTimeMillis() - service.blockChain.getChainHead().getHeader().getTime().getTime();

					final boolean blockchainUptodate = blockchainLag < Constants.BLOCKCHAIN_UPTODATE_THRESHOLD_MS;

					if (!blockchainUptodate) {
						activity.longToast(R.string.blockchain_not_upto_date);
						return;
					}

					// create spend
					final SendRequest sendRequest = SendRequest.to(receivingAddress, getBTCEnteredOutputValue(edAmount1.getText().toString()));
					sendRequest.fee = fee;

					new Thread(new Runnable()
					{
						public void run()
						{
							final Transaction transaction = application.bitcoinjWallet.sendCoinsOffline(sendRequest);

							handler.post(new Runnable()
							{
								public void run()
								{
									if (transaction != null)
									{
										application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);

										updateView();

										service.broadcastTransaction(transaction);

										application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);

										activity.longToast(R.string.wallet_transactions_fragment_tab_sent);

										Intent intent = activity.getIntent();
										intent.putExtra("tx", transaction.getHash());
										activity.setResult(Activity.RESULT_OK, intent);

										updateView();

										EventListeners.invokeOnTransactionsChanged();
										
									}
									else
									{
										application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);

										updateView();

										activity.longToast(R.string.send_coins_error_msg);
									}
								}
							});
						}
					}).start();
					*/
				} else {
					application.getRemoteWallet().simpleSendCoinsAsync(receivingAddress.toString(), amount, feePolicy, fee, progress);
				}
			}

			public void customSend(Address receivingAddress, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy) {
				if (application.getRemoteWallet() == null)
					return;

				if (sendType != null && !sendType.equals(SendTypeQuickSend) && application.isInP2PFallbackMode()) {
		            Toast.makeText(getActivity(), R.string.only_quick_supported, Toast.LENGTH_LONG).show();
					return;
				}

				final BigInteger amount = getBTCEnteredOutputValue(edAmount1.getText().toString());

				customSendCoinsAsync(cs.getSendingAddresses(), receivingAddress.toString(), amount, feePolicy, fee, progress);
			}

			
		    private void customSendCoinsAsync(final HashMap<String, BigInteger> sendingAddresses, final String toAddress, final BigInteger amount, final FeePolicy feePolicy, final BigInteger fee, final SendProgress progress) {
				String changeAddress = cs.getChangeAddress();
//				Log.d("MyRemoteWallet", "MyRemoteWallet customSendCoinsAsync changeAddress: " + changeAddress);
				
				if (changeAddress == null || changeAddress.equals(SELECT_ADDRESS)) {
					
					application.getRemoteWallet().sendCoinsAsync(cs.getSendingAddresses(), currentSelectedAddress, amount, feePolicy, fee, null, csProgress);
					
				} else if (! changeAddress.equals(NEW_ADDRESS)) {
					
					application.getRemoteWallet().sendCoinsAsync(cs.getSendingAddresses(), currentSelectedAddress, amount, feePolicy, fee, changeAddress, csProgress);
					
				} else {
					progress.onStart();
		        	final ECKey key = application.getRemoteWallet().generateECKey();			
		    		application.addKeyToWallet(key, key.toAddress(MainNetParams.get()).toString(), null, 0, new AddAddressCallback() {
		    			public void onSavedAddress(String address) {
		    				application.getRemoteWallet().sendCoinsAsync(cs.getSendingAddresses(), currentSelectedAddress, amount, feePolicy, fee, address, csProgress);
		    			}

		    			public void onError(String reason) {
		    				Toast.makeText(getActivity(), reason, Toast.LENGTH_LONG).show();
							progress.onError(reason);
		    			}
		    		});
		    	}
				
		    }
		    
            public void onClick(View v) {
				if (application.getRemoteWallet() == null)
					return;

				final MyRemoteWallet remoteWallet = application.getRemoteWallet();

				final MyRemoteWallet.FeePolicy feePolicy;

				if (sendType != null && sendType == SendTypeQuickSend) {
					
					feePolicy = MyRemoteWallet.FeePolicy.FeeForce;
				} else {
					feePolicy = MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded;
				}

				if (remoteWallet.isDoubleEncrypted() && remoteWallet.temporySecondPassword == null) {
					RequestPasswordDialog.show(getFragmentManager(), new SuccessCallback() {

						public void onSuccess() {							
							if(sendViaEmail && emailOrNumber != null && emailOrNumber.contains("@")) {	

				            	try {
									remoteWallet.sendCoinsEmail(emailOrNumber, getBTCEnteredOutputValue(edAmount1.getText().toString()), progressEmailSMS);
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else if (sentViaSMS && emailOrNumber != null) {								
								try {
									String numberFormated = emailOrNumber.replaceAll("\\D+","");	
									numberFormated = "+"+numberFormated;
//									Log.d("sendCoinsSMS", "numberFormated: "+ numberFormated);
									remoteWallet.sendCoinsSMS(numberFormated, getBTCEnteredOutputValue(edAmount1.getText().toString()), progressEmailSMS);										
								} catch (Exception e) {
									e.printStackTrace();
								}								
							} else {
								makeTransaction(feePolicy);
							}
						}

						public void onFail() {
							Toast.makeText(application, R.string.send_no_password_error, Toast.LENGTH_LONG).show();
						}
					}, RequestPasswordDialog.PasswordTypeSecond);
				} else {
					if(sendViaEmail && emailOrNumber != null && emailOrNumber.contains("@")) {	
						try {
							remoteWallet.sendCoinsEmail(emailOrNumber, getBTCEnteredOutputValue(edAmount1.getText().toString()), progressEmailSMS);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (sentViaSMS && emailOrNumber != null) {								
						try {
							String numberFormated = emailOrNumber.replaceAll("\\D+","");	
							numberFormated = "+"+numberFormated;
//							Log.d("sendCoinsSMS", "numberFormated: "+ numberFormated);
							remoteWallet.sendCoinsSMS(numberFormated, getBTCEnteredOutputValue(edAmount1.getText().toString()), progressEmailSMS);										
						} catch (Exception e) {
							e.printStackTrace();
						}								
					} else {
						makeTransaction(feePolicy);
					}
				}

            }

        });

        tvAmount2 = ((TextView)rootView.findViewById(R.id.amount2));
        tvAmount2.setText("0.00" + " " + strCurrentFiatCode);
        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
      	edAmount1.setText("");
      	if(isBTC) {
          	edAmount1.setHint("0.0000");
      	}
      	else {
          	edAmount1.setHint("0.00");
      	}
        edAmount1.setFocusableInTouchMode(true);
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {

		        	if(edAddress.getText().toString() == null || edAddress.getText().toString().length() < 1) {
						Toast.makeText(getActivity(), "Include a Bitcoin sending address", Toast.LENGTH_LONG).show();
		        		return false;
		        	}
		        	
		        	if(sendType == SendTypeCustomSend) {
                        //doCustomSend();
		            }
		        	
		        	
		        	

			        	summary2.setVisibility(View.VISIBLE);
			        	tvAddress.setVisibility(View.VISIBLE);
			        	tvAddressBis.setVisibility(View.VISIBLE);
			        	tvArrow.setVisibility(View.VISIBLE);
			        	tvAmount.setVisibility(View.VISIBLE);
			        	tvAmountBis.setVisibility(View.VISIBLE);

			        	if(edAddress.getText().toString().length() > 15) {
				        	tvAddress.setText(edAddress.getText().toString().substring(0, 15) + "...");
			        	}
			        	else {
				        	tvAddress.setText(edAddress.getText().toString());
			        	}

	 		            if(currentSelectedAddress != null) {
	 		            	tvAddressBis.setText(currentSelectedAddress.substring(0, 20) + "...");
	 		            }
	 		            else {
	 		            	tvAddressBis.setVisibility(View.GONE);
	 		            }

	 		            if(BitcoinAddressCheck.isValidAddress(edAddress.getText().toString())) {
	 		            	tvAddressBis.setVisibility(View.GONE);
	 		            }

			        	tvArrow.setText(Character.toString((char)0x2192));

			        	String amount1 = edAmount1.getText().toString();
			        	if(amount1 == null || amount1.length() < 1) {
			        		amount1 = "0.00";
			        	}
			        	String amount2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);	// buggy
			        	if(isBTC) {
			        		amount1 += " BTC";
			        		amount2 += " " + strCurrentFiatCode;
			        	}
			        	else {
			        		amount1 += " " + strCurrentFiatCode;
			        		amount2 += " BTC";
			        	}
			        	SpannableStringBuilder a1 = new SpannableStringBuilder(amount1);
			        	SpannableStringBuilder a2 = new SpannableStringBuilder(amount2);
			        	a1.setSpan(new SuperscriptSpan(), amount1.length() - 4, amount1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	a1.setSpan(new RelativeSizeSpan((float)0.50), amount1.length() - 4, amount1.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	a2.setSpan(new SuperscriptSpan(), amount2.length() - 4, amount2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	a2.setSpan(new RelativeSizeSpan((float)0.50), amount2.length() - 4, amount2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			        	tvAmount.setText(a1);
			        	tvAmountBis.setText(a2);

		            	btSend.setVisibility(View.VISIBLE);

			        	edAmount1.clearFocus();
		                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		                imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

		        	

		        }
		        return false;
		    }
		});

        edAmount1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	;
            }
        });

        edAmount1.addTextChangedListener(new TextWatcher()	{

        	public void afterTextChanged(Editable s) {
        		if((edAddress.getText().toString() != null && edAddress.getText().toString().length() > 0) || (edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0)) {
        			
        			if(isBTC)	{
            			tvAmount2.setText(BlockchainUtil.BTC2Fiat(edAmount1.getText().toString()) + " " + strCurrentFiatCode);
        			}
        			else	{
        				tvAmount2.setText(BlockchainUtil.Fiat2BTC(edAmount1.getText().toString()) + " BTC");
        			}

        			ivClearInput.setVisibility(View.VISIBLE);
        		}
        		else {
        			ivClearInput.setVisibility(View.INVISIBLE);
        		}
        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{ ; }
        });

        /*
        edAmount1.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                    if(edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0) {
            			edAmount1.setText("");
                    	if(isBTC) {
                			edAmount1.setHint("0.0000");
                    	}
                    	else {
                			edAmount1.setHint("0.00");
                    	}
                    }
                }
            }
        });
        */

        edAddress = ((EditText)rootView.findViewById(R.id.address));
        edAddress.setHint(R.string.send_payment_hint);
        edAddress.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	/*
            	if(!isMagic) {
            		displayMagicList();
            	}
            	else {
            		removeMagicList();
            	}
            	*/
            }
        });

        edAddress.setOnTouchListener(new OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	int inType = edAddress.getInputType(); // backup the input type

                edAddress.setInputType(InputType.TYPE_NULL); // disable soft input
                edAddress.onTouchEvent(event); // call native handler
                edAddress.setInputType(inType); // restore input type
                edAddress.setFocusable(true);

                return true; // consume touch even
            }
        });

        edAddress.addTextChangedListener(new TextWatcher()	{

        	public void afterTextChanged(Editable s) {
        		;
        	}

        	public void beforeTextChanged(CharSequence s, int start, int count, int after)	{ ; }
        
        	public void onTextChanged(CharSequence s, int start, int before, int count)	{        		
        		String inputAddress = edAddress.getText().toString();
        		int len = edAddress.getText().length();
        		
                if(len < 1) {
                	ivClearInput.setVisibility(View.INVISIBLE);
                	isKeyboard = true;
                }
                else {
                	ivClearInput.setVisibility(View.VISIBLE);
                	isKeyboard = false;
                }

        		List<HashMap<String,String>> filtered = new ArrayList<HashMap<String,String>>();
        		
        		for (HashMap<String,String> row : magicData) {
        			String labelOrAddress = row.get("labelOrAddress");
        		    if (len <= labelOrAddress.length()) {
            			if(inputAddress.equalsIgnoreCase((String) labelOrAddress.subSequence(0, len))) {
            				filtered.add(row);
            			}
        		    }
        		}

        		if (BitcoinAddressCheck.isValidAddress(inputAddress)) {
            		currentSelectedAddress = inputAddress;                        			
        		} else {
            		currentSelectedAddress = null;                        			
        		}
                filteredDisplayList = filtered;
                if(adapter != null)	{
            		adapter.notifyDataSetChanged();
                }
                
                //clear emailOrNumber if made change to name in edit text
                emailOrNumber = "";
            	sendViaEmail = false;
            	sentViaSMS = false;
            }
        });

        edAddress.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_NEXT) {

		        	if(labels.get(edAddress.getText().toString()) == null) {
	 		            if(!BitcoinAddressCheck.isValidAddress(edAddress.getText().toString())) {
							Toast.makeText(getActivity(), edAddress.getText().toString() + " is not a valid Bitcoin address", Toast.LENGTH_LONG).show();
	 		            	return false;
	 		            }
		        	}

		        	if(isMagic) {
		        		removeMagicList();
		        	}

	                icon_row.setVisibility(View.GONE);

	                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
	                edAmount1.requestFocus();
	                edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
	                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

		        }
		        return false;
		    }
		});

        ivClearInput.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
            	clearSend();

                return false;
            }
        });

        /*
        final ImageView imgSimpleSend = ((ImageView)rootView.findViewById(R.id.simple));
        final ImageView imgCustomSend = ((ImageView)rootView.findViewById(R.id.custom));
//        final ImageView imgSharedSend = ((ImageView)rootView.findViewById(R.id.shared));
        final LinearLayout layoutSimpleSend = ((LinearLayout)rootView.findViewById(R.id.simple_bg));
        final LinearLayout layoutCustomSend = ((LinearLayout)rootView.findViewById(R.id.custom_bg));
//        final LinearLayout layoutSharedSend = ((LinearLayout)rootView.findViewById(R.id.shared_bg));
        
        final int color_spend_selected = 0xff808080;
        final int color_spend_unselected = 0xffa0a0a0;
        
    	imgSimpleSend.setBackgroundColor(color_spend_selected);
    	imgCustomSend.setBackgroundColor(color_spend_unselected);
//    	imgSharedSend.setBackgroundColor(color_spend_unselected);
    	layoutSimpleSend.setBackgroundColor(color_spend_selected);
    	layoutCustomSend.setBackgroundColor(color_spend_unselected);
//    	layoutSharedSend.setBackgroundColor(color_spend_unselected);

        layoutSimpleSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	sendType = SendTypeQuickSend;

            	imgSimpleSend.setBackgroundColor(color_spend_selected);
            	imgCustomSend.setBackgroundColor(color_spend_unselected);
//            	imgSharedSend.setBackgroundColor(color_spend_unselected);
            	layoutSimpleSend.setBackgroundColor(color_spend_selected);
            	layoutCustomSend.setBackgroundColor(color_spend_unselected);
//            	layoutSharedSend.setBackgroundColor(color_spend_unselected);

            	doSimpleSend();

                return false;
            }
        });

        layoutCustomSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	sendType = SendTypeCustomSend;

            	imgSimpleSend.setBackgroundColor(color_spend_unselected);
            	imgCustomSend.setBackgroundColor(color_spend_selected);
//            	imgSharedSend.setBackgroundColor(color_spend_unselected);
            	layoutSimpleSend.setBackgroundColor(color_spend_unselected);
            	layoutCustomSend.setBackgroundColor(color_spend_selected);
//            	layoutSharedSend.setBackgroundColor(color_spend_unselected);
            	
    			doCustomSend();

                return false;
            }
        });
        */

        /*
        imgSharedSend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	sendType = SendTypeSharedCoin;            	

            	imgSimpleSend.setBackgroundColor(color_spend_unselected);
            	imgCustomSend.setBackgroundColor(color_spend_unselected);
            	imgSharedSend.setBackgroundColor(color_spend_selected);
            	layoutSimpleSend.setBackgroundColor(color_spend_unselected);
            	layoutCustomSend.setBackgroundColor(color_spend_unselected);
            	layoutSharedSend.setBackgroundColor(color_spend_selected);

    			doSharedCoin();

                return true;
            }
        });
        */
        
        sendMode = (Switch)rootView.findViewById(R.id.mode);
        sendMode.setChecked(false);
        sendMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        	@Override
        	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        		if(isChecked)	{
        			doCustomSend();
        		}
        		else	{
        			doSimpleSend();
        		}
        	}
        });

		final int colorOn = 0xFF808080;
		final int colorOff = 0xFFffffff;
        icon_row = ((LinearLayout)rootView.findViewById(R.id.icon_row));
        magic = ((LinearLayout)rootView.findViewById(R.id.magic_input));

        magic_contacts = (LinearLayout)magic.findViewById(R.id.magic2_contact);
        magic_contacts.setBackgroundColor(colorOff);
        magic_contacts.setOnTouchListener(new OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
          	
              switch (event.getAction())	{
              	case android.view.MotionEvent.ACTION_DOWN:
              	case android.view.MotionEvent.ACTION_MOVE:
              		magic_contacts.setBackgroundColor(colorOn);                		
              		break;
              	case android.view.MotionEvent.ACTION_UP:
              	case android.view.MotionEvent.ACTION_CANCEL:
              		magic_contacts.setBackgroundColor(colorOff);
              		
                	if(!isMagic) {
                		displayMagicList();
                	}
                	else {
                		removeMagicList();
                	}

              		break;
              	}

              return true;
          }
        });

        magic_qr = (LinearLayout)magic.findViewById(R.id.magic2_camera);
        magic_qr.setBackgroundColor(colorOff);
        magic_qr.setOnTouchListener(new OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
          	
              switch (event.getAction())	{
              	case android.view.MotionEvent.ACTION_DOWN:
              	case android.view.MotionEvent.ACTION_MOVE:
              		magic_qr.setBackgroundColor(colorOn);
              		Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
              		intent.putExtra(ZBarConstants.SCAN_MODES, new int[]{ Symbol.QRCODE } );
              		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);
              		magic_qr.setBackgroundColor(colorOff);
              		break;
              	case android.view.MotionEvent.ACTION_UP:
              	case android.view.MotionEvent.ACTION_CANCEL:
              		magic_qr.setBackgroundColor(colorOff);
              		break;
              	}

              return false;
          }
        });

        magic_keyboard = (LinearLayout)magic.findViewById(R.id.magic2_keyboard);
        magic_keyboard.setBackgroundColor(colorOff);
        magic_keyboard.setOnTouchListener(new OnTouchListener() {
          @Override
          public boolean onTouch(View v, MotionEvent event) {
          	
              switch (event.getAction())	{
              	case android.view.MotionEvent.ACTION_DOWN:
              	case android.view.MotionEvent.ACTION_MOVE:
              		magic_keyboard.setBackgroundColor(colorOn);                		
              		break;
              	case android.view.MotionEvent.ACTION_UP:
              	case android.view.MotionEvent.ACTION_CANCEL:
              		magic_keyboard.setBackgroundColor(colorOff);
              		
                	if(isKeyboard) {
                		edAddress.requestFocus();
                    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(edAddress, InputMethodManager.SHOW_IMPLICIT);
                    	isKeyboard = false;
                	}
                	else {
//                    	clearSend();
                    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    	                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
                    	isKeyboard = true;
                	}

              		break;
              	}

              return true;
          }
        });

		EventListeners.addEventListener(eventListener);

	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
	    tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));

	    mListener.onComplete();

        return rootView;
    }

    public BigInteger getBTCEnteredOutputValue(String edAmount) {
		String amountString = edAmount.trim();
    	if(!isBTC) {
    		return BlockchainUtil.bitcoinAmountStringToBigInteger(BlockchainUtil.Fiat2BTC(amountString));
    	} else {
    		return BlockchainUtil.bitcoinAmountStringToBigInteger(amountString);
    	}
    }    
    
    public static interface OnCompleteListener {
        public abstract void onComplete();
    }

    private OnCompleteListener mListener;

    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	try {
            this.mListener = (OnCompleteListener)activity;
        }
        catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        
//        Log.d("BlockchainWallet", "setUserVisible");

        if(isVisibleToUser) {
        	
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            strCurrentFiatCode = prefs.getString("ccurrency", "USD");
            strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

            if(isBTC) {
        		tvAmount2.setText(tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4) + " " + strCurrentFiatCode);
            }
            else {
        		tvCurrency.setText(strCurrentFiatSymbol);
            }

            if(edAddress.getText().length() < 1 && (edAmount1.getText().length() < 1 || edAmount1.getText().equals("0.0000"))) {
            	ivClearInput.setVisibility(View.INVISIBLE);
            	isKeyboard = true;
            }
            else {
            	ivClearInput.setVisibility(View.VISIBLE);
            	isKeyboard = false;
            }
        }
    }

    @Override
    public void onResume() {
    	super.onResume();

//        Log.d("BlockchainWallet", "onResume");

//        IntentFilter filter = new IntentFilter(ACTION_INTENT);
//        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

        if(edAddress.getText().length() < 1 && (edAmount1.getText().length() < 1 || edAmount1.getText().equals("0.0000"))) {
        	ivClearInput.setVisibility(View.INVISIBLE);
        	isKeyboard = true;
        }
        else {
        	ivClearInput.setVisibility(View.VISIBLE);
        	isKeyboard = false;
        }

    }

    @Override
    public void onPause() {
    	super.onPause();

 //       LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

	@Override
	public void onDestroyView()
	{
		super.onDestroyView();

		handler.removeCallbacks(sentRunnable);

		//getActivity().unbindService(serviceConnection);
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if (application.getRemoteWallet() == null)
			return;

		//Clear the second password
		MyRemoteWallet remoteWallet = application.getRemoteWallet();

		remoteWallet.setTemporySecondPassword(null);

	    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);

	    EventListeners.removeEventListener(eventListener);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		application.setIsScanning(false);
		if(resultCode == Activity.RESULT_OK && requestCode == SCAN_PRIVATE_KEY_FOR_SENDING)	{
    		String scanData = data.getStringExtra(ZBarConstants.SCAN_RESULT);
    		try {
				this.handleScanPrivateKey(scanData);
			} catch (Exception e) {
	            Toast.makeText(application, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();				
				e.printStackTrace();
			}			
		} else if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{

			String address = data.getStringExtra(ZBarConstants.SCAN_RESULT);
//        	Log.d("Scan result", strResult);

			doScanInput(address);

        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == ZBAR_SCANNER_REQUEST) {
//            Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
        }
		else if(requestCode == PICK_CONTACT && resultCode == Activity.RESULT_OK) {

			if (data != null) {

				Uri uri = data.getData();

		        if (uri != null) {

		    	    Cursor cur = getActivity().getContentResolver().query(uri, null, null, null, null);

                	String strEmail = null;
                	String strNumber = null;

		    	    try 
		    	    {
		                while(cur.moveToNext())
		                {
		                	strEmail = strNumber = null;

		                    String id = cur.getString(cur.getColumnIndex(Contacts._ID));
		                    String strName = cur.getString(cur.getColumnIndex(Contacts.DISPLAY_NAME));

//		                    strImageURI = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));

		                    Cursor ce = getActivity().getContentResolver().query(CommonDataKinds.Email.CONTENT_URI, null, CommonDataKinds.Email.CONTACT_ID +" = ?", new String[]{id}, null);
		                    while(ce.moveToNext())
		                    {
		                        strEmail = ce.getString(ce.getColumnIndex(CommonDataKinds.Email.ADDRESS));
		                        strEmail = (strEmail.equals("null")) ? null : strEmail;
		                    }
		                    ce.close();

		                    Cursor cn = getActivity().getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI, null, CommonDataKinds.Phone.CONTACT_ID +" = ?", new String[]{id}, null);
		                    while(cn.moveToNext())
		                    {
		                        int type = cn.getInt(cn.getColumnIndex(CommonDataKinds.Phone.TYPE));
		                        if (type == Phone.TYPE_MOBILE)
		                        {
		                            strNumber = cn.getString(cn.getColumnIndex(CommonDataKinds.Phone.NUMBER));
		                            strNumber = (strNumber.equals("null")) ? null : strNumber;
		                        }
		                    }
		                    cn.close();

		                    if(strEmail != null && strEmail.equals("null"))	{
		                    	strEmail = null;
		                    }
		                    if(strNumber != null && strNumber.equals("null"))	{
		                    	strNumber = null;
		                    }
		                    if(strName != null && strName.equals("null"))	{
		                    	strName = "";
		                    }

		                    if(strEmail != null && strNumber != null)	{
		                    		//
		                    		// choose send method here
		                    		//

		                    		final String em = strEmail;
		                    		final String sms = strNumber;
		                    		final String name = strName;

		                			new AlertDialog.Builder(getActivity())
		                            .setIcon(R.drawable.ic_launcher).setTitle("Send Bitcoins to a Friend")
		                            .setMessage("Send Bitcoins to " + strName + " via which method?")
		                            .setPositiveButton(em, new DialogInterface.OnClickListener() {
//		                              @Override
		                              public void onClick(DialogInterface dialog, int which) {

				                    		edAddress.setText(name);
				                    		emailOrNumber = em;
				                        	sendViaEmail = true;
				                        	sentViaSMS = false;

				                    		// go out via email here
				                    		Toast.makeText(getActivity(), em, Toast.LENGTH_SHORT).show();
		                              }
		                           })
		                            .setNegativeButton(sms, new DialogInterface.OnClickListener() {
//		                              @Override
		                              public void onClick(DialogInterface dialog, int which) {

				                    		edAddress.setText(name);
				                        	sendViaEmail = false;
				                        	sentViaSMS = true;

				                    		emailOrNumber = sms;	
				                        	if (sms.substring(0, 2).equals("00") || sms.charAt(0) == '+') {
//					                    		Log.d("emailOrNumber", "setSMSNumber: " + emailOrNumber);
				                        	} else {
				                    			doSelectInternationalPrefix();				                        		
				                        	}

				                    		// go out via sms here
				                    		Toast.makeText(getActivity(), sms, Toast.LENGTH_SHORT).show();
		                              }
		                            }
		                            ).show();

		                    	}
		                    	else if(strEmail != null)	{
		                    		//
		                    		// send via email here
		                    		//
		                    		Toast.makeText(getActivity(), strEmail, Toast.LENGTH_SHORT).show();

		                    		edAddress.setText(strName);
		                    		emailOrNumber = strEmail;
		                        	sendViaEmail = true;
		                        	sentViaSMS = false;
		                    		// go out via email here
			                    }
		                    	else if(strNumber != null)	{
		                    		//
		                    		// send via sms here
		                    		//
		                    		Toast.makeText(getActivity(), strNumber, Toast.LENGTH_SHORT).show();

		                    		edAddress.setText(strName);
		                    		emailOrNumber = strNumber;
		                        	if (strNumber.substring(0, 2).equals("00") || strNumber.charAt(0) == '+') {
//			                    		Log.d("emailOrNumber", "setSMSNumber: " + emailOrNumber);
		                        	} else {
		                    			doSelectInternationalPrefix();				                        		
		                        	}
		                        	sendViaEmail = false;
		                        	sentViaSMS = true;
		                    		//go out via sms here

			                    }
		                    	else
		                    	{
		                    		// this will be replaced by proper model dialog by Bill w/ official text
		                    		Toast.makeText(getActivity(), "To use this service select a contact with an email address or a mobile phone number. Thank you.", Toast.LENGTH_SHORT).show();
		                    	}

	                    	//
	                    	//
	                    	//
		            		if(isMagic) {
		            			removeMagicList();
		            		}
	                        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	                        imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
		                        edAmount1.requestFocus();
		                        edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
		                        imm.showSoftInput(edAmount1, InputMethodManager.SHOW_FORCED);

		                }
		    	    }
		    	    finally
		    	    {
		    	        cur.close();
		    	    }

		        }
		    }

		}
		else if(resultCode == Activity.RESULT_OK && requestCode == SELECT_INTL_PREFIX) {
    		//Toast.makeText(getActivity(), "prefix returned:" + data.getAction(), Toast.LENGTH_SHORT).show();

    		String region = "US";
    		PhoneNumberUtil p = PhoneNumberUtil.getInstance();
    		PhoneNumber pn;
    		try {
        		//emailOrNumber = "+442012345678";
    			pn = p.parse(emailOrNumber, region);
    			String nationalnumber = String.valueOf(pn.getNationalNumber());
        		emailOrNumber = "+" + data.getAction() + nationalnumber;
//        		Log.d("emailOrNumber", "setSMSNumber with prefix: " + emailOrNumber);
    		} catch (NumberParseException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
	      }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == SELECT_INTL_PREFIX) {
	  		final Context context = getActivity().getApplicationContext();

    		String region = context.getResources().getConfiguration().locale.getCountry();
    		PhoneNumberUtil p = PhoneNumberUtil.getInstance();
    		PhoneNumber pn;
    		try {
				pn = p.parse(emailOrNumber, region);
				String nationalnumber = String.valueOf(pn.getNationalNumber());
	    		emailOrNumber = "+" + pn.getCountryCode() + nationalnumber;
//        		Log.d("emailOrNumber", "setSMSNumber default to local: " + emailOrNumber);
    		} catch (NumberParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
		else {
			;
		}

	}

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 150;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

    /*
    public static void sendViewToBack(final View child) {
    	if(child != null) {
            final ViewGroup parent = (ViewGroup)child.getParent();
            if (null != parent) {
                parent.removeView(child);
                parent.addView(child, 0);
            }
    	}
    }
    */

    private class MagicAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    MagicAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if(filteredDisplayList != null) {
				return filteredDisplayList.size();
			}
			else {
				return 0;
			}
		}

		@Override
		public String getItem(int position) {
			HashMap<String,String> row = filteredDisplayList.get(position);
			return row.get("labelOrAddress");
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View view;

	        if (convertView == null) {
	            view = inflater.inflate(R.layout.magic_entry, parent, false);
	        } else {
	            view = convertView;
	        }

	        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        HashMap<String,String> row = filteredDisplayList.get(position);
	        if(row.get("label") != null) {
		        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        }
	        else {
		        ((TextView)view.findViewById(R.id.p1)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityLightTypeface());
	        }

	        // Names, Labels in black, addresses in GREY
	        if(row.get("label") != null) {
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(Color.BLACK);
	        }
	        else {
		        ((TextView)view.findViewById(R.id.p1)).setTextColor(0xFF616161);
	        }

	        if(row.get("labelOrAddress").equals(BlockchainUtil.BLOCKCHAIN_DONATE2)) {
		        ((TextView)view.findViewById(R.id.p1)).setText(row.get("labelOrAddress"));
	        }
	        else {
		        String labelOrAddress = BlockchainUtil.formatAddress(row.get("labelOrAddress"), 15) ;
		        ((TextView)view.findViewById(R.id.p1)).setText(labelOrAddress);
	        }

	        if (contactsOn) {
		        String address = BlockchainUtil.formatAddress(row.get("address"), 15) ;
		        ((TextView)view.findViewById(R.id.p2)).setText(address);
	        } else {
		        ((TextView)view.findViewById(R.id.p2)).setText(row.get("amount") + " BTC");	        	
	        }

	        return view;
		}

    }

    private void initMagicList() {

		final WalletApplication application = (WalletApplication)getActivity().getApplication();
		MyRemoteWallet wallet = application.getRemoteWallet();
		activeAddresses = Arrays.asList(wallet.getActiveAddresses());
		labels = wallet.getLabelMap();
        
        magicData =  new ArrayList<HashMap<String,String>>();
        
        filteredDisplayList = new ArrayList<HashMap<String,String>>();

        for(int i = 0; i < activeAddresses.size(); i++) {
		    String address = activeAddresses.get(i);
        	String amount = "0.000";
		    BigInteger finalBalance = wallet.getBalance(address);	
		    if (finalBalance != null)
		    	amount = BlockchainUtil.formatBitcoin(finalBalance);

		        HashMap<String,String> row = new HashMap<String,String>();

		        String label = labels.get(address);
		        String labelOrAddress;
		        if (label != null) {
		            row.put("label", label.toString());	
		            labelOrAddress = label;
		        } else {
		        	labelOrAddress = address;
		        }
		        row.put("address", address.toString());
		        row.put("amount", amount);
		        row.put("labelOrAddress", labelOrAddress);

				magicData.add(row);    

	        	filteredDisplayList.add(row);
        }

    }
    
    private void initAddressBookList() {
 		final WalletApplication application = (WalletApplication)getActivity().getApplication();
 		MyRemoteWallet wallet = application.getRemoteWallet();
 		
        magicData =  new ArrayList<HashMap<String,String>>();

        addressBookMapList = wallet.getAddressBookMap();
        filteredDisplayList = new ArrayList<HashMap<String,String>>();

        if (addressBookMapList != null && addressBookMapList.size() > 0) {
  		    for (Iterator<Map<String, Object>> iti = addressBookMapList.iterator(); iti.hasNext();) {
 		    	Map<String, Object> addressBookMap = iti.next();
 		    	Object address = addressBookMap.get("addr");
 		    	Object label = addressBookMap.get("label");

 		        HashMap<String,String> row = new HashMap<String,String>();
 		        if (label != null) {
 	 		        row.put("label", label.toString()); 		        	
 			        row.put("labelOrAddress", label.toString());
 		        } else {
 	 		        row.put("label", "null"); 		        	
 			        row.put("labelOrAddress", "null");
 		        }
 		        if (address != null) {
 	 		        row.put("address", address.toString());
 		        } else {
 	 		        row.put("address", "null");
 		        }

    			magicData.add(row);
	         	filteredDisplayList.add(row);
 		    }

        }
        else {
		    HashMap<String,String> row = new HashMap<String,String>();
		    row.put("label", BlockchainUtil.BLOCKCHAIN_DONATE2);
		    row.put("address", BlockchainUtil.BLOCKCHAIN_DONATE);
	        row.put("labelOrAddress", BlockchainUtil.BLOCKCHAIN_DONATE2);

			magicData.add(row);
         	filteredDisplayList.add(row);
        }
        
     }

    private void displayMagicList() {
    	
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");
        strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

    	LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	isMagic = true;

		//
		//
		//
		if(rootView == null) {
	        rootView = inflater.inflate(R.layout.fragment_send, null, false);
		}

		tvCurrency.setBackgroundColor(0xFF232223);
        if(isBTC) {
    	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
    		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
        }
        else {
    		tvCurrency.setText(strCurrentFiatSymbol);
        }


		//
        // add view with my_addresses and contacts
        //
        magic = ((LinearLayout)rootView.findViewById(R.id.magic_input));
        oldView = ((LinearLayout)magic.findViewById(R.id.magic_bis));
        parent = (LinearLayout)oldView.getParent();
        oldView.setVisibility(View.GONE);
		childIcons = inflater.inflate(R.layout.magic, null);

        final int color_contacts_selected = 0xff808080;
        final int color_contacts_unselected = 0xffe0e0e0;

        layoutAddresses = (LinearLayout)childIcons.findViewById(R.id.addresses_bg);
        layoutContacts = (LinearLayout)childIcons.findViewById(R.id.contacts_bg);
        layoutPhoneContacts = (LinearLayout)childIcons.findViewById(R.id.phone_contacts_bg);
        tvAddresses = (TextView)childIcons.findViewById(R.id.addresses);
        tvContacts = (TextView)childIcons.findViewById(R.id.contacts);
        tvPhoneContacts = (TextView)childIcons.findViewById(R.id.phone_contacts);

        addressesOn = false;
        contactsOn = true;
        phoneContactsOn = false;

        layoutAddresses.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!addressesOn) {
            			addressesOn = true;
            			contactsOn = false;
            			phoneContactsOn = false;
            			layoutAddresses.setBackgroundColor(color_contacts_selected);
            			tvAddresses.setTextColor(0xFFffffff);
            			layoutContacts.setBackgroundColor(color_contacts_unselected);
            			tvContacts.setTextColor(0xFF000000);
            			layoutPhoneContacts.setBackgroundColor(color_contacts_unselected);
            			tvPhoneContacts.setTextColor(0xFF000000);
            		}
            		initMagicList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        layoutContacts.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!contactsOn) {
            			contactsOn = true;
            			phoneContactsOn = false;
            			addressesOn = false;
            			layoutPhoneContacts.setBackgroundColor(color_contacts_unselected);
            			tvPhoneContacts.setTextColor(0xFF000000);
            			layoutContacts.setBackgroundColor(color_contacts_selected);
            			tvContacts.setTextColor(0xFFffffff);
            			layoutAddresses.setBackgroundColor(color_contacts_unselected);
            			tvAddresses.setTextColor(0xFF000000);
            		}
            		initAddressBookList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        layoutPhoneContacts.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!phoneContactsOn) {
            			contactsOn = false;
            			phoneContactsOn = true;
            			addressesOn = false;
            			layoutContacts.setBackgroundColor(color_contacts_unselected);
            			tvContacts.setTextColor(0xFF000000);
            			layoutAddresses.setBackgroundColor(color_contacts_unselected);
            			tvAddresses.setTextColor(0xFF000000);
            			layoutPhoneContacts.setBackgroundColor(color_contacts_selected);
            			tvPhoneContacts.setTextColor(0xFFffffff);

            			filteredDisplayList = new ArrayList<HashMap<String,String>>();
            		}
            		try {
						doSend2Friends();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
//            		adapter.notifyDataSetChanged();                            		
                }
        });

//	    parent.addView(child, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	    parent.addView(childIcons);
	    children++;

    	LinearLayout divider1 = (LinearLayout)childIcons.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);

        //
        // add view with list
        //
		childList = inflater.inflate(R.layout.magic2, null);
    	divider1 = (LinearLayout)childList.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
        magicList = ((ListView)childList.findViewById(R.id.magicList));
        magicList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	    parent.addView(childList);
	    children++;

        magicList.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3)	{

                HashMap<String, String> map = filteredDisplayList.get(position);
                String labelOrAddress = map.get("labelOrAddress");
            	edAddress.setText(labelOrAddress);         	                	               
            	currentSelectedAddress = map.get("address");
            	
            	if(position == 0 && currentSelectedAddress.equals(BlockchainUtil.BLOCKCHAIN_DONATE))	{
                	currentSelectedAddress = null;
                	edAddress.setText("");
                	
                   	final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final String message = "You do not yet have any sending addresses in your addressbook. "
                        + " Would you like to create one?";
                    builder.setMessage(message)
                        .setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.dismiss();
                                	Intent intent = new Intent(getActivity(), info.blockchain.wallet.ui.AddressBookActivity.class);
                                	intent.putExtra("SENDING", true);
                            		startActivity(intent);
                                }
                        })
                        .setNegativeButton("No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface d, int id) {
                                    d.dismiss();
                                }
                        });
                    builder.create().show();
                    
                    return;
            	}
            	
                icon_row.setVisibility(View.GONE);

                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);

                /*
                HashMap<String, String> map = filteredDisplayList.get(position);
                String labelOrAddress = map.get("labelOrAddress");
            	edAddress.setText(labelOrAddress);         	                	               
            	currentSelectedAddress = map.get("address");
            	*/

                removeMagicList();
                edAmount1.requestFocus();
                edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);

            	if(isBTC) {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            	}
            	else {
            	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
            		tvCurrency.setText(strCurrentFiatSymbol);
            	}
                
            }
        });

        adapter = new MagicAdapter();
        magicList.setAdapter(adapter);

//        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.send_container));
//        sendViewToBack(container);
        
//	    parent.bringToFront();
//	    parent.requestLayout();
//	    parent.invalidate();
    }

    private void removeMagicList() {
		isMagic = false;

		tvCurrency.setBackgroundColor(Color.WHITE);
        if(isBTC) {
    	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
    		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
        }
        else {
    		tvCurrency.setText(strCurrentFiatSymbol);
        }
        
        if(parent != null) {
            parent.removeViews(parent.getChildCount() - children, children);
            children = 0;
            oldView.setVisibility(View.VISIBLE);
        }

        if(addressesOn) {
            initMagicList();
        }
        else {
    		initAddressBookList();
        }

    }

    private void doSimpleSend() {
    	if(magic != null) {
    		magic.setVisibility(View.VISIBLE);
    	}

    	simple_spend.setVisibility(View.VISIBLE);
    	custom_spend.setVisibility(View.GONE);
//        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.send_container));
//        sendViewToBack(container);
    	CURRENT_SEND = SIMPLE_SEND;
    	sendType = SendTypeQuickSend;
    }

    private void doCustomSend() {
    	/*
    	if(isMagic) {
    		removeMagicList();
    	}
    	*/

		final WalletApplication application = (WalletApplication)getActivity().getApplication();
		final MyRemoteWallet wallet = application.getRemoteWallet();

		Map<String,String> labels = wallet.getLabelMap();

		final List<String> fromAddresses = new ArrayList<String>();
		fromAddresses.add("");
		fromAddresses.addAll(Arrays.asList(wallet.getActiveAddresses()));
		final List<String> displayFromAddresses = new ArrayList<String>();
		displayFromAddresses.add(SELECT_ADDRESS);
        for(int i = 1; i < fromAddresses.size(); i++) {
	        String label = labels.get(fromAddresses.get(i));
	        String amount = null;
		    BigInteger finalBalance = wallet.getBalance(fromAddresses.get(i));

		    if(finalBalance != null) {
		    	amount = ", " + BlockchainUtil.formatBitcoin(finalBalance) + "BTC";
		    }
		    else {
		    	amount = "";
		    }

	        if (label != null) {
	        	displayFromAddresses.add(label + amount);
	        } else {
	        	displayFromAddresses.add(fromAddresses.get(i).substring(0,  15) + "..." + amount);
	        }
        }
        
		final List<String> changeAddresses = new ArrayList<String>();
		changeAddresses.add(SELECT_ADDRESS);
		changeAddresses.add(NEW_ADDRESS);
		changeAddresses.addAll(Arrays.asList(wallet.getActiveAddresses()));

		final List<String> displayChangeAddresses = new ArrayList<String>();
		displayChangeAddresses.add(SELECT_ADDRESS);
		displayChangeAddresses.add(NEW_ADDRESS);
        for(int i = 2; i < changeAddresses.size(); i++) {
	        String label = labels.get(changeAddresses.get(i));
	        String amount = null;
		    BigInteger finalBalance = wallet.getBalance(changeAddresses.get(i));

		    if(finalBalance != null) {
		    	amount = ", " + BlockchainUtil.formatBitcoin(finalBalance) + "BTC";
		    }
		    else {
		    	amount = "";
		    }

	        if (label != null) {
	        	displayChangeAddresses.add(label + amount);
	        } else {
	        	displayChangeAddresses.add(changeAddresses.get(i).substring(0,  15) + "..." + amount);
	        }
        }

		final List<String> feeTypes = new ArrayList<String>();
		feeTypes.add("Select fee");
		feeTypes.add("Frugal");
		feeTypes.add("Standard");
		feeTypes.add("Generous");

    	simple_spend.setVisibility(View.GONE);
    	custom_spend.setVisibility(View.VISIBLE);
    	CURRENT_SEND = CUSTOM_SEND;
    	sendType = SendTypeCustomSend;
    	
    	lastSendingAddress = null;

    	final LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	LinearLayout layout_custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);

    	// all 'sending address' entries go here:
    	final LinearLayout layout_froms = (LinearLayout)layout_custom_spend.findViewById(R.id.froms);
    	// first 'sending address':
        LinearLayout layout_from = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);
    	// additional 'sending address':
//        LinearLayout layout_from2 = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);
    	// 'fee':
        LinearLayout layout_fee = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);
    	// 'change address':
        LinearLayout layout_change = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);

        // remove any previous views
        if(layout_custom_spend.getChildCount() > 1) {
            layout_custom_spend.removeViews(1, layout_custom_spend.getChildCount() - 1);
        }
        layout_froms.removeAllViews();

        //
        // 'FROM' layout
        //
        TextView tvSpend = new TextView(getActivity());
    	tvSpend.setTextColor(0xFF3eb6e2);
    	tvSpend.setTypeface(null, Typeface.BOLD);
        tvSpend.setText("FROM");
        tvSpend.setTextSize(12);
        tvSpend.setPadding(5, 5, 5, 5);
        tvSpend.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LayoutParams layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSpend.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.divider1)).setBackgroundColor(0xFF3eb6e2);
    	((LinearLayout)layout_from.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p1)).addView(tvSpend);

    	final Spinner spAddress = new Spinner(getActivity());
        ArrayAdapter<String> fromAddressSpinnerArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.layout_spinner_item, displayFromAddresses);
        fromAddressSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAddress.setAdapter(fromAddressSpinnerArrayAdapter);
        spAddress.setSelection(0);
        spAddress.setPadding(5, 5, 5, 5);
        spAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p2)).addView(spAddress);

    	final EditText edAmount = new EditText(new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_InputMethod));
        edAmount.setId(ViewIdGenerator.generateViewId());
        edAmount.setText("0.0000");
        edAmount.setTextSize(16);
        edAmount.setTextColor(Color.BLACK);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edAmount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edAmount.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        edAmount.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p3)).addView(edAmount);

    	ImageView ibPlus = (ImageView)layout_from.findViewById(R.id.plus_icon);
        ibPlus.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	addSendingAddress(displayFromAddresses, wallet, fromAddresses, "0.0000");
            }
        });

        TextView tvCurrency = new TextView(getActivity());
        tvCurrency.setText("BTC");
        tvCurrency.setTextSize(12);
        tvCurrency.setPadding(5, 5, 5, 5);
        tvCurrency.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvCurrency.setLayoutParams(layout_params);
    	((LinearLayout)layout_from.findViewById(R.id.p4)).setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from.findViewById(R.id.p4)).addView(tvCurrency);

        spAddress.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	if(layout_froms.getChildCount() == 1 && edAmount1.getText().toString().length() > 0 && edAmount.getText().toString().equals("0.0000")) {
                	edAmount.setText(BlockchainUtil.formatBitcoin(getBTCEnteredOutputValue(edAmount1.getText().toString())));
            	}
            	return false;
            }
        });

    	spAddress.setOnItemSelectedListener(new OnItemSelectedListener()	{
	    	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)	{

            	if(edAmount.getText().toString().length() > 0) {
            		if(getBTCEnteredOutputValue(edAmount.getText().toString()).compareTo(wallet.getBalance(fromAddresses.get(spAddress.getSelectedItemPosition()))) == 1) {
            			edAmount.setText(BlockchainUtil.formatBitcoin(wallet.getBalance(fromAddresses.get(spAddress.getSelectedItemPosition()))));
            			BigInteger remainder = getBTCEnteredOutputValue(edAmount1.getText().toString()).subtract(wallet.getBalance(fromAddresses.get(spAddress.getSelectedItemPosition())));
            			addSendingAddress(displayFromAddresses, wallet, fromAddresses, BlockchainUtil.formatBitcoin(remainder));
            		}
            	}

	    	}
	        public void onNothingSelected(AdapterView<?> arg0) {
	        	;
	        }
    	});

        edAmount.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                	if(layout_froms.getChildCount() == 1 && edAmount1.getText().toString().length() > 0 && edAmount.getText().toString().equals("0.0000")) {
                		if(spAddress.getSelectedItemPosition() == 0) {
                        	edAmount.setText(BlockchainUtil.formatBitcoin(getBTCEnteredOutputValue(edAmount1.getText().toString())));
                		}
                		else {
                    		if(getBTCEnteredOutputValue(edAmount1.getText().toString()).compareTo(wallet.getBalance(fromAddresses.get(spAddress.getSelectedItemPosition()))) == 1) {
                    			edAmount.setText(BlockchainUtil.formatBitcoin(wallet.getBalance(fromAddresses.get(spAddress.getSelectedItemPosition()))));
                    		}
                    		else {
                    			BigInteger remainder = getBTCEnteredOutputValue(edAmount1.getText().toString()).subtract(wallet.getBalance(fromAddresses.get(spAddress.getSelectedItemPosition())));
                    			edAmount.setText(BlockchainUtil.formatBitcoin(remainder));
                    		}
                		}
                	}
                }
            }
        });

    	((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).addView(layout_from);
    	lastSendingAddress = layout_from;

        //
        // 'FEE' layout
        //
        TextView tvFee = new TextView(getActivity());
        tvFee.setId(ViewIdGenerator.generateViewId());
    	tvFee.setTextColor(0xFFFF0000);
    	tvFee.setTypeface(null, Typeface.BOLD);
        tvFee.setText("FEE");
        tvFee.setTextSize(12);
        tvFee.setPadding(5, 5, 5, 5);
        tvFee.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvFee.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.divider1)).setBackgroundColor(BlockchainUtil.BLOCKCHAIN_RED);
    	((LinearLayout)layout_fee.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_fee.findViewById(R.id.p1)).addView(tvFee);
    	final Spinner spFeeType = new Spinner(getActivity());
        ArrayAdapter<String> spinnerArrayAdapter2 = new ArrayAdapter<String>(getActivity(), R.layout.layout_spinner_item, feeTypes);
        spinnerArrayAdapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFeeType.setAdapter(spinnerArrayAdapter2);
        spFeeType.setSelection(0);
        spFeeType.setPadding(5, 5, 5, 5);
        spFeeType.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spFeeType.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.p2)).setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	final EditText edFee = new EditText(new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_InputMethod));
        edFee.setId(ViewIdGenerator.generateViewId());
        edFee.setText("0.0001");
        edFee.setTextSize(16);
        edFee.setTextColor(Color.BLACK);
        edFee.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edFee.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edFee.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_fee.findViewById(R.id.p3)).addView(edFee);
        tvCurrency = new TextView(getActivity());
        tvCurrency.setText("BTC");
        tvCurrency.setTextSize(12);
        tvCurrency.setPadding(5, 5, 5, 5);
        tvCurrency.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvCurrency.setLayoutParams(layout_params);
    	((LinearLayout)layout_fee.findViewById(R.id.p4)).setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_fee.findViewById(R.id.p4)).addView(tvCurrency);

    	layout_fee.setPadding(0, 10, 0, 0);
    	ImageView ibPlusF = (ImageView)layout_fee.findViewById(R.id.plus_icon);
    	ibPlusF.setVisibility(View.INVISIBLE);
    	((LinearLayout)layout_custom_spend.findViewById(R.id.custom_spend)).addView(layout_fee);

    	spFeeType.setOnItemSelectedListener(new OnItemSelectedListener()	{
	    	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)	{
	    		if(arg2 != 0)	{
	    			if(arg2 == 1)	{
		    			edFee.setText("0.0000");
	    			}
	    			else if(arg2 == 2)	{
		    			edFee.setText("0.0001");
	    			}
	    			else if(arg2 == 3)	{
		    			edFee.setText("0.0005");
	    			}
	    			else	{
	    				;
	    			}
	    		}
	    	}
	        public void onNothingSelected(AdapterView<?> arg0) {
	        	;
	        }
    	});
    	((LinearLayout)layout_fee.findViewById(R.id.p2)).addView(spFeeType);

    	//
    	// 'CHANGE' layout
    	//
        TextView tvChange = new TextView(getActivity());
        tvChange.setTypeface(null, Typeface.BOLD);
        tvChange.setText("CHANGE");
        tvChange.setTextSize(12);
        tvChange.setPadding(5, 5, 5, 5);
        tvChange.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvChange.setLayoutParams(layout_params);
    	((LinearLayout)layout_change.findViewById(R.id.divider1)).setBackgroundColor(0xFF808080);
    	((LinearLayout)layout_change.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p1)).addView(tvChange);

    	final Spinner spChangeAddress = new Spinner(getActivity());
        ArrayAdapter<String> changeAddressSpinnerArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.layout_spinner_item, displayChangeAddresses);
        changeAddressSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spChangeAddress.setAdapter(changeAddressSpinnerArrayAdapter);

        spChangeAddress.setSelection(0);
        spChangeAddress.setPadding(5, 5, 5, 5);
        spChangeAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spChangeAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_change.findViewById(R.id.p2)).setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_change.findViewById(R.id.p2)).addView(spChangeAddress);
    	layout_change.setPadding(0, 10, 0, 0);
    	ImageView ibPlusC = (ImageView)layout_change.findViewById(R.id.plus_icon);
    	ibPlusC.setVisibility(View.INVISIBLE);
    	((LinearLayout)layout_custom_spend.findViewById(R.id.custom_spend)).addView(layout_change);
    	
    	btConfirm = new Button(getActivity());
    	btConfirm.setText("Send");
    	btConfirm.setTextSize(22);
    	btConfirm.setBackgroundResource(R.color.blockchain_blue);
    	btConfirm.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        btConfirm.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            	//
            	// get data entered by user and pass custom send instance
            	//
            	cs = new CustomSend();

            	if(spAddress.getSelectedItemPosition() != 0 &&
            			edAmount.getText().toString() != null && edAmount.getText().toString().length() > 0 &&
            			Double.parseDouble(edAmount.getText().toString()) > 0.0) {
            		cs.addSendingAddress(fromAddresses.get(spAddress.getSelectedItemPosition()), getBTCEnteredOutputValue(edAmount.getText().toString()));
            	}

                LinearLayout sending_layout = null;
                LinearLayout p_layout = null;
                Spinner selected_address = null;
                EditText amount = null;
            	for(int i = 0; i < layout_froms.getChildCount(); i++) {
            		sending_layout = (LinearLayout)layout_froms.getChildAt(i);
                	p_layout = (LinearLayout)sending_layout.findViewById(R.id.p2);
                	selected_address = (Spinner)p_layout.getChildAt(0);
                	p_layout = (LinearLayout)sending_layout.findViewById(R.id.p3);
                	amount = ((EditText)p_layout.getChildAt(0));

                	if(selected_address.getSelectedItemPosition() != 0 &&
                			amount.getText().toString() != null && amount.getText().toString().length() > 0 &&
                			Double.parseDouble(amount.getText().toString()) > 0.0) {
                		cs.addSendingAddress(fromAddresses.get(selected_address.getSelectedItemPosition()), getBTCEnteredOutputValue(amount.getText().toString()));
                	}

            	}

            	if(edFee.getText().toString() != null && edFee.getText().toString().length() > 0) {
            		cs.setFee(getBTCEnteredOutputValue(edFee.getText().toString()));
            	}
            	else {
            		cs.setFee(getBTCEnteredOutputValue("0.00"));
            	}

        		cs.setChangeAddress(changeAddresses.get(spChangeAddress.getSelectedItemPosition()));
            	
            	//doBeforeCustomSend();
            }
        });

    }
    private boolean isCustomSendInputsCorrect() {
    	//
    	//
    	//
//		Toast.makeText(getActivity(), "Sending addresses:" + cs.getSendingAddresses().size(), Toast.LENGTH_SHORT).show();

    	BigInteger total_amount = BigInteger.ZERO;
    	HashMap<String, BigInteger> addresses = cs.getSendingAddresses();
    	Set<String> keys = addresses.keySet();
    	for (Iterator iterator = keys.iterator(); iterator.hasNext();) {
            String s = (String)iterator.next();
            total_amount = total_amount.add(addresses.get(s));
        }
		//
		//
		//

		final BigInteger entered_amount = getBTCEnteredOutputValue(edAmount1.getText().toString());
		final WalletApplication application = (WalletApplication) getActivity().getApplication();

		MyRemoteWallet.FeePolicy feePolicy = MyRemoteWallet.FeePolicy.FeeOnlyIfNeeded;
		BigInteger fee = cs.getFee();

		if (application.getRemoteWallet() == null)
			return false;

		csProgress = new SendProgress() {
			public void onSend(final Transaction tx, final String message) {
				handler.post(new Runnable() {
					public void run() {
						application.getRemoteWallet().setState(MyRemoteWallet.State.SENT);
				        if(sendingProgressDialog != null) {
					        sendingProgressDialog.dismiss();
				        }
	            		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
					}
				});

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				application.doMultiAddr(true);
			}

			public void onError(final String message) {
				handler.post(new Runnable() {
					public void run() {

						System.out.println("On Error");

				        if(sendingProgressDialog != null) {
					        sendingProgressDialog.dismiss();
				        }
						if (message != null)
		            		Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();

						application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);
					}
				});
			}

			public void onProgress(final String message) {
				handler.post(new Runnable() {
					public void run() {
						application.getRemoteWallet().setState(MyRemoteWallet.State.SENDING);
					}
				});
			}

			public boolean onReady(Transaction tx, BigInteger fee, MyRemoteWallet.FeePolicy feePolicy, long priority) {

				boolean containsOutputLessThanThreshold = false;
				for (TransactionOutput output : tx.getOutputs()) {
					if (output.getValue().compareTo(Constants.FEE_THRESHOLD_MIN) < 0) {
						containsOutputLessThanThreshold = true;
						break;
					}
				}

				if (feePolicy != MyRemoteWallet.FeePolicy.FeeNever && fee.compareTo(BigInteger.ZERO) == 0) {
					if (tx.bitcoinSerialize().length > 1000 || containsOutputLessThanThreshold) {
//						makeTransaction(MyRemoteWallet.FeePolicy.FeeForce);
						return false;
					} else if (priority < 97600000L) {
						handler.post(new Runnable() {
							public void run() {
						        if(sendingProgressDialog != null) {
							        sendingProgressDialog.dismiss();
						        }
						        
								AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
								builder.setMessage(R.string.ask_for_fee)
								.setCancelable(false);

								AlertDialog alert = builder.create();

								alert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.continue_without_fee), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
//										makeTransaction(MyRemoteWallet.FeePolicy.FeeNever);
										dialog.dismiss();
									} }); 

								alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.add_fee), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
//										makeTransaction(MyRemoteWallet.FeePolicy.FeeForce);

										dialog.dismiss();
									}}); 

								alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.dismiss();
									}});

								alert.show();
							}
						});

						handler.post(new Runnable() {
							public void run() {
								application.getRemoteWallet().setState(MyRemoteWallet.State.INPUT);
							}
						});

						return false;
					}
				}

				return true;
			}

			public ECKey onPrivateKeyMissing(final String address) {

				if (SendFragment.temporaryPrivateKeys.containsKey(address)) {
					return SendFragment.temporaryPrivateKeys.get(address);
				}

				handler.post(new Runnable() {
					public void run() {
				        if(sendingProgressDialog != null) {
					        sendingProgressDialog.dismiss();
				        }
						AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
						builder.setMessage(getString(R.string.ask_for_private_key, address))
						.setCancelable(false)
						.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								SendFragment.scanPrivateKeyAddress = address;

								Intent intent = new Intent(getActivity(), ZBarScannerActivity.class);
								intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
				        		startActivityForResult(intent, SCAN_PRIVATE_KEY_FOR_SENDING);	    		
							}
						})
						.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {

								synchronized (SendFragment.temporaryPrivateKeys) {
									SendFragment.temporaryPrivateKeys.notify();
								}

								dialog.cancel();
							}
						});

						AlertDialog alert = builder.create();

						alert.show();
					}
				});

				try {
					synchronized (SendFragment.temporaryPrivateKeys) {
						SendFragment.temporaryPrivateKeys.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				return SendFragment.temporaryPrivateKeys.get(address);
			}

			@Override
			public void onStart() {
				handler.post(new Runnable() {
					public void run() {
						if (SendFragment.this.sendingProgressDialog == null || (SendFragment.this.sendingProgressDialog != null
								&& !SendFragment.this.sendingProgressDialog.isShowing())) {
					    	SendFragment.this.sendingProgressDialog = new ProgressDialog(getActivity());
					    	SendFragment.this.sendingProgressDialog.setCancelable(true);
					    	SendFragment.this.sendingProgressDialog.setIndeterminate(true);
					    	SendFragment.this.sendingProgressDialog.setTitle("Sending...");
					    	SendFragment.this.sendingProgressDialog.setMessage("Please wait");
					    	SendFragment.this.sendingProgressDialog.show();
						}
					}
				});
			}
		};
		
		//
		//
		//
		if(total_amount.compareTo(entered_amount) != 0) {
    		Toast.makeText(getActivity(), "The sum of the amounts for all sending addresses must be equal to the amount specified on the top of the screen.", Toast.LENGTH_LONG).show();
			return false;
		}
		else {
			return true;
		}
    }
    
    private void addSendingAddress(final List<String> displayAddresses, final MyRemoteWallet wallet, final List<String> addresses, final String remainder) {

    	final LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    	final LinearLayout layout_custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);
    	// additional 'sending address':
        final LinearLayout layout_from2 = (LinearLayout)inflater.inflate(R.layout.layout_custom_segment, layout_custom_spend, false);

        // second send address
        TextView tvSpend = new TextView(getActivity());
        tvSpend.setText("");
        tvSpend.setTextSize(12);
        tvSpend.setPadding(5, 5, 5, 5);
        tvSpend.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        LayoutParams layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvSpend.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.divider1)).setBackgroundColor(0xFF3eb6e2);
    	((LinearLayout)layout_from2.findViewById(R.id.p1)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p1)).addView(tvSpend);
    	
    	final Spinner spAddress = new Spinner(getActivity());
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.layout_spinner_item, displayAddresses);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAddress.setAdapter(spinnerArrayAdapter);
        spAddress.setSelection(0);
        spAddress.setPadding(5, 5, 5, 5);
        spAddress.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        spAddress.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p2)).addView(spAddress);

    	final EditText edAmount = new EditText(new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_InputMethod));
        edAmount.setId(ViewIdGenerator.generateViewId());
        edAmount.setText(remainder);
        edAmount.setTextSize(16);
        edAmount.setTextColor(Color.BLACK);
        edAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        edAmount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        edAmount.setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        edAmount.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p3)).setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p3)).addView(edAmount);

        tvCurrency = new TextView(getActivity());
        tvCurrency.setText("BTC");
        tvCurrency.setTextSize(12);
        tvCurrency.setPadding(5, 5, 5, 5);
        tvCurrency.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layout_params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        tvCurrency.setLayoutParams(layout_params);
    	((LinearLayout)layout_from2.findViewById(R.id.p4)).setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
    	((LinearLayout)layout_from2.findViewById(R.id.p4)).addView(tvCurrency);
    	
        spAddress.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	if(edAmount1.getText().toString().length() > 0 && edAmount.getText().toString().equals("0.0000")) {
                	edAmount.setText(BlockchainUtil.formatBitcoin(getBTCEnteredOutputValue(edAmount1.getText().toString()).subtract(getBTCEnteredOutputValue(remainder))));
            	}
            	return false;
            }
        });

    	spAddress.setOnItemSelectedListener(new OnItemSelectedListener()	{
    		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)	{
            	if(edAmount.getText().toString().length() > 0) {
            		if(arg2 != 0 && getBTCEnteredOutputValue(remainder).compareTo(wallet.getBalance(addresses.get(spAddress.getSelectedItemPosition()))) == 1) {
            			edAmount.setText(BlockchainUtil.formatBitcoin(wallet.getBalance(addresses.get(spAddress.getSelectedItemPosition()))));
            			BigInteger remaining = getBTCEnteredOutputValue(remainder).subtract(wallet.getBalance(addresses.get(spAddress.getSelectedItemPosition())));
            			addSendingAddress(displayAddresses, wallet, addresses, BlockchainUtil.formatBitcoin(remaining));
            		}
            	}
	    	}

	    	public void onNothingSelected(AdapterView<?> arg0) {
	        	;
	        }
    	});

        edAmount.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
                	if(edAmount1.getText().toString().length() > 0 && edAmount.getText().toString().equals("0.0000")) {
                		if(spAddress.getSelectedItemPosition() == 0) {
                        	edAmount.setText(remainder);
                		}
                		else {
                    		if(getBTCEnteredOutputValue(remainder).compareTo(wallet.getBalance(addresses.get(spAddress.getSelectedItemPosition()))) == 1) {
                    			edAmount.setText(BlockchainUtil.formatBitcoin(wallet.getBalance(addresses.get(spAddress.getSelectedItemPosition()))));
                    		}
                    		else {
                    			BigInteger remaining = getBTCEnteredOutputValue(remainder).subtract(wallet.getBalance(addresses.get(spAddress.getSelectedItemPosition())));
                    			edAmount.setText(BlockchainUtil.formatBitcoin(remaining));
                    		}
                		}
                	}
                }
            }
        });

    	ImageView ibPlus = (ImageView)layout_from2.findViewById(R.id.plus_icon);
    	ibPlus.setVisibility(View.INVISIBLE);

        layout_from2.setOnLongClickListener(new View.OnLongClickListener() {
        	  public boolean onLongClick(View view) {
        	    ((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).removeView(layout_from2);
        	    return true;
        	  }
        });

        layout_from2.setOnTouchListener(new SwipeDismissTouchListener(layout_from2, null,
                new SwipeDismissTouchListener.DismissCallbacks() {
                    @Override
                    public boolean canDismiss(Object token) {
                        return true;
                    }

                    @Override
                    public void onDismiss(View view, Object token) {
                	    ((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).removeView(layout_from2);
                    }
                }));

    	((LinearLayout)layout_custom_spend.findViewById(R.id.froms)).addView(layout_from2);
    	lastSendingAddress = layout_from2;
    }

    private void doSharedCoin() {
    	CURRENT_SEND = SHARED_SEND;
    	sendType = SendTypeSharedCoin;
    }

    private void doSend2Friends() throws Exception	{
    	Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	intent.setData(ContactsContract.Contacts.CONTENT_URI);
    	startActivityForResult(intent, PICK_CONTACT);    	
    }

    private void doSelectInternationalPrefix()	{
		Intent intent = new Intent(getActivity(), InternationalPrefixActivity.class);
    	startActivityForResult(intent, SELECT_INTL_PREFIX);
    }
    
    private void doScanInput(String address)	{
        if(BitcoinAddressCheck.isValidAddress(address)) {
    		if(isMagic) {
    			removeMagicList();
    		}
    		
            icon_row.setVisibility(View.GONE);

            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

            edAddress.setText(address);

            edAmount1.requestFocus();
            edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(edAmount1, InputMethodManager.SHOW_IMPLICIT);

        }
        else if(BitcoinAddressCheck.isUri(address)) {
    		if(isMagic) {
    			removeMagicList();
    		}

            icon_row.setVisibility(View.GONE);

            String btc_address = BitcoinAddressCheck.getAddress(address);
            String btc_amount = BitcoinAddressCheck.getAmount(address);
            if(btc_amount == null) {
            	btc_amount = "0.0000";
            }
            
            edAddress.setText(btc_address);
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
            imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

            edAmount1.requestFocus();
            edAmount1.setText(Double.toString(Double.parseDouble(btc_amount) / 100000000.0));
            edAmount1.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

            InputMethodManager inputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(edAmount1, InputMethodManager.SHOW_IMPLICIT);

            isBTC = true;
    	    tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
    		tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            
        }
        else {
    		Toast.makeText(getActivity(), "not processed", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearSend()	{
    	edAddress.setText("");
        edAddress.setHint(R.string.send_payment_hint);
      	edAmount1.setText("");
      	if(isBTC) {
          	edAmount1.setHint("0.0000");
      	}
      	else {
          	edAmount1.setHint("0.00");
      	}
      	if(isBTC) {
          	tvAmount2.setText("0.00" + " " + strCurrentFiatCode);
      	}
      	else {
          	tvAmount2.setText("0.0000" + " BTC");
      	}
    	
        summary2.setVisibility(View.INVISIBLE);
        tvAmount.setText("");
        tvAmount.setVisibility(View.INVISIBLE);
        tvAmountBis.setText("");
        tvAmountBis.setVisibility(View.INVISIBLE);
        tvArrow.setText("");
        tvArrow.setVisibility(View.INVISIBLE);
        tvAddress.setText("");
        tvAddress.setVisibility(View.INVISIBLE);
        tvAddressBis.setText("");
        tvAddressBis.setVisibility(View.INVISIBLE);

        btSend.setText("Send");
        btSend.setVisibility(View.INVISIBLE);

        summary3.setVisibility(View.GONE);
        tvSentPrompt.setVisibility(View.GONE);
        
        ivClearInput.setVisibility(View.INVISIBLE);
        icon_row.setVisibility(View.VISIBLE);

    	LinearLayout layout_custom_spend = (LinearLayout)rootView.findViewById(R.id.custom_spend);
    	// all 'sending address' entries go here:
    	LinearLayout layout_froms = (LinearLayout)layout_custom_spend.findViewById(R.id.froms);
    	layout_froms.removeAllViews();
    	layout_custom_spend.removeViews(1, layout_custom_spend.getChildCount() - 1);
    	
    	if(sendMode.isChecked()) {
    		doCustomSend();
    	}

    }
    
	public void handleScanPrivateKey(final String contents) throws Exception {
		System.out.println("Scanned PK " + contents);

		if (SendFragment.scanPrivateKeyAddress != null) {
			final String format = WalletUtils.detectPrivateKeyFormat(contents);

			System.out.println("Scanned Private Key Format " + format);

			if (format.equals("bip38")) {
				handler.postDelayed(new Runnable() {

					@Override
					public void run() {
						RequestPasswordDialog.show(getFragmentManager(), new SuccessCallback() {

							public void onSuccess() {
								String password = RequestPasswordDialog.getPasswordResult();

								System.out.println("Password " + password);

								try {
									ECKey key = WalletUtils.parsePrivateKey(format, contents, password);

									if (!key.toAddressCompressed(Constants.NETWORK_PARAMETERS)
											.toString().equals(SendFragment.scanPrivateKeyAddress) &&
											!key.toAddressUnCompressed(Constants.NETWORK_PARAMETERS)
											.toString().equals(SendFragment.scanPrivateKeyAddress)) {
										System.out.println("Scanned Password wrong_private_key");

										String scannedPrivateAddress = key.toAddress(Constants.NETWORK_PARAMETERS)
												.toString();
										throw new Exception(getString(R.string.wrong_private_key, scannedPrivateAddress));
									} else {
										System.out.println("Scanned Password temporaryPrivateKeys");
										//Success
										SendFragment.temporaryPrivateKeys.put(SendFragment.scanPrivateKeyAddress, key);

										synchronized (SendFragment.temporaryPrivateKeys) {
											SendFragment.temporaryPrivateKeys.notify();
										}

										SendFragment.scanPrivateKeyAddress = null;
									}
								} catch (Exception e) {
									e.printStackTrace();

									//longToast("Error Decrypting Private Key");
									Toast.makeText(application, "Error Decrypting Private Key", Toast.LENGTH_LONG).show();
									//updateSendCoinsFragment(contents, null);
								}
							}

							public void onFail() {
								//updateSendCoinsFragment(contents, null);
								Toast.makeText(application, "Incorrect password", Toast.LENGTH_LONG).show();
							}
						}, RequestPasswordDialog.PasswordTypePrivateKey);
					}
				}, 100);
			} else {
				ECKey key = WalletUtils.parsePrivateKey(format, contents, null);

				if (!key.toAddressCompressed(Constants.NETWORK_PARAMETERS)
						.toString().equals(SendFragment.scanPrivateKeyAddress) &&
						!key.toAddressUnCompressed(Constants.NETWORK_PARAMETERS)
						.toString().equals(SendFragment.scanPrivateKeyAddress)) {
					String scannedPrivateAddress = key.toAddress(Constants.NETWORK_PARAMETERS)
							.toString();
					throw new Exception(getString(R.string.wrong_private_key, scannedPrivateAddress));
				} else {
					//Success
					SendFragment.temporaryPrivateKeys.put(SendFragment.scanPrivateKeyAddress, key);
					Toast.makeText(application, "Success. Private key temporary imported", Toast.LENGTH_LONG).show();
				}

				synchronized (SendFragment.temporaryPrivateKeys) {
					SendFragment.temporaryPrivateKeys.notify();
				}

				SendFragment.scanPrivateKeyAddress = null;
			}
		} else {
			//updateSendCoinsFragment(contents, null);
			throw new Exception("scanPrivateKeyAddress not set");
		}
	}
}
