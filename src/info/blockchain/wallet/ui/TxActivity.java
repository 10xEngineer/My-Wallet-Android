package info.blockchain.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.math.BigInteger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import info.blockchain.api.LatestBlock;
import info.blockchain.api.Transaction;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyTransaction;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.util.WalletUtils;

public class TxActivity extends Activity	{

	private TextView tvLabelConfirmations = null;
	private TextView tvLabelAmount = null;
	private TextView tvLabelFee = null;
	private TextView tvLabelTx = null;
	private TextView tvValueConfirmations = null;
	private TextView tvValueAmount = null;
	private TextView tvValueFee = null;
	private TextView tvValueTx = null;
	private TextView tvResult = null;
	private TextView tvTS = null;
	private TextView tvFrom = null;
	private TextView tvTo = null;
	private TextView tvFromAddress = null;
	private TextView tvToAddress = null;
	private TextView tvFromAddress2 = null;
	private TextView tvToAddress2 = null;
	private TextView tvValueThenLabel = null;
	private TextView tvValueNowLabel = null;
	private TextView tvValueThenValue = null;
	private TextView tvValueNowValue = null;
    
	private TextView tvNoteLabel = null;
	private TextView tvValueNote = null;

	private LinearLayout txNoteRowLayout = null;

	private String strTxHash = null;
	private boolean isSending = false;
	private String strResult = null;
	private long height = -1L;
	private long latest_block = -1L;
	private long ts = 0L;

	private LatestBlock latestBlock = null;
	private Transaction transaction = null;

	private Map<String,String> labels = null;

	private AddressManager addressManager = null;
	private MyRemoteWallet remoteWallet = null;
	private WalletApplication application = null;
	private boolean isDialogDisplayed = false;
	
	private String strCurrency = null;
	private String strResultHistorical = "";
	private boolean isHistorical = false;
	private String strFiat = null;
	
	private HashMap<TextView,String> txAmounts = null;
	
	private ProgressDialog progress = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.layout_tx);

        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	strTxHash = extras.getString("TX");
        	isSending = extras.getBoolean("SENDING");
        	strResult = extras.getString("RESULT");
        	strCurrency = extras.getString("CURRENCY");
        	ts = extras.getLong("TS");
        }
        
		labels = WalletUtil.getInstance(this,  this).getRemoteWallet().getLabelMap();

		application = WalletUtil.getInstance(this, this).getWalletApplication();
		remoteWallet =  WalletUtil.getInstance(this, this).getRemoteWallet();
        addressManager = new AddressManager(remoteWallet, application, this);        

        latestBlock = new LatestBlock();
        transaction = new Transaction(strTxHash);
        
    	txAmounts = new HashMap<TextView,String>(); 

        tvLabelConfirmations = (TextView)findViewById(R.id.confirm_label);
        tvLabelAmount = (TextView)findViewById(R.id.amount_label);
        tvLabelFee = (TextView)findViewById(R.id.fee_label);
        tvLabelTx = (TextView)findViewById(R.id.tx_label);
        tvValueConfirmations = (TextView)findViewById(R.id.confirm_value);
        tvValueAmount = (TextView)findViewById(R.id.amount_value);
        tvValueFee = (TextView)findViewById(R.id.fee_value);
        tvValueTx = (TextView)findViewById(R.id.tx_value);
        tvResult = (TextView)findViewById(R.id.result);
        tvResult.setTypeface(TypefaceUtil.getInstance(this).getGravityBoldTypeface());
        tvTS = (TextView)findViewById(R.id.ts);
        tvFrom = (TextView)findViewById(R.id.from);
        tvTo = (TextView)findViewById(R.id.to);
        tvToAddress = (TextView)findViewById(R.id.to_address);
        tvToAddress.setTypeface(TypefaceUtil.getInstance(this).getGravityBoldTypeface());
        tvToAddress.setTextColor(0xFF676767);
        tvToAddress2 = (TextView)findViewById(R.id.to_address2);
        tvToAddress2.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
        tvToAddress2.setTextColor(0xFF949ea3);

        tvValueThenLabel = (TextView)findViewById(R.id.value_then_label);
        tvValueThenLabel.setTextColor(0xFF949ea3);
        tvValueNowLabel = (TextView)findViewById(R.id.value_now_label);
        tvValueNowLabel.setTextColor(0xFF949ea3);
        tvValueThenValue = (TextView)findViewById(R.id.value_then_value);
        tvValueThenValue.setTextColor(0xFF949ea3);
        tvValueNowValue = (TextView)findViewById(R.id.value_now_value);
        tvValueNowValue.setTextColor(0xFF949ea3);
        tvValueThenLabel.setText("Value Then");
        tvValueNowLabel.setText("Value Now");
        setValueThenAndNow(remoteWallet, strTxHash);

        tvNoteLabel = (TextView)findViewById(R.id.tx_note_label);
        tvValueNote = (TextView)findViewById(R.id.tx_note_value);
        tvNoteLabel.setText("Transaction Note");
        tvValueNote.setText(remoteWallet.getTxNote(strTxHash));
        txNoteRowLayout = (LinearLayout)findViewById(R.id.txNoteRowLayout);
        txNoteRowLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	promptDialogForAddNoteToTx();
            }
        });
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        tvTS.setText(sdf.format(new Date(ts * 1000)));
        
        tvLabelConfirmations.setText("Confirmations");
        if(isSending)	{
            tvLabelAmount.setText("Amount sent");
            tvResult.setText("SENT " + strResult + " BTC");
            tvResult.setBackgroundResource(R.drawable.rounded_view_red);
            tvTS.setTextColor(getResources().getColor(R.color.blockchain_red));
            tvTo.setTextColor(getResources().getColor(R.color.blockchain_red));
        }
        else	{
            tvLabelAmount.setText("Amount received");
            tvResult.setText("RECEIVED " + strResult + " BTC");
            tvResult.setBackgroundResource(R.drawable.rounded_view_green);
            tvTo.setTextColor(getResources().getColor(R.color.blockchain_green));
            tvTS.setTextColor(getResources().getColor(R.color.blockchain_green));
        }
    	strFiat = BlockchainUtil.BTC2Fiat(strResult);
        tvResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(!isHistorical) {
                    if(isSending)	{
                		tvResult.setText("SENT " + strFiat + " " + strCurrency);
                    }
                    else	{
                		tvResult.setText("RECEIVED " + strFiat + " " + strCurrency);
                    }

                    for (TextView key : txAmounts.keySet()) {
                    	key.setText(BlockchainUtil.BTC2Fiat(txAmounts.get(key))  + " " + strCurrency);
                    }
                    isHistorical = true;
            	}
            	else {
                    if(isSending)	{
                		tvResult.setText("SENT " + strResult + " BTC");
                    }
                    else	{
                		tvResult.setText("RECEIVED " + strResult + " BTC");
                    }

                    for (TextView key : txAmounts.keySet()) {
                    	key.setText(txAmounts.get(key) + " BTC");
                    }
                    isHistorical = false;
            	}
            }
        });
        tvLabelFee.setText("Transaction fee");
        tvLabelTx.setText("Transaction hash");

        tvValueConfirmations.setText("");
        tvValueAmount.setText(strResult + " BTC");
        tvValueFee.setText("");
        tvValueTx.setText(strTxHash);
        
    	tvValueTx.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
      			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Hash", strTxHash);
      		    clipboard.setPrimaryClip(clip);
     			Toast.makeText(TxActivity.this, "Hash copied to clipboard", Toast.LENGTH_LONG).show();

                return false;
            }
        });

    	progress = new ProgressDialog(this);
    	progress.setCancelable(true);
    	progress.setIndeterminate(true);
    	progress.setTitle("Downloading transaction info");
    	progress.setMessage("Please wait");
    	progress.show();
		
    	setValueHistorical(remoteWallet, strTxHash);
    	setTxValues(transaction.getUrl(), latestBlock.getUrl());
    }

    private void promptDialogForAddNoteToTx() {
       	if (isDialogDisplayed)
    		return;
       	
    	AlertDialog.Builder alert = new AlertDialog.Builder(TxActivity.this);

		alert.setTitle(R.string.edit_note);
		alert.setMessage(R.string.enter_note_below);

		final EditText input = new EditText(TxActivity.this);
		input.setHint(remoteWallet.getTxNote(strTxHash));
		alert.setView(input);

		String txNote = remoteWallet.getTxNote(strTxHash);
		String alertPositiveButtonText;
		if (txNote == null) 
			alertPositiveButtonText = getString(R.string.add);
		else 
			alertPositiveButtonText = getString(R.string.update);

		alert.setPositiveButton(alertPositiveButtonText, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			final DialogInterface d = dialog;
			final String note = input.getText().toString();
   			try {
   				if (remoteWallet.addTxNote(strTxHash, note)) {
   					application.saveWallet(new SuccessCallback() {
   						@Override
   						public void onSuccess() {
   	   	 	 	 	        tvValueNote.setText(note);
   	 	 	 				d.dismiss();
   	 	 	 				isDialogDisplayed = false;
   	 	 	 				Toast.makeText(TxActivity.this.getApplication(),
   									R.string.note_saved,
   									Toast.LENGTH_SHORT).show();
   						}

   						@Override
   						public void onFail() { 									
   	   	 	 	 	        tvValueNote.setText(note);
   	 	 	 				d.dismiss();
   	 	 	 				isDialogDisplayed = false;
   							Toast.makeText(TxActivity.this.getApplication(),
   									R.string.toast_error_syncing_wallet,
   									Toast.LENGTH_SHORT).show();
   						}
   					});
   				} 						 	         			
   			} catch (Exception e) {
   	 			Toast.makeText(TxActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
   				e.printStackTrace();
   			} 	   			   
		  }
		});

		alert.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
 			  public void onClick(DialogInterface dialog, int whichButton) {
 					final DialogInterface d = dialog;
 	   				if (remoteWallet.deleteTxNote(strTxHash)) {
 	   					application.saveWallet(new SuccessCallback() {
 	   						@Override
 	   						public void onSuccess() {
 	   	   	 	 	 	        tvValueNote.setText(null);
 	   	 	 	 				d.dismiss();
 	   	 	 	 				isDialogDisplayed = false;
 	   							Toast.makeText(TxActivity.this.getApplication(),
 	   									R.string.note_deleted,
 	   									Toast.LENGTH_SHORT).show();
 	   						}

 	   						@Override
 	   						public void onFail() { 									
 	   	   	 	 	 	        tvValueNote.setText(null);
 	   	 	 	 				d.dismiss();
 	   	 	 	 				isDialogDisplayed = false;
 	   							Toast.makeText(TxActivity.this.getApplication(),
 	   									R.string.toast_error_syncing_wallet,
 	   									Toast.LENGTH_SHORT).show();
 	   						}
 	   					});
 	   				} else {
	 	 	 				isDialogDisplayed = false;
 	   				}
 			  }
		});

		alert.setOnCancelListener(new DialogInterface.OnCancelListener() {         
		    @Override
		    public void onCancel(DialogInterface dialog) {
	 				isDialogDisplayed = false;
		    }
		});

    	isDialogDisplayed = true;
		alert.show();  
    }
    
    private void promptDialogForAddToAddressBook(final String address) {
    	if (isDialogDisplayed)
    		return;
       	
    	AlertDialog.Builder alert = new AlertDialog.Builder(TxActivity.this);

			alert.setTitle(R.string.add_to_address_book);
			alert.setMessage(R.string.set_label_below);

			// Set an EditText view to get user input 
			final EditText input = new EditText(TxActivity.this);
			alert.setView(input);

			alert.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
	   			  String label = input.getText().toString();
	 				if (addressManager.canAddAddressBookEntry(address, label)) {
						addressManager.handleAddAddressBookEntry(address, label, new SuccessCallback() {
							@Override
							public void onSuccess() {
			         			Toast.makeText(TxActivity.this, R.string.added_to_address_book, Toast.LENGTH_LONG).show();
							}

							@Override
							public void onFail() {
				        		Toast.makeText(TxActivity.this, R.string.wallet_sync_error, Toast.LENGTH_LONG).show();							
							}
							
						});
	 				} else {
	 		    		Toast.makeText(TxActivity.this, R.string.address_already_exist, Toast.LENGTH_LONG).show();
	 				}			

	 				dialog.dismiss();
	 				isDialogDisplayed = false;
		  }
			});

			alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
	 			  public void onClick(DialogInterface dialog, int whichButton) {
						dialog.dismiss();
	 	 				isDialogDisplayed = false;
	 			  }
			});

		alert.setOnCancelListener(new DialogInterface.OnCancelListener() {         
	    	@Override
	    	public void onCancel(DialogInterface dialog) {
 				isDialogDisplayed = false;
	    	}
		});

    	isDialogDisplayed = true;
			alert.show();  
    }

	public static String getTxData(String txUrl, String blockUrl) throws Exception {

		String response = WalletUtils.getURL(txUrl);
		response += "\\|";
		response += WalletUtils.getURL(blockUrl);
		
		return response;
	}

	public static JSONObject getTransactionSummary(long txIndex, String guid, long result) throws Exception {
		final String WebROOT = "https://" + Constants.BLOCKCHAIN_DOMAIN + "/tx-summary";
		String url = WebROOT + "/" + txIndex + "?guid=" + guid + "&result=" + result + "&format=json";

		String response = WalletUtils.getURL(url);

		return (JSONObject) new JSONParser().parse(response);
	}

    private void setValueThenAndNow(final MyRemoteWallet remoteWallet, final String txHash) {
		
    	MyTransaction tx = remoteWallet.getTransaction(txHash);
    	if (tx == null)
    		return;
    	
    	long realResult = tx.getResult().longValue();
		final long finalResult = realResult;
		
		final long txIndex = tx.getTxIndex();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {							
					final JSONObject obj = getTransactionSummary(txIndex, remoteWallet.getGUID(), finalResult);

					handler.post(new Runnable() {
						@Override
						public void run() {
							try {
								String result_local = (String)obj.get("result_local");
								String result_local_historical = (String) obj.get("result_local_historical");
								
								if(result_local != null) {
									while(!Character.isDigit(result_local.charAt(0))) {
										result_local = result_local.substring(1);
									}
								}

								if(result_local_historical != null) {
									while(!Character.isDigit(result_local_historical.charAt(0))) {
										result_local_historical = result_local_historical.substring(1);
									}
								}

						        tvValueThenValue.setText(result_local_historical + " USD");
						        tvValueNowValue.setText(result_local + " USD");	
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

    private void setValueHistorical(final MyRemoteWallet remoteWallet, final String txHash) {
		
    	MyTransaction tx = remoteWallet.getTransaction(txHash);
    	if (tx == null)
    		return;
    	
    	long realResult = tx.getResult().longValue();
		final long finalResult = realResult;
		
		final long txIndex = tx.getTxIndex();

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {							
					final JSONObject obj = getTransactionSummary(txIndex, remoteWallet.getGUID(), finalResult);

					handler.post(new Runnable() {
						@Override
						public void run() {
							try {
								strResultHistorical = (String) obj.get("result_local_historical");
								if(strResultHistorical != null) {
									while(Character.isDigit(strResultHistorical.charAt(0))) {
										strResultHistorical = strResultHistorical.substring(1);
									}
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

    private void setTxValues(final String txUrl, final String blockUrl) {

		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {							
					final String result = getTxData(txUrl, blockUrl);

					handler.post(new Runnable() {
						@Override
						public void run() {
							try {

						    	String[] results = result.split("\\|");

						    	transaction.setData(results[0]);
						    	transaction.parse();
						    	height = transaction.getHeight();

						    	tvValueFee.setText(BlockchainUtil.formatBitcoin(BigInteger.valueOf(transaction.getFee())) + " BTC");

						    	/*
			                    if(isSending)	{
							    	String strUpdatedResult = BlockchainUtil.formatBitcoin(BigInteger.valueOf(Long.parseLong(strResult)).subtract(BigInteger.valueOf(transaction.getFee())));
			                		tvResult.setText("SENT " + strUpdatedResult + " BTC");
			                    }
			                    */

						    	((TextView)findViewById(R.id.link_label)).setOnTouchListener(new OnTouchListener() {
						            @Override
						            public boolean onTouch(View v, MotionEvent event) {
						            	Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://blockchain.info/tx/" + transaction.getHash())); 
						            	startActivity(i);
						                return false;
						            }
						        });

						        LayoutInflater inflater = (LayoutInflater)TxActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

						        TableLayout froms = (TableLayout)findViewById(R.id.froms);

						    	String from = null;
						    	String to = null;
						    	ArrayList<String> seenAddresses = new ArrayList<String>();
						    	int processed = 0;
						        //for(int i = 0; i < transaction.getInputs().size(); i++)	{
							    for(int i = 0; i < 1; i++)	{

						        	if(seenAddresses.contains(transaction.getInputs().get(i).addr))	{
						        		continue;
						        	}
						        	else	{
							        	seenAddresses.add(transaction.getInputs().get(i).addr);
						        	}

							        LinearLayout row = (LinearLayout)inflater.inflate(R.layout.layout_tx2, null, false);
						            tvFrom = (TextView)row.findViewById(R.id.from);
						            if(i > 0)	{
						            	tvFrom.setText("");
						            }
						            tvFromAddress = (TextView)row.findViewById(R.id.from_address);
						            tvFromAddress.setTypeface(TypefaceUtil.getInstance(TxActivity.this).getGravityBoldTypeface());
						            tvFromAddress.setTextColor(0xFF676767);
						            tvFromAddress2 = (TextView)row.findViewById(R.id.from_address2);
						            tvFromAddress2.setTypeface(TypefaceUtil.getInstance(TxActivity.this).getGravityLightTypeface());
						            tvFromAddress2.setTextColor(0xFF949ea3);
						            
						            if(isSending)	{
						                tvFrom.setTextColor(getResources().getColor(R.color.blockchain_red));
						                ((LinearLayout)findViewById(R.id.div1)).setBackgroundResource(R.color.blockchain_red);
						                ((LinearLayout)row.findViewById(R.id.div2)).setBackgroundResource(R.color.blockchain_red);
						                ((LinearLayout)findViewById(R.id.div3)).setBackgroundResource(R.color.blockchain_red);
						                ((LinearLayout)findViewById(R.id.div4)).setBackgroundResource(R.color.blockchain_red);
						            }
						            else	{
						                tvFrom.setTextColor(getResources().getColor(R.color.blockchain_green));
						                ((LinearLayout)findViewById(R.id.div1)).setBackgroundResource(R.color.blockchain_green);
						                ((LinearLayout)row.findViewById(R.id.div2)).setBackgroundResource(R.color.blockchain_green);
						                ((LinearLayout)findViewById(R.id.div3)).setBackgroundResource(R.color.blockchain_green);
						                ((LinearLayout)findViewById(R.id.div4)).setBackgroundResource(R.color.blockchain_green);
						            }

						        	if(transaction.getInputValues().size() == 1) {
						                ((TextView)row.findViewById(R.id.result2)).setVisibility(View.INVISIBLE);
						        	}
						        	else {
						                TextView tvResult2 = (TextView)row.findViewById(R.id.result2);
						                long value = transaction.getTotalValues().get(transaction.getInputs().get(i).addr);
						                String strValue = BlockchainUtil.formatBitcoin(BigInteger.valueOf(value).abs());
//						                tvResult2.setText(strValue + " BTC");
						                tvResult2.setVisibility(View.GONE);
//						                txAmounts.put(tvResult2, strValue);
						        	}

							        //
							        // FROM
							        //
						        	if(labels.get(transaction.getInputs().get(i).addr) != null) {
						        		from = labels.get(transaction.getInputs().get(i).addr);
						        	}
						        	else {
						        		from = transaction.getInputs().get(i).addr;
						        		final String address = from;
						        	}
						        	if(from.length() > 15) {
						        		from = from.substring(0, 15) + "...";
						        	}
						        	String shortAddress = transaction.getInputs().get(i).addr;
						        	if(shortAddress.length() > 15) {
						        		shortAddress = shortAddress.substring(0, 15) + "...";
						        	}

						        	if(labels.get(transaction.getInputs().get(i).addr) != null) {
							        	tvFromAddress.setText(from);
							        	tvFromAddress2.setText(shortAddress);
						        	}
						        	else {
							        	tvFromAddress.setText(shortAddress);
							        	tvFromAddress2.setVisibility(View.GONE);
						        	}

						        	final String addr = transaction.getInputs().get(i).addr;
						        	tvFromAddress.setOnClickListener(new OnClickListener() {
						        	    public void onClick(View arg0) {
						          			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
						          		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", addr);
						          		    clipboard.setPrimaryClip(clip);
						         			Toast.makeText(TxActivity.this, "Address copied to clipboard", Toast.LENGTH_LONG).show();
						        	    }
						        	});
						        	if(labels.get(addr) == null) {
						            	tvFromAddress.setOnLongClickListener(new OnLongClickListener() {
						            	    public boolean onLongClick(View arg0) {
						                    	promptDialogForAddToAddressBook(addr);            			
						            	        return true;
						            	    }
						            	});
						        	}
						        	tvFromAddress2.setOnClickListener(new OnClickListener() {
						        	    public void onClick(View arg0) {
						          			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
						          		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", addr);
						          		    clipboard.setPrimaryClip(clip);
						         			Toast.makeText(TxActivity.this, "Address copied to clipboard", Toast.LENGTH_LONG).show();
						        	    }
						        	});
						        	if(labels.get(addr) == null) {
						            	tvFromAddress2.setOnLongClickListener(new OnLongClickListener() {
						            	    public boolean onLongClick(View arg0) {
						                    	promptDialogForAddToAddressBook(addr);            			
						            	        return true;
						            	    }
						            	});
						        	}

						        	TableRow tablerow = new TableRow(TxActivity.this);
						            tablerow.setOrientation(TableRow.HORIZONTAL);
						            tablerow.addView(row);
						            froms.addView(tablerow);

						            if(processed == 2) {
						            	break;
						            }
						            processed++;

						        }

						        String toTxAddress = null;
						        if(isSending) {
						        	toTxAddress = transaction.getOutputs().get(0).addr;
						        }
						        else {
						    		final WalletApplication application = (WalletApplication)TxActivity.this.getApplication();
						    		MyRemoteWallet wallet = application.getRemoteWallet();
						    		List<String> activeAddresses = Arrays.asList(wallet.getActiveAddresses());

						        	if(transaction.getOutputs() != null && transaction.getOutputs().size() > 0) {
							        	for(int i = 0; i < transaction.getOutputs().size(); i++) {
							        		if(activeAddresses.contains(transaction.getOutputs().get(i).addr)) {
							        			toTxAddress = transaction.getOutputs().get(i).addr;
							        			break;
							        		}
							        	}
							        	
							        	toTxAddress = transaction.getOutputs().get(0).addr;
						        	}
						        }

						        //
						        // TO
						        //
						    	if(labels.get(toTxAddress) != null) {
						    		to = labels.get(toTxAddress);
						    	}
						    	else {
						    		final String address = toTxAddress;
						    		to = toTxAddress;
						    	}
						    	if(to.length() > 15) {
						    		to = to.substring(0, 15) + "...";
						    	}
						    	String shortAddress = toTxAddress;
						    	if(shortAddress.length() > 15) {
						    		shortAddress = shortAddress.substring(0, 15) + "...";
						    	}

						    	if(labels.get(toTxAddress) != null) {
						        	tvToAddress.setText(to);
						        	tvToAddress2.setText(shortAddress);
						    	}
						    	else {
						        	tvToAddress.setText(shortAddress);
						        	tvToAddress2.setVisibility(View.GONE);
						    	}

						    	final String addr = toTxAddress;
						    	tvToAddress.setOnClickListener(new OnClickListener() {
						    	    public void onClick(View arg0) {
						      			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
						      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", addr);
						      		    clipboard.setPrimaryClip(clip);
						     			Toast.makeText(TxActivity.this, "Address copied to clipboard", Toast.LENGTH_LONG).show();
						    	    }
						    	});
						    	if(labels.get(addr) == null) {
						        	tvToAddress.setOnLongClickListener(new OnLongClickListener() {
						        	    public boolean onLongClick(View arg0) {
						                	promptDialogForAddToAddressBook(addr);            			
						        	        return true;
						        	    }
						        	});
						    	}
						    	tvToAddress2.setOnClickListener(new OnClickListener() {
						    	    public void onClick(View arg0) {
						      			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)TxActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
						      		    android.content.ClipData clip = android.content.ClipData.newPlainText("Address", addr);
						      		    clipboard.setPrimaryClip(clip);
						     			Toast.makeText(TxActivity.this, "Address copied to clipboard", Toast.LENGTH_LONG).show();
						    	    }
						    	});
						    	if(labels.get(addr) == null) {
						        	tvToAddress2.setOnLongClickListener(new OnLongClickListener() {
						        	    public boolean onLongClick(View arg0) {
						                	promptDialogForAddToAddressBook(addr);            			
						        	        return true;
						        	    }
						        	});
						    	}
						    	
						    	latestBlock.setData(results[1]);
						    	latestBlock.parse();
						    	latest_block = latestBlock.getLatestBlock();

						    	if(height > 01L && latest_block > 0L) {
						        	tvValueConfirmations.setText(Long.toString((latest_block - height) + 1));
						    	}
						    	
						    	if(progress != null) {
							    	progress.dismiss();
						    	}
							
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}

