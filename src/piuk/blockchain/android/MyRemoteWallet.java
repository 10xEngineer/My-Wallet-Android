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

import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.spongycastle.util.encoders.Hex;

import piuk.blockchain.android.Constants;
import piuk.blockchain.android.SuccessCallback;
import piuk.blockchain.android.util.WalletUtils;
import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.params.MainNetParams;


@SuppressLint("DefaultLocale")
@SuppressWarnings("unchecked")
public class MyRemoteWallet extends MyWallet {
	private static final String WebROOT = "https://"+Constants.BLOCKCHAIN_DOMAIN+"/";
	private static final String ApiCode = "--REDACTED--";

    public static final String NotificationsTypeEmail = "1";
    public static final String NotificationsTypeSMS = "32";
    
	private String _checksum;
	private boolean _isNew = false;
	private MyBlock latestBlock;
	private long lastMultiAddress;
	private BigInteger final_balance = BigInteger.ZERO;
	private BigInteger total_received = BigInteger.ZERO;
	private BigInteger total_sent = BigInteger.ZERO;

	private String language = "en";
	private String btcCurrencyCode = "USD";
	private String localCurrencyCode = "BTC";
	private boolean sync_pubkeys = true;
	private double localCurrencyConversion;
	private double btcCurrencyConversion;
	private long serverTimeOffset = 0;
	private HashSet<String> notificationsTypeSet = new HashSet<String>();
	private String smsNumber = null;
	private String email = null;

	private Map<String, JSONObject> multiAddrBalancesRoot;
	private JSONObject multiAddrRoot;

	private double sharedFee;
	private List<MyTransaction> transactions = Collections.synchronizedList(new ArrayList<MyTransaction>());
	public byte[] extra_seed;

	public static enum FeePolicy {
		FeeOnlyIfNeeded,
		FeeForce,
		FeeNever
	}

	private State state = State.INPUT;

	public static enum State
	{
		INPUT, SENDING, SENT
	}

	public void setState(State state) {
		this.state =  state;
	}

	public State getState() {
		return this.state;
	}


	public MyBlock getLatestBlock() {
		return latestBlock;
	}


	public void setFinal_balance(BigInteger final_balance) {
		this.final_balance = final_balance;
	}


	public void setTotal_received(BigInteger total_received) {
		this.total_received = total_received;
	}


	public void setTotal_sent(BigInteger total_sent) {
		this.total_sent = total_sent;
	}


	public long getLastMultiAddress() {
		return lastMultiAddress;
	}

	public void setLatestBlock(MyBlock latestBlock) {
		this.latestBlock = latestBlock;
	}

	public BigInteger getFinal_balance() {
		return final_balance;
	}


	public BigInteger getTotal_received() {
		return total_received;
	}

	public BigInteger getTotal_sent() {
		return total_sent;
	}


	public String getLocalCurrencyCode() {
		return localCurrencyCode;
	}


	public double getLocalCurrencyConversion() {
		return localCurrencyConversion;
	}


	public Map<String, JSONObject> getMultiAddrBalancesRoot() {
		return multiAddrBalancesRoot;
	}

	public BigInteger getBalanceOfAddress(String address) {
		if (multiAddrBalancesRoot == null) return null;
		JSONObject addressRoot = multiAddrBalancesRoot.get(address);	    
		return BigInteger.valueOf(((Number)addressRoot.get("final_balance")).longValue());
	}

	public JSONObject getMultiAddrRoot() {
		return multiAddrRoot;
	}


	public void setMultiAddrRoot(JSONObject multiAddrRoot) {
		this.multiAddrRoot = multiAddrRoot;
	}


	public double getSharedFee() {
		return sharedFee;
	}

	public boolean isAddressMine(String address) {
		for (Map<String, Object> map : this.getKeysMap()) {
			String addr = (String) map.get("addr");

			if (address.equals(addr))
				return true;
		}

		return false;
	}

	public static class Latestblock {
		int height;
		int block_index;
		Hash hash;
		long time;
	}

	public synchronized BigInteger getBalance() {
		return final_balance;
	}


	public static String generateSharedAddress(String destination) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("address=" + destination);
		args.append("&shared=true");
		args.append("&format=plain");
		args.append("&method=create");

		final String response = postURL("https://"+Constants.BLOCKCHAIN_DOMAIN+"/api/receive", args.toString());

		JSONObject object = (JSONObject) new JSONParser().parse(response);

		return (String)object.get("input_address");
	}

	public synchronized BigInteger getBalance(String address) {
		if (this.multiAddrBalancesRoot != null && this.multiAddrBalancesRoot.containsKey(address)) {
			return BigInteger.valueOf(((Number)this.multiAddrBalancesRoot.get(address).get("final_balance")).longValue());	
		}

		return BigInteger.ZERO;
	}

	public boolean isNew() {
		return _isNew;
	}

	public MyRemoteWallet() throws Exception {
		super();

		this.temporyPassword = null;

		this._checksum  = null;

		this._isNew = true;
	}


	public MyRemoteWallet(JSONObject walletJSONObj, String password) throws Exception {
		super(walletJSONObj.get("payload").toString(), password);

		handleWalletPayloadObj(walletJSONObj);

		this.temporyPassword = password;

		this._checksum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(walletJSONObj.get("payload").toString().getBytes("UTF-8"))));

		this._isNew = false;
	}

	public MyRemoteWallet( String base64Payload, String password) throws Exception {
		super(base64Payload, password);

		this.temporyPassword = password;

		this._checksum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(base64Payload.getBytes("UTF-8"))));

		this._isNew = false;
	}

	private static String fetchURL(String URL) throws Exception {
		return WalletUtils.getURL(URL);
	}

	public static String postURL(String request, String urlParameters) throws Exception {
        if (urlParameters.length() > 0) {
            urlParameters += "&";
        }
        
        //Include the Android API Code in POST requests
        urlParameters += "api_code="+ApiCode;
        
		return WalletUtils.postURL(request, urlParameters);
	}

	@Override
	public synchronized boolean addKey(ECKey key, String address, String label) throws Exception {
		boolean success = super.addKey(key, address, label);

		EventListeners.invokeWalletDidChange();

		return success;
	}


	@Override
	public synchronized boolean addKey(ECKey key, String address, String label, String device_name, String device_version) throws Exception {
		boolean success = super.addKey(key, address, label, device_name, device_version);

		EventListeners.invokeWalletDidChange();

		return success;
	}

	@Override
	public void setTag(String address, long tag) {
		super.setTag(address, tag);

		EventListeners.invokeWalletDidChange();
	}

	@Override
	public synchronized boolean addWatchOnly(String address, String source) throws Exception {
		boolean success = super.addWatchOnly(address, source);

		EventListeners.invokeWalletDidChange();

		return success;
	}

	public String[] getNotWatchOnlyActiveAddresses() {
		String[] from = getActiveAddresses();
		List<String> notWatchOnlyActiveAddresses = new ArrayList<String>(from.length);

        for(int i = 0; i < from.length; i++){
            try {
				if (! isWatchOnly(from[i]))
				    notWatchOnlyActiveAddresses.add(from[i]);
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
        return notWatchOnlyActiveAddresses.toArray(new String[notWatchOnlyActiveAddresses.size()]);
	}

	@Override
	public synchronized boolean removeKey(ECKey key) {
		boolean success = super.removeKey(key);

		EventListeners.invokeWalletDidChange();

		return success;
	}

	public List<MyTransaction> getMyTransactions() {
		return transactions;
	}

	public boolean addTransaction(MyTransaction tx) {

		for (MyTransaction existing_tx : transactions) {
			if (existing_tx.getTxIndex() == tx.getTxIndex())
				return false;
		}

		this.transactions.add(tx);

		return true;
	}

	public boolean prependTransaction(MyTransaction tx) {

		for (MyTransaction existing_tx : transactions) {
			if (existing_tx.getTxIndex() == tx.getTxIndex())
				return false;
		}

		this.transactions.add(0, tx);

		return true;
	}

	public BigInteger getBaseFee() {
		BigInteger baseFee = null; 
		if (getFeePolicy() == -1) {
			baseFee = Utils.toNanoCoins("0.0001");
		} else if (getFeePolicy() == 1) {
			baseFee = Utils.toNanoCoins("0.0005");
		} else {
			baseFee = Utils.toNanoCoins("0.0001");
		}

		return baseFee;
	}

	public List<MyTransaction> getTransactions() {
		return this.transactions;
	}

	public MyTransaction getTransaction(String txHash) {
		if (this.transactions == null)
			return null;

		for (Iterator<MyTransaction> iterator = this.transactions.iterator(); iterator.hasNext();) {
			MyTransaction tx = iterator.next();
			if (tx.getHashAsString().equals(txHash))
				return tx;
		}
		return null;
	}

	public void parseMultiAddr(String response, boolean notifications) throws Exception {

		transactions.clear();

		BigInteger previousBalance = final_balance;

		Map<String, Object> top = (Map<String, Object>) JSONValue.parse(response);

		this.multiAddrRoot = (JSONObject) top;

		Map<String, Object> info_obj = (Map<String, Object>) top.get("info");

		Map<String, Object> block_obj = (Map<String, Object>) info_obj.get("latest_block");

		if (block_obj != null) {
			Sha256Hash hash = new Sha256Hash(Hex.decode((String)block_obj.get("hash")));
			int blockIndex = ((Number)block_obj.get("block_index")).intValue();
			int blockHeight = ((Number)block_obj.get("height")).intValue();
			long time = ((Number)block_obj.get("time")).longValue();

			MyBlock block = new MyBlock();

			block.height = blockHeight;
			block.hash = hash;
			block.blockIndex = blockIndex;
			block.time = time;

			this.latestBlock = block;
		}

		List<JSONObject> multiAddrBalances = (List<JSONObject>) top.get("addresses");

		Map<String, JSONObject> multiAddrBalancesRoot = new HashMap<String, JSONObject>();

		for (JSONObject obj : multiAddrBalances) {
			multiAddrBalancesRoot.put((String) obj.get("address"), obj);
		}

		this.multiAddrBalancesRoot = multiAddrBalancesRoot;

		Map<String, Object> symbol_local = (Map<String, Object>) info_obj.get("symbol_local");

		boolean didUpdateCurrency = false;

		if (symbol_local != null && symbol_local.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.localCurrencyCode == null || !this.localCurrencyCode.equals(currencyCode) || this.localCurrencyConversion != currencyConversion) {
				this.localCurrencyCode = currencyCode;
				this.localCurrencyConversion = currencyConversion;
				didUpdateCurrency = true;
			}
		}


		Map<String, Object> symbol_btc = (Map<String, Object>) info_obj.get("symbol_btc");
		if (symbol_btc != null && symbol_btc.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.btcCurrencyCode == null || !this.btcCurrencyCode.equals(currencyCode) || this.btcCurrencyConversion != currencyConversion) {
				this.btcCurrencyCode = currencyCode;
				this.btcCurrencyConversion = currencyConversion;
				//didUpdateCurrency = true;
			}
		}

		if (didUpdateCurrency) {
			EventListeners.invokeCurrencyDidChange();
		}

		if (top.containsKey("mixer_fee")) {
			sharedFee = ((Number)top.get("mixer_fee")).doubleValue();
		}

		Map<String, Object> wallet_obj = (Map<String, Object>) top.get("wallet");

		this.final_balance = BigInteger.valueOf(((Number)wallet_obj.get("final_balance")).longValue());
		this.total_sent = BigInteger.valueOf(((Number)wallet_obj.get("total_sent")).longValue());
		this.total_received = BigInteger.valueOf(((Number)wallet_obj.get("total_received")).longValue());

		List<Map<String, Object>> transactions = (List<Map<String, Object>>) top.get("txs");

		MyTransaction newestTransaction = null;
		if (transactions != null) {
			for (Map<String, Object> transactionDict : transactions) {
				MyTransaction tx = MyTransaction.fromJSONDict(transactionDict);

				if (tx == null)
					continue;

				if (newestTransaction == null)
					newestTransaction = tx;

				addTransaction(tx);
			}
		}

		if (notifications) {
			if (this.final_balance.compareTo(previousBalance) != 0 && newestTransaction != null) {
				if (newestTransaction.getResult().compareTo(BigInteger.ZERO) >= 0)
					EventListeners.invokeOnCoinsReceived(newestTransaction, newestTransaction.getResult().longValue());
				else
					EventListeners.invokeOnCoinsSent(newestTransaction, newestTransaction.getResult().longValue());
			}
		} else {
			EventListeners.invokeOnTransactionsChanged();
		}
	}

	public boolean isUptoDate(long time) {
		long now = System.currentTimeMillis();

		if (lastMultiAddress < now - time) {
			return false;
		} else {
			return true;
		}
	}

	public static synchronized String getBalances(String[] addresses, boolean notifications) throws Exception {
		String url =  WebROOT + "multiaddr?active=" + StringUtils.join(addresses, "|") + "&simple=true&format=json";

		String response = fetchURL(url);

		return response;
	}

	public synchronized String doMultiAddr(boolean notifications) throws Exception {
		String url =  WebROOT + "multiaddr?active=" + StringUtils.join(getActiveAddresses(), "|") + "&symbol_btc="+btcCurrencyCode + "&symbol_local=" + localCurrencyCode;

		String response = fetchURL(url);

		parseMultiAddr(response, notifications);

		lastMultiAddress = System.currentTimeMillis();

		return response;
	}

	public synchronized void doMultiAddr(boolean notifications, SuccessCallback callback) {
		try {
			String url =  WebROOT + "multiaddr?active=" + StringUtils.join(getActiveAddresses(), "|") + "&symbol_btc=" + btcCurrencyCode + "&symbol_local=" + localCurrencyCode;

			String response = fetchURL(url);

			parseMultiAddr(response, notifications);

			lastMultiAddress = System.currentTimeMillis();

			callback.onSuccess();
		} catch (Exception e) {
			e.printStackTrace();

			callback.onFail();
		}
	}


	public interface SendProgress {
		public void onStart();

		//Return false to cancel
		public boolean onReady(Transaction tx, BigInteger fee, FeePolicy feePolicy, long priority);
		public void onSend(Transaction tx, String message);

		//Return true to cancel the transaction or false to continue without it
		public ECKey onPrivateKeyMissing(String address);

		public void onError(String message);
		public void onProgress(String message);
	}


	private List<MyTransactionOutPoint> filter(List<MyTransactionOutPoint> unspent, List<ECKey> tempKeys, boolean askForPrivateKeys, final SendProgress progress) throws Exception {		
		List<MyTransactionOutPoint> filtered = new ArrayList<MyTransactionOutPoint>();

		Set<String> alreadyAskedFor = new HashSet<String>();

		for (MyTransactionOutPoint output : unspent) {
			BitcoinScript script = new BitcoinScript(output.getScriptBytes());

			String addr = script.getAddress().toString();

			Map<String, Object> keyMap = findKey(addr);

			if (keyMap.get("priv") == null) {
				if (askForPrivateKeys && alreadyAskedFor.add(addr)) {

					ECKey key = progress.onPrivateKeyMissing(addr);

					if (key != null) {
						filtered.add(output);

						tempKeys.add(key);
					}
				}
			} else {
				filtered.add(output);
			}
		}

		return filtered;
	}

	private Pair<ECKey, String> generateNewMiniPrivateKey() {
		SecureRandom random = new SecureRandom();

		if (extra_seed != null) {
			random.setSeed(extra_seed);
		}

		while (true) {
			Log.d("generateNewMiniPrivateKey", "generateNewMiniPrivateKey");

	        //Make Candidate Mini Key
		    byte randomBytes[] = new byte[16];
		    random.nextBytes(randomBytes);		 
	        String encodedBytes = Base58.encode(randomBytes);
	        //TODO: Casascius Series 1 22-character variant, remember to notify Ben about updating to 30-character variant
	        String minikey = 'S' + encodedBytes.substring(0,21);
	        //minikey = "S8osZG4hyGCsMxfxuTUfrF"; // canned data

	        try {
		        //Append ? & hash it again
				byte[] bytes_appended = MessageDigest.getInstance("SHA-256").digest((minikey + '?').getBytes());

		        //If zero byte then the key is valid
		        if (bytes_appended[0] == 0) {		        					

					try {
			            //SHA256
						byte[] bytes = MessageDigest.getInstance("SHA-256").digest(minikey.getBytes());

						final ECKey eckey = new ECKey(bytes, null);

				        final DumpedPrivateKey dumpedPrivateKey1 = eckey.getPrivateKeyEncoded(getParams());
				        String privateKey1 = Base58.encode(dumpedPrivateKey1.bytes);

				        final String toAddress = eckey.toAddress(getParams()).toString();
						Log.d("sendCoinsToFriend", "generateNewMiniPrivateKey: minikey: " + minikey);
						Log.d("sendCoinsToFriend", "generateNewMiniPrivateKey: privateKey: " + privateKey1);
						Log.d("sendCoinsToFriend", "generateNewMiniPrivateKey: address: " + toAddress);

		            	return new Pair<ECKey, String>(eckey, minikey);

					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	            	            	
		        }
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}

	public void sendCoinsEmail(final String email, final BigInteger amount, final SendProgress progress) throws Exception {
		sendCoinsToFriend("email", email, amount, progress);
	}

	public void sendCoinsSMS(final String number, final BigInteger amount, final SendProgress progress) throws Exception {
		sendCoinsToFriend("sms", number, amount, progress);
	}

	private void sendCoinsToFriend(final String sendType,final String emailOrNumber, final BigInteger amount, final SendProgress progress) throws Exception {

		new Thread() {
			@Override
			public void run() {
				progress.onStart();
				final List<ECKey> tempKeys = new ArrayList<ECKey>();

				try {
					final String[] from = getActiveAddresses();					
					final ECKey key;
					Pair<ECKey, String> keyAndMiniKey = null;
					if (sendType == "sms") {
						keyAndMiniKey = generateNewMiniPrivateKey();
						key = keyAndMiniKey.first;							
					} else {
						key = generateECKey();
					}

					//Construct a new transaction
					progress.onProgress("Getting Unspent Outputs");

					List<MyTransactionOutPoint> allUnspent = getUnspentOutputPoints(from);

					Pair<Transaction, Long> pair = null;

					progress.onProgress("Constructing Transaction");

			        final DumpedPrivateKey dumpedPrivateKey = key.getPrivateKeyEncoded(getParams());
			        String privateKey = Base58.encode(dumpedPrivateKey.bytes);

			        final String toAddress = key.toAddress(getParams()).toString();
					Log.d("sendCoinsToFriend", "sendCoinsToFriend: privateKey: " + privateKey);
					Log.d("sendCoinsToFriend", "sendCoinsToFriend: toAddress: " + toAddress);
					BigInteger fee = BigInteger.ZERO;

					try {
						//Try without asking for watch only addresses
						List<MyTransactionOutPoint> unspent = filter(allUnspent, tempKeys, false, progress);
						HashMap<String, BigInteger> receivingAddresses = new HashMap<String, BigInteger>();
						receivingAddresses.put(toAddress, amount);
						pair = makeTransaction(false, unspent, receivingAddresses, fee, null);

						//Transaction cancelled
						if (pair == null)
							return;
					} catch (InsufficientFundsException e) {

						//Try with asking for watch only
						List<MyTransactionOutPoint> unspent = filter(allUnspent, tempKeys, true, progress);
						HashMap<String, BigInteger> receivingAddresses = new HashMap<String, BigInteger>();
						receivingAddresses.put(toAddress, amount);
						pair = makeTransaction(false, unspent, receivingAddresses, fee, null);

						//Transaction cancelled
						if (pair == null)
							return;
					}


					if (allUnspent != null) {
						Transaction tx = pair.first;

						progress.onProgress("Signing Inputs");

						Wallet wallet = new Wallet(MainNetParams.get());
						for (TransactionInput input : tx.getInputs()) {
							byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
							String address = new BitcoinScript(scriptBytes).getAddress().toString();
							final ECKey walletKey;
							try {
								walletKey = getECKey(address);
							} catch (Exception e) {
								// skip add Watch Only Bitcoin Address key because already accounted for  later with tempKeys
								continue;
							}
							ECKey keyCompressed;
							ECKey keyUnCompressed;
							BigInteger priv = new BigInteger(walletKey.getPrivKeyBytes());
							if (priv.compareTo(BigInteger.ZERO) >= 0) {
								keyCompressed = new ECKey(priv, null, true);
								keyUnCompressed = new ECKey(priv, null, false);
							} else {
								byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], walletKey.getPrivKeyBytes());
								BigInteger priv2 = new BigInteger(appendZeroByte);
								keyCompressed = new ECKey(priv2, null, true);			
								keyUnCompressed = new ECKey(priv2, null, false);												
							}

							if (keyCompressed != null) {
								wallet.addKey(keyCompressed);
							}

							if (keyUnCompressed != null) {
								wallet.addKey(keyUnCompressed);
							}
						}

						wallet.addKeys(tempKeys);

						//Now sign the inputs
						tx.signInputs(SigHash.ALL, wallet);

						progress.onProgress("Broadcasting Transaction");

				        final String txHash = tx.getHashAsString();

						Log.d("sendCoinsToFriend", "sendCoinsToFriend: txHash: " + txHash);						
						Log.d("sendCoinsToFriend", "sendCoinsToFriend: emailOrNumber: " + emailOrNumber);						

  						Map<Object, Object> params = new HashMap<Object, Object>();
						params.put("type", sendType);
						String privParameter = (sendType == "sms") ? keyAndMiniKey.second : privateKey;
						params.put("priv", privParameter);
						params.put("hash", txHash);
						params.put("to", emailOrNumber);			
						params.put("guid", getGUID());						
						params.put("sharedKey", getSharedKey());

						try {
							String response = WalletUtils.postURLWithParams(WebROOT + "send-via", params);
							if (response != null && response.length() > 0) {
								progress.onProgress("Send Transaction");
								Log.d("sendCoinsToFriend", "sendCoinsToFriend: send-via response: " + response);
								String response2 = pushTx(tx);						
								if (response2 != null && response2.length() > 0) {
									Log.d("sendCoinsToFriend", "sendCoinsToFriend: pushTx response: " + response2);
									progress.onSend(tx, response2);

									String label = sendType == "email" ? emailOrNumber + " Sent Via Email" : emailOrNumber + " Sent Via SMS";									
									addKey(key, toAddress, label);
									setTag(toAddress, 2);
								}
							}
						} catch (Exception e) {
							progress.onError(e.getMessage());							
							e.printStackTrace();
						}
					}

				} catch (Exception e) {
					progress.onError(e.getMessage());							
					e.printStackTrace();
				}				


			}
		}.start();

	}

	public void simpleSendCoinsAsync(final String toAddress, final BigInteger amount, final FeePolicy feePolicy, final BigInteger fee, final SendProgress progress) {
		HashMap<String, BigInteger> receivingAddresses = new HashMap<String, BigInteger>();
		receivingAddresses.put(toAddress, amount);

		final String[] from = getActiveAddresses();
		HashMap<String, BigInteger> sendingAddresses = new HashMap<String, BigInteger>();
		for(int i = 0; i < from.length; i++)
			sendingAddresses.put(from[i], null);

        sendCoinsAsync(true, sendingAddresses, receivingAddresses, feePolicy, fee, null, progress);
	}

	public void sendCoinsAsync(final String[] from, final String toAddress, final BigInteger amount, final FeePolicy feePolicy, final BigInteger fee, final String changeAddress, final SendProgress progress) {
		HashMap<String, BigInteger> receivingAddresses = new HashMap<String, BigInteger>();
		receivingAddresses.put(toAddress, amount);

		HashMap<String, BigInteger> sendingAddresses = new HashMap<String, BigInteger>();
		for(int i = 0; i < from.length; i++)
			sendingAddresses.put(from[i], null);

		sendCoinsAsync(false, sendingAddresses, receivingAddresses, feePolicy, fee, changeAddress, progress);
	}

	public void sendCoinsAsync(final HashMap<String, BigInteger> sendingAddresses, final String toAddress, final BigInteger amount, final FeePolicy feePolicy, final BigInteger fee, final String changeAddress, final SendProgress progress) {
		BigInteger sum = BigInteger.ZERO;
		for (Iterator<Entry<String, BigInteger>> iterator = sendingAddresses.entrySet().iterator(); iterator.hasNext();) {
			Entry<String, BigInteger> entry = iterator.next();
			sum = sum.add(entry.getValue());			
		}

		if (sum.compareTo(amount) != 0) {
			progress.onError("Internal error input amounts not validating correctly");
			return;
		}

		HashMap<String, BigInteger> receivingAddresses = new HashMap<String, BigInteger>();
		receivingAddresses.put(toAddress, amount);

		sendCoinsAsync(false, sendingAddresses, receivingAddresses, feePolicy, fee, changeAddress, progress);
	}

	private void sendCoinsAsync(final boolean isSimpleSend, final HashMap<String, BigInteger> sendingAddresses, final HashMap<String, BigInteger> receivingAddresses, final FeePolicy feePolicy, final BigInteger fee, final String changeAddress, final SendProgress progress) {

		new Thread() {
			@Override
			public void run() {
				progress.onStart();

				final  BigInteger feeAmount;
				if (fee == null)
					feeAmount = BigInteger.ZERO;
				else
					feeAmount = fee;

				final List<ECKey> tempKeys = new ArrayList<ECKey>();

				try {

					//Construct a new transaction
					progress.onProgress("Getting Unspent Outputs");

					List<String> from = new ArrayList<String>(sendingAddresses.keySet());
					List<MyTransactionOutPoint> allUnspent = getUnspentOutputPoints(from.toArray(new String[from.size()]));

					Pair<Transaction, Long> pair = null;

					progress.onProgress("Constructing Transaction");

					try {
						//Try without asking for watch only addresses
						List<MyTransactionOutPoint> unspent = filter(allUnspent, tempKeys, false, progress);

						if (isSimpleSend) {
							pair = makeTransaction(isSimpleSend, unspent, receivingAddresses, feeAmount, changeAddress);							
						} else {
							pair = makeTransactionCustom(sendingAddresses, unspent, receivingAddresses, feeAmount, changeAddress);
						}

						//Transaction cancelled
						if (pair == null)
							return;
					} catch (InsufficientFundsException e) {

						//Try with asking for watch only
						List<MyTransactionOutPoint> unspent = filter(allUnspent, tempKeys, true, progress);

						if (isSimpleSend) {
							pair = makeTransaction(isSimpleSend, unspent, receivingAddresses, feeAmount, changeAddress);							
						} else {
							pair = makeTransactionCustom(sendingAddresses, unspent, receivingAddresses, feeAmount, changeAddress);
						}

						//Transaction cancelled
						if (pair == null)
							return;
					}

					Transaction tx = pair.first;
					Long priority = pair.second; 

					if (isSimpleSend) {
						//If returns false user cancelled
						//Probably because they want to recreate the transaction with different fees
						if (!progress.onReady(tx, feeAmount, feePolicy, priority))
							return;
					}

					progress.onProgress("Signing Inputs");

					Wallet wallet = new Wallet(MainNetParams.get());
					for (TransactionInput input : tx.getInputs()) {
						byte[] scriptBytes = input.getOutpoint().getConnectedPubKeyScript();
						String address = new BitcoinScript(scriptBytes).getAddress().toString();
						final ECKey walletKey;
						try {
							walletKey = getECKey(address);
						} catch (Exception e) {
							// skip add Watch Only Bitcoin Address key because already accounted for  later with tempKeys
							continue;
						}
						ECKey keyCompressed;
						ECKey keyUnCompressed;
						BigInteger priv = new BigInteger(walletKey.getPrivKeyBytes());
						if (priv.compareTo(BigInteger.ZERO) >= 0) {
							keyCompressed = new ECKey(priv, null, true);
							keyUnCompressed = new ECKey(priv, null, false);
						} else {
							byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], walletKey.getPrivKeyBytes());
							BigInteger priv2 = new BigInteger(appendZeroByte);
							keyCompressed = new ECKey(priv2, null, true);			
							keyUnCompressed = new ECKey(priv2, null, false);												
						}

						if (keyCompressed != null) {
							wallet.addKey(keyCompressed);
						}

						if (keyUnCompressed != null) {
							wallet.addKey(keyUnCompressed);
						}
					}

					wallet.addKeys(tempKeys);

					//Now sign the inputs
					tx.signInputs(SigHash.ALL, wallet);

					progress.onProgress("Broadcasting Transaction");

					String response = pushTx(tx);

					progress.onSend(tx, response);

				} catch (Exception e) {
					e.printStackTrace();

					progress.onError(e.getLocalizedMessage());

				} 
			}
		}.start();
	}

	//Returns response message
	public String pushTx(Transaction tx) throws Exception {

		String hexString = new String(Hex.encode(tx.bitcoinSerialize()));

		if (hexString.length() > 16384)
			throw new Exception("Blockchain wallet's cannot handle transactions over 16kb in size. Please try splitting your transaction");

		String response = postURL(WebROOT + "pushtx", "tx="+hexString);

		return response;
	}

	public static class InsufficientFundsException extends Exception {
		private static final long serialVersionUID = 1L;

		public InsufficientFundsException(String string) {
			super(string);
		}
	}

	//You must sign the inputs
	public Pair<Transaction, Long> makeTransaction(boolean isSimpleSend, List<MyTransactionOutPoint> unspent, HashMap<String, BigInteger> receivingAddresses, BigInteger fee, final String changeAddress) throws Exception {

		long priority = 0;

		if (unspent == null || unspent.size() == 0)
			throw new InsufficientFundsException("No free outputs to spend.");

		if (fee == null)
			fee = BigInteger.ZERO;

		//Construct a new transaction
		Transaction tx = new Transaction(getParams());

		BigInteger outputValueSum = BigInteger.ZERO;

		for (Iterator<Entry<String, BigInteger>> iterator = receivingAddresses.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, BigInteger> mapEntry = iterator.next();
			String toAddress = mapEntry.getKey();
			BigInteger amount = mapEntry.getValue();

			if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0)
				throw new Exception("You must provide an amount");

			outputValueSum = outputValueSum.add(amount);
			//Add the output
			BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(toAddress));

			TransactionOutput output = new TransactionOutput(getParams(), null, amount, toOutputScript.getProgram());

			tx.addOutput(output);
		}

		//Now select the appropriate inputs
		BigInteger valueSelected = BigInteger.ZERO;
		BigInteger valueNeeded =  outputValueSum.add(fee);
		BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

		MyTransactionOutPoint changeOutPoint = null;

		for (MyTransactionOutPoint outPoint : unspent) {

			BitcoinScript script = new BitcoinScript(outPoint.getScriptBytes());

			if (script.getOutType() == BitcoinScript.ScriptOutTypeStrange)
				continue;

			BitcoinScript inputScript = new BitcoinScript(outPoint.getConnectedPubKeyScript());
			String address = inputScript.getAddress().toString();

			//if isSimpleSend don't use address as input if is output 
			if (isSimpleSend && receivingAddresses.get(address) != null)
				continue;

			MyTransactionInput input = new MyTransactionInput(getParams(), null, new byte[0], outPoint);

			input.outpoint = outPoint;

			tx.addInput(input);

			valueSelected = valueSelected.add(outPoint.value);

			priority += outPoint.value.longValue() * outPoint.confirmations;

			if (changeAddress == null)
				changeOutPoint = outPoint;

			if (valueSelected.compareTo(valueNeeded) == 0 || valueSelected.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0)
				break;
		}

		//Check the amount we have selected is greater than the amount we need
		if (valueSelected.compareTo(valueNeeded) < 0) {
			throw new InsufficientFundsException("Insufficient Funds");
		}

		BigInteger change = valueSelected.subtract(outputValueSum).subtract(fee);

		//Now add the change if there is any
		if (change.compareTo(BigInteger.ZERO) > 0) {
			BitcoinScript change_script;
			if (changeAddress != null) {
				change_script = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(changeAddress));
			} else if (changeOutPoint != null) {
				BitcoinScript inputScript = new BitcoinScript(changeOutPoint.getConnectedPubKeyScript());

				//Return change to the first address
				change_script = BitcoinScript.createSimpleOutBitoinScript(inputScript.getAddress());
			} else {
				throw new Exception("Invalid transaction attempt");
			}
			TransactionOutput change_output = new TransactionOutput(getParams(), null, change, change_script.getProgram());

			tx.addOutput(change_output);				
		}

		long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());

		priority /= estimatedSize;

		return new Pair<Transaction, Long>(tx, priority);
	}

	//You must sign the inputs
	public Pair<Transaction, Long> makeTransactionCustom(final HashMap<String, BigInteger> sendingAddresses, List<MyTransactionOutPoint> unspent, HashMap<String, BigInteger> receivingAddresses, BigInteger fee, final String changeAddress) throws Exception {

		long priority = 0;

		if (unspent == null || unspent.size() == 0)
			throw new InsufficientFundsException("No free outputs to spend.");

		if (fee == null)
			fee = BigInteger.ZERO;

		//Construct a new transaction
		Transaction tx = new Transaction(getParams());

		BigInteger outputValueSum = BigInteger.ZERO;

		for (Iterator<Entry<String, BigInteger>> iterator = receivingAddresses.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, BigInteger> mapEntry = iterator.next();
			String toAddress = mapEntry.getKey();
			BigInteger amount = mapEntry.getValue();

			if (amount == null || amount.compareTo(BigInteger.ZERO) <= 0)
				throw new Exception("You must provide an amount");

			outputValueSum = outputValueSum.add(amount);
			//Add the output
			BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(toAddress));
			Log.d("MyRemoteWallet", "MyRemoteWallet makeTransactionCustom toAddress: " + toAddress + "amount: " + amount);

			TransactionOutput output = new TransactionOutput(getParams(), null, amount, toOutputScript.getProgram());
			tx.addOutput(output);
		}

		//Now select the appropriate inputs
		BigInteger valueSelected = BigInteger.ZERO;
		BigInteger valueNeeded =  outputValueSum.add(fee);

		Map<String, BigInteger> addressTotalUnspentValues = new HashMap<String, BigInteger>();

		for (MyTransactionOutPoint outPoint : unspent) {

			BitcoinScript script = new BitcoinScript(outPoint.getScriptBytes());

			if (script.getOutType() == BitcoinScript.ScriptOutTypeStrange)
				continue;

			BitcoinScript inputScript = new BitcoinScript(outPoint.getConnectedPubKeyScript());
			String address = inputScript.getAddress().toString();

			BigInteger addressSendAmount = sendingAddresses.get(address);
			if (addressSendAmount == null) {
				throw new Exception("Invalid transaction address send amount is null");
			}

			final BigInteger addressTotalUnspentValue = addressTotalUnspentValues.get(address);

			if (addressTotalUnspentValue == null) {
				addressTotalUnspentValues.put(address, outPoint.value);
			} else {
				addressTotalUnspentValues.put(address, addressTotalUnspentValue.add(outPoint.value));
			}

			MyTransactionInput input = new MyTransactionInput(getParams(), null, new byte[0], outPoint);

			input.outpoint = outPoint;
			Log.d("MyRemoteWallet", "MyRemoteWallet makeTransactionCustom fromAddress: " + address + "amount: " + outPoint.value);

			tx.addInput(input);

			valueSelected = valueSelected.add(outPoint.value);

			priority += outPoint.value.longValue() * outPoint.confirmations;

			//if (valueSelected.compareTo(valueNeeded) == 0 || valueSelected.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0)
			//	break;
		}

		//Check the amount we have selected is greater than the amount we need
		if (valueSelected.compareTo(valueNeeded) < 0) {
			throw new InsufficientFundsException("Insufficient Funds");
		}

		//decide change
		if (changeAddress == null) {
			BigInteger feeAmountLeftToAccountedFor = fee;

	        for (Iterator<Entry<String, BigInteger>> iterator = addressTotalUnspentValues.entrySet().iterator(); iterator.hasNext();) {
	        	final Entry<String, BigInteger> entry = iterator.next();
	        	final String address = entry.getKey();
	        	final BigInteger addressTotalUnspentValue = entry.getValue();
	        	final BigInteger addressSendAmount = sendingAddresses.get(address);
	        	BigInteger addressChangeAmount = addressTotalUnspentValue.subtract(addressSendAmount);

	        	if (feeAmountLeftToAccountedFor.compareTo(BigInteger.ZERO) > 0) {

	        		if (addressChangeAmount.compareTo(feeAmountLeftToAccountedFor) >= 0) {
		        		//have enough to fill fee
	        			addressChangeAmount = addressChangeAmount.subtract(feeAmountLeftToAccountedFor);
	        			feeAmountLeftToAccountedFor = BigInteger.ZERO;
	        		} else {
		        		// do not have enough to fill fee
	        			feeAmountLeftToAccountedFor = feeAmountLeftToAccountedFor.subtract(addressChangeAmount);
	        			addressChangeAmount = BigInteger.ZERO;
	        		}
	        	}

	        	if (addressChangeAmount.compareTo(BigInteger.ZERO) > 0) {
	    			//Add the output
	    			BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(address));
	    			Log.d("MyRemoteWallet", "MyRemoteWallet makeTransactionCustom changeAddress == null: " + address + "addressChangeAmount: " + addressChangeAmount);

	    			TransactionOutput output = new TransactionOutput(getParams(), null, addressChangeAmount, toOutputScript.getProgram());

	    			tx.addOutput(output);        		
	        	}
	        }
		} else {
			BigInteger addressChangeAmountSum = BigInteger.ZERO;
			for (Iterator<Entry<String, BigInteger>> iterator = addressTotalUnspentValues.entrySet().iterator(); iterator.hasNext();) {
				final Entry<String, BigInteger> entry = iterator.next();
	        	final String address = entry.getKey();
	        	final BigInteger addressTotalUnspentValue = entry.getValue();
	        	final BigInteger addressSendAmount = sendingAddresses.get(address);
	        	final BigInteger addressChangeAmount = addressTotalUnspentValue.subtract(addressSendAmount);
	        	addressChangeAmountSum = addressChangeAmountSum.add(addressChangeAmount);
	        }

        	if (addressChangeAmountSum.compareTo(BigInteger.ZERO) > 0) {
    			//Add the output
    			BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(changeAddress));

    			TransactionOutput output = new TransactionOutput(getParams(), null, addressChangeAmountSum.subtract(fee), toOutputScript.getProgram());
    			Log.d("MyRemoteWallet", "MyRemoteWallet makeTransactionCustom changeAddress != null: " + changeAddress + "addressChangeAmount: " + output.getValue());
    			tx.addOutput(output);        		
        	}
		}

		long estimatedSize = tx.bitcoinSerialize().length + (114 * tx.getInputs().size());

		priority /= estimatedSize;

		return new Pair<Transaction, Long>(tx, priority);
	}

	public static List<MyTransactionOutPoint> getUnspentOutputPoints(String[] from) throws Exception {

		StringBuffer buffer =  new StringBuffer(WebROOT + "unspent?active=");

		int ii = 0;
		for (String address : from) {
			buffer.append(address);

			if (ii < from.length-1)
				buffer.append("|");

			++ii;
		}

		List<MyTransactionOutPoint> outputs = new ArrayList<MyTransactionOutPoint>();

		String response = fetchURL(buffer.toString());

		Map<String, Object> root = (Map<String, Object>) JSONValue.parse(response);

		List<Map<String, Object>> outputsRoot = (List<Map<String, Object>>) root.get("unspent_outputs");

		for (Map<String, Object> outDict : outputsRoot) {

			byte[] hashBytes = Hex.decode((String)outDict.get("tx_hash"));

			ArrayUtils.reverse(hashBytes);

			Sha256Hash txHash = new Sha256Hash(hashBytes);

			int txOutputN = ((Number)outDict.get("tx_output_n")).intValue();
			BigInteger value = BigInteger.valueOf(((Number)outDict.get("value")).longValue());
			byte[] scriptBytes = Hex.decode((String)outDict.get("script"));
			int confirmations = ((Number)outDict.get("confirmations")).intValue();

			//Contrstuct the output
			MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes);

			outPoint.setConfirmations(confirmations);

			outputs.add(outPoint);
		}

		return outputs;
	}

	/**
	 * Register this account/device pair within the server.
	 * @throws Exception 
	 *
	 */
	public boolean registerNotifications(final String regId) throws Exception {
		if (_isNew) return false;

		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&method=register-android-device");
		args.append("&payload="+URLEncoder.encode(regId));
		args.append("&length="+regId.length());

		String response = postURL(WebROOT + "wallet", args.toString());

		return response != null && response.length() > 0;
	}

	/** k
	 * Unregister this account/device pair within the server.
	 * @throws Exception 
	 */
	public boolean unregisterNotifications(final String regId) throws Exception {    
		if (_isNew) return false;

		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&method=unregister-android-device");
		args.append("&payload="+URLEncoder.encode(regId));
		args.append("&length="+regId.length());

		String response = postURL(WebROOT + "wallet", args.toString());

		return response != null && response.length() > 0;
	}

	public boolean getIsEmailNotificationEnabled() {    
		return notificationsTypeSet.contains(NotificationsTypeEmail);
	}

	public boolean getIsSMSNotificationEnabled() {    
		return notificationsTypeSet.contains(NotificationsTypeSMS);
	}

	public String getSmsNumber() {
		return smsNumber;
	}

	public void setSmsNumber(String smsNumber) {
		this.smsNumber = smsNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void getAccountInformation() throws Exception {    
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("method", "get-info");
		params.put("format", "json");

		String response = securePost(WebROOT + "wallet", params);
		JSONObject obj  = (JSONObject) new JSONParser().parse(response);
		setEmail((String)obj.get("email"));
		setSmsNumber((String)obj.get("sms_number"));

		List<Long> notificationsType = (List<Long>) obj.get("notifications_type"); 
		notificationsTypeSet = new HashSet<String>();

		for (Long value : notificationsType)
			notificationsTypeSet.add(value.toString());
	}

	public String updateEmail(String email) throws Exception {    	
		if (email == null) {
			throw new Exception("Email cannot be null");
		}

		Map<Object, Object> params = new HashMap<Object, Object>();
		String length =  Integer.toString(email.length());

		params.put("length", length);
		params.put("payload", email);
		params.put("method", "update-email");

		String response = securePost(WebROOT + "wallet", params);
		return response;
	}

	public String updateSMS(String smsNumber) throws Exception {    	
		if (smsNumber == null) {
			throw new Exception("smsNumber cannot be null");
		}

		Map<Object, Object> params = new HashMap<Object, Object>();
		String length =  Integer.toString(smsNumber.length());

		params.put("length", length);
		params.put("payload", smsNumber);
		params.put("method", "update-sms");

		String response = securePost(WebROOT + "wallet", params);
		return response;
	}

	public String enableEmailNotification(boolean enable) throws Exception {    		
		if (enable)
			notificationsTypeSet.add(NotificationsTypeEmail);
		else
			notificationsTypeSet.remove(NotificationsTypeEmail);

		List<String> list = new ArrayList<String>(notificationsTypeSet);

		return updateNotificationsType(list.toArray(new String[list.size()]));
	}

	public String enableSMSNotification(boolean enable) throws Exception {
		if (enable)
			notificationsTypeSet.add(NotificationsTypeSMS);
		else
			notificationsTypeSet.remove(NotificationsTypeSMS);

		List<String> list = new ArrayList<String>(notificationsTypeSet);

		return updateNotificationsType(list.toArray(new String[list.size()]));
	}

	public boolean isEnableEmailNotification() {    		
		return notificationsTypeSet.contains(NotificationsTypeEmail);
	}

	public boolean isEnableSMSNotification() {    		
		return notificationsTypeSet.contains(NotificationsTypeSMS);
	}

	public String updateNotificationsType(boolean enableEmailNotification, boolean enableSMSNotification) throws Exception {    		
		if (enableSMSNotification)
			notificationsTypeSet.add(NotificationsTypeSMS);
		else
			notificationsTypeSet.remove(NotificationsTypeSMS);

		if (enableEmailNotification)
			notificationsTypeSet.add(NotificationsTypeEmail);
		else
			notificationsTypeSet.remove(NotificationsTypeEmail);

		List<String> list = new ArrayList<String>(notificationsTypeSet);
		return updateNotificationsType(list.toArray(new String[list.size()]));
	}

	private String updateNotificationsType(String[] values) throws Exception {    		

		String payload = StringUtils.join(values, "|");
		String length =  Integer.toString(payload.length());

		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("length", length);
		params.put("payload", payload);
		params.put("method", "update-notifications-type");

		String response = securePost(WebROOT + "wallet", params);
		return response;
	}

	public String securePost(String url, Map<Object, Object> data) throws Exception {  
		Map<Object, Object> params = new HashMap<Object, Object>(data);

		if (! data.containsKey("sharedKey")) {
			serverTimeOffset = 500; //TODO dont hard code serverTimeOffset

			String sharedKey = getSharedKey().toLowerCase();
			long now = new Date().getTime();

			long timestamp = (now - serverTimeOffset) / 10000;

			String text = sharedKey + Long.toString(timestamp);
			String SKHashHex = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(text.getBytes("UTF-8"))));
			int i = 0;
			String tSKUID = SKHashHex.substring(i, i+=8)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=4)+"-"+SKHashHex.substring(i, i+=12);

			params.put("sharedKey", tSKUID);
			params.put("sKTimestamp", Long.toString(timestamp));
			params.put("sKDebugHexHash", SKHashHex);
			params.put("sKDebugTimeOffset", Long.toString(serverTimeOffset));
			params.put("sKDebugOriginalClientTime", Long.toString(now));
			params.put("sKDebugOriginalSharedKey", sharedKey);

			if (! params.containsKey("guid"))
				params.put("guid", this.getGUID());

			if (! params.containsKey("format"))
				params.put("format", "plain");	
		}

		String response = WalletUtils.postURLWithParams(url, params);
		return response;
	}

	public boolean updateRemoteLocalCurrency(String currency_code) throws Exception {    
		if (_isNew) return false;

		localCurrencyCode = currency_code;

		StringBuilder args = new StringBuilder();

		args.append("guid=" + getGUID());
		args.append("&sharedKey=" + getSharedKey());
		args.append("&payload=" + currency_code);
		args.append("&length=" + currency_code.length());
		args.append("&method=update-currency");;

		String response = postURL(WebROOT + "wallet", args.toString());

		return response != null;
	}

	/**
	 * Get the tempoary paring encryption password
	 * @throws Exception 
	 *
	 */
	public static String getPairingEncryptionPassword(final String guid) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("guid=" + guid);
		args.append("&method=pairing-encryption-password");

		return postURL(WebROOT + "wallet", args.toString());
	}

	public static BigInteger getAddressBalance(final String address) throws Exception {
		return new BigInteger(fetchURL(WebROOT + "q/addressbalance/"+address));
	}

	public static String getWalletManualPairing(final String guid) throws Exception {
		StringBuilder args = new StringBuilder();

		args.append("guid=" + guid);
		args.append("&method=pairing-encryption-password");

		String response = fetchURL(WebROOT + "wallet/" + guid + "?format=json&resend_code=false");

		JSONObject object = (JSONObject) new JSONParser().parse(response);

		String payload = (String) object.get("payload");
		if (payload == null || payload.length() == 0) {
			throw new Exception("Error Fetching Wallet Payload");
		}

		return payload;
	}

	public synchronized boolean remoteSave() throws Exception {
		return remoteSave(null);
	}

	public synchronized boolean remoteSave(String email) throws Exception {

		String payload = this.getPayload();

		String old_checksum = this._checksum;
		this._checksum  = new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(payload.getBytes("UTF-8"))));

		String method = _isNew ? "insert" : "update";

		String urlEncodedPayload = URLEncoder.encode(payload);

		StringBuilder args = new StringBuilder();
		args.append("guid=");
		args.append(URLEncoder.encode(this.getGUID(), "utf-8"));
		args.append("&sharedKey=");
		args.append(URLEncoder.encode(this.getSharedKey(), "utf-8"));
		args.append("&payload=");
		args.append(urlEncodedPayload);
		args.append("&method=");
		args.append(method);
		args.append("&length=");
		args.append(payload.length());
		args.append("&checksum=");
		args.append(URLEncoder.encode(_checksum, "utf-8"));

		if (sync_pubkeys) {
			args.append("&active=");
			args.append(StringUtils.join(getActiveAddresses(), "|"));
		}

		if (email != null && email.length() > 0) {
			args.append("&email=");
			args.append(URLEncoder.encode(email, "utf-8"));
		}

		args.append("&device=");
		args.append("android");

		if (old_checksum != null && old_checksum.length() > 0)
		{
			args.append("&old_checksum=");
			args.append(old_checksum);
		}

		postURL(WebROOT + "wallet", args.toString());

		_isNew = false;

		return true;
	}

	public void remoteDownload() {

	}

	public String getChecksum() {
		return _checksum;
	}


	public synchronized String setPayload(JSONObject walletJSONObj) throws Exception {
		handleWalletPayloadObj(walletJSONObj);

		return setPayload(walletJSONObj.get("payload").toString());
	}

	public synchronized String setPayload(String payload) throws Exception {
		MyRemoteWallet tempWallet = new MyRemoteWallet(payload, temporyPassword);

		this.root = tempWallet.root;
		this.rootContainer = tempWallet.rootContainer;

		if (this.temporySecondPassword != null && !this.validateSecondPassword(temporySecondPassword)) {
			this.temporySecondPassword = null;
		}

		this._checksum = tempWallet._checksum;

		_isNew = false;

		return payload;
	}

	public static class NotModfiedException extends Exception {
		private static final long serialVersionUID = 1L;
	}


	public void handleWalletPayloadObj(JSONObject obj) {
		Map<String, Object> symbol_local = (Map<String, Object>) obj.get("symbol_local");

		boolean didUpdateCurrency = false;

		if (symbol_local != null && symbol_local.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.localCurrencyCode == null || !this.localCurrencyCode.equals(currencyCode) || this.localCurrencyConversion != currencyConversion) {
				this.localCurrencyCode = currencyCode;
				this.localCurrencyConversion = currencyConversion;
				didUpdateCurrency = true;
			}
		}


		Map<String, Object> symbol_btc = (Map<String, Object>) obj.get("symbol_btc");
		if (symbol_btc != null && symbol_btc.containsKey("code")) {
			String currencyCode = (String) symbol_local.get("code");
			Double currencyConversion = (Double) symbol_local.get("conversion");

			if (currencyConversion == null)
				currencyConversion = 0d;

			if (this.btcCurrencyCode == null || !this.btcCurrencyCode.equals(currencyCode) || this.btcCurrencyConversion != currencyConversion) {
				this.btcCurrencyCode = currencyCode;
				this.btcCurrencyConversion = currencyConversion;
				//didUpdateCurrency = true;
			}
		}

		if (didUpdateCurrency) {
			EventListeners.invokeCurrencyDidChange();
		}

		if (obj.containsKey("sync_pubkeys")) {
			sync_pubkeys = Boolean.valueOf(obj.get("sync_pubkeys").toString());
		}
	}

	public static JSONObject getWalletPayload(String guid, String sharedKey, String checkSumString) throws Exception {
		String response = postURL(WebROOT + "wallet", "method=wallet.aes.json&guid="+guid+"&sharedKey="+sharedKey+"&checksum="+checkSumString+"&format=json");

		if (response == null) {
			throw new Exception("Error downloading wallet");
		}

		JSONObject obj = (JSONObject) new JSONParser().parse(response);

		String payload = obj.get("payload").toString();

		if (payload == null) {
			throw new Exception("Error downloading wallet");
		}

		if (payload.equals("Not modified")) {
			throw new NotModfiedException();
		}

		return obj;
	}

	public static JSONObject getWalletPayload(String guid, String sharedKey) throws Exception {
		String response = postURL(WebROOT + "wallet","method=wallet.aes.json&guid="+guid+"&sharedKey="+sharedKey+"&format=json");

		if (response == null) {
			throw new Exception("Error downloading wallet");
		}

		JSONObject obj = (JSONObject) new JSONParser().parse(response);

		String payload = obj.get("payload").toString();

		if (payload == null) {
			throw new Exception("Error downloading wallet");
		}

		return obj;
	}

	public List<Pair<String, String>> getLabelList() {
		List<Pair<String, String>> array = new ArrayList<Pair<String, String>>();

		Map<String, String> labelMap = this.getLabelMap();

		synchronized(labelMap) {
			for (Map.Entry<String, String> entry : labelMap.entrySet()) {
				array.add(new Pair<String, String>(entry.getValue(), entry.getKey()) {
					public String toString() {
						return first.toString();
					}
				});
			}
		}

		return array;
	}

	public String getToAddress(String inputAddress) {
		final String userEntered = inputAddress;
		if (userEntered.length() > 0) {
			try {
				new Address(Constants.NETWORK_PARAMETERS, userEntered);

				return userEntered;
			} catch (AddressFormatException e) {
				List<Pair<String, String>> labels = this.getLabelList();

				for (Pair<String, String> label : labels) {
					if (label.first.toLowerCase(Locale.ENGLISH).equals(userEntered.toLowerCase(Locale.ENGLISH))) {
						try {
							new Address(Constants.NETWORK_PARAMETERS, label.second);

							return label.second;
						} catch (AddressFormatException e1) {}
					}
				}
			}
		}

		return null;
	}
}
