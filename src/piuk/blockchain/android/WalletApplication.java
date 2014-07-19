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

package piuk.blockchain.android;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
//import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.bitcoin.core.*;
import com.google.bitcoin.params.MainNetParams;
//import com.google.bitcoin.core.Wallet.AutosaveEventListener;
//import com.google.bitcoin.store.WalletExtensionSerializer;
import com.google.bitcoin.store.WalletProtobufSerializer;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.spongycastle.util.encoders.Hex;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyRemoteWallet.NotModfiedException;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.R;
//import piuk.blockchain.android.service.BlockchainServiceImpl;
import piuk.blockchain.android.service.WebsocketService;
import piuk.blockchain.android.ui.AbstractWalletActivity;
import piuk.blockchain.android.ui.PinEntryActivity;
import piuk.blockchain.android.SuccessCallback;
//import piuk.blockchain.android.ui.dialogs.RekeyWalletDialog;
//import piuk.blockchain.android.util.ErrorReporter;
import piuk.blockchain.android.util.RandomOrgGenerator;
import piuk.blockchain.android.util.WalletUtils;
import info.blockchain.wallet.ui.ObjectSuccessCallback;
import info.blockchain.wallet.ui.WalletUtil;

import java.io.File;
import java.io.FileInputStream; 
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("SimpleDateFormat")
public class WalletApplication extends Application {
	private MyRemoteWallet blockchainWallet;
	private SharedCoin sharedCoin;

	private final Handler handler = new Handler();
	private Timer timer;
	public int decryptionErrors = 0;
	//private Intent blockchainServiceIntent;
	private Intent websocketServiceIntent;
	public boolean didEncounterFatalPINServerError = false;
	public Wallet bitcoinjWallet;
	public Pair<Block, Integer> blockExplorerBlockPair;
	public long earliestKeyTime;
	private volatile boolean checkWalletStatusScheduled = false;
    private boolean isPassedPinScreen = false;
    private boolean isScanning = false;
    private String temporyPIN = null;

	public void setTemporyPIN(String PIN) {
		this.temporyPIN = PIN;
	}

	public String getTemporyPIN() {
		return temporyPIN;
	}

    public boolean getIsScanning(){
        return isScanning;
    }
    
    public void setIsScanning(boolean isScanning){
        this.isScanning = isScanning;
    }  
    
    public boolean getIsPassedPinScreen(){
        return isPassedPinScreen;
    }
    
    public void setIsPassedPinScreen(boolean isPassPinScreen){
        this.isPassedPinScreen = isPassPinScreen;
    }
  
    public List<String> getSharedPrefsActiveAddresses() {
    	Set<String> activeAddressesSet = PreferenceManager.getDefaultSharedPreferences(this).getStringSet("activeAddresses", null);
    	if (activeAddressesSet != null)
    		return new ArrayList<String>(activeAddressesSet);
    	else
    		return null;
    }
    
    public boolean setSharedPrefsActiveAddresses(List<String> activeAddresses) {
    	Set<String> activeAddressesSet = new HashSet<String>();
    	activeAddressesSet.addAll(activeAddresses);
		Editor edit = PreferenceManager.getDefaultSharedPreferences(this).edit();
		edit.putStringSet("activeAddresses", activeAddressesSet);  
		return edit.commit();
    }

    
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();

			handler.post(new Runnable() {
				public void run() {
					if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
					{
						boolean hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

						if (hasConnectivity) {
							checkWalletStatus(null);
						}
					}	
				}
			});
		}
	};

	public Handler getHandler() {
		return handler;
	}
	
	public boolean isGeoEnabled() {
    	LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
    	return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	public void clearWallet() {

		if (this.isInP2PFallbackMode())
			this.leaveP2PMode();

		Editor edit = PreferenceManager.getDefaultSharedPreferences(
				this).edit();

		edit.remove("guid");
		edit.remove("sharedKey");

		edit.commit();

		this.blockchainWallet = null;
		this.didEncounterFatalPINServerError = false;
		this.decryptionErrors = 0;

		this.deleteLocalWallet();
	}

	public void connect() {
		if (timer != null) {
			try {
				timer.cancel();

				timer.purge();

				timer = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		registerReceiver(broadcastReceiver, intentFilter);

		if (!WebsocketService.isRunning)
			startService(websocketServiceIntent);
	}

	public Integer getLatestHeightFromBlockExplorer() throws Exception {
		return Integer.valueOf(WalletUtils.getURL("http://blockexplorer.com/q/getblockcount"));
	}
	/*
	public Pair<Block, Integer> getLatestBlockHeaderFromBlockExplorer(Integer blockHeight) throws Exception {

		String hash = WalletUtils.getURL("http://blockexplorer.com/q/getblockhash/"+blockHeight);

		JSONObject obj = (JSONObject) new JSONParser().parse(WalletUtils.getURL("http://blockexplorer.com/rawblock/"+hash));

		Block block = new Block(Constants.NETWORK_PARAMETERS);

		block.version = ((Number)obj.get("ver")).longValue();
		block.prevBlockHash = new Sha256Hash((String)obj.get("prev_block"));
		block.merkleRoot = new Sha256Hash((String)obj.get("mrkl_root"));
		block.time = ((Number)obj.get("time")).longValue();		
		block.difficultyTarget = ((Number)obj.get("bits")).longValue();		
		block.nonce = ((Number)obj.get("nonce")).longValue();		

		block.hash = new Sha256Hash((String)obj.get("hash"));

		block.headerParsed = true;
		block.transactionsParsed = true;
		block.headerBytesValid = true;
		block.transactionBytesValid = true;

		return new Pair<Block, Integer>(block, blockHeight);
	}
*/
	public long estimateFirstSeenFromBlockExplorer() throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");

		long earliest = 0;
		for (ECKey key : bitcoinjWallet.getKeys()) {
			try {

				String url = "http://blockexplorer.com/q/addressfirstseen/" + key.toAddress(Constants.NETWORK_PARAMETERS).toString();

				String response = WalletUtils.getURL(url);

				Date date = null;
				if (response.contains("Never")) {
					date = new Date();
				} else {
					date = format.parse(response);
				}

				if (earliest == 0) {
					earliest = date.getTime();
				} else if (date.getTime() < earliest) {
					earliest = date.getTime();
				}
			} catch (Exception e) {
				e.printStackTrace(); 
			}
		}

		return earliest;
	}

	//BitcoinJ Temp Wallet
	public void saveBitcoinJWallet()
	{

		if (bitcoinjWallet == null)
			return;

		try
		{
			File walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

			bitcoinjWallet.saveToFile(walletFile);
		}
		catch (final IOException x)
		{
			throw new RuntimeException(x);
		}
	}
	/*
	private static final class WalletAutosaveEventListener implements AutosaveEventListener
	{
		public boolean caughtException(final Throwable throwable)
		{

			throwable.printStackTrace();
			return true;
		}

		public void onBeforeAutoSave(final File file)
		{
		}

		public void onAfterAutoSave(final File file)
		{
		}
	}

	//BitcoinJ Temp Wallet
	private void loadBitcoinJWallet()
	{

		File walletFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

		if (walletFile.exists())
		{
			FileInputStream walletStream = null;

			try
			{
				walletStream = new FileInputStream(walletFile);

				WalletProtobufSerializer serializer = new WalletProtobufSerializer();

				serializer.setWalletExtensionSerializer(new WalletExtensionSerializer() {
					public Wallet newWallet(NetworkParameters params) {
						return new MyWallet.WalletOverride(params);
					}
				});


				Wallet wallet = serializer.readWallet(walletStream); 

				if (wallet.getKeychainSize() > 0) {

					bitcoinjWallet = wallet;

					bitcoinjWallet.autosaveToFile(walletFile, 1, TimeUnit.SECONDS, new WalletAutosaveEventListener());
				}

			}
			catch (final Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if (walletStream != null)
				{
					try
					{
						walletStream.close();
					}
					catch (final IOException x)
					{
						x.printStackTrace();
					}
				}
			}
		}
	}
*/

	public void deleteBitcoinJLocalData() {
		try {
			//Delete the wallet file
			File bitcoinJFile = getFileStreamPath(Constants.WALLET_FILENAME_PROTOBUF);

			if (bitcoinJFile.exists()) {
				bitcoinJFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			//Clear the blockchain file (we need to rescan)
			File blockChainFile = new File(getDir("blockstore", Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE), Constants.BLOCKCHAIN_FILENAME);

			if (blockChainFile.exists())
				blockChainFile.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startBlockchainService()
	{
		if (blockchainWallet == null)
			return;

		try {
			if (bitcoinjWallet == null) {
				deleteBitcoinJLocalData();

				this.bitcoinjWallet = blockchainWallet.getBitcoinJWallet();
			} 

			new Thread(new Runnable() {

				@Override
				public void run() {
					earliestKeyTime = 0;
					blockExplorerBlockPair = null;

					try {
						earliestKeyTime = estimateFirstSeenFromBlockExplorer();
					} catch (Exception e) {
						e.printStackTrace(); 
					}

					/*if (earliestKeyTime > 0) {
						try {

							Integer height = getLatestHeightFromBlockExplorer();

							long elapsedSeconds = ((System.currentTimeMillis() - earliestKeyTime) / 1000);

							for (int ii = 0; ii < 10; ++ii) {								
								//One block every 10 minutes (600 seconds)
								int estimatedHeight = height - (ii*10000) - (int)(elapsedSeconds / 600);

								Pair<Block, Integer> pair = getLatestBlockHeaderFromBlockExplorer(estimatedHeight);

								if (pair.first.getTime().getTime() < earliestKeyTime) {
									blockExplorerBlockPair = pair;
									break;
								}
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}*/

					//if (!isBitcoinJServiceRunning())
					//	startService(blockchainServiceIntent);

					EventListeners.invokeWalletDidChange();
				}

			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void leaveP2PMode() {
		this.bitcoinjWallet = null;

		//stopService(blockchainServiceIntent);

		deleteBitcoinJLocalData();

		EventListeners.invokeWalletDidChange();
	}

	public void disconnectSoon() {

		try {
			if (timer == null) {
				timer = new Timer(); 

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.post(new Runnable() {
							public void run() {
								try {
									if (WebsocketService.isRunning)
										stopService(websocketServiceIntent);

									AbstractWalletActivity.lastDisplayedNetworkError = 0;

									if (isInP2PFallbackMode())
										saveBitcoinJWallet();

									unregisterReceiver(broadcastReceiver);

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				}, 5000);

				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.post(new Runnable() {
							public void run() {
								try {
									blockchainWallet = null;
									didEncounterFatalPINServerError = false;
									decryptionErrors = 0;

								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
					}
				}, 120000);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateNewWallet() throws Exception {
		this.blockchainWallet = new MyRemoteWallet();

		this.decryptionErrors = 0;

		this.didEncounterFatalPINServerError = false;
	}

	public void checkWalletStatus(final AbstractWalletActivity activity) {

		boolean passwordSaved = PreferenceManager.getDefaultSharedPreferences(this).contains("encrypted_password");

		if (this.isInP2PFallbackMode()) {
			if (!this.isBitcoinJServiceRunning()) {
				//startService(blockchainServiceIntent);
			}	
		} 

		if (blockchainWallet != null && decryptionErrors == 0 && (passwordSaved || didEncounterFatalPINServerError)) {
			if (!blockchainWallet.isUptoDate(Constants.MultiAddrTimeThreshold)) {
				if (checkWalletStatusScheduled) {
					return;
				}
				checkWalletStatusScheduled = true;

				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (blockchainWallet != null) {
							checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword());
						}

						checkWalletStatusScheduled = false;
					} 
				}, 2500);
			} 
		} else if (blockchainWallet == null || decryptionErrors > 0 || !passwordSaved) {

			if (activity == null || PinEntryActivity.active)
				return;

			//Remove old password 
			String old_password = PreferenceManager.getDefaultSharedPreferences(this).getString("password", null);

			if (old_password != null) {
				decryptLocalWallet(readLocalWallet(), old_password);

				PreferenceManager.getDefaultSharedPreferences(this).edit().remove("password").commit();
			}

			handler.post(new Runnable() {
				@Override
				public void run() {	
					if (!PinEntryActivity.active) {

						Intent intent = new Intent(activity, PinEntryActivity.class);

						activity.startActivity(intent);
					}
				}
			});
		}	
	}


	@Override
	public void onCreate() {
		super.onCreate();

//		ErrorReporter.getInstance().init(this);

		//blockchainServiceIntent = new Intent(this, BlockchainServiceImpl.class);
		websocketServiceIntent = new Intent(this, WebsocketService.class);

		System.setProperty("device_name", "android");

		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

			System.setProperty("device_version", pInfo.versionName);
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}

		try { 
			// Need to save session cookie for kaptcha
			CookieHandler.setDefault(new CookieManager());

			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		} catch (Throwable e) {
			e.printStackTrace();
		}

		seedFromRandomOrg();

		//loadBitcoinJWallet();

		connect();
	}

	public MyRemoteWallet getRemoteWallet() {
		return blockchainWallet;
	}

	public SharedCoin getSharedCoin() {
		return sharedCoin;
	}

	public void setSharedCoin(SharedCoin sharedCoin) {
		this.sharedCoin = sharedCoin;
	}
	
	public String getGUID() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString("guid", null);
	}

	public long getLastTriedToRegisterForNotifications() {
		return PreferenceManager.getDefaultSharedPreferences(this).getLong("last_notification_register", 0);
	}
/*
	public boolean needsWalletRekey() { 
		MyRemoteWallet wallet = getRemoteWallet();

		if (wallet == null || wallet.isNew())
			return false;

		List<String> insecure_addresses = RekeyWalletDialog.getPossiblyInsecureAddresses(wallet);

		return !getHasAskedToRekeyWallet() && insecure_addresses.size() > 0 && !RekeyWalletDialog.hasKnownAndroidAddresses(wallet);
	}
*/
	public boolean getHasAskedToRekeyWallet() { 
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("has_asked_rekeyed_wallet4", false);
	}

	public boolean setHasAskedRekeyedWallet(boolean value) { 
		return PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("has_asked_rekeyed_wallet4", value).commit();
	}

	public boolean hasRegisteredForNotifications(String guid) {
		String registered_guid = PreferenceManager.getDefaultSharedPreferences(this).getString("registered_guid", null);

		return registered_guid != null && registered_guid.equals(guid); 
	}

	public boolean setLastRegisteredForNotifications(long time) {
		Editor edit = PreferenceManager
				.getDefaultSharedPreferences(
						this
						.getApplicationContext())
						.edit(); 

		edit.putLong("last_notification_register", time);

		return edit.commit();
	}

	public boolean setRegisteredForNotifications(String guid) {
		Editor edit = PreferenceManager
				.getDefaultSharedPreferences(
						this
						.getApplicationContext())
						.edit();

		edit.putString("registered_guid", guid);

		return edit.commit();
	}

	public void registerForNotificationsIfNeeded(final String registration_id) {

		if (blockchainWallet == null)
			return;

		if (!blockchainWallet.isNew() && !hasRegisteredForNotifications(getGUID())) {

			if (getLastTriedToRegisterForNotifications() > System.currentTimeMillis()-30000) {
				System.out.println("Registered Recently");
				return;
			}

			setLastRegisteredForNotifications(System.currentTimeMillis());

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						if (blockchainWallet.registerNotifications(registration_id)) {
							setRegisteredForNotifications(getGUID());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}).start();
		} else {
			System.out.println("New wallet or already Registered");
		}
	}

	private boolean isBitcoinJServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//			if (BlockchainServiceImpl.class.getName().equals(service.service.getClassName())) {
//				return true;
//			}
		}
		return false;
	}

	public boolean isInP2PFallbackMode() {
		return bitcoinjWallet != null;
	}

	public void unRegisterForNotifications(final String registration_id) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (blockchainWallet.unregisterNotifications(registration_id)) {
						setRegisteredForNotifications(null);
					}
				} catch (Exception e) {
					e.printStackTrace(); 
				}
			}

		}).start();
	}

	public String getSharedKey() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString(
				"sharedKey", null);
	}

	public void notifyWidgets() {
		final Context context = getApplicationContext();

		// notify widgets
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		for (final AppWidgetProviderInfo providerInfo : appWidgetManager
				.getInstalledProviders()) {
			// limit to own widgets
			if (providerInfo.provider.getPackageName().equals(
					context.getPackageName())) {
				final Intent intent = new Intent(
						AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
						appWidgetManager.getAppWidgetIds(providerInfo.provider));
				context.sendBroadcast(intent);
			}
		}
	}

	public synchronized String readExceptionLog() {
		try {
			FileInputStream multiaddrCacheFile = openFileInput(Constants.EXCEPTION_LOG);

			return IOUtils.toString(multiaddrCacheFile);

		} catch (IOException e1) {
			e1.printStackTrace();

			return null;
		}
	}

	public synchronized void writeException(Exception e) {
		try {
			FileOutputStream file = openFileOutput(Constants.EXCEPTION_LOG,
					MODE_APPEND);

			PrintStream stream = new PrintStream(file);

			e.printStackTrace(stream);

			stream.close();

			file.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public synchronized void writeMultiAddrCache(String repsonse) {
		if (blockchainWallet == null)
			return;

		try {
			FileOutputStream file = openFileOutput(blockchainWallet.getGUID()
					+ Constants.MULTIADDR_FILENAME, Constants.WALLET_MODE);

			file.write(repsonse.getBytes());

			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public synchronized void checkIfWalletHasUpdatedAndFetchTransactions(final String password) {
		checkIfWalletHasUpdatedAndFetchTransactions(password, null);
	}

	public synchronized void checkIfWalletHasUpdatedAndFetchTransactions(final String password, final SuccessCallback callbackFinal) {
		checkIfWalletHasUpdated(password, true, callbackFinal);
	}

	public synchronized void checkIfWalletHasUpdated(final String password, boolean fetchTransactions, final SuccessCallback callbackFinal) {
		if (getGUID() == null || getSharedKey() == null) {
			if (callbackFinal != null) callbackFinal.onFail();
			return;
		}

		checkIfWalletHasUpdated(password, getGUID(), getSharedKey(), fetchTransactions, callbackFinal);
	}

	public void seedFromRandomOrg() {
		new Thread(new Runnable() {
			public void run() {
				try {
					MyWallet.extra_seed = RandomOrgGenerator.getRandomBytes(32);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public synchronized void checkIfWalletHasUpdated(final String password, final String guid, final String sharedKey, final boolean fetchTransactions, final SuccessCallback callbackFinal) {

		final boolean fetchTx = true;
		
//		System.out.println("checkIfWalletHasUpdated: password");

		final WalletApplication application = this;

		new Thread(new Runnable() {
			public void run() {
				JSONObject walletPayloadObj = null;
				SuccessCallback callback = callbackFinal;

				try {
					walletPayloadObj = MyRemoteWallet.getWalletPayload(guid, sharedKey);
				}
				catch(Exception e) {
					handler.post(new Runnable() {
						public void run() {
							callbackFinal.onFail();
						};
					});
				}
				
				if(walletPayloadObj == null) {
					handler.post(new Runnable() {
						public void run() {
							callbackFinal.onFail();
							return;
						};
					});
					return;
				}

				try {
					if (blockchainWallet == null) {
//						System.out.println("7:blockchainWallet == null:");
						blockchainWallet = new MyRemoteWallet(walletPayloadObj, password);
						doMultiAddr(false, null);
					} else {						
//						System.out.println("8:blockchainWallet setTemporaryPassword:");
//						blockchainWallet.setTemporyPassword(password);
						blockchainWallet.setPayload(walletPayloadObj);
					}

					decryptionErrors = 0;

					if (callback != null)  {
						handler.post(new Runnable() {
							public void run() {
								callbackFinal.onSuccess();
							};
						});
						callback = null;
					}

					EventListeners.invokeWalletDidChange();

				} catch (Exception e) {
					e.printStackTrace();

					decryptionErrors++;

					blockchainWallet = null;

					if (callback != null)  {
						handler.post(new Runnable() {
							public void run() {
								Toast.makeText(WalletApplication.this,
										R.string.toast_wallet_decryption_failed,
										Toast.LENGTH_LONG).show();

								callbackFinal.onFail();
							};
						});
						callback = null;
					}

					EventListeners.invokeWalletDidChange();

					writeException(e);

					return;
				}

				if (decryptionErrors > 0)
					return;

				localSaveWallet();

				try {
					// Get the balance and transaction
					if (fetchTx)
						doMultiAddr(true);

				} catch (Exception e) {
					e.printStackTrace();

					writeException(e);

					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(WalletApplication.this,
									R.string.toast_error_syncing_wallet,
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	private AtomicBoolean isRunningMultiAddr = new AtomicBoolean(false);

	public void doMultiAddr(final boolean notifications) {
		doMultiAddr(notifications, null);
	}

	public void doMultiAddr(final boolean notifications, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

		if (!isRunningMultiAddr.compareAndSet(false, true)) {
			if (callback != null)
				callback.onFail();

			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					String multiAddr = null;

					try {
						multiAddr = blockchainWallet.doMultiAddr(notifications);
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							multiAddr = blockchainWallet.doMultiAddr(notifications);
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					try {
						writeMultiAddrCache(multiAddr);
					} catch (Exception e) {
						e.printStackTrace();
					}

					//After multi addr the currency is set
					if (blockchainWallet.getLocalCurrencyCode() != null)
						setCurrency(blockchainWallet.getLocalCurrencyCode());

					handler.post(new Runnable() {
						public void run() {
							notifyWidgets();
						}
					});
				} finally {
					isRunningMultiAddr.set(false);
				}
			}
		}).start();
	}
	
	public void getBalances(final String[] addresses, final boolean notifications, final ObjectSuccessCallback callback) {		
		new Thread(new Runnable() {
			public void run() {
				try {
					String response = null;

					try {
						response = MyRemoteWallet.getBalances(addresses, notifications);
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							response = MyRemoteWallet.getBalances(addresses, notifications);
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail(e1.getLocalizedMessage());

							return;
						}
					}

					if (callback != null) {
						try {
							callback.onSuccess((JSONObject) new JSONParser().parse(response));
						} catch (ParseException e) {
							callback.onFail(e.getLocalizedMessage());
						}
					}

					handler.post(new Runnable() {
						public void run() {
							notifyWidgets();
						}
					});
				} finally {
				}
			}
		}).start();
	}
	
	public void getAccountInformation(final boolean notifications, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();
			
			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					try {
						blockchainWallet.getAccountInformation();
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							blockchainWallet.getAccountInformation();
						} catch (Exception e1) {
							e1.printStackTrace(); 

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					handler.post(new Runnable() {
						public void run() {

						}
					});
				} finally {

				}
			}
		}).start();
	}
	
	public static interface AddAddressCallback {
		public void onSavedAddress(String address);

		public void onError(String reason);
	}

	public void saveWallet(final SuccessCallback callback) {

		if (this.isInP2PFallbackMode()) {
			callback.onFail();
			return;
		};

		new Thread() {
			@Override
			public void run() {
				try {
					if (blockchainWallet.remoteSave()) {
						handler.post(new Runnable() {
							public void run() {

								callback.onSuccess();

								notifyWidgets();
							}
						});
					} else {
						handler.post(new Runnable() {
							public void run() {
								callback.onFail();
							}
						});
					}

				} catch (Exception e) {
					e.printStackTrace();

					writeException(e);

					handler.post(new Runnable() {
						public void run() {
							callback.onFail();

							Toast.makeText(WalletApplication.this,
									R.string.toast_error_syncing_wallet,
									Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}.start();
	}

	public void addKeyToWallet(final ECKey key, final String address, final String label, final int tag,
			final AddAddressCallback callback) {

		if (blockchainWallet == null) {
			callback.onError("Wallet null.");
			return;
		}

		if (isInP2PFallbackMode()) {
			callback.onError("Error saving wallet.");
			return;
		}

		try {
			final boolean success = blockchainWallet.addKey(key, address, label);
			if (success) {
				if (tag != 0) {
					blockchainWallet.setTag(address, tag);
				}

				localSaveWallet();

				saveWallet(new SuccessCallback() {
					@Override
					public void onSuccess() {
						checkIfWalletHasUpdated(blockchainWallet.getTemporyPassword(), false, new SuccessCallback() {
							@Override
							public void onSuccess() {	
								try {
									ECKey key = blockchainWallet.getECKey(address);									
						    		if (key != null && (key.toAddressCompressed(MainNetParams.get()).toString().equals(address) ||
						    				key.toAddressUnCompressed(MainNetParams.get()).toString().equals(address))) {
										callback.onSavedAddress(address);
									} else {
										blockchainWallet.removeKey(key);

										callback.onError("WARNING! Wallet saved but address doesn't seem to exist after re-read.");
									}
								} catch (Exception e) {
									blockchainWallet.removeKey(key);

									callback.onError("WARNING! Error checking if ECKey is valid on re-read.");
								}
							}

							@Override
							public void onFail() {
								blockchainWallet.removeKey(key);

								callback.onError("WARNING! Error checking if address was correctly saved.");
							}
						});
					}

					@Override
					public void onFail() {
						blockchainWallet.removeKey(key);

						callback.onError("Error saving wallet");
					}
				});
			} else {
				callback.onError("addKey returned false");
			}

		} catch (Exception e) {
			e.printStackTrace();

			writeException(e);

			callback.onError(e.getLocalizedMessage());
		}
	}
	
	public void updateEmail(final String email, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {

					try {
						blockchainWallet.updateEmail(email);
						blockchainWallet.setEmail(email);
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							blockchainWallet.updateEmail(email);
							blockchainWallet.setEmail(email);
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					handler.post(new Runnable() {
						public void run() {
						}
					});
				} finally {
				}
			}
		}).start();
	}
	
	
	public void updateSMS(final String smsNumber, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {

					try {
						blockchainWallet.updateSMS(smsNumber);
						blockchainWallet.setSmsNumber(smsNumber);
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							blockchainWallet.updateSMS(smsNumber);
							blockchainWallet.setSmsNumber(smsNumber);
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					handler.post(new Runnable() {
						public void run() {
						}
					});
				} finally {
				}
			}
		}).start();
	}
	
	public void updateNotificationsType(final boolean enableEmailNotification, final boolean enableSMSNotification, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {

					try {
						blockchainWallet.updateNotificationsType(enableEmailNotification, enableSMSNotification);
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							blockchainWallet.updateNotificationsType(enableEmailNotification, enableSMSNotification);
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					handler.post(new Runnable() {
						public void run() {
						}
					});
				} finally {
				}
			}
		}).start();
	}
	
	public void apiStoreKey(final String pin, final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

		final WalletApplication application = this;

		new Thread(new Runnable(){
		    @Override
		    public void run() {
				Looper.prepare();

				Editor edit = PreferenceManager.getDefaultSharedPreferences(application).edit();

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
					final JSONObject response = piuk.blockchain.android.ui.PinEntryActivity.apiStoreKey(key, value, pin);
					if (response.get("success") != null) {
						callback.onSuccess();
						edit.putString("pin_kookup_key", key);
						edit.putString("encrypted_password", MyWallet.encrypt(application.getRemoteWallet().getTemporyPassword(), value, piuk.blockchain.android.ui.PinEntryActivity.PBKDF2Iterations));

						if (!edit.commit()) {
							throw new Exception("Error Saving Preferences");
						}
						else {
						}
					}
					else {
						Toast.makeText(application, response.toString(), Toast.LENGTH_LONG).show();
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
	
	public boolean setCurrency(String currency) { 
		return PreferenceManager.getDefaultSharedPreferences(this).edit().putString(Constants.PREFS_KEY_EXCHANGE_CURRENCY, currency).commit();
	}

	public boolean setShouldDisplayLocalCurrency(boolean value) { 
		return PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("should_display_local_currency", value).commit();
	}

	public boolean getShouldDisplayLocalCurrency() { 
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("should_display_local_currency", false);
	}

	public boolean readLocalMultiAddr() {
		if (blockchainWallet == null)
			return false;

		try {
			// Restore the multi address cache
			FileInputStream multiaddrCacheFile = openFileInput(blockchainWallet.getGUID() + Constants.MULTIADDR_FILENAME);

			String multiAddr = IOUtils.toString(multiaddrCacheFile);

			blockchainWallet.parseMultiAddr(multiAddr, false);

			if (blockchainWallet.getLocalCurrencyCode() != null)
				setCurrency(blockchainWallet.getLocalCurrencyCode());

			return true;

		} catch (Exception e) {
			writeException(e);

			e.printStackTrace();

			return false;
		}
	}


	public String makeWalletChecksum(String payload) {
		try {
			return new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(payload.getBytes("UTF-8"))));
		} catch (Exception e) {}

		return null;
	}

	public String readLocalWallet() { 
		try {
			// Read the wallet from local file
			FileInputStream file = openFileInput(Constants.WALLET_FILENAME);

			return IOUtils.toString(file, "UTF-8");
		} catch (Exception e) {}

		return null;
	}

	public boolean deleteLocalWallet() {
		try {
			if (deleteFile(Constants.WALLET_FILENAME)) {
				System.out.println("Removed Local Wallet");
			} else {
				System.out.println("Error Removing Local Wallet");
			}
		} catch (Exception e) {
			writeException(e);

			e.printStackTrace();
		}  

		return false;
	}

	public boolean decryptLocalWallet(String payload, String password) {
		try {

			MyRemoteWallet wallet = new MyRemoteWallet(payload, password);

			if (wallet.getGUID().equals(getGUID())) {
				this.blockchainWallet = wallet;

				this.decryptionErrors = 0;

				EventListeners.invokeWalletDidChange();

				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			writeException(e);

			e.printStackTrace();
		}

		return false;
	}

	public void localSaveWallet() {
		if (blockchainWallet == null)
			return;

		try {
			if (blockchainWallet.isNew())
				return;

			FileOutputStream file = openFileOutput(
					Constants.WALLET_FILENAME, Constants.WALLET_MODE);

			file.write(blockchainWallet.getPayload().getBytes());

			file.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Address determineSelectedAddress() {
		if (blockchainWallet == null)
			return null;

		final String[] addresses = blockchainWallet.getActiveAddresses();

		if (addresses.length == 0)
			return null;

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final String selectedAddress = prefs.getString(Constants.PREFS_KEY_SELECTED_ADDRESS, null);

		if (selectedAddress != null) {
			for (final String address : addresses) {
				if (address.equals(selectedAddress)) {
					try {
						return new Address(Constants.NETWORK_PARAMETERS, address);
					} catch (WrongNetworkException e) {
						e.printStackTrace();
					} catch (AddressFormatException e) {
						e.printStackTrace();
					}
				}
			}
		}

		try {
			return new Address(Constants.NETWORK_PARAMETERS, addresses[0]);
		} catch (WrongNetworkException e) {
			e.printStackTrace();
		} catch (AddressFormatException e) {
			e.printStackTrace();
		}

		return null;
	}

	public final int applicationVersionCode() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException x) {
			return 0;
		}
	}

	public final String applicationVersionName() {
		try {
			return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException x) {
			return "unknown";
		}
	}
 
	public void sharedCoinRecoverSeeds(List<String> shared_coin_seeds) {
		sharedCoin.recoverSeeds(shared_coin_seeds, new SuccessCallback() {
			@Override
			public void onSuccess() {
//	            Log.d("SharedCoin", "SharedCoin recoverSeeds onSuccess");				
				handler.post(new Runnable() {
					public void run() {
						Toast.makeText(WalletApplication.this, "SharedCoin recoverSeeds Success", Toast.LENGTH_LONG).show();
					}
				});
			}

			@Override
			public void onFail() {
//	            Log.d("SharedCoin", "SharedCoin recoverSeeds onFail");				
				handler.post(new Runnable() {
					public void run() {
						Toast.makeText(WalletApplication.this, "SharedCoin recoverSeeds fail", Toast.LENGTH_LONG).show();
					}
				});
			}
		});
	}
	
	public void sendSharedCoin(List<String> fromAddresses, String toAddress, BigInteger amount) {
        if (SharedCoin.VERSION > sharedCoin.getMinSupportedVersion()) {
            try {
				sharedCoin.sendSharedCoin(2, fromAddresses, amount, toAddress, new ObjectSuccessCallback() {
					@Override
					public void onSuccess(final Object obj) {
//			            Log.d("SharedCoin", "SharedCoin sendSharedCoin onSuccess");				
						handler.post(new Runnable() {
							public void run() {
								Toast.makeText(WalletApplication.this, (String) obj, Toast.LENGTH_LONG).show();
							}
						});
					}

					@Override
					public void onFail(final String error) {
//			            Log.d("SharedCoin", "SharedCoin sendSharedCoin onFail " + error);				
						handler.post(new Runnable() {
							public void run() {
								Toast.makeText(WalletApplication.this, error, Toast.LENGTH_LONG).show();
							}
						});
					}
					
				});
			} catch (Exception e) {
//	            Log.d("SharedCoin", "SharedCoin sendSharedCoin Exception " + e.getLocalizedMessage());				
				Toast.makeText(WalletApplication.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
        }
	}


	public void sharedCoinGetInfo(final SuccessCallback callback) {
		final MyRemoteWallet blockchainWallet = this.blockchainWallet;

		if (blockchainWallet == null) {
			if (callback != null)
				callback.onFail();

			return;
		}

    	sharedCoin = SharedCoin.getInstance(this, blockchainWallet);

		new Thread(new Runnable() {
			public void run() {
				try {

					try {
						sharedCoin.getInfo();
					} catch (Exception e) {
						e.printStackTrace(); 

						try {
							//Sleep for a bit and retry
							Thread.sleep(5000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}

						try {
							sharedCoin.getInfo();
						} catch (Exception e1) {
							e1.printStackTrace(); 

							EventListeners.invokeOnMultiAddrError();

							if (callback != null)
								callback.onFail();

							return;
						}
					}

					if (callback != null)
						callback.onSuccess();

					handler.post(new Runnable() {
						public void run() {
						}
					});
				} finally {
				}
			}
		}).start();
	}
}
