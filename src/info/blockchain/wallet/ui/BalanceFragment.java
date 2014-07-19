package info.blockchain.wallet.ui;

import info.blockchain.api.ExchangeRates;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.json.simple.JSONObject;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyTransaction;
import piuk.blockchain.android.MyTransactionInput;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.util.WalletUtils;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.script.Script;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
//import android.util.Log;

@SuppressLint("NewApi")
public class BalanceFragment extends Fragment   {

	private View rootView = null;
	private LinearLayout balanceLayout = null;
	private LinearLayout balance_extLayout = null;
	private LinearLayout balance_extHiddenLayout = null;
	private TextView tViewCurrencySymbol = null;
	private TextView tViewAmount1 = null;
	private TextView tViewAmount2 = null;
	private ListView txList = null;
	private Animation slideUp = null;
	private Animation slideDown = null;
	private boolean isSwipedDown = false;
    private String[] addressLabels = null;
    private boolean[] addressLabelTxsDisplayed = null;
    
    private boolean[] isWatchOnlys = null;
    private String[] addressAmounts = null;
	private TransactionAdapter adapter = null;
	private boolean isBTC = true;
	private BigInteger totalInputsValue = BigInteger.ZERO;
	private BigInteger totalOutputsValue = BigInteger.ZERO;
	private String strCurrentFiatSymbol = "$";
	private String strCurrentFiatCode = "USD";
	private boolean isAccountInformationIntialized = false;

	private WalletApplication application;
	private Map<String, String> labelMap;
	
	private static int QR_GENERATION = 1;
	private static int TX_ACTIVITY = 2;
	
	private boolean isNoRefreshOnReturn = false;
	private Transaction sentTx = null;
	private List<String> activeAddresses;

	public static final String ACTION_INTENT = "info.blockchain.wallet.ui.BalanceFragment.REFRESH";
	
    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_INTENT.equals(intent.getAction())) {

        		refreshPayload();

        		ExchangeRates fxRates = new ExchangeRates();
                DownloadFXRatesTask task = new DownloadFXRatesTask(context, fxRates);
                task.execute(new String[] { fxRates.getUrl() });

        		setAdapterContent();
        		adapter.notifyDataSetChanged();
            }
        }
    };

	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Wallet Balance Listener";
		}

		@Override
		public void onCoinsSent(final Transaction tx, final long result) {
			sentTx = tx;
	        ((ViewPager)getActivity().findViewById(R.id.pager)).setCurrentItem(1);
			setAdapterContent();
			sentTx = null;
			
    		try {
        		WalletUtil.getInstance(getActivity(), getActivity()).getWalletApplication().doMultiAddr(false, null);
    		}
    		catch(Exception e) {
        		Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
    		}

			adapter.notifyDataSetChanged();
		};

		@Override
		public void onCoinsReceived(final Transaction tx, final long result) {
			setAdapterContent();
			adapter.notifyDataSetChanged();
		};

		@Override
		public void onTransactionsChanged() {
			setAdapterContent();
			adapter.notifyDataSetChanged();
		};
		
		@Override
		public void onWalletDidChange() {
			setAdapterContent();
			adapter.notifyDataSetChanged();
		}
		
		@Override
		public void onCurrencyChanged() {
			setAdapterContent();
			adapter.notifyDataSetChanged();
		};
	};

	public List<String> getAddressesPartOfLastSentTransaction(final Transaction tx, MyRemoteWallet remoteWallet) {
    	List<String> addressesPartOfLastSentTransaction = new ArrayList<String>();

    	List<TransactionOutput> transactionOutputs = tx.getOutputs();
		for (Iterator<TransactionOutput> ito = transactionOutputs.iterator(); ito.hasNext();) {
			TransactionOutput transactionOutput = ito.next();
        	try {
        		Script script = transactionOutput.getScriptPubKey();
        		String addr = null;
        		if (script != null)
        			addr = script.getToAddress(MyRemoteWallet.getParams()).toString();

        		addressesPartOfLastSentTransaction.add(addr);
            } catch (ScriptException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }				
		}
		
    	List<TransactionInput> transactionInputs = tx.getInputs();	 
	    for (Iterator<TransactionInput> iti = transactionInputs.iterator(); iti.hasNext();) {
    		TransactionInput transactionInput = iti.next();
        	try {
        		Address addr = transactionInput.getFromAddress();
        		if (addr != null) {
        			addressesPartOfLastSentTransaction.add(addr.toString());
        		}
            } catch (ScriptException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }			    		
    	}
		
//        Log.d("transaction", "transaction addressesPartOfLastSentTransaction: " + addressesPartOfLastSentTransaction);
        return addressesPartOfLastSentTransaction;	
	}

	public void setAdapterContent() {
		if (getActivity() == null)
			return;
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");
        strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

		application = WalletUtil.getRefreshedInstance(getActivity(),  getActivity()).getWalletApplication();

		if (application == null) {
			return;
		}

		MyRemoteWallet remoteWallet = WalletUtil.getInstance(getActivity(), getActivity()).getRemoteWallet();
		if (remoteWallet == null) {
			return;
		}

		addressLabels = remoteWallet.getActiveAddresses();
		if (addressLabels == null) {
			return;
		}
		
	    activeAddresses = Arrays.asList(addressLabels);
		addressAmounts = new String[addressLabels.length];
		isWatchOnlys = new boolean[addressLabels.length];
		
   		if(!isNoRefreshOnReturn) {
   			
			addressLabelTxsDisplayed = new boolean[addressLabels.length];

			if(sentTx != null) {
				List<String> addressesPartOfLastSentTransaction = getAddressesPartOfLastSentTransaction(sentTx, remoteWallet);
				for (int i = 0; i < addressLabelTxsDisplayed.length; i++) {
					if (addressesPartOfLastSentTransaction.contains(activeAddresses.get(i))) {
						addressLabelTxsDisplayed[i] = true;
					}
					else {
						addressLabelTxsDisplayed[i] = false;
					}
				}
				isNoRefreshOnReturn = true;
			}
			else {
				for (int i = 0; i < addressLabelTxsDisplayed.length; i++) {
					addressLabelTxsDisplayed[i] = false;
				}			
			}
		}
		else {
			isNoRefreshOnReturn = false;
		}

		labelMap = remoteWallet.getLabelMap();

		for (int i = 0; i < addressLabels.length; i++) {
			String address = addressLabels[i];

		    BigInteger finalBalance = remoteWallet.getBalance(address);
		    if (finalBalance != null)
		    	addressAmounts[i] = BlockchainUtil.formatBitcoin(finalBalance);
		    else
		    	addressAmounts[i] = "0.0000";
		    
		    String label = labelMap.get(address);
		    if (label != null) {
		    	addressLabels[i] = label;	
		    }  		   		    
		    
		    try {
				if (remoteWallet.isWatchOnly(address))
					isWatchOnlys[i] = true;
				else
					isWatchOnlys[i] = false;					
		    } catch (Exception e) {
				e.printStackTrace();
			}	
	    }

		totalInputsValue = remoteWallet.getTotal_received();
		totalOutputsValue = remoteWallet.getTotal_sent();
		
		if(remoteWallet != null) {
			BigInteger balance = remoteWallet.getBalance();
			if(isBTC) {
		        tViewCurrencySymbol.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
		        tViewAmount1.setText(BlockchainUtil.formatBitcoin(balance));
		        tViewAmount2.setText(strCurrentFiatSymbol + BlockchainUtil.BTC2Fiat(WalletUtils.formatValue(balance)));
			}
			else {
		        tViewCurrencySymbol.setText(strCurrentFiatSymbol);
		        tViewAmount1.setText(BlockchainUtil.BTC2Fiat(WalletUtils.formatValue(balance)));
		        tViewAmount2.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()) + BlockchainUtil.formatBitcoin(balance));
			}
		}

        if (adapter != null) {
        	adapter.notifyDataSetChanged();
        }

        if (isAccountInformationIntialized == false) {
        	isAccountInformationIntialized = true;

    		application.getAccountInformation(false, new SuccessCallback() {
    			@Override
    			public void onSuccess() {
    				/*
    	    		Log.d("getAccountInformation", "getAccountInformation isEnableEmailNotification " + application.getRemoteWallet().isEnableEmailNotification());
    	    		Log.d("getAccountInformation", "getAccountInformation isEnableSMSNotification " + application.getRemoteWallet().isEnableSMSNotification());
    	    		Log.d("getAccountInformation", "getAccountInformation getEmail " + application.getRemoteWallet().getEmail());
    	    		Log.d("getAccountInformation", "getAccountInformation getSMSNumber " + application.getRemoteWallet().getSmsNumber());			    		
    	    		*/
    			}
    			
    			@Override
    			public void onFail() {
//    	    		Log.d("getAccountInformation", "getAccountInformation fail");	
    			}
    		});   
    		
    		application.setSharedPrefsActiveAddresses(Arrays.asList(remoteWallet.getActiveAddresses()));    		
        }
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

		final Activity activity = getActivity();
//		application = (WalletApplication) activity.getApplication();

        rootView = inflater.inflate(R.layout.fragment_balance, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");
        strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

        slideUp = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_up);
        slideDown = AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_down);

        tViewCurrencySymbol = (TextView)rootView.findViewById(R.id.currency_symbol);
        tViewCurrencySymbol.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
        tViewCurrencySymbol.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
        tViewCurrencySymbol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(isBTC) {
            		tViewCurrencySymbol.setText(strCurrentFiatSymbol);
            		String tmp = tViewAmount1.getText().toString(); 
            		tViewAmount1.setText(tViewAmount2.getText().toString().substring(1));
            		tViewAmount2.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
                    tViewAmount2.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()) + tmp);
            	}
            	else {
                    tViewCurrencySymbol.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));
            		String tmp = tViewAmount1.getText().toString(); 
                    tViewAmount1.setText(tViewAmount2.getText().toString().substring(1));
                    tViewAmount2.setText(strCurrentFiatSymbol + tmp);
            	}
            	isBTC = isBTC ? false : true;

            	adapter.notifyDataSetChanged();
            }
        });

        tViewAmount1 = (TextView)rootView.findViewById(R.id.amount1);
        tViewAmount1.setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoLightTypeface());
        tViewAmount2 = (TextView)rootView.findViewById(R.id.amount2);
		MyRemoteWallet remoteWallet = WalletUtil.getInstance(getActivity(), getActivity()).getRemoteWallet();
		if(remoteWallet != null) {
	        tViewAmount1.setText(BlockchainUtil.formatBitcoin(remoteWallet.getBalance()));
	        tViewAmount2.setText(strCurrentFiatSymbol + BlockchainUtil.BTC2Fiat(BlockchainUtil.formatBitcoin(remoteWallet.getBalance())));
		}
		else {
	        tViewAmount1.setText("0");
	        tViewAmount2.setText(strCurrentFiatSymbol + BlockchainUtil.BTC2Fiat("0"));
		}

        txList = (ListView)rootView.findViewById(R.id.txList);

        adapter = new TransactionAdapter();
        txList.setAdapter(adapter);
        txList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
    	    	final LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
    	    	final LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);

	    		if(balance_extHiddenLayout.getChildCount() > 1) {
    		        balance_extHiddenLayout.removeViews(1, balance_extHiddenLayout.getChildCount() - 1);
	    		}

    	    	if(balance_extHiddenLayout.getVisibility() == View.VISIBLE) {
    	    		addressLabelTxsDisplayed[position] = false;

			        if (isWatchOnlys[position])
				        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_watch_inactive);
			        else
				        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_inactive);

//    		        balance_extLayout.startAnimation(slideUp);
    		        balance_extLayout.setVisibility(View.GONE);
    		        balance_extHiddenLayout.setVisibility(View.GONE);
    		    	System.gc();
    	    	}
    	    	else {
    	    		addressLabelTxsDisplayed[position] = true;
    	    		
			        if (isWatchOnlys[position])
				        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_watch);
			        else
				        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_active);

    	        	System.gc();
    	    		doDisplaySubList(view, position);
    	    	}
            }
        });
//	    txList.setDivider(getActivity().getResources().getDrawable(R.drawable.list_divider));

        balance_extHiddenLayout = (LinearLayout)rootView.findViewById(R.id.balance_ext_hidden);
        balance_extHiddenLayout.setVisibility(View.GONE);

        balanceLayout = (LinearLayout)rootView.findViewById(R.id.balance);
		balanceLayout.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
		    public void onSwipeBottom() {
		    	if(!isSwipedDown) {
		    		isSwipedDown = true;
		    		
//			        Toast.makeText(BalanceFragment.this.getActivity(), "bottom", Toast.LENGTH_SHORT).show();
			        balance_extHiddenLayout.setVisibility(View.VISIBLE);
			        balance_extLayout.setVisibility(View.VISIBLE);
			        balance_extLayout.startAnimation(slideDown);
			        
	    	        ((LinearLayout)rootView.findViewById(R.id.divider)).setVisibility(View.GONE);
	    	        
	    	        LinearLayout progression_sent = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_sent));
	    	        ((TextView)progression_sent.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_sent.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_sent.findViewById(R.id.total_type)).setText("Total Sent");
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setTextColor(Color.BLACK);	    	        
	    	        ((TextView)progression_sent.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalOutputsValue) + " BTC");	    	        
	    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setMax(100);

	    	        LinearLayout progression_received = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_received));
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_received.findViewById(R.id.total_type)).setText("Total Received");
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setTextColor(Color.BLACK);
	    	        ((TextView)progression_received.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalInputsValue) + " BTC");
	    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setMax(100);

	    	        if (totalOutputsValue.doubleValue() > 0 || totalInputsValue.doubleValue() > 0) {        	
	    	            ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgress((int)((totalOutputsValue.doubleValue() / (totalOutputsValue.doubleValue() + totalInputsValue.doubleValue())) * 100));
		    	        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_red));
	    	            ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgress((int)((totalInputsValue.doubleValue() / (totalOutputsValue.doubleValue() + totalInputsValue.doubleValue())) * 100));
		    	        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_green));
	    	        }
		    	}
		    }

		});

        balance_extLayout = (LinearLayout)rootView.findViewById(R.id.balance_ext);
		balance_extLayout.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {
		    public void onSwipeTop() {
		    	isSwipedDown = false;

    	        ((LinearLayout)rootView.findViewById(R.id.divider)).setVisibility(View.VISIBLE);

//		        Toast.makeText(BalanceFragment.this.getActivity(), "top", Toast.LENGTH_SHORT).show();
		        balance_extLayout.startAnimation(slideUp);
		        balance_extLayout.setVisibility(View.GONE);
		        balance_extHiddenLayout.setVisibility(View.GONE);
		    }
		});
        balance_extLayout.setVisibility(View.GONE);

		EventListeners.addEventListener(eventListener);
		
        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if(isVisibleToUser) {
        	System.gc();

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            strCurrentFiatCode = prefs.getString("ccurrency", "USD");
            strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

//            BlockchainUtil.getInstance(getActivity());

        }
        else {
        	;
        }
    }

    @Override
    public void onResume() {
    	super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
//        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");
        strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

        BlockchainUtil.getInstance(getActivity());

		setAdapterContent();
		
    	System.gc();

    }

    @Override
    public void onPause() {
    	super.onPause();

//        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
    }

	@Override
	public void onDestroy() {
		super.onDestroy();

		EventListeners.removeEventListener(eventListener);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(requestCode == QR_GENERATION)	{
	        if(adapter != null)	{
	        	isNoRefreshOnReturn = true;
	        	adapter.notifyDataSetChanged();
	        }
	    }
		else if(requestCode == TX_ACTIVITY)	{
	        if(adapter != null)	{
	        	isNoRefreshOnReturn = true;
	        	adapter.notifyDataSetChanged();
	        }
	    }
		else {
			;
		}
		
	}

    private class TransactionAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    TransactionAdapter() {
	        inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			if(addressLabels != null) {
				return addressLabels.length;
			}
			else {
				return 0;
			}
		}

		@Override
		public String getItem(int position) {
	        return addressLabels[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
//			Log.d("List refresh", "" + position);

			View view;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.txs_layout, parent, false);
	        } else {
	            view = convertView;
	        }
	        
	    	LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
	    	LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);
	        balance_extLayout.setVisibility(View.GONE);
	        balance_extHiddenLayout.setVisibility(View.GONE);

	        String amount = null;
	        DecimalFormat df = null;
	        if(isBTC) {
	        	df = new DecimalFormat("######0.0000");
	        	if(addressAmounts != null && addressAmounts[position] != null) {
		        	amount = df.format(Double.parseDouble(addressAmounts[position]));
	        	}
	        	else {
		        	amount = "0.0000";
	        	}
	        }
	        else {
	        	if(addressAmounts != null && addressAmounts[position] != null) {
		        	amount = BlockchainUtil.BTC2Fiat(addressAmounts[position]);
	        	}
	        	else {
		        	amount = "0.00";
	        	}
	        }

	        ((TextView)view.findViewById(R.id.address)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
        	if(addressLabels != null && addressLabels[position] != null) {
    	        ((TextView)view.findViewById(R.id.address)).setText(addressLabels[position].length() > 15 ? addressLabels[position].substring(0, 15) + "..." : addressLabels[position]);
        	}
        	else {
    	        ((TextView)view.findViewById(R.id.address)).setText("");
        	}
	        ((TextView)view.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoBoldTypeface());
	        ((TextView)view.findViewById(R.id.amount)).setText(amount);
	        ((TextView)view.findViewById(R.id.currency_code)).setText(isBTC ? "BTC" : strCurrentFiatCode);

	        if(addressLabelTxsDisplayed[position]) {
//				Log.d("List refresh sub", "" + position);
		    	System.gc();
		    	
	    		if(balance_extHiddenLayout.getChildCount() > 1) {
    		        balance_extHiddenLayout.removeViews(1, balance_extHiddenLayout.getChildCount() - 1);
	    		}

		        doDisplaySubList(view, position);
	        }

	        if(addressLabelTxsDisplayed[position]) {
		        if (isWatchOnlys[position])
			        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_watch);
		        else
			        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_active);
	        }
	        else {
		        if (isWatchOnlys[position])
			        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_watch_inactive);
		        else
			        ((ImageView)view.findViewById(R.id.address_type)).setImageResource(R.drawable.address_inactive);
	        }

	        return view;
		}

    }

    public void doDisplaySubList(final View view, int position) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");

    	final LinearLayout balance_extLayout = (LinearLayout)view.findViewById(R.id.balance_ext);
    	final LinearLayout balance_extHiddenLayout = (LinearLayout)view.findViewById(R.id.balance_ext_hidden);

    	MyRemoteWallet remoteWallet = application.getRemoteWallet();
		if (remoteWallet == null) {
			return;
		}

	    final String[] activeAddresses = remoteWallet.getActiveAddresses();
    	final String address = activeAddresses[position];

    	ImageView qr_icon = ((ImageView)balance_extLayout.findViewById(R.id.balance_qr_icon));
        qr_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
      			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", address);
      		    clipboard.setPrimaryClip(clip);
     			Toast.makeText(getActivity(), "Address copied to clipboard", Toast.LENGTH_LONG).show();

                Intent intent;
        		intent = new Intent(getActivity(), QRActivity.class);
        		intent.putExtra("BTC_ADDRESS", address);
        		startActivityForResult(intent, QR_GENERATION);

                return false;
            }
        });

		final Map<String, JSONObject> multiAddrBalancesRoot = remoteWallet.getMultiAddrBalancesRoot();
		final JSONObject addressRoot = multiAddrBalancesRoot.get(address);
	    final BigInteger totalReceived = BigInteger.valueOf(((Number)addressRoot.get("total_received")).longValue());
	    final BigInteger totalSent = BigInteger.valueOf(((Number)addressRoot.get("total_sent")).longValue());
//	    Log.d("totalReceived: ", "totalReceived: " + totalReceived);

//	    Log.d("totalSent: ", "totalSent: " + totalSent);
        LinearLayout progression_sent = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_sent));
        ((TextView)progression_sent.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_sent.findViewById(R.id.total_type)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_sent.findViewById(R.id.total_type)).setText("SENT");
        ((TextView)progression_sent.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_sent.findViewById(R.id.amount)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_sent.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalSent) + " BTC");
        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setMax(100);

        LinearLayout progression_received = ((LinearLayout)balance_extLayout.findViewById(R.id.progression_received));
        ((TextView)progression_received.findViewById(R.id.total_type)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_received.findViewById(R.id.total_type)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_received.findViewById(R.id.total_type)).setText("RECEIVED");
        ((TextView)progression_received.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getRobotoTypeface());
        ((TextView)progression_received.findViewById(R.id.amount)).setTextColor(0xFF9b9b9b);
        ((TextView)progression_received.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(totalReceived) + " BTC");
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setMax(100);

        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgress((int)((totalSent.doubleValue() / (totalSent.doubleValue() + totalReceived.doubleValue())) * 100));
        ((ProgressBar)progression_sent.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_red2));
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgress((int)((totalReceived.doubleValue() / (totalSent.doubleValue() + totalReceived.doubleValue())) * 100));
        ((ProgressBar)progression_received.findViewById(R.id.bar)).setProgressDrawable(getResources().getDrawable(R.drawable.progress_green2));

        final List<MyTransaction> transactionsList = remoteWallet.getTransactions();
        final List<MyTransaction> filteredTxList = new ArrayList<MyTransaction>();

        HashMap<String,BigInteger> txAmounts = new HashMap<String,BigInteger>();
        HashMap<String,BigInteger> txAmounts2 = new HashMap<String,BigInteger>();
        boolean isPartOfTx = false;

        //
        // check for txs that include selected address
        //
        List<MyTransaction> transactionsListClone = transactionsList;
		for (Iterator<MyTransaction> it = transactionsListClone.iterator(); it.hasNext();) {
			MyTransaction transaction = it.next();
	    	List<TransactionOutput> transactionOutputs = transaction.getOutputs();
	    	List<TransactionInput> transactionInputs = transaction.getInputs();
	        isPartOfTx = false;

			for (Iterator<TransactionInput> iti = transactionInputs.iterator(); iti.hasNext();) {
				TransactionInput transactionInput = iti.next();
	        	try {
	        		String addr = transactionInput.getFromAddress().toString();
	        		if(addr != null && addr.equals(address)) {
	        			filteredTxList.add(transaction);
//	    				Log.d("TxBitmapPrep", transaction.getHashAsString() + " contains:" + addr);
	        			isPartOfTx = true;
	        			break;
	        		}
	            } catch (ScriptException e) {
	                e.printStackTrace();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }			    		
			}

			if(isPartOfTx) {
				continue;
			}

			for (Iterator<TransactionOutput> ito = transactionOutputs.iterator(); ito.hasNext();) {
				TransactionOutput transactionOutput = ito.next();
	        	try {
	        		Script script = transactionOutput.getScriptPubKey();
	        		String addr = null;
	        		if (script != null) {
	        			addr = script.getToAddress(MyRemoteWallet.getParams()).toString();
		        		if (addr != null && addr.equals(address)) {
		        			filteredTxList.add(transaction);
//		    				Log.d("TxBitmapPrep", transaction.getHashAsString() + " contains:" + addr);
		        			break;
		        		}
	        		}
	            } catch (ScriptException e) {
	                e.printStackTrace();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }			    		
			}

	    }

        //
        // build map of addresses <-> amounts for retained txs
        //
		List<String> myAddresses = Arrays.asList(activeAddresses);
 		final WalletApplication application = (WalletApplication)getActivity().getApplication();
 		MyRemoteWallet wallet = application.getRemoteWallet();
		Map<String,String> labels = wallet.getLabelMap();

		for (Iterator<MyTransaction> it = filteredTxList.iterator(); it.hasNext();) {

			MyTransaction transaction = it.next();

			BigInteger result = BigInteger.ZERO;
    		txAmounts.clear();
    	    List<Map.Entry<String, String>> addressValueEntryList = new ArrayList<Map.Entry<String, String>>();

    	    List<TransactionInput> transactionInputs = transaction.getInputs();

			for (Iterator<TransactionInput> iti = transactionInputs.iterator(); iti.hasNext();) {
				TransactionInput transactionInput = iti.next();
	        	try {
	        		String addr = transactionInput.getFromAddress().toString();
	        		if (addr != null) {
		        		MyTransactionInput ti = (MyTransactionInput)transactionInput;
		        		if(addr.equals(address)) {
		        			result = result.subtract(ti.getValue());
//		    				Log.d("TxBitmapPrep", transaction.getHashAsString() + ":" + address + ", -" + ti.getValue());
		        		}
	        			if(txAmounts.get(addr) != null) {
		        			txAmounts.put(addr, txAmounts.get(addr).subtract(ti.getValue()));
//		    				Log.d("TxBitmapPrep", transaction.getHashAsString() + "/" + addr + ":" + "subtract " + ti.getValue());
	        			}
	        			else {
		        			txAmounts.put(addr, BigInteger.ZERO.subtract(ti.getValue()));
//		    				Log.d("TxBitmapPrep", transaction.getHashAsString() + "/" + addr + ":" + "subtract " + ti.getValue());
	        			}
	        		}
	            } catch (ScriptException e) {
	                e.printStackTrace();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }			    		
	    	}

	    	List<TransactionOutput> transactionOutputs = transaction.getOutputs();

			for (Iterator<TransactionOutput> ito = transactionOutputs.iterator(); ito.hasNext();) {
				TransactionOutput transactionOutput = ito.next();
	        	try {
	        		Script script = transactionOutput.getScriptPubKey();
	        		String addr = null;
	        		if (script != null) {
	        			addr = script.getToAddress(MyRemoteWallet.getParams()).toString();
		        		if (addr != null) {
			        		if(addr.equals(address)) {
			        			result = result.add(transactionOutput.getValue());
//			    				Log.d("TxBitmapPrep", transaction.getHashAsString() + ":" + address + ", -" + transactionOutput.getValue());
			        		}
		        			if(txAmounts.get(addr) != null) {
			        			txAmounts.put(addr, txAmounts.get(addr).add(transactionOutput.getValue()));
//			    				Log.d("TxBitmapPrep", transaction.getHashAsString() + "/" + addr + ":" + "add " + transactionOutput.getValue());
		        			}
		        			else {
			        			txAmounts.put(addr, transactionOutput.getValue());
//			    				Log.d("TxBitmapPrep", transaction.getHashAsString() + "/" + addr + ":" + "add " + transactionOutput.getValue());
		        			}
		        		}
	        		}
	            } catch (ScriptException e) {
	                e.printStackTrace();
	            } catch (Exception e) {
	                e.printStackTrace();
	            }			    		
	    	}

			boolean isSending = true;
			if(result.compareTo(BigInteger.ZERO) == 1) {
				isSending = false;
			}
			else {
				isSending = true;
			}

//			Log.d("TxBitmapPrep", "Result:" + result.toString());
//			Log.d("TxBitmapPrep", "isSending:" + isSending);
			
			addressValueEntryList.clear();
			Map.Entry<String, String> addressValueEntry = null;
			for (String key : txAmounts.keySet()) {
				if(key.equals(address)) {
					continue;
				}
				
				if(labels.get(key) != null) {
					addressValueEntry = new AbstractMap.SimpleEntry<String, String>(labels.get(key), BlockchainUtil.formatBitcoin(txAmounts.get(key)) + " BTC");
				}
				else {
					addressValueEntry = new AbstractMap.SimpleEntry<String, String>(key, BlockchainUtil.formatBitcoin(txAmounts.get(key)) + " BTC");
				}

				addressValueEntryList.add(addressValueEntry);
			}

	    	if (addressValueEntryList.size() > 0) {
	    		View child = getTxChildView(view, addressValueEntryList, result, transaction, isSending);
	    		balance_extHiddenLayout.addView(child);	    	
	    	}

	    }

        balance_extHiddenLayout.setVisibility(View.VISIBLE);
        balance_extLayout.setVisibility(View.VISIBLE);
//	    balance_extLayout.startAnimation(slideDown);
    }
    
    private View getTxChildView(final View view, List<Map.Entry<String, String>> addressValueEntryList, final BigInteger result, final MyTransaction transaction, final boolean isSending) {
  		View child = null;
        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		child = inflater.inflate(R.layout.tx_layout, null);

        ((TextView)child.findViewById(R.id.ts)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
        
        ((TextView)child.findViewById(R.id.ts)).setText(DateUtil.getInstance().formatted(transaction.getTime().getTime() / 1000));

        if (isSending) {
	        TxBitmap txBitmap = new TxBitmap(getActivity(), addressValueEntryList);
	        ((ImageView)child.findViewById(R.id.txbitmap)).setImageBitmap(txBitmap.createArrowsBitmap(200, TxBitmap.SENDING, addressValueEntryList.size()));
	        ((ImageView)child.findViewById(R.id.address)).setImageBitmap(txBitmap.createListBitmap(200));
	        ((TextView)child.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        ((TextView)child.findViewById(R.id.amount)).setTextColor(BlockchainUtil.BLOCKCHAIN_RED);
        }
        else {
//	        TxBitmap txBitmap = new TxBitmap(getActivity(), addressValueEntryList);
	        TxBitmap txBitmap = new TxBitmap(getActivity(), addressValueEntryList.subList(0, 1));
//	        ((ImageView)child.findViewById(R.id.txbitmap)).setImageBitmap(txBitmap.createArrowsBitmap(200, TxBitmap.RECEIVING, addressValueEntryList.size()));
	        ((ImageView)child.findViewById(R.id.txbitmap)).setImageBitmap(txBitmap.createArrowsBitmap(200, TxBitmap.RECEIVING, 1));
	        ((ImageView)child.findViewById(R.id.address)).setImageBitmap(txBitmap.createListBitmap(200));
	        ((TextView)child.findViewById(R.id.amount)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());
	        ((TextView)child.findViewById(R.id.amount)).setTextColor(BlockchainUtil.BLOCKCHAIN_GREEN);
        }
        
        if(isBTC) {
	        ((TextView)child.findViewById(R.id.amount)).setText(BlockchainUtil.formatBitcoin(result) + " BTC");
        }
        else {
	        ((TextView)child.findViewById(R.id.amount)).setText((BlockchainUtil.BTC2Fiat(WalletUtils.formatValue(result)) + " " + strCurrentFiatCode));
        }
        
        final String transactionHash = transaction.getHashAsString();
        
		child.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
        		intent = new Intent(getActivity(), TxActivity.class);
        		intent.putExtra("TX", transactionHash);
        		intent.putExtra("TS", transaction.getTime().getTime() / 1000);
        		intent.putExtra("RESULT", BlockchainUtil.formatBitcoin(result.abs()));
        		intent.putExtra("SENDING", isSending);
        		intent.putExtra("CURRENCY", strCurrentFiatCode);
        		startActivityForResult(intent, TX_ACTIVITY);
            }
        });
		
		return child;
    }
    
    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 380;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }
    
    public boolean refreshPayload() {
		Toast.makeText(getActivity(), "Refreshing...", Toast.LENGTH_LONG).show();

		try {
    		WalletUtil.getRefreshedInstance(getActivity(), getActivity()).getWalletApplication().doMultiAddr(false, null);
		}
		catch(Exception e) {
    		Toast.makeText(getActivity(), e.toString(), Toast.LENGTH_SHORT).show();
		}

		return false;
    }

}
