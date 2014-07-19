package info.blockchain.wallet.ui;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


//import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import net.sourceforge.zbar.Symbol;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.bitcoin.core.Transaction;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.util.WalletUtils;
import piuk.blockchain.android.SuccessCallback;
import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.MenuInflater;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
 
public class AddressBookActivity extends Activity {

	private ArrayList<String> allAddresses = null;
	private Map<String, String> labelMap = null;
	private AddressAdapter adapter = null;
    private List<Map<String, Object>> addressBookMapList = null;
    private AddressManager addressManager = null;
    private int curSelection = -1;
    WalletApplication application = null;
    
    private static int QR_GENERATION = 1;
    private static int EDIT_LABEL = 2;
    private static int SCAN_WATCH_ONLY = 3;
    private static int SCAN_CONTACTS_ADDRESS = 4;
    private static int SCAN_PRIVATE_KEY = 5;
	private String editLabelAddress = null;
	
	private final int color_spend_selected = 0xff808080;
	private final int color_spend_unselected = 0xffa0a0a0;
    
	private ImageView imgArchived;
	private ImageView imgActive;
	private ImageView imgContacts;
	private LinearLayout layoutArchived;
	private LinearLayout layoutActive;
	private LinearLayout layoutContacts;
    
    private static enum DisplayedAddresses {
		ContactsAddresses,
		ActiveAddresses,
		ArchivedAddresses
	}
	
    private DisplayedAddresses displayedAddresses = null;
    
	private EventListeners.EventListener eventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "AddressBookActivity Listener";
		}

		@Override
		public void onCoinsSent(final Transaction tx, final long result) {
			setAdapterContent();
		};

		@Override
		public void onCoinsReceived(final Transaction tx, final long result) {
			setAdapterContent();
		};

		@Override
		public void onTransactionsChanged() {
			setAdapterContent();
		};
		
		@Override
		public void onWalletDidChange() {
			setAdapterContent();
		}
		
		@Override
		public void onCurrencyChanged() {
			setAdapterContent();
		};
	};
	
	public  void setAdapterContent() {
		if (displayedAddresses == DisplayedAddresses.ActiveAddresses) {
			initActiveList();	
		} else if (displayedAddresses == DisplayedAddresses.ArchivedAddresses) {
			initArchivedList();	

		} else if (displayedAddresses == DisplayedAddresses.ContactsAddresses) {
			initContactsList();	
		} 
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addressbook);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActionBar actionBar = getActionBar();
        actionBar.hide();
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setLogo(R.drawable.masthead);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));
        actionBar.show();
        
        boolean gotoContactsAddresses = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	if(extras.getBoolean("SENDING") == true)	{
        		gotoContactsAddresses = true;
        	}
        }

        //
        //
        //
		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] activeAddresses = remoteWallet.getActiveAddresses();

		allAddresses = new ArrayList<String>();
        for(int i = 0; i < activeAddresses.length; i++)	{
        	allAddresses.add("A" + activeAddresses[i]);
        }
        
        displayedAddresses = DisplayedAddresses.ActiveAddresses;
        
		labelMap = remoteWallet.getLabelMap();
        //
        //
        //

        ListView listView = (ListView)findViewById(R.id.listview);
        listView.setLongClickable(true);
        adapter = new AddressAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
//				Toast.makeText(AddressBookActivity.this, allAddresses.get(position), Toast.LENGTH_LONG).show();
				curSelection = position;
            }
        });

        listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override 
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
//        		Toast.makeText(AddressBookActivity.this, "" + info.position, Toast.LENGTH_LONG).show();
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.address_list, menu);

				curSelection = info.position;

    	        String type = allAddresses.get(info.position).substring(0, 1);
        	    if(type.equals("A")) {
            	    menu.removeItem(R.id.unarchive_address);
            	    menu.removeItem(R.id.remove_address);
        	    }
        	    else if(type.equals("S")) {
            	    menu.removeItem(R.id.archive_address);
            	    menu.removeItem(R.id.unarchive_address);
            	    menu.removeItem(R.id.default_address);
        	    }
        	    else {
            	    menu.removeItem(R.id.edit_label);
            	    menu.removeItem(R.id.archive_address);
            	    menu.removeItem(R.id.remove_address);
            	    menu.removeItem(R.id.qr_code);
            	    menu.removeItem(R.id.default_address);
        	    }

            }
        });
        
        imgArchived = ((ImageView)findViewById(R.id.archived));
        imgActive = ((ImageView)findViewById(R.id.active));
        imgContacts = ((ImageView)findViewById(R.id.contacts));
        layoutArchived = ((LinearLayout)findViewById(R.id.archived_bg));
        layoutActive = ((LinearLayout)findViewById(R.id.active_bg));
        layoutContacts = ((LinearLayout)findViewById(R.id.contacts_bg));
        
    	imgArchived.setBackgroundColor(color_spend_unselected);
    	imgActive.setBackgroundColor(color_spend_selected);
    	imgContacts.setBackgroundColor(color_spend_unselected);
    	layoutArchived.setBackgroundColor(color_spend_unselected);
    	layoutActive.setBackgroundColor(color_spend_selected);
    	layoutContacts.setBackgroundColor(color_spend_unselected);

        layoutArchived.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	AddressBookActivity.this.imgArchived.setBackgroundColor(color_spend_selected);
            	AddressBookActivity.this.imgActive.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.imgContacts.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.layoutArchived.setBackgroundColor(color_spend_selected);
            	AddressBookActivity.this.layoutActive.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.layoutContacts.setBackgroundColor(color_spend_unselected);
            	
            	initArchivedList();

                return false;
            }
        });

        layoutActive.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	AddressBookActivity.this.imgArchived.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.imgActive.setBackgroundColor(color_spend_selected);
            	AddressBookActivity.this.imgContacts.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.layoutArchived.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.layoutActive.setBackgroundColor(color_spend_selected);
            	AddressBookActivity.this.layoutContacts.setBackgroundColor(color_spend_unselected);

            	initActiveList();

                return false;
            }
        });

        layoutContacts.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	AddressBookActivity.this.imgArchived.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.imgActive.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.imgContacts.setBackgroundColor(color_spend_selected);
            	AddressBookActivity.this.layoutArchived.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.layoutActive.setBackgroundColor(color_spend_unselected);
            	AddressBookActivity.this.layoutContacts.setBackgroundColor(color_spend_selected);

            	initContactsList();

                return false;
            }
        });

		application = WalletUtil.getInstance(this, this).getWalletApplication();
        addressManager = new AddressManager(remoteWallet, application, this);        
		EventListeners.addEventListener(eventListener);
		
		application.checkIfWalletHasUpdatedAndFetchTransactions(application.getRemoteWallet().getTemporyPassword());
		
		if(gotoContactsAddresses) {
			gotoContactsAddresses();
		}
    }

    
	public void gotoContactsAddresses() {
		imgArchived.setBackgroundColor(color_spend_unselected);
    	imgActive.setBackgroundColor(color_spend_unselected);
    	imgContacts.setBackgroundColor(color_spend_selected);
    	layoutArchived.setBackgroundColor(color_spend_unselected);
    	layoutActive.setBackgroundColor(color_spend_unselected);
    	layoutContacts.setBackgroundColor(color_spend_selected);

    	initContactsList();
	}
	
	public void goToActiveAddresses() {
		imgArchived.setBackgroundColor(color_spend_unselected);
    	imgActive.setBackgroundColor(color_spend_selected);
    	imgContacts.setBackgroundColor(color_spend_unselected);
    	layoutArchived.setBackgroundColor(color_spend_unselected);
    	layoutActive.setBackgroundColor(color_spend_selected);
    	layoutContacts.setBackgroundColor(color_spend_unselected);

    	initActiveList();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.addressbook, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    	case R.id.new_address:
	    		addressManager.newAddress(new AddAddressCallback() {
	    			public void onSavedAddress(String address) {
	    	    		Toast.makeText(AddressBookActivity.this, R.string.toast_new_address_generated, Toast.LENGTH_LONG).show();
	    	    		goToActiveAddresses();
	    			}

	    			public void onError(String reason) {
	    				Toast.makeText(AddressBookActivity.this, reason, Toast.LENGTH_LONG).show();
	    			}
	    		});

	    		return true;
	    	case R.id.scan_watch_only:
	    	 {
		    		Intent intent = new Intent(AddressBookActivity.this, ZBarScannerActivity.class);
		    		intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
	        		startActivityForResult(intent, SCAN_WATCH_ONLY);
	    	 }
	    		return true;
	    	case R.id.scan_contacts_address:
	    	 {
		    		Intent intent = new Intent(AddressBookActivity.this, ZBarScannerActivity.class);
		    		intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
	        		startActivityForResult(intent, SCAN_CONTACTS_ADDRESS);	    		
	    	 }
	    		return true;
	    	case R.id.scan_private_key:
	    	 {
		    		Intent intent = new Intent(AddressBookActivity.this, ZBarScannerActivity.class);
		    		intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
	        		startActivityForResult(intent, SCAN_PRIVATE_KEY);	    		
	    	 }
	    		return true;
	    	default:
	    		return super.onOptionsItemSelected(item);
	    }
	}

	public void handleScanPrivateKey(final String data) throws Exception {
		application.getHandler().postDelayed(new Runnable() {
			@Override
			public void run() {
				try {
					final String format = WalletUtils.detectPrivateKeyFormat(data);

					System.out.println("Scanned Private Key Format " + format);

					if (format.equals("bip38")) {
						AlertDialog.Builder builder = new AlertDialog.Builder(AddressBookActivity.this);
						builder.setMessage(R.string.enter_bip38_passphrase)
						.setCancelable(false);

						AlertDialog alert = builder.create();
						alert.setTitle(R.string.passphrase_required);

						final EditText input = new EditText(AddressBookActivity.this);
						input.setHint(R.string.password_hint);
						alert.setView(input);
						
						alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.enter), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								final String password = input.getText().toString();
								
								try {
									addressManager.handleScanPrivateKeyPair(WalletUtils.parsePrivateKey(format, data, password));
								} catch (Exception e) {
						    		Toast.makeText(AddressBookActivity.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
								}
							}}); 

						alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}});

						alert.show();
					} else {
						addressManager.handleScanPrivateKeyPair(WalletUtils.parsePrivateKey(format, data, null));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, 100);

	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(resultCode == Activity.RESULT_OK && requestCode == SCAN_WATCH_ONLY) {
			String scanData = data.getStringExtra(ZBarConstants.SCAN_RESULT);
			try {
				addressManager.handleAddWatchOnly(scanData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(resultCode == Activity.RESULT_OK && requestCode == SCAN_CONTACTS_ADDRESS) {
			String scanData = data.getStringExtra(ZBarConstants.SCAN_RESULT);
			if (addressManager.canAddAddressBookEntry(scanData, "")) {
				addressManager.handleAddAddressBookEntry(scanData, "", new SuccessCallback() {

					@Override
					public void onSuccess() {
		        		Toast.makeText(AddressBookActivity.this, R.string.success_contact_added, Toast.LENGTH_LONG).show();	
		        		gotoContactsAddresses();
					}

					@Override
					public void onFail() {
		        		Toast.makeText(AddressBookActivity.this, R.string.wallet_sync_error, Toast.LENGTH_LONG).show();	
					}
					
				});
			} else {
	    		Toast.makeText(AddressBookActivity.this, R.string.address_already_exist, Toast.LENGTH_LONG).show();
			}			
		} else if(resultCode == Activity.RESULT_OK && requestCode == SCAN_PRIVATE_KEY) {
			String scanData = data.getStringExtra(ZBarConstants.SCAN_RESULT);
			try {
				handleScanPrivateKey(scanData);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(resultCode == Activity.RESULT_OK && requestCode == EDIT_LABEL)	{
			
			if(data != null && data.getAction() != null)	{
				String label = data.getAction();
				
	    		Toast.makeText(AddressBookActivity.this, label, Toast.LENGTH_LONG).show();

	    		addressManager.setAddressLabel(editLabelAddress, label, new Runnable() {
					public void run() {
						Toast.makeText(AddressBookActivity.this,
								R.string.toast_error_syncing_wallet,
								Toast.LENGTH_LONG).show();
					}
				}, new Runnable() {
					public void run() {
						Toast.makeText(AddressBookActivity.this,
								R.string.error_setting_label,
								Toast.LENGTH_LONG).show();
					}
				}, new Runnable() {
					public void run() {
						Toast.makeText(AddressBookActivity.this,
								R.string.toast_error_syncing_wallet,
								Toast.LENGTH_LONG).show();
					}
				});
			}

        }
		else {
			;
		}
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	    EventListeners.removeEventListener(eventListener);
	}
/*	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		
		Toast.makeText(AddressBookActivity.this, "" + curSelection, Toast.LENGTH_LONG).show();

	    menu.removeItem(R.id.edit_label);
	}
*/	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
	    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo(); 
	    String address = allAddresses.get(menuInfo.position).substring(1);
		
	    switch (item.getItemId()) {
	    	case R.id.edit_label:
//	    		Toast.makeText(AddressBookActivity.this, "edit label", Toast.LENGTH_LONG).show();
	    		
	        	Intent intent = new Intent(AddressBookActivity.this, info.blockchain.wallet.ui.EditSetting.class);
	        	intent.putExtra("prompt", "Edit label");
	        	intent.putExtra("existing", address);
	        	intent.putExtra("value", labelMap.get(address));
	        	editLabelAddress = address;

	        	startActivityForResult(intent, EDIT_LABEL);

	    		return true;
	    	case R.id.archive_address:
	    		addressManager.archiveAddress(address);
	    		return true;
	    	case R.id.unarchive_address:
	    		addressManager.unArchiveAddress(address);
	    		return true;
	    	case R.id.remove_address:
	    		addressManager.deleteAddressBook(address);
	    		return true;
	    	case R.id.qr_code:
//	    		Toast.makeText(AddressBookActivity.this, "qr code address", Toast.LENGTH_LONG).show();
	    		doQRActivity();
	    		return true;
	    	case R.id.default_address:
	    		addressManager.setDefaultAddress(address);
	    		return true;
	    	default:
	    		return super.onContextItemSelected(item);
	    }
	}

    private void initArchivedList() {
        displayedAddresses = DisplayedAddresses.ArchivedAddresses;

        MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] archivedAddresses = remoteWallet.getArchivedAddresses();

		allAddresses = new ArrayList<String>();
        for(int i = 0; i < archivedAddresses.length; i++)	{
        	allAddresses.add("R" + archivedAddresses[i]);
        }

		labelMap = remoteWallet.getLabelMap();
		adapter.notifyDataSetChanged();
    }

    private void initActiveList() {
        displayedAddresses = DisplayedAddresses.ActiveAddresses;

		MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		String[] activeAddresses = remoteWallet.getActiveAddresses();

		allAddresses = new ArrayList<String>();
        for(int i = 0; i < activeAddresses.length; i++)	{
        	allAddresses.add("A" + activeAddresses[i]);
        }

		labelMap = remoteWallet.getLabelMap();
		adapter.notifyDataSetChanged();
    }

    private void initContactsList() {
        displayedAddresses = DisplayedAddresses.ContactsAddresses;

        MyRemoteWallet remoteWallet = WalletUtil.getInstance(this, this).getRemoteWallet();
        addressBookMapList = remoteWallet.getAddressBookMap();

		allAddresses = new ArrayList<String>();
		if(addressBookMapList != null) {
		    for (Iterator<Map<String, Object>> iti = addressBookMapList.iterator(); iti.hasNext();) {
		    	Map<String, Object> addressBookMap = iti.next();
		    	String address = (String)addressBookMap.get("addr");
		    	allAddresses.add("S" + address);
		    }
		}

		labelMap = remoteWallet.getLabelMap();
		adapter.notifyDataSetChanged();
    }

    private class AddressAdapter extends BaseAdapter {
    	
		private LayoutInflater inflater = null;

	    AddressAdapter() {
	        inflater = (LayoutInflater)AddressBookActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			return allAddresses.size();
		}

		@Override
		public String getItem(int position) {
	        return "";
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = null;
	        
	        if (convertView == null) {
	            view = inflater.inflate(R.layout.address_list, parent, false);
	        } else {
	            view = convertView;
	        }
	        
	        String type = allAddresses.get(position).substring(0, 1);
	        final String addr = allAddresses.get(position).substring(1);
	        
    	    String label = labelMap.get(addr);
    	    if (label == null) {
    	    	label = "Unlabeled";	
    	    }
    	    
	        ((TextView)view.findViewById(R.id.txt1)).setText(label);
	        ((TextView)view.findViewById(R.id.txt2)).setText(addr);
	       
	        if (displayedAddresses == DisplayedAddresses.ActiveAddresses || displayedAddresses == DisplayedAddresses.ArchivedAddresses) {
	        	
            	String amount = "0.000";
    	    	BigInteger balance = addressManager.getBalance(addr);
    		    if (balance != null) {
    		    	amount = BlockchainUtil.formatBitcoin(balance) + " BTC";
        		    ((TextView)view.findViewById(R.id.txt3)).setText(amount);
    		    }
    		    
    		    if (addressManager.isWatchOnly(addr)) {
    		        ((TextView)view.findViewById(R.id.txt4)).setText("Watch only");
    		    } else {
    		        ((TextView)view.findViewById(R.id.txt4)).setText("");    		    	
    		    }

	        } else {
    		    ((TextView)view.findViewById(R.id.txt3)).setText("");
		        ((TextView)view.findViewById(R.id.txt4)).setText("");    		    	
	        }
	        
	        return view;
		}

    }
    
    private void doQRActivity() {
    	
		android.content.ClipboardManager clipboard = (android.content.ClipboardManager)this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
  		android.content.ClipData clip = android.content.ClipData.newPlainText("Address", allAddresses.get(curSelection).substring(1));
  		clipboard.setPrimaryClip(clip);
 		Toast.makeText(this, "Address copied to clipboard", Toast.LENGTH_LONG).show();

        Intent intent;
    	intent = new Intent(this, QRActivity.class);
    	intent.putExtra("BTC_ADDRESS", allAddresses.get(curSelection).substring(1));
    	startActivityForResult(intent, QR_GENERATION);
    }

}

