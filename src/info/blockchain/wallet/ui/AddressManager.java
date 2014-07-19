package info.blockchain.wallet.ui;

import java.math.BigInteger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.widget.EditText;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.uri.BitcoinURI;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.util.WalletUtils;

public class AddressManager {
	private MyRemoteWallet blockchainWallet = null;
	private WalletApplication application = null;
	private Activity activity = null;
	private static AddressManager instance = null;
	protected Handler handler;

	public static AddressManager getInstance(MyRemoteWallet remoteWallet, WalletApplication application, Activity activity) {
		if(instance == null) {
			instance = new AddressManager(remoteWallet, application, activity);
		}
		
		return instance;
	}

	public AddressManager(MyRemoteWallet remoteWallet, WalletApplication application, Activity activity) {
		this.blockchainWallet = remoteWallet;
		this.application = application;
		this.activity = activity;
		this.handler = new Handler();
	}	
		
	public BigInteger getBalance(final String address) {
		return this.blockchainWallet.getBalance(address);
	}
	
	public boolean isWatchOnly(String address) {
		try {
			return this.blockchainWallet.isWatchOnly(address);
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean canAddAddressBookEntry(final String address, final String label) {
		if (blockchainWallet.findAddressBookEntry(address) == null)
			return true;
		else
			return false;
	}
	
	public boolean handleAddAddressBookEntry(final String address, final String label, final SuccessCallback callback) {
		try {
			if (blockchainWallet == null)
				return true;

			if (! BitcoinAddressCheck.isValidAddress(address)) {
        		Toast.makeText(activity, R.string.invalid_bitcoin_address, Toast.LENGTH_LONG).show();
				return false;
			}
				
			blockchainWallet.addAddressBookEntry(address, label);

			application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
		    		Log.d("AddressManager", "AddressManager saveWallet onSuccess");			    		
					EventListeners.invokeWalletDidChange();
					callback.onSuccess();
				}

				@Override
				public void onFail() {
		    		Log.d("AddressManager", "AddressManager saveWallet onFail");
		    		callback.onFail();
				}
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
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
        		Toast.makeText(activity, R.string.send_coins_fragment_receiving_address_error, Toast.LENGTH_LONG).show();
				return;
			}
		}

		final String finalAddress = address;

		try {
			application.getRemoteWallet().addWatchOnly(finalAddress, "android_watch_only");
			
			application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
		    		Log.d("AddressManager", "AddressManager onSavedAddress onSuccess");			    		
		    		application.checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword(), new SuccessCallback() {
		    			@Override
		    			public void onSuccess() {
			        		Toast.makeText(activity, R.string.scanned_watch_only_address, Toast.LENGTH_LONG).show();

		    			}
		    			
		    			public void onFail() {
			        		Toast.makeText(activity, R.string.scanned_watch_only_address, Toast.LENGTH_LONG).show();
		    			}
		    		});
				}

				@Override
				public void onFail() {
	        		Toast.makeText(activity, R.string.error_scanning_address, Toast.LENGTH_LONG).show();
				}
			});
		} catch (Exception e) {					
    		Toast.makeText(activity, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}
	
	public void handleScanPrivateKeyPair(final ECKey key) throws Exception {

		new Thread() {
			public void run() {
				try {
					final String address;
					if (key.isCompressed()) {
						address = key.toAddressCompressed(MainNetParams.get()).toString();
					} else {
						address = key.toAddressUnCompressed(MainNetParams.get()).toString();
					}

					application.addKeyToWallet(key, key.toAddress(MainNetParams.get()).toString(), null, 0,
							new AddAddressCallback() {

						public void onSavedAddress(String address) {
				    		application.checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword(), new SuccessCallback() {
				    			@Override
				    			public void onSuccess() {
					        		Toast.makeText(activity, R.string.scanned_private_key, Toast.LENGTH_LONG).show();
				    			}
				    			
				    			public void onFail() {
					        		Toast.makeText(activity, R.string.scanned_private_key, Toast.LENGTH_LONG).show();
				    			}
				    		});
				    						    			
						}

						public void onError(String reason) {
			        		Toast.makeText(activity, R.string.error_scanning_private_key, Toast.LENGTH_LONG).show();
						}
					});
				} catch (final Exception e) {
					e.printStackTrace();

					handler.post(new Runnable() {
						public void run() {

						}
					});
				}				
				
			}
		}.start();					
	}
	
	public void setAddressLabel(final String address, final String label,
			final Runnable checkIfWalletHasUpdatedAndFetchTransactionsFail,
			final Runnable settingLabelFail,
			final Runnable syncingWalletFail) {
		if (blockchainWallet == null)
			return;

		application.checkIfWalletHasUpdatedAndFetchTransactions(blockchainWallet.getTemporyPassword(), new SuccessCallback() {

			@Override
			public void onSuccess() {
				try {
					blockchainWallet.addLabel(address, label);

					new Thread() {
						@Override
						public void run() {
							try {
								blockchainWallet.remoteSave();

								System.out.println("invokeWalletDidChange()");

								EventListeners.invokeWalletDidChange();
							} catch (Exception e) {
								e.printStackTrace(); 

								application.writeException(e);

								application.getHandler().post(syncingWalletFail);
							}
						}
					}.start();
				} catch (Exception e) {
					e.printStackTrace();

					application.getHandler().post(settingLabelFail);
				}
			}

			@Override
			public void onFail() {
				application.getHandler().post(checkIfWalletHasUpdatedAndFetchTransactionsFail);
			}
		});
	}
	
	public void newAddress(final AddAddressCallback callback) {
		final ECKey key = application.getRemoteWallet().generateECKey();			
		addKeyToWallet(key, key.toAddress(MainNetParams.get()).toString(), null, 0, callback);
	}

	public void addKeyToWallet(final ECKey key, final String address, final String label, final int tag,
			final AddAddressCallback callback) {

		if (blockchainWallet == null) {
			callback.onError("Wallet null.");
			return;
		}

		if (application.isInP2PFallbackMode()) {
			callback.onError("Error saving wallet.");
			return;
		}

		try {
			final boolean success = blockchainWallet.addKey(key, address, label);
			if (success) {
				application.localSaveWallet();

				application.saveWallet(new SuccessCallback() {
					@Override
					public void onSuccess() {
						application.checkIfWalletHasUpdated(blockchainWallet.getTemporyPassword(), false, new SuccessCallback() {
							@Override
							public void onSuccess() {	
								try {
									ECKey key = blockchainWallet.getECKey(address);									
									//TODO fix so address equality matches
						    		Log.d("AddressManager", "AddressManager addKeyToWallet address1 " + address); // compressed address				    		
						    		Log.d("AddressManager", "AddressManager addKeyToWallet address2 " + key.toAddress(MainNetParams.get()).toString());	// compressed address		    		
						    		Log.d("AddressManager", "AddressManager addKeyToWallet address3 " + key.toAddressUnCompressed(MainNetParams.get()).toString());	    		
						    		Log.d("AddressManager", "AddressManager addKeyToWallet address4 " + key.toAddressCompressed(MainNetParams.get()).toString());

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

			application.writeException(e);

			callback.onError(e.getLocalizedMessage());
		}
	}
	
	
	public boolean deleteAddressBook(final String address) {
		try {
			if (blockchainWallet == null)
				return true;

			blockchainWallet.deleteAddressBook(address);
			
			application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
					EventListeners.invokeWalletDidChange();
				}

				@Override
				public void onFail() {
				}
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public boolean archiveAddress(final String address) {
		return setAddressTag(address, 2);
	}

	public boolean unArchiveAddress(final String address) {
		return setAddressTag(address, 0);
	}

	private boolean setAddressTag(final String address, long tag) {
		try {
			if (blockchainWallet == null)
				return true;

			blockchainWallet.setTag(address, tag);

			application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
					EventListeners.invokeWalletDidChange();
				}

				@Override
				public void onFail() {
				}
			});
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	public void setDefaultAddress(final String address) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
		prefs.edit().putString(Constants.PREFS_KEY_SELECTED_ADDRESS, address.toString()).commit();
	}
}