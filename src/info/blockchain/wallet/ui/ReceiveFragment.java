package info.blockchain.wallet.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.zbar.Symbol;

//import org.json.simple.JSONObject;

import piuk.blockchain.android.Constants;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.View.OnFocusChangeListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.OnEditorActionListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.text.InputType;
//import android.util.Log;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

public class ReceiveFragment extends Fragment   {

	private boolean addressesOn = true;
	private boolean contactsOn = true;

	private View rootView = null;

    private EditText edAmount1 = null;
    private TextView tvAmount2 = null;
    private EditText edAddress = null;
    private TextView tvCurrency = null;

    private TextView tvAddress = null;
    private TextView tvAddressBis = null;
    private ImageView ivReceivingQR = null;
	private String strCurrentFiatSymbol = "$";
	private String strCurrentFiatCode = "USD";

	private boolean isMagic = false;
    private View oldView = null;
    private LinearLayout parent = null;
    private LinearLayout magic = null;
    private int children = 0;
    private View childIcons = null;
    private View childList = null;
    private ListView magicList = null;

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

    private ImageView ivClearInput = null;
    private boolean isKeyboard = false;

    private List<HashMap<String,String>> magicData = null;
    private List<HashMap<String,String>> filteredDisplayList = null;
	private MagicAdapter adapter = null;
	private String currentSelectedAddress = null;
	private String defaultAddress = null;

	private List<String> activeAddresses;
	private Map<String,String> labels;
	private List<Map<String, Object>> addressBookMapList;

	private boolean isReturnFromOutsideApp = false;
	private boolean isBTC = true;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_receive, container, false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        strCurrentFiatCode = prefs.getString("ccurrency", "USD");
        strCurrentFiatSymbol = prefs.getString(strCurrentFiatCode + "-SYM", "$");

        tvAddress = (TextView)rootView.findViewById(R.id.receiving_address);
//        tvAddress.setVisibility(View.INVISIBLE);
        tvAddressBis = (TextView)rootView.findViewById(R.id.receiving_address_bis);
//        tvAddressBis.setVisibility(View.INVISIBLE);

        initMagicList();

        ivReceivingQR = (ImageView)rootView.findViewById(R.id.qr);
//        ivReceivingQR.setVisibility(View.INVISIBLE);
        ivReceivingQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
    			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    		    android.content.ClipData clip = null;
	        	if (currentSelectedAddress != null) {
	    		    clip = android.content.ClipData.newPlainText("Send address", currentSelectedAddress);
	    			Toast.makeText(getActivity(), R.string.copied_address_to_clipboard, Toast.LENGTH_LONG).show();
	        	} else {
	    		    clip = android.content.ClipData.newPlainText("Send address", edAddress.getText().toString());
	    			Toast.makeText(getActivity(), R.string.copied_address_to_clipboard, Toast.LENGTH_LONG).show();
	        	}
    		    clipboard.setPrimaryClip(clip);
            }
      	});
        
        ivReceivingQR.setOnLongClickListener(new View.OnLongClickListener() {
      	  public boolean onLongClick(View view) {
    			
      		  String strFileName = getActivity().getCacheDir() + File.separator + "qr.png";
      		  File file = new File(strFileName);
      		  if(!file.exists()) {
      			  try {
          			  file.createNewFile();
      			  }
      			  catch(Exception e) {
						Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
      			  }
      		  }
      		  file.setReadable(true,  false);

    			FileOutputStream fos = null;
    			try {
        			fos = new FileOutputStream(file);
    			}
    			catch(FileNotFoundException fnfe) {
    				;
    			}
    			
    			android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
    		    android.content.ClipData clip = null;
	        	if (currentSelectedAddress != null) {
	    		    clip = android.content.ClipData.newPlainText("Send address", currentSelectedAddress);
	        	} else {
	    		    clip = android.content.ClipData.newPlainText("Send address", edAddress.getText().toString());
	        	}
    		    clipboard.setPrimaryClip(clip);

    			if(file != null && fos != null) {
        			Bitmap bitmap = ((BitmapDrawable)ivReceivingQR.getDrawable()).getBitmap();
        	        bitmap.compress(CompressFormat.PNG, 0, fos);
        	        
        			try {
            			fos.close();
        			}
        			catch(IOException ioe) {
        				;
        			}

        			isReturnFromOutsideApp = true;
        			
        	        Intent intent = new Intent(); 
        	        intent.setAction(Intent.ACTION_SEND); 
        	        intent.setType("image/png"); 
        	        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        	        startActivity(Intent.createChooser(intent, "Send payment code"));
    			}
    	        
      	    return true;
      	  }
      	});
        
        currentSelectedAddress = defaultAddress;
        ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
        String label = labels.get(currentSelectedAddress);
        String defaultDest = currentSelectedAddress;
        if(label != null) {
        	defaultDest = label;
        }
        tvAddress.setText(defaultDest);
        
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
                // ivReceivingQR.setVisibility(View.INVISIBLE);
            }
        });

        ivClearInput = (ImageView)rootView.findViewById(R.id.input_toggle);

    	LinearLayout divider1 = (LinearLayout)rootView.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
    	LinearLayout divider2 = (LinearLayout)rootView.findViewById(R.id.divider2);
    	divider2.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
    	LinearLayout divider3 = (LinearLayout)rootView.findViewById(R.id.divider3);
    	divider3.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
    	LinearLayout divider4 = (LinearLayout)rootView.findViewById(R.id.divider4);
    	divider4.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

        ((ImageView)rootView.findViewById(R.id.direction)).setImageResource(R.drawable.green_arrow);
        ((TextView)rootView.findViewById(R.id.currency)).setText(strCurrentFiatSymbol);
        ((TextView)rootView.findViewById(R.id.currency)).setTypeface(TypefaceUtil.getInstance(getActivity()).getGravityBoldTypeface());

//        initMagicList();

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
        edAmount1.setOnEditorActionListener(new OnEditorActionListener() {
		    @Override
		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        if(actionId == EditorInfo.IME_ACTION_DONE) {

		        	if(edAddress.getText().toString() == null || edAddress.getText().toString().length() < 1) {
//						Toast.makeText(getActivity(), "Include a Bitcoin receiving address", Toast.LENGTH_LONG).show();
//		        		return false;
		        		currentSelectedAddress = defaultAddress;
			        	tvAddress.setText(currentSelectedAddress.substring(0, 15));
		        	}

		        	icon_row.setVisibility(View.GONE);

		        	tvAddress.setVisibility(View.VISIBLE);
		        	tvAddressBis.setVisibility(View.VISIBLE);
		        	ivReceivingQR.setVisibility(View.VISIBLE);

 		            if(currentSelectedAddress != null) {
 		            	tvAddressBis.setText(currentSelectedAddress);
 		            }
 		            else {
 		            	tvAddressBis.setVisibility(View.GONE);
 		            }

		        	if(edAddress.getText().toString().length() > 15) {
			        	tvAddress.setText(edAddress.getText().toString());
		        	}
		        	else {
			        	tvAddress.setText(edAddress.getText().toString());
		        	}
		        	
 		            if(BitcoinAddressCheck.isValidAddress(edAddress.getText().toString())) {
 		            	tvAddressBis.setVisibility(View.GONE);
 		            }

		        	String amount1 = edAmount1.getText().toString();
		        	if(amount1 == null || amount1.length() < 1) {
		        		amount1 = "0.00";
		        	}
		        	String amount2 = tvAmount2.getText().toString().substring(0, tvAmount2.getText().toString().length() - 4);
		        	long btcValue;
		        	double value;
		        	if(isBTC) {
		            	value = Math.round(Double.parseDouble(amount1) * 100000000.0);
		            	btcValue = (Double.valueOf(value)).longValue();
		        		amount1 += " BTC";
		        		amount2 += " " + strCurrentFiatCode;
		        	}
		        	else {
		            	value = Math.round(Double.parseDouble(amount2) * 100000000.0);
		            	btcValue = (Double.valueOf(value)).longValue();
		        		amount1 += " " + strCurrentFiatCode;
		        		amount2 += " BTC";
		        	}

		        	if (currentSelectedAddress != null) {
//		        		Log.d("currentSelectedAddress", "currentSelectedAddress " + currentSelectedAddress);
			            ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.valueOf(btcValue), "", "")));		        		
		        	} else {
						Toast.makeText(getActivity(), "Include a valid Bitcoin receiving address", Toast.LENGTH_LONG).show();
						clearReceive();
		        		return false;
		        	}

		        	edAmount1.clearFocus();
	                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	                imm.hideSoftInputFromWindow(edAmount1.getWindowToken(), 0);

		        }
		        return false;
		    }
		});

        edAmount1 = ((EditText)rootView.findViewById(R.id.amount1));
        edAmount1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(ivReceivingQR.getVisibility() == View.VISIBLE) {
            		clearReceive();
            	}
            		
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
        edAddress.setHint(R.string.request_payment_hint);
        edAddress.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
            	
            	if(ivReceivingQR.getVisibility() == View.VISIBLE) {
            		clearReceive();
            	}

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
        		if((edAddress.getText().toString() != null && edAddress.getText().toString().length() > 0) || (edAmount1.getText().toString() != null && edAmount1.getText().toString().length() > 0)) {
        			String addr = edAddress.getText().toString();
        			if (BitcoinAddressCheck.isValidAddress(addr)) {
        				ivReceivingQR.setVisibility(View.VISIBLE);
        				ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(addr, null, "", "")));		        		
        			} else {
           				ivReceivingQR.setVisibility(View.INVISIBLE);       				
        			}

        		}
        		else {
        			;
        		}
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

		            icon_row.setVisibility(View.INVISIBLE);

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
            	
            	clearReceive();

                return false;
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
        magic_qr.setVisibility(View.GONE);

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
//                    	clearReceive();
                    	InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    	                imm.hideSoftInputFromWindow(edAddress.getWindowToken(), 0);
                    	isKeyboard = true;
                	}

              		break;
              	}

              return true;
          }
        });

        tvCurrency.setTypeface(TypefaceUtil.getInstance(getActivity()).getBTCTypeface());
        tvCurrency.setText(Character.toString((char)TypefaceUtil.getInstance(getActivity()).getBTCSymbol()));

        return rootView;
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
        
        if(edAddress.getText().length() < 1 && (edAmount1.getText().length() < 1 || edAmount1.getText().equals("0.0000"))) {
        	ivClearInput.setVisibility(View.INVISIBLE);
        	isKeyboard = true;
        }
        else {
        	ivClearInput.setVisibility(View.VISIBLE);
        	isKeyboard = false;
        }
        
        if(!isReturnFromOutsideApp) {
//            removeMagicList();
//        	displayMagicList();
        }
        else {
        	isReturnFromOutsideApp = false;
        }

    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;
        int qrCodeDimension = 280;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

    public static void sendViewToBack(final View child) {
    	if(child != null) {
            final ViewGroup parent = (ViewGroup)child.getParent();
            if (null != parent) {
                parent.removeView(child);
                parent.addView(child, 0);
            }
    	}
    }

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
        AddressManager addressManager = new AddressManager(wallet, application, getActivity());        
        
        magicData =  new ArrayList<HashMap<String,String>>();
        
        filteredDisplayList = new ArrayList<HashMap<String,String>>();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		defaultAddress = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);

        for(int i = 0; i < activeAddresses.size(); i++) {
        	
        	if(defaultAddress == null && !addressManager.isWatchOnly(activeAddresses.get(i))) {
                defaultAddress = activeAddresses.get(i);
        	}

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
	        rootView = inflater.inflate(R.layout.fragment_receive, null, false);
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
        layoutContacts.setVisibility(View.GONE);
        layoutPhoneContacts = (LinearLayout)childIcons.findViewById(R.id.phone_contacts_bg);
        layoutPhoneContacts.setVisibility(View.GONE);
        tvAddresses = (TextView)childIcons.findViewById(R.id.addresses);
        tvContacts = (TextView)childIcons.findViewById(R.id.contacts);
        tvPhoneContacts = (TextView)childIcons.findViewById(R.id.phone_contacts);

		layoutAddresses.setBackgroundColor(color_contacts_selected);
		tvAddresses.setTextColor(0xFFffffff);
//		layoutContacts.setBackgroundColor(color_contacts_unselected);
//		tvContacts.setTextColor(0xFF000000);

        addressesOn = true;
//        contactsOn = false;
        layoutAddresses.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!addressesOn) {
            			addressesOn = true;
//            			contactsOn = false;
            			layoutAddresses.setBackgroundColor(color_contacts_selected);
            			tvAddresses.setTextColor(0xFFffffff);
//            			layoutContacts.setBackgroundColor(color_contacts_unselected);
//            			tvContacts.setTextColor(0xFF000000);
            		}
            		initMagicList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        /*
        layoutContacts.setOnClickListener(new View.OnClickListener() {        
            @Override
                public void onClick(View view) {
            		if(!contactsOn) {
            			contactsOn = true;
            			addressesOn = false;
            			layoutContacts.setBackgroundColor(color_contacts_selected);
            			tvContacts.setTextColor(0xFFffffff);
            			layoutAddresses.setBackgroundColor(color_contacts_unselected);
            			tvAddresses.setTextColor(0xFF000000);
            		}
            		initAddressBookList();
            		adapter.notifyDataSetChanged();                            		
                }
        });
        */

        //	    parent.addView(child, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
	    parent.addView(childIcons);
	    children++;

    	LinearLayout divider1 = (LinearLayout)childIcons.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);

        //
        // add view with list
        //
		childList = inflater.inflate(R.layout.magic2, null);
    	divider1 = (LinearLayout)childList.findViewById(R.id.divider1);
    	divider1.setBackgroundColor(BlockchainUtil.BLOCKCHAIN_GREEN);
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

				ivReceivingQR.setVisibility(View.VISIBLE);
				ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, null, "", "")));		        		
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

//        LinearLayout container = ((LinearLayout)rootView.findViewById(R.id.qr_container));
//        sendViewToBack(container);
        
//	    parent.bringToFront();
	    parent.requestLayout();
	    parent.invalidate();
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

    private void clearReceive()	{
    	edAddress.setText("");
        edAddress.setHint(R.string.request_payment_hint);
      	edAmount1.setText("");
      	if(isBTC) {
          	edAmount1.setHint("0.0000");
      	}
      	else {
          	edAmount1.setHint("0.00");
      	}
    	edAmount1.setText("0.0000");
    	
      	if(isBTC) {
          	tvAmount2.setText("0.00" + " " + strCurrentFiatCode);
      	}
      	else {
          	tvAmount2.setText("0.0000" + " BTC");
      	}

        tvAddress.setText("");
//        tvAddress.setVisibility(View.INVISIBLE);
        tvAddressBis.setText("");
//        tvAddressBis.setVisibility(View.INVISIBLE);
        
//        ivReceivingQR.setVisibility(View.INVISIBLE);
        
        currentSelectedAddress = defaultAddress;
        ivReceivingQR.setVisibility(View.VISIBLE);
        ivReceivingQR.setImageBitmap(generateQRCode(BitcoinURI.convertToBitcoinURI(currentSelectedAddress, BigInteger.ZERO, "", "")));
        String label = labels.get(currentSelectedAddress);
        String defaultDest = currentSelectedAddress;
        if(label != null) {
        	defaultDest = label;
        }
        tvAddress.setText(defaultDest);

        ivClearInput.setVisibility(View.INVISIBLE);
        icon_row.setVisibility(View.VISIBLE);

    }

}
