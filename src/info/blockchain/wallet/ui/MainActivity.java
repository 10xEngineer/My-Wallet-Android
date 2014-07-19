package info.blockchain.wallet.ui;

import net.sourceforge.zbar.Symbol;

import com.dm.zbar.android.scanner.ZBarConstants;
import com.dm.zbar.android.scanner.ZBarScannerActivity;
import com.google.android.gcm.GCMRegistrar;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;
//import android.location.Location;
//import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.Toast;

//import android.util.Log;
import info.blockchain.wallet.ui.SendFragment;
import piuk.blockchain.android.R;
import piuk.blockchain.android.SharedCoin;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.SuccessCallback;
import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.FeedbackManager;
import net.hockeyapp.android.Tracking;
import net.hockeyapp.android.UpdateManager;

@SuppressLint("NewApi")
public class MainActivity extends FragmentActivity implements ActionBar.TabListener, SendFragment.OnCompleteListener {

    private static int ABOUT_ACTIVITY 		= 1;
    private static int PICK_CONTACT 		= 2;
    private static int SETTINGS_ACTIVITY	= 3;
    private static int ADDRESSBOOK_ACTIVITY	= 4;
    private static int MERCHANT_ACTIVITY	= 5;

	private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;

    private String[] tabs = null;

	private static int ZBAR_SCANNER_REQUEST = 2026;

	long lastMesssageTime = 0;

	private WalletApplication application;
	
	private boolean returningFromActivity = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    setContentView(R.layout.activity_main);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    
        boolean isFirst = false;
        boolean isSecured = false;
        boolean isDismissed = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	isFirst = extras.getBoolean("first");
        	isDismissed = extras.getBoolean("dismissed");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isValidated = false;
        isValidated = prefs.getBoolean("validated", false);
    	isSecured = prefs.getBoolean("PWSecured", false) && prefs.getBoolean("EmailBackups", false) ? true : false;
        boolean isPaired = prefs.getBoolean("paired", false);
        boolean isVirgin = prefs.getBoolean("virgin", false);

        if(isValidated || isSecured || isDismissed || isPaired || !isVirgin) {
        	;
        }
        else if(!isSecured && isFirst) {
			Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
			edit.putBoolean("first", false);
			edit.commit();

			Intent intent = new Intent(this, SecureWallet.class);
			intent.putExtra("first", true);
			startActivity(intent);
        }
        else if(!isSecured && !isFirst) {
			Intent intent = new Intent(this, SecureWallet.class);
			intent.putExtra("first", false);
			startActivity(intent);
        }
        else {
			Intent intent = new Intent(this, SetupActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
        }

	    tabs = new String[3];
	    tabs[0] = "Send";
	    tabs[1] = "Balance";
	    tabs[2] = "Receive";

        viewPager = (ViewPager) findViewById(R.id.pager);
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);

        actionBar = getActionBar();
        actionBar.hide();

        //
        // masthead logo placement
        //
//        actionBar.setTitle("");
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        
        LinearLayout layout_icons = new LinearLayout(actionBar.getThemedContext());
        ActionBar.LayoutParams layoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        layoutParams.height = 72;
        layoutParams.width = (72 * 2) + 5 + 60;
        layout_icons.setLayoutParams(layoutParams);
        layout_icons.setOrientation(LinearLayout.HORIZONTAL);

        ActionBar.LayoutParams imgParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL);
        imgParams.height = 72;
        imgParams.width = 72;
        imgParams.rightMargin = 5;

        final ImageView qr_icon = new ImageView(actionBar.getThemedContext());
        qr_icon.setImageResource(R.drawable.top_camera_icon);
        qr_icon.setScaleType(ImageView.ScaleType.FIT_XY);
        qr_icon.setLayoutParams(imgParams);
        qr_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	
        		Intent intent = new Intent(MainActivity.this, ZBarScannerActivity.class);
        		intent.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
        		startActivityForResult(intent, ZBAR_SCANNER_REQUEST);

        		return false;
            }
        });

        application = WalletUtil.getInstance(this, this).getWalletApplication();
        
        final ImageView refresh_icon = new ImageView(actionBar.getThemedContext());
        refresh_icon.setImageResource(R.drawable.refresh_icon);
        refresh_icon.setScaleType(ImageView.ScaleType.FIT_XY);
        refresh_icon.setLayoutParams(imgParams);
        refresh_icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
				Toast.makeText(MainActivity.this, "Refreshing...", Toast.LENGTH_LONG).show();
//        		application.checkIfWalletHasUpdatedAndFetchTransactions(application.getRemoteWallet().getTemporyPassword());
        		
        		try {
            		WalletUtil.getInstance(MainActivity.this, MainActivity.this).getWalletApplication().doMultiAddr(false, null);
        		}
        		catch(Exception e) {
            		Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        		}

        		return false;
            }
        });
        
        LinearLayout filler_layout = new LinearLayout(actionBar.getThemedContext());
        ActionBar.LayoutParams fillerParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        fillerParams.height = 72;
        fillerParams.width = 60;
        filler_layout.setLayoutParams(fillerParams);
        
        layout_icons.addView(refresh_icon);
        layout_icons.addView(filler_layout);
        layout_icons.addView(qr_icon);

        actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setLogo(R.drawable.masthead);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));

        actionBar.setCustomView(layout_icons);
        //
        actionBar.show();
                
        for (String tab : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab).setTabListener(this));

            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
     
                @Override
                public void onPageSelected(int position) {
                    actionBar.setSelectedNavigationItem(position);
                    
                    if(position == 1) {
                        refresh_icon.setVisibility(View.VISIBLE);
                    }
                    else {
                        refresh_icon.setVisibility(View.INVISIBLE);
                    }
                }
     
                @Override
                public void onPageScrolled(int arg0, float arg1, int arg2) { ; }
     
                @Override
                public void onPageScrollStateChanged(int arg0) { ; }
            });
        }
        
        viewPager.setCurrentItem(1);
        
        BlockchainUtil.getInstance(this);

		if (application.getRemoteWallet() != null) {
			application.checkIfWalletHasUpdatedAndFetchTransactions(application.getRemoteWallet().getTemporyPassword());
		}
/*	
		application.sharedCoinGetInfo(new SuccessCallback() {

			public void onSuccess() {			
				SharedCoin sharedCoin = application.getSharedCoin();
                Log.d("SharedCoin", "SharedCoin getInfo: onSuccess ");
                Log.d("SharedCoin", "SharedCoin getInfo isEnabled " + sharedCoin.isEnabled());
                Log.d("SharedCoin", "SharedCoin getInfo getFeePercent " + sharedCoin.getFeePercent());
                Log.d("SharedCoin", "SharedCoin getInfo getMaximumInputValue " + sharedCoin.getMaximumInputValue());
                Log.d("SharedCoin", "SharedCoin getInfo getMaximumOfferNumberOfInputs " + sharedCoin.getMaximumOfferNumberOfInputs());
                Log.d("SharedCoin", "SharedCoin getInfo getMaximumOfferNumberOfOutputs " + sharedCoin.getMaximumOfferNumberOfOutputs());
                Log.d("SharedCoin", "SharedCoin getInfo getMaximumOutputValue " + sharedCoin.getMaximumOutputValue());
                Log.d("SharedCoin", "SharedCoin getInfo getMinSupportedVersion " + sharedCoin.getMinSupportedVersion());
                Log.d("SharedCoin", "SharedCoin getInfo getMinimumFee " + sharedCoin.getMinimumFee());
                Log.d("SharedCoin", "SharedCoin getInfo getMinimumInputValue " + sharedCoin.getMinimumInputValue());
                Log.d("SharedCoin", "SharedCoin getInfo getMinimumOutputValue " + sharedCoin.getMinimumOutputValue());
                Log.d("SharedCoin", "SharedCoin getInfo getMinimumOutputValueExcludeFee " + sharedCoin.getMinimumOutputValueExcludeFee());
                Log.d("SharedCoin", "SharedCoin getInfo getRecommendedIterations " + sharedCoin.getRecommendedIterations());
                Log.d("SharedCoin", "SharedCoin getInfo getRecommendedMaxIterations " + sharedCoin.getRecommendedMaxIterations());
                Log.d("SharedCoin", "SharedCoin getInfo getRecommendedMinIterations " + sharedCoin.getRecommendedMinIterations());
                Log.d("SharedCoin", "SharedCoin getInfo getToken " + sharedCoin.getToken());

                if (sharedCoin.isEnabled()) {
	                	
                    List<String> fromAddresses = new ArrayList<String>();
                    fromAddresses.add("1BrFyKUJ2tesPnwJQ2pSnBjxfwKbFhQNrS");
                    String toAddress = "1NYVmXwijjGq43qxLscMrZq4dYh1YYUDzn";
                    BigInteger amount =  new BigInteger("10000000");
                    application.sendSharedCoin(fromAddresses, toAddress, amount);
            		
                	List<String> shared_coin_seeds = new ArrayList<String>();
            		shared_coin_seeds.add("sharedcoin-seed:a43790c285abb25bf80ed0008f1abbe1738f");	
            		//application.sharedCoinRecoverSeeds(shared_coin_seeds);
                }
			}
			
			public void onFail() {			
                Log.d("SharedCoin", "SharedCoin getInfo: onFail ");						
			}
		});            	
//*/
		checkForCrashes();
	    checkForUpdates();

	}

	public void showFeedbackActivity() {
		  FeedbackManager.register(this, getHockeyAppID(), null);
		  FeedbackManager.showFeedbackActivity(this);
	}
	
	@Override
	public void onComplete() {
		handleNavigateTo();		
	}
	
	public String getHockeyAppID() {
		return "44b8c28075f744024dd98e3774bef41f";
	}
	
	void handleNavigateTo() {
		Intent intent = getIntent();
		String navigateTo = intent.getStringExtra("navigateTo");
		if (navigateTo != null) {
			if (navigateTo.equals("merchantDirectory")) {
				doMerchantDirectory();
			} else if (navigateTo.equals("scanReceiving")) {
    			Intent intent2 = new Intent(MainActivity.this, ZBarScannerActivity.class);
    			intent2.putExtra(ZBarConstants.SCAN_MODES, new int[] { Symbol.QRCODE } );
    			startActivityForResult(intent2, ZBAR_SCANNER_REQUEST);	
			}
		}
	}
	 
	private void checkForCrashes() {
		CrashManager.register(this, getHockeyAppID());
	}

	private void checkForUpdates() {
		// Remove this for store builds!
		UpdateManager.register(this, getHockeyAppID());
	}	
	 
	@Override
	protected void onPause() {
		Tracking.stopUsage(this);                 
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Tracking.startUsage(this);
		application.setIsPassedPinScreen(true);

	    
		if(!returningFromActivity) {
			if(TimeOutUtil.getInstance().isTimedOut()) {
	        	Intent intent = new Intent(MainActivity.this, PinEntryActivity.class);
				String navigateTo = getIntent().getStringExtra("navigateTo");
				intent.putExtra("navigateTo", navigateTo);   
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
	        	intent.putExtra("verified", true);
	    		startActivity(intent);
			}
			else {
				TimeOutUtil.getInstance().updatePin();
			}
		}
		else {
			returningFromActivity = false;
		}

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		application.setIsPassedPinScreen(false);
	}
		  
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
    	case R.id.action_settings:
    		doSettings();
    		return true;
    	case R.id.address_book:
    		doAddressBook();
    		return true;
    	case R.id.nearby_merchants:
    		doMerchantDirectory();
    		return true;
    	case R.id.action_about:
    		doAbout();
    		return true;
    	case R.id.action_feedback:
    		showFeedbackActivity();
    		return true;
    	default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		application.setIsScanning(false);

		if(resultCode == Activity.RESULT_OK && requestCode == ZBAR_SCANNER_REQUEST)	{
			String strResult = data.getStringExtra(ZBarConstants.SCAN_RESULT);

        	if(strResult != null) {

		        viewPager.setCurrentItem(0);

				Intent intent = new Intent("info.blockchain.wallet.ui.SendFragment.BTC_ADDRESS_SCAN");
			    intent.putExtra("BTC_ADDRESS", strResult);
			    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        	}
			else {
				Toast.makeText(this, "Invalid address", Toast.LENGTH_LONG).show();
			}

        }
		else if(resultCode == Activity.RESULT_CANCELED && requestCode == ZBAR_SCANNER_REQUEST) {
//          Toast.makeText(this, R.string.camera_unavailable, Toast.LENGTH_SHORT).show();
		}
		else {
			;
		}
		
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
        	
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.ask_you_sure_exit).setCancelable(false);
			AlertDialog alert = builder.create();

			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					application.setIsPassedPinScreen(false);
					
					finish();
					
					dialog.dismiss();
				}}); 

			alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					
					dialog.dismiss();
				}});

			alert.show();
        	
            return true;
        }
        else	{
        	;
        }

        return false;
    }

	@Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) { ; }
 
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) { viewPager.setCurrentItem(tab.getPosition()); }
 
    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) { ; }

    private void doExchangeRates()	{
        if(hasZeroBlock())	{
            Intent intent = getPackageManager().getLaunchIntentForPackage(BlockchainUtil.ZEROBLOCK_PACKAGE);
            startActivity(intent);
        }
        else	{
        	Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BlockchainUtil.ZEROBLOCK_PACKAGE));
        	startActivity(intent);
        }
    }

    private boolean hasZeroBlock()	{
    	PackageManager pm = this.getPackageManager();
    	try	{
    		pm.getPackageInfo(BlockchainUtil.ZEROBLOCK_PACKAGE, 0);
    		return true;
    	}
    	catch(NameNotFoundException nnfe)	{
    		return false;
    	}
    }

    private void doMerchantDirectory()	{
    	if (!application.isGeoEnabled()) {
    		EnableGeo.displayGPSPrompt(this);
    	}
    	else {
			returningFromActivity = true;
        	Intent intent = new Intent(MainActivity.this, info.blockchain.merchant.directory.MapActivity.class);
    		startActivityForResult(intent, MERCHANT_ACTIVITY);
    	}
    }

    private void doAbout()	{
		returningFromActivity = true;
    	Intent intent = new Intent(MainActivity.this, AboutActivity.class);
		startActivityForResult(intent, ABOUT_ACTIVITY);
    }

    private void doSettings()	{
		returningFromActivity = true;
    	Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.ui.SettingsActivity.class);
		startActivityForResult(intent, SETTINGS_ACTIVITY);
    }

    private void doAddressBook()	{
		returningFromActivity = true;
    	Intent intent = new Intent(MainActivity.this, info.blockchain.wallet.ui.AddressBookActivity.class);
		startActivityForResult(intent, ADDRESSBOOK_ACTIVITY);
    }

    private void doSend2Friends()	{
    	Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
//    	intent.setType(ContactsContract.CommonDataKinds.Email.CONTENT_TYPE);
    	startActivityForResult(intent, PICK_CONTACT);
    }
}
