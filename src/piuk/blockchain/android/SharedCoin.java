package piuk.blockchain.android;

import info.blockchain.wallet.ui.ObjectSuccessCallback;
import info.blockchain.wallet.ui.SharedCoinSuccessCallback;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.spongycastle.util.encoders.Hex;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Transaction.SigHash;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;
import piuk.blockchain.android.WalletApplication.AddAddressCallback;
import piuk.blockchain.android.util.WalletUtils;

public class SharedCoin {
	//private static final boolean SHARED_COIN_DEBUG = true;
	private static final boolean SHARED_COIN_DEBUG = false;	

	static private Long getLongFromLong(JSONObject obj, String key) {
		return (Long) obj.get(key);
	}
	
	static private BigInteger getBigIntegerFromLong(JSONObject obj, String key) {
		return BigInteger.valueOf((Long) obj.get(key));
	}
	
	static private String getIntegerStringFromIntegerString(JSONObject obj, String key) {
		return (String) obj.get(key);
	}
	
	static private BigInteger getIntegerFromIntegerString(JSONObject obj, String key) {
		return new BigInteger((String) obj.get(key));
	}
	
	private static void setTimeout(long milliseconds) {
		try {
			Log.d("SharedCoin", "SharedCoin etTimeout b4 " + milliseconds);
			Thread.sleep(milliseconds);
			Log.d("SharedCoin", "SharedCoin etTimeout ad " + milliseconds);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static byte[] signInput(NetworkParameters params, Transaction tx, int tx_input_index, String base58PrivKey, BitcoinScript script, SigHash sigHash) {
    	Log.d("SharedCoin", "SharedCoin signInput tx.getInputs().size " + tx.getInputs().size());	
    	Log.d("SharedCoin", "SharedCoin signInput tx_input_index " + tx_input_index);	
    	Log.d("SharedCoin", "SharedCoin signInput base58PrivKey " + base58PrivKey);	
        	
		try {
			ECKey key = new ECKey(Base58.decode(base58PrivKey), null);
	    	Log.d("SharedCoin", "SharedCoin signInput key.toAddress " + key.toAddress(params).toString());	
			
			TransactionSignature transactionSignature = tx.calculateSignature(tx_input_index, key, null, script.getProgram(), SigHash.ALL, false);
			
			byte[] signedScript = Script.createInputScript(transactionSignature.encodeToBitcoin(), key.getPubKey());
			//ArrayUtils.reverse(signedScript);

			String signedScriptHex = new String(Hex.encode(signedScript));
	    	Log.d("SharedCoin", "SharedCoin signInput signedScriptHex " + signedScriptHex);		
	    	Log.d("SharedCoin", "SharedCoin signInput script.program hex " + new String(Hex.encode(script.getProgram())));		

			return signedScript;
		} catch (Exception e) {
	    	Log.d("SharedCoin", "SharedCoin signInput e " + e.getLocalizedMessage());		
			e.printStackTrace();
		}
		
		return null;
	}
	
	private final static String STATUS_WAITING = "waiting";
	private final static String STATUS_NOT_FOUND = "not_found";
	private final static String STATUS_ACTIVE_PROPOSAL = "active_proposal";
	private final static String STATUS_COMPLETE = "complete";
	private final static String STATUS_SIGNATURES_NEEDED = "signatures_needed";	
	private final static String STATUS_VERIFICATION_FAILED = "verification_failed";
	private final static String STATUS_SIGNATURES_ACCEPTED = "signatures_accepted";
	
	//private static String SHARED_COIN_ENDPOINT = "https://api.sharedcoin.com?";
	private static String SHARED_COIN_ENDPOINT = "https://api.sharedcoin.com/";
	public static final int VERSION = 3;
	private static final String SEED_PREFIX = "sharedcoin-seed:";
	private static final long MIN_TIME_BETWEEN_SUBMITS = 120000;	
	private static final int SATOSHI = 100000000;

	private static SharedCoin instance = null;
	
	private Map<String, String> extra_private_keys; //{address : base58privkey}
	private NetworkParameters params = null;
	private long lastSignatureSubmitTime = 0;
	private JSONObject info = null;
	private MyRemoteWallet remoteWallet;
	private WalletApplication application;
	
	public SharedCoin(WalletApplication application, MyRemoteWallet remoteWallet) {
		this.extra_private_keys = new HashMap<String,String>();
		this.remoteWallet = remoteWallet;
		this.application = application;
		this.params = MyRemoteWallet.getParams();
	}
	
	public static SharedCoin getInstance(WalletApplication application, MyRemoteWallet remoteWallet) {				
		if(instance == null) {
			instance = new SharedCoin(application, remoteWallet);
		}
		
		return instance;
	}

	private long getLastSignatureSubmitTime() {
		return this.lastSignatureSubmitTime;
	}
	
	private void setLastSignatureSubmitTime(long lastSignatureSubmitTime) {
		this.lastSignatureSubmitTime = lastSignatureSubmitTime;
	}
	
	private JSONObject _pollForCompleted(final BigInteger proposalID) {
		return SharedCoin.pollForProposalCompleted(proposalID);
	}
	
	private void pollForCompleted(final BigInteger proposalID, final SharedCoinSuccessCallback sharedCoinSuccessCallback) {		
		while (true) {
	    	Log.d("SharedCoin", "SharedCoin pollForCompleted");		
			JSONObject obj = this._pollForCompleted(proposalID);
			if (obj == null) {
		    	Log.d("SharedCoin", "SharedCoin pollForCompleted _pollForCompleted error");		
				break;
			}
				
			String status = (String) obj.get("status");
	    	Log.d("SharedCoin", "SharedCoin pollForCompleted getOfferID status " + status);		
	    	if (status.equals(STATUS_WAITING)) {			    		
		    	Log.d("SharedCoin", "SharedCoin pollForCompleted waiting continue");		
	    		continue;
			} else if (status.equals(STATUS_NOT_FOUND)) {
				sharedCoinSuccessCallback.onFail("Proposal ID Not Found");					
				break;
			} else if (status.equals(STATUS_COMPLETE)) {
				String txHash = (String) obj.get("tx_hash");
				String tx = (String) obj.get("tx");				
				sharedCoinSuccessCallback.onComplete(txHash, tx);
				break;
			} else {
				sharedCoinSuccessCallback.onFail("Unknown status " + status);
				break;
			}		    	
		}					
	}
	
	private String generateAddressFromCustomSeed(String seed, int n) {
	    Log.d("SharedCoin", "SharedCoin generateAddressFromCustomSeed -------------------------------");
	    Log.d("SharedCoin", "SharedCoin generateAddressFromCustomSeed seed: " + seed);
	    Log.d("SharedCoin", "SharedCoin generateAddressFromCustomSeed n: " + n);
		
	    String seedn = seed + Integer.toString(n);
		
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(seedn.getBytes());
			
			ECKey key;
			BigInteger num = new BigInteger(hash);
			if (num.compareTo(BigInteger.ZERO) >= 0) {
				// condition is needed to match sharedCoin.js implementation
		        if (hash[0] % 2 == 0) {	        	
		        	key = new ECKey(num, null, false);
		        } else {
		        	// keep in mind private key will not match in js version, but address will match
		        	// if compressed parameter was false instead private key will match in js version, but address will not match
		        	key = new ECKey(num, null, true);
		        }
			} else {
				Log.d("SharedCoin", "SharedCoin generateAddressFromCustomSeed: appendZeroByte");
				// Prepend a zero byte to make the BigInteger positive
				byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], hash);			
				// condition is needed to match sharedCoin.js implementation
		        if (hash[0] % 2 == 0) {		        	
		        	key = new ECKey(new BigInteger(appendZeroByte), null, false);				
		        } else {
		        	// keep in mind private key will not match in js version, but address will match
		        	// if compressed parameter was false instead private key will match in js version, but address will not match
		        	key = new ECKey(new BigInteger(appendZeroByte), null, true);				
		        }
			}
									
			String address = key.toAddress(this.params).toString();
			final DumpedPrivateKey dumpedPrivateKey = key.getPrivateKeyEncoded(params);
			String privateKey = Base58.encode(dumpedPrivateKey.bytes);

			Log.d("SharedCoin", "SharedCoin generateAddressFromCustomSeed: privateKey " + privateKey);
			Log.d("SharedCoin", "SharedCoin generateAddressFromCustomSeed: address " + address);

			this.extra_private_keys.put(address, privateKey);
			return address;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void recover(final SuccessCallback successCallback, int key, final List<String> addresses) {
		Log.d("SharedCoin", "SharedCoin recover ------------------ ");
		Log.d("SharedCoin", "SharedCoin recover addresses " + addresses.toString());
		Log.d("SharedCoin", "SharedCoin recover key " + key);
		
		this.application.getBalances(addresses.toArray(new String[addresses.size()]), false, new ObjectSuccessCallback() {
			@Override
			public void onSuccess(Object obj) {
				long total_balance = 0;
				JSONObject results = (JSONObject) obj;
				Log.d("SharedCoin", "SharedCoin recover getBalances results " + results.toString());
				for (final String address : addresses) {
			        JSONObject addressDict = (JSONObject) results.get(address);
					long balance = SharedCoin.getLongFromLong(addressDict, "final_balance");
					Log.d("SharedCoin", "SharedCoin recover address: " + address + " balance: " + balance);
                    if (balance > 0) {
						Log.d("SharedCoin", "SharedCoin recover extra_private_keys.get(address): " + extra_private_keys.get(address));
						try {
							byte[] privateKeyBytes = Base58.decode(extra_private_keys.get(address));
					        ECKey ecKey = new ECKey(privateKeyBytes, null);
							Log.d("SharedCoin", "SharedCoin recover ecKey toAddress: " + ecKey.toAddress(SharedCoin.this.params).toString());

							application.addKeyToWallet(ecKey, address, null, 0, new AddAddressCallback(){
								@Override
								public void onSavedAddress(
										String address) {
	    							Log.d("SharedCoin", "SharedCoin Imported: " + address);
								}

								@Override
								public void onError(String reason) {
	    							Log.d("SharedCoin", "SharedCoin Error importing: " + address + " " + reason);
								}
							});
							

						} catch (AddressFormatException e) {
							Log.d("SharedCoin", "SharedCoin recover AddressFormatException: " + e.getLocalizedMessage());
							e.printStackTrace();
						}

                    	total_balance += balance;
                    }

					Log.d("SharedCoin", "SharedCoin recover " + total_balance + " recovered from intermediate addresses");

					
                    if (total_balance > 0) {
            			application.saveWallet(new SuccessCallback() {
            				@Override
            				public void onSuccess() {
    							Log.d("SharedCoin", "SharedCoin recover saveWallet: onSuccess");
            					EventListeners.invokeWalletDidChange();
            				}

            				@Override
            				public void onFail() {
    							Log.d("SharedCoin", "SharedCoin recover saveWallet: onFail");
            				}
            			});
                    }
			    }					
				successCallback.onSuccess();
			}
			
			
			@Override
			public void onFail(String error) {
				Log.d("SharedCoin", "SharedCoin recover getBalances " + error);
				successCallback.onFail();						
			}
    	});            	
	}
	
	private void doNext(final List<String> shared_coin_seeds, final SuccessCallback successCallback, int key, List<String> addresses) {
		Log.d("SharedCoin", "SharedCoin doNext ------------");
		String seed = shared_coin_seeds.get(key);
		++key;
		final int keyTmp = key;
		
		//for (int i = 0; i < 1; ++i) { //debug
        for (int i = 0; i < 100; ++i) {
            String address = generateAddressFromCustomSeed(seed, i);
            addresses.add(address);
        }	
        
        final List<String> addressesTmp = addresses;
        if (key == shared_coin_seeds.size()) {
			Log.d("SharedCoin", "SharedCoin doNext key == shared_coin_seeds.size() ");
            while(addresses.size() > 0) {
				Log.d("SharedCoin", "SharedCoin doNext addresses1 " + addresses.toString());
				Log.d("SharedCoin", "SharedCoin doNext addresses.size1 " + addresses.size());
            	recover(successCallback, key, addresses);
            	
//            	addresses = addresses.subList(1000, addresses.size()); 
            	addresses = new ArrayList<String>();

            	Log.d("SharedCoin", "SharedCoin doNext addresses2 " + addresses.toString());
				Log.d("SharedCoin", "SharedCoin doNext addresses.size2 " + addresses.size());
            }
        } else {
			Log.d("SharedCoin", "SharedCoin doNext key != shared_coin_seeds.size() ");
			
    		SharedCoin.setTimeout(100);
    		doNext(shared_coin_seeds, successCallback, keyTmp, addressesTmp);
        }
	}
	
	public void recoverSeeds(final List<String> shared_coin_seeds, final SuccessCallback successCallback) {
		Log.d("SharedCoin", "SharedCoin recoverSeeds ------------------");
		int key = 0;
		List<String> addresses = new ArrayList<String>();
		SharedCoin.setTimeout(100);
		doNext(shared_coin_seeds, successCallback, key, addresses);
	}
	
	private void error(final String errorMsg, Plan plan, final ObjectSuccessCallback objectSuccessCallback) {
		List<String> shared_coin_seeds = new ArrayList<String>();
		shared_coin_seeds.add(SharedCoin.SEED_PREFIX+plan.address_seed);        

		Log.d("SharedCoin", "SharedCoin misc-error " + errorMsg);
		Log.d("SharedCoin", "SharedCoin Recover Seed");

		SharedCoin.setTimeout(2000);		
		if (plan != null && plan.c_stage >= 0) {
			recoverSeeds(shared_coin_seeds,	new SuccessCallback() {
				@Override
				public void onSuccess() {
				    Log.d("SharedCoin", "SharedCoin Recover Success");
				    objectSuccessCallback.onFail("Error With SharedCoin, but SharedCoin Recover Success");
				}

				@Override
				public void onFail() {
				    Log.d("SharedCoin", "SharedCoin Recover Error");
				    objectSuccessCallback.onFail("SharedCoin Recover Error");
				}
			});
	    }
	}
	
	public void sendSharedCoin(final int repetitions, final List<String> fromAddresses, final BigInteger amount,
			final String toAddress, final ObjectSuccessCallback objectSuccessCallback) throws Exception {
        if (repetitions <= 0) {
			throw new Exception("invalid number of repetitions");
        }
        
		if (amount == null || amount.intValue() <= 0) {
        	throw new Exception("You must enter a value greater than zero");
        }
		List<HashMap<String,String>> toAddresses = new ArrayList<HashMap<String,String>>();
        
        HashMap<String,String> map = new HashMap<String,String>();
        map.put("value", amount.toString());
        map.put("address", toAddress);
        toAddresses.add(map);
        
        
        long timeSinceLastSubmit = new Date().getTime() - getLastSignatureSubmitTime();
        long interval = Math.max(0, MIN_TIME_BETWEEN_SUBMITS - timeSinceLastSubmit);

        Log.d("SharedCoin", "SharedCoin constructPlan timeSinceLastSubmit " + timeSinceLastSubmit);
	    Log.d("SharedCoin", "SharedCoin constructPlan getLastSignatureSubmitTime() " + getLastSignatureSubmitTime());
	    Log.d("SharedCoin", "SharedCoin constructPlan interval " + interval);
		
	    SharedCoin.setTimeout(interval);		
        constructPlan(repetitions, fromAddresses, toAddresses, new ObjectSuccessCallback() {

			@Override
			public void onSuccess(Object obj) {
			    Log.d("SharedCoin", "SharedCoin Created Plan");
	            
			    final Plan plan = (Plan) obj;
				plan.execute(new ObjectSuccessCallback() {

					@Override
					public void onSuccess(Object obj) {

						final Plan plan = (Plan) obj;
						plan.execute(new ObjectSuccessCallback() {
							
							@Override
							public void onSuccess(Object obj) {
	                            //MyWallet.makeNotice('success', 'misc-success', 'Sharedcoin Transaction Successfully Completed');

								//Toast 
							    Log.d("SharedCoin", "Sharedcoin Transaction Successfully Completed");
							    objectSuccessCallback.onSuccess("Transaction Successfully Completed");
							}

							@Override
							public void onFail(String error) {
							    Log.d("SharedCoin", "SharedCoin plan.execute: error: " + error);
							    error(error, plan, objectSuccessCallback);
							}
						});
						
					}

					@Override
					public void onFail(String error) {
					    error(error, plan, objectSuccessCallback);
					}
					
				});
				
			}

			@Override
			public void onFail(String error) {
			    Log.d("SharedCoin", "SharedCoin constructPlan error " + error);
			}
        });
	}
	
	private void constructPlan(int repetitions, List<String> fromAddresses, List<HashMap<String,String>> to_addresses, final ObjectSuccessCallback objectSuccessCallback) {
        final Plan plan = new Plan(this);

        try {
			List<BigInteger> to_values_before_fees = new ArrayList<BigInteger>(to_addresses.size());
	        //List<BigInteger> fee_each_repetition = new ArrayList<BigInteger>(repetitions);
	        BigInteger[] fee_each_repetition = new BigInteger[repetitions];
	        Log.d("SharedCoin", "SharedCoin constructPlan: 1");
	        HashMap<String,BigInteger> to_addressesMap =  new HashMap<String,BigInteger>();
	        
	        for (HashMap<String,String> to_address : to_addresses) {
	        	
	        	BigInteger amt = new BigInteger(to_address.get("value"));
	        	
	        	to_addressesMap.put(to_address.get("address"), amt);
	        	
	        	to_values_before_fees.add(amt);
	        	//to_values_before_fees.add(to_address.get("amount"));
	            Log.d("SharedCoin", "SharedCoin to_values_before_fees: " + to_values_before_fees);
	            Log.d("SharedCoin", "SharedCoin constructPlan: 2");
	            for (int ii = repetitions-1; ii >= 0; --ii) {
	            	BigInteger feeThisOutput = calculateFeeForValue(amt);
	            	//BigInteger feeThisOutput = new BigInteger("777");

	            	BigInteger existing = fee_each_repetition[ii];
	                if (existing != null) {                	
	                    fee_each_repetition[ii] = existing.add(feeThisOutput);
	                } else {
	                    fee_each_repetition[ii] = feeThisOutput;
	                }
	            }        	
	        }
	        BigInteger feeSum = BigInteger.ZERO;
	        for (int i = 0; i < fee_each_repetition.length; i++)
	        	feeSum = feeSum.add(fee_each_repetition[i]);
	        
	        Log.d("SharedCoin", "SharedCoin to_values_before_fees: " + Arrays.toString(to_values_before_fees.toArray()));
	        Log.d("SharedCoin", "SharedCoin fee_each_repetition: " + Arrays.toString(fee_each_repetition));

	        
	        //ECKey change_key = new ECKey();
	        final String change_address;
	        
	        
	        
	        Offer offer = new Offer(this);
	        List<MyTransactionOutPoint> unspent;
	        if (! SHARED_COIN_DEBUG) {
	        	unspent = MyRemoteWallet.getUnspentOutputPoints(fromAddresses.toArray(new String[fromAddresses.size()]));

	        	change_address = plan.generateAddressFromSeed();
	        } else {
	        	unspent = MyRemoteWallet.getUnspentOutputPoints(fromAddresses.toArray(new String[fromAddresses.size()]));

	        	change_address = plan.generateAddressFromSeed();
	        	/*
	        	unspent = new ArrayList<MyTransactionOutPoint>();
	        	
	        	String tx_hash = "6e659f6cb8a160c55e65d589c85bdbaa9cd0b6bfb2e68939f5ad68ffa6fe9c84";
				byte[] hashBytes = Hex.decode(tx_hash);
				ArrayUtils.reverse(hashBytes);
				Sha256Hash txHash = new Sha256Hash(hashBytes);
				byte[] scriptBytes = Hex.decode("76a91423757b5b32e39b05fb5280aba05cdee5c6acd91088ac");				
	        	int txOutputN = 0;
	   
	        	BigInteger value = new BigInteger("12507377");
				MyTransactionOutPoint outPoint = new MyTransactionOutPoint(txHash, txOutputN, value, scriptBytes);
				unspent.add(outPoint);
				
				change_address = "1EQc2WicMjAV9mox4oMRx7sb6ivNYweeNR";
				//*/
	        }
	        
	        Log.d("SharedCoin", "SharedCoin makeTransaction: 1");

			Pair<Transaction, Long> pair = this.remoteWallet.makeTransaction(false, unspent, to_addressesMap, feeSum, change_address);
	        Log.d("SharedCoin", "SharedCoin makeTransaction: 2");
			Transaction transaction = pair.first;
			

			/*
			for (int i = 0; i < unspent.size(); i++) {
				MyTransactionOutPoint myTransactionOutPoint = unspent.get(i);
				myTransactionOutPoint.getTxHash();		
			}
			//*/
		    List<TransactionInput> transactionInputs = transaction.getInputs();
			for (Iterator<TransactionInput> iti = transactionInputs.iterator(); iti.hasNext();) {
				TransactionInput transactionInput = iti.next();
				TransactionOutPoint transactionOutPoint = transactionInput.getOutpoint();
				if (transactionOutPoint instanceof MyTransactionOutPoint) {
					MyTransactionOutPoint myTransactionOutPoint = (MyTransactionOutPoint)transactionOutPoint;
			        Log.d("SharedCoin", "SharedCoin myTransactionOutPoint.getTxHash(): " + myTransactionOutPoint.getTxHash());
			        Log.d("SharedCoin", "SharedCoin myTransactionOutPoint.getIndex(): " + myTransactionOutPoint.getIndex());
			        Log.d("SharedCoin", "SharedCoin myTransactionOutPoint.getValue(): " + myTransactionOutPoint.getValue());
			        offer.addOfferedOutpoint(myTransactionOutPoint.getTxHash().toString(), myTransactionOutPoint.getIndex(), myTransactionOutPoint.getValue().toString());
		            Log.d("SharedCoin", "SharedCoin -----------------------------------");
				} else {
					throw new Exception("transactionOutPoint not instanceof MyTransactionOutPoint");
				}
	    	}
			
	    	List<TransactionOutput> transactionOutputs = transaction.getOutputs();

			for (Iterator<TransactionOutput> ito = transactionOutputs.iterator(); ito.hasNext();) {
				TransactionOutput transactionOutput = ito.next();
	    		
				com.google.bitcoin.script.Script script = transactionOutput.getScriptPubKey();
	    		String addr = script.getToAddress(this.params).toString();
	    		//com.google.bitcoin.core.Script script = transactionOutput.getScriptPubKey();	    		
	    		//String addr = script.getToAddress().toString();
	    		
	    		BigInteger value = transactionOutput.getValue();
	    		
	  
	    		BitcoinScript toOutputScript = new BitcoinScript(transactionOutput.getScriptBytes());
	    		//BitcoinScript toOutputScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(addr));
	    		byte[] program = toOutputScript.getProgram();
			    String programHex = new String(Hex.encode(program));
	                        
	            if (addr.equals(change_address)) {
	    			offer.addRequestOutputs(value.toString(), programHex, true);
	    		} else {
	    			offer.addRequestOutputs(value.toString(), programHex);
	    		}
	            Log.d("SharedCoin", "SharedCoin transactionOutput programHex: " + programHex);
	            Log.d("SharedCoin", "SharedCoin transactionOutput getValue: " + value);
	            Log.d("SharedCoin", "SharedCoin transactionOutput addr: " + addr);		
	            Log.d("SharedCoin", "SharedCoin -----------------------------------");		
			}
	        Log.d("SharedCoin", "SharedCoin constructPlan getOfferedOutpoint " + offer.getOfferedOutpoints().toString());		
	        Log.d("SharedCoin", "SharedCoin constructPlan getRequestOutputs " + offer.getRequestOutputs().toString());		
	        Log.d("SharedCoin", "SharedCoin constructPlan getOffer " + offer.getOffer().toString());		

	        plan.n_stages = repetitions;
	        plan.constructRepetitions(offer, fee_each_repetition, new ObjectSuccessCallback() {
				@Override
				public void onSuccess(Object obj) {
					objectSuccessCallback.onSuccess(obj);
				}

				@Override
				public void onFail(String error) {
					_error(plan, error, objectSuccessCallback);
				}
	        });			        
		} catch (Exception e) {
			_error(plan, e.getLocalizedMessage(), objectSuccessCallback);
			e.printStackTrace();
		}
	}
	
	
	//TODO call in same places as in js 
	private void _error(Plan plan, String error, ObjectSuccessCallback objectSuccessCallback) {
		if (plan.generated_addresses == null)
			return;
		
        for (String key : plan.generated_addresses) {
	        Log.d("SharedCoin", "SharedCoin _error deleteAddress " + key);		
            //MyWallet.deleteAddress(key);        
	        
	        //remoteWallet.removeKey(key);
        }
        
        objectSuccessCallback.onFail(error);
	}
	
	
    private static int[] divideUniformlyRandomly(final int sum, final int n) {
    	int[] nums = new int[n];
        long upperbound = Math.round(sum * 1.0 / n);
        long offset = Math.round(0.5 * upperbound);

        double cursum = 0;
        for (int i = 0; i < n; i++)
        {
        	double rand = Math.floor((Math.random() * upperbound) + offset);
            if (cursum + rand > sum || i == n - 1)
            {
                rand = sum - cursum;
            }
            cursum += rand;
            nums[i] = (int) rand;
            if (cursum == sum)
            {
                break;
            }
        }
    	return nums;
    }        
	
    private BigInteger calculateFeeForValue(final BigInteger input_value) {
		BigInteger minFee = BigInteger.valueOf(getMinimumFee());
		BigDecimal feePercent = new BigDecimal(getFeePercent());

        //Log.d("SharedCoin", "SharedCoin calculateFeeForValue: minFee" + minFee);
        //Log.d("SharedCoin", "SharedCoin calculateFeeForValue: feePercent" + feePercent);

        if (input_value.compareTo(BigInteger.ZERO) > 0 && feePercent.intValue() > 0) {
        	int mod = (int) Math.ceil(100 / feePercent.doubleValue());

        	BigInteger fee = input_value.divide(BigInteger.valueOf(mod));

            if (minFee.compareTo(fee) > 0) {
                return minFee;
            } else {
                return fee;
            }
        } else {
            return minFee;
        }
    }
    

    private void complete(final Offer offer, final String tx_hash, final String tx, final ObjectSuccessCallback objectSuccessCallback) {
    	Log.d("SharedCoin", "SharedCoin complete tx_hash: " + tx_hash);		

		offer.determineOutputsToOfferNextStage(tx, new ObjectSuccessCallback() {

			@SuppressWarnings("unchecked")
			@Override
			public void onSuccess(Object obj) {
		    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage onSuccess");		

				JSONArray outpoints_to_offer_next_stage = (JSONArray) obj;
		    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage onSuccess outpoints_to_offer_next_stage.size() " + outpoints_to_offer_next_stage.size());		

				for (int i = 0; i < outpoints_to_offer_next_stage.size(); i ++) {					
			    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage onSuccess i " + i);		

					JSONObject outpoint_to_offer_next_stage = (JSONObject) outpoints_to_offer_next_stage.get(i);
					outpoint_to_offer_next_stage.put("hash", tx);
				}
		    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage onSuccess 2");		

				objectSuccessCallback.onSuccess(outpoints_to_offer_next_stage);
			}

			@Override
			public void onFail(String error) {
		    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage onFail " + error);		
				objectSuccessCallback.onFail(error);
			}		
		});
	}

	
    /*
	public boolean isEnabled() {
		return true;
    }

	public Double getFeePercent() {
    	return 0.0;	
    }

	public Long getMaximumInputValue() {
    	return 100000000000L;	
    }

	public Long getMaximumOfferNumberOfInputs() {
    	return 20L;	
    }

	public Long getMaximumOfferNumberOfOutputs() {
    	return 20L;	
    }

	public Long getMaximumOutputValue() {
    	return 5000000000L;	
    }

	public Long getMinSupportedVersion() {
		return 2L;
    }
	
	public Long getMinimumFee() {
    	return 50000L;	
    }
	
	public Long getMinimumInputValue() {
    	return 1000000L;	
    }

	public Long getMinimumOutputValue() {
    	return 1000000L;	
    }
	
	public Long getMinimumOutputValueExcludeFee() {
    	return 5460L;	
    }
	
	public Long getRecommendedIterations() {
    	return 4L;	
    }

	public Long getRecommendedMaxIterations() {
		return 10L;
	}

	public Long getRecommendedMinIterations() {
		return 2L;
    }

	public String getToken() {
    	return "e1n5RxXrCS/K67WPoD+2MQuAd7BHnKocxeXb3XShc8C6Vgp1P7Q0tm9NG6nhFcHv33BKFTuxk4mJ8vVlRlx1t11qtRsbv43yCBQ4kL+O4TmiljvpdL/TSlw2pbO27vRf";	
    }
	//*/
	
    //*
	public boolean isEnabled() {
    	return info != null ? (Boolean) info.get("enabled") : false;	
    }

	public Double getFeePercent() {
    	return info != null ? (Double) info.get("fee_percent") : null;	
    }

	public Long getMaximumInputValue() {
    	return info != null ? (Long) info.get("maximum_input_value") : null;	
    }

	public Long getMaximumOfferNumberOfInputs() {
    	return info != null ? (Long) info.get("maximum_offer_number_of_inputs") : null;	
    }

	public Long getMaximumOfferNumberOfOutputs() {
    	return info != null ? (Long) info.get("maximum_offer_number_of_outputs") : null;	
    }

	public Long getMaximumOutputValue() {
    	return info != null ? (Long) info.get("maximum_output_value") : null;	
    }

	public Long getMinSupportedVersion() {
    	return info != null ? (Long) info.get("min_supported_version") : null;	
    }
	
	public Long getMinimumFee() {
    	return info != null ? (Long) info.get("minimum_fee") : null;	
    }
	
	public Long getMinimumInputValue() {
    	return info != null ? (Long) info.get("minimum_output_value") : null;	
    }

	public Long getMinimumOutputValue() {
    	return info != null ? (Long) info.get("minimum_output_value") : null;	
    }
	
	public Long getMinimumOutputValueExcludeFee() {
    	return info != null ? (Long) info.get("minimum_output_value_exclude_fee") : null;	
    }
	
	public Long getRecommendedIterations() {
    	return info != null ? (Long) info.get("recommended_iterations") : null;	
    }

	public Long getRecommendedMaxIterations() {
    	return info != null ? (Long) info.get("recommended_max_iterations") : null;	
	}

	public Long getRecommendedMinIterations() {
    	return info != null ? (Long) info.get("recommended_min_iterations") : null;	
    }

	public String getToken() {
    	return info != null ? (String) info.get("token") : null;	
    }
//*/
	
	
	private class Offer {
		private long offer_id; //A unique ID for this offer (set by server)

		private JSONArray offered_outpoints;
		private JSONArray request_outputs;

		private SharedCoin sharedCoin;
		
		public Offer(SharedCoin sharedCoin) {
			this.sharedCoin = sharedCoin;
			this.offered_outpoints = new JSONArray();
			this.request_outputs = new JSONArray();
			this.offer_id = 0;
		}
		
		@SuppressWarnings("unchecked")
		public void addOfferedOutpoint(String hash, long index, String value) {
			JSONObject dict = new JSONObject();
			dict.put("hash", hash);
			dict.put("index", index);
			dict.put("value", value);
			this.offered_outpoints.add(dict);			
		}
		
		@SuppressWarnings("unchecked")
		public void addRequestOutputs(String value, String script) {
			JSONObject dict = new JSONObject();
			dict.put("value", value);
			dict.put("script", script);
			this.request_outputs.add(dict);			
		}
		
		@SuppressWarnings("unchecked")
		public void addRequestOutputs(String value, String script, boolean exclude_from_fee) {
			JSONObject dict = new JSONObject();
			dict.put("value", value);
			dict.put("script", script);
			dict.put("exclude_from_fee", new Boolean(exclude_from_fee));
			//dict.put("exclude_from_fee", exclude_from_fee);
			this.request_outputs.add(dict);			
		}

		public void setOfferID(long id) {
			this.offer_id = id;
		}

		public long getOfferID() {
			return this.offer_id;
		}

		public JSONArray getOfferedOutpoints() {
			return this.offered_outpoints;
		}
		
		public JSONArray getRequestOutputs() {
			return this.request_outputs;
		}
		
		public void setOfferedOutpoints(Object object) {
			this.offered_outpoints = (JSONArray) object;
		}
		
		public void clearOfferedOutpoints() {
			this.offered_outpoints.clear();
		}
		
		@SuppressWarnings("unchecked")
		public JSONObject getOffer() {
			JSONObject offerObject = new JSONObject();
			offerObject.put("offered_outpoints", this.offered_outpoints);
			offerObject.put("request_outputs", this.request_outputs);
			offerObject.put("offer_id", this.offer_id);
			return offerObject;
		}
		
		
		
		private boolean isOutpointOneWeOffered(TransactionInput input) {
			try {
				byte[] bytes = input.getOutpoint().getHash().getBytes();
				String hexHash = new String(Hex.encode(bytes));
								
				long inputIndex = input.getOutpoint().getIndex();

			    Log.d("SharedCoin", "SharedCoin isOutpointOneWeOffered hexHash: " + hexHash);
			    Log.d("SharedCoin", "SharedCoin isOutpointOneWeOffered inputIndex: " + inputIndex);

			    for (int i = 0; i < this.offered_outpoints.size(); i++) {
			    	JSONObject request_outpoint = (JSONObject) this.offered_outpoints.get(i);
			    	String hash = (String) request_outpoint.get("hash");
			    	long index =  SharedCoin.getLongFromLong(request_outpoint, "index");
			    	if (hash.equals(hexHash) && index == inputIndex) {
			    		return true;
			    	}
		    	}
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
		    
            return false;
		}

		private boolean isOutputOneWeRequested(TransactionOutput output) {
		    //Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested ----------------------------------- ");
			try {
				BitcoinScript toOutputScript = new BitcoinScript(output.getScriptBytes());

	    		byte[] program = toOutputScript.getProgram();
			    String scriptHex = new String(Hex.encode(program));
			    BigInteger value = output.getValue();
			    
			    //Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested self.request_outputs: " + this.request_outputs.toString());
			    //Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested scriptHex: " + scriptHex);
			    //Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested value: " + value);

			    for (int i = 0; i < this.request_outputs.size(); i++) {
			    	JSONObject request_output = (JSONObject) this.request_outputs.get(i);
			    	String script = (String) request_output.get("script");
			    	String valueStr = SharedCoin.getIntegerStringFromIntegerString(request_output, "value");

			    	//Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested TransactionOutput script: " + script);
				    //Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested TransactionOutput value: " + valueStr);

			    	if (script.equals(scriptHex) && valueStr.equals(value.toString())) {
					    //Log.d("SharedCoin", "SharedCoin sOutputOneWeRequested return true: ");
			    		return true;
			    	}
		    	}
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
		    
            return false;
		}
		
		private boolean isOutputChange(TransactionOutput output) {

			try {
				BitcoinScript toOutputScript = new BitcoinScript(output.getScriptBytes());

	    		byte[] program = toOutputScript.getProgram();
			    String scriptHex = new String(Hex.encode(program));
			    BigInteger value = output.getValue();
			    
			    Log.d("SharedCoin", "SharedCoin isOutputChange self.request_outputs: " + this.request_outputs.toString());
			    Log.d("SharedCoin", "SharedCoin isOutputChange scriptHex: " + scriptHex);
			    Log.d("SharedCoin", "SharedCoin isOutputChange value: " + value);

			    for (int i = 0; i < this.request_outputs.size(); i++) {
			    	JSONObject request_output = (JSONObject) this.request_outputs.get(i);
			    	String script = (String) request_output.get("script");
			    	String valueStr = SharedCoin.getIntegerStringFromIntegerString(request_output, "value");
			    	if (script.equals(scriptHex) && valueStr.equals(value.toString())) {
			        	return request_output.get("exclude_from_fee") == null ? false : (Boolean) request_output.get("exclude_from_fee");	
			    	}
		    	}
			    
			} catch (Exception e) {
				e.printStackTrace();
			}
		    
            return false;
		}
		
		@SuppressWarnings("unchecked")
		public void determineOutputsToOfferNextStage(String tx_hex, ObjectSuccessCallback objectSuccessCallback) {
			try {
		    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage ");		

				Transaction tx = new Transaction(this.sharedCoin.params, Hex.decode(tx_hex.getBytes()), 0, null, false, true, Message.UNKNOWN_LENGTH);
		    	List<TransactionOutput> transactionOutputs = tx.getOutputs();

				JSONArray outpoints_to_offer_next_stage = new JSONArray();
		    	Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage transactionOutputs.size() " + transactionOutputs.size());		
				
				for (int i = 0; i < transactionOutputs.size(); i++) {					
					TransactionOutput output = transactionOutputs.get(i);

					Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage i " + i);		

		    		if (isOutputOneWeRequested(output)) {
		    			if (isOutputChange(output)) {   			
							JSONObject dict = new JSONObject();
							dict.put("hash", null);
							dict.put("index", (long) i);
							dict.put("value", output.getValue().toString());
				    		outpoints_to_offer_next_stage.add(dict);
		    			}		    			
		    		}
				}
				Log.d("SharedCoin", "SharedCoin determineOutputsToOfferNextStage outpoints_to_offer_next_stage.size " + outpoints_to_offer_next_stage.size());		

				objectSuccessCallback.onSuccess(outpoints_to_offer_next_stage);
				
			} catch (ProtocolException e) {
				objectSuccessCallback.onFail(e.getLocalizedMessage());
				e.printStackTrace();
			}			
		}
		
		public void checkProposal(JSONObject proposal, ObjectSuccessCallback objectSuccessCallback) {
			try {
		    	Log.d("SharedCoin", "SharedCoin checkProposal proposal" + proposal.toString());		

		    	if (proposal.get("tx") == null) {
	            	throw new Exception("Proposal Transaction Is Null");
	            }
	            
				String tx_hex = (String) proposal.get("tx");

				Transaction tx = new Transaction(this.sharedCoin.params, Hex.decode(tx_hex.getBytes()));		
				
                int output_matches = 0;
            	List<TransactionOutput> transactionOutputs = tx.getOutputs();
        		Log.d("SharedCoin", "SharedCoin checkProposal transactionOutputs.size " + transactionOutputs.size());		
        		for (Iterator<TransactionOutput> ito = transactionOutputs.iterator(); ito.hasNext();) {
        			TransactionOutput output = ito.next();
                    if (this.isOutputOneWeRequested(output)) {
                        ++output_matches;
                    }        			
        		}

        		Log.d("SharedCoin", "SharedCoin checkProposal output_matches " + output_matches);		
		    	Log.d("SharedCoin", "SharedCoin checkProposal this.request_outputs.size() " + this.request_outputs.size());		

                if (output_matches < this.request_outputs.size()) {
                	throw new Exception("Could not find all our requested outputs (" + output_matches + " < " + this.request_outputs.size() + ")");
                }
                
                int input_matches = 0;
            	
                JSONArray signatureRequests = (JSONArray) proposal.get("signature_requests");
        		Log.d("SharedCoin", "SharedCoin checkProposal signatureRequests.size " + signatureRequests.size());		

        		for (int i = 0; i < signatureRequests.size(); ++i) {
        			JSONObject signatureRequest = (JSONObject) signatureRequests.get(i);
        			BigInteger tx_index = SharedCoin.getBigIntegerFromLong(signatureRequest, "tx_input_index");
                    if (this.isOutpointOneWeOffered(tx.getInput(tx_index.intValue()))) {
                        ++input_matches;
                    }        			
        		}
        		
                if (this.offered_outpoints.size() != input_matches) {
                	throw new Exception("Could not find all our offered outpoints " + this.offered_outpoints.size() + " != " + input_matches + ")");
                }

		    	Log.d("SharedCoin", "SharedCoin checkProposal onSuccess");		
				objectSuccessCallback.onSuccess(tx); 
			} catch (ProtocolException e) {				
		    	Log.d("SharedCoin", "SharedCoin checkProposal ProtocolException" + e.getLocalizedMessage());		
				objectSuccessCallback.onFail(e.getLocalizedMessage()); 
				e.printStackTrace();
			} catch (Exception e) {
		    	Log.d("SharedCoin", "SharedCoin checkProposal Exception" + e.getLocalizedMessage());		
				objectSuccessCallback.onFail(e.getLocalizedMessage()); 
				e.printStackTrace();
			}
		}
		
		public void submit(SharedCoinSuccessCallback objectSuccessCallback) {
        	try {
				JSONObject obj = SharedCoin.submitOffer(getOffer(), getToken(), null, null);
				String status = (String) obj.get("status");
            	Log.d("SharedCoin", "SharedCoin submit obj " + obj.toString());

				
		    	if (status != null && status.equals(STATUS_COMPLETE)) {
		    		objectSuccessCallback.onComplete((String)obj.get("tx_hash"), (String)obj.get("tx"));
                } else if (obj.get("offer_id") == null) {
                	objectSuccessCallback.onFail("Null offer_id returned");
                } else {
                    this.offer_id = SharedCoin.getLongFromLong(obj, "offer_id");
                    objectSuccessCallback.onSuccess(obj);
                }												
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		private JSONObject _pollForProposalID() {
			return SharedCoin.getOfferID(getOfferID());
		}
		
		public void pollForProposalID(ObjectSuccessCallback objectSuccessCallback) {
			while (true) {
		    	Log.d("SharedCoin", "SharedCoin pollForProposalID");		
				JSONObject obj = this._pollForProposalID();
				if (obj == null) {
			    	Log.d("SharedCoin", "SharedCoin pollForProposalID _pollForProposalID error obj == null");		
					break;
				}
				
				String status = (String) obj.get("status");
		    	Log.d("SharedCoin", "SharedCoin pollForProposalID status " + status);		
		    	if (status.equals(STATUS_WAITING)) {
		    		continue;
				} else if (status.equals(STATUS_NOT_FOUND)) {
					objectSuccessCallback.onFail("Offer ID Not Found");
					break;
				} else if (status.equals(STATUS_ACTIVE_PROPOSAL)) {
					BigInteger proposalID = SharedCoin.getBigIntegerFromLong(obj, "proposal_id");
					objectSuccessCallback.onSuccess(proposalID);
					break;
				} else {
					objectSuccessCallback.onFail("Unknown status " + status);
					break;
				}		    	
			}			
		}
		
		public void getProposal(BigInteger proposal_id, final SharedCoinSuccessCallback sharedCoinSuccessCallback) {
			SharedCoin.getProposalID(proposal_id, this.offer_id, new ObjectSuccessCallback() {

				@Override
				public void onSuccess(Object obj) {
			    	Log.d("SharedCoin", "SharedCoin getProposal " + obj.toString());		
					JSONObject jsonObject = (JSONObject)obj;
					
					String status = (String) jsonObject.get("status");
					
			    	Log.d("SharedCoin", "SharedCoin getProposal status " + status);		

			    	if (status.equals(STATUS_NOT_FOUND)) {
						sharedCoinSuccessCallback.onFail("Proposal or Offer ID Not Found");
					} else if (status.equals(STATUS_COMPLETE)) {
						String txHash = (String) jsonObject.get("tx_hash");
						String tx = (String) jsonObject.get("tx");				
						sharedCoinSuccessCallback.onComplete(txHash, tx);
					} else if (status.equals(STATUS_SIGNATURES_NEEDED)) {
						sharedCoinSuccessCallback.onSuccess(jsonObject);
					} else {
				    	Log.d("SharedCoin", "SharedCoin getProposal invalid status " + status);		
					}
				}

				@Override
				public void onFail(String error) {
			    	Log.d("SharedCoin", "SharedCoin getProposalID error " + error);		
			    	sharedCoinSuccessCallback.onFail(error);
				}
			});
		}
		
		
		@SuppressWarnings("unchecked")
		public void signInputs(JSONObject proposal, Transaction tx, final ObjectSuccessCallback objectSuccessCallback) {
	    	Log.d("SharedCoin", "SharedCoin signInputs ----------------------------------- ");		
	    	Log.d("SharedCoin", "SharedCoin signInputs proposal " + proposal.toString());		

	    	try {
		        HashMap<String,String> tmp_cache = new HashMap<String,String>();
				
				JSONArray connected_scripts = new JSONArray();
	            JSONArray signatureRequests = (JSONArray) proposal.get("signature_requests");
	    		for (int i = 0; i < signatureRequests.size(); ++i) {
	    			JSONObject request = (JSONObject) signatureRequests.get(i);

	    			String tmp = (String) request.get("connected_script");
	    			BitcoinScript connected_script = new BitcoinScript(Hex.decode(tmp.getBytes()));
	    	    	
//	                if (connected_script == null)
//	                	throw new Exception("signInputs() Connected script is null");	    			
	    			
	    			JSONObject connectedScriptStuff = new JSONObject();
	    			
	    			
	    			
	    			/*
	    			connectedScriptStuff.put("connected_script", connected_script);
	    	    	//*/
	    			//*
	    			connectedScriptStuff.put("connected_script_hex", tmp);
	    	    	//*/

	    			
	    			
        			long tx_index = SharedCoin.getLongFromLong(request, "tx_input_index");
	    			long offer_outpoint_index = SharedCoin.getLongFromLong(request, "offer_outpoint_index");

	    			connectedScriptStuff.put("tx_input_index", tx_index);
	    			connectedScriptStuff.put("offer_outpoint_index", offer_outpoint_index);

	    			String inputAddress = connected_script.getAddress().toString();
        	    	Log.d("SharedCoin", "SharedCoin signInputs inputAddress " + inputAddress);		

	    			if (tmp_cache.containsKey(inputAddress)) {
	        	    	Log.d("SharedCoin", "SharedCoin signInputs extra_private_keys key " + tmp_cache.get(inputAddress));		

	        	    	connectedScriptStuff.put("priv_to_use", tmp_cache.get(inputAddress));
	    			} else if (sharedCoin.extra_private_keys.containsKey(inputAddress)) {
	        	    	Log.d("SharedCoin", "SharedCoin signInputs extra_private_keys key " + sharedCoin.extra_private_keys.get(inputAddress));		

	    				connectedScriptStuff.put("priv_to_use", sharedCoin.extra_private_keys.get(inputAddress));
	    			} else if (Offer.this.sharedCoin.remoteWallet.isMine(inputAddress) && ! Offer.this.sharedCoin.remoteWallet.isWatchOnly(inputAddress)) {
	        	    	Log.d("SharedCoin", "SharedCoin signInputs remoteWallet.getPrivateKey key " + Offer.this.sharedCoin.remoteWallet.getPrivateKey(inputAddress));		
	    				connectedScriptStuff.put("priv_to_use", Offer.this.sharedCoin.remoteWallet.getPrivateKey(inputAddress));
	    			}
	    			
	    			if (! connectedScriptStuff.containsKey("priv_to_use")) {
	                	throw new Exception("Private key not found");
	    			} else { 
	    				tmp_cache.put(inputAddress, (String) connectedScriptStuff.get("priv_to_use"));
	    			}
 	    		
	    	    	Log.d("SharedCoin", "SharedCoin signInputs connectedScriptStuff " + connectedScriptStuff.toString());		

	    	    	// different from Javascript version connected_scripts is an array of Bitcoin.Script while java version is a JSONArray of JSONObject
	    			connected_scripts.add(connectedScriptStuff);
	    		}

	    		this.signNormal(tx, connected_scripts, new ObjectSuccessCallback() {

					@Override
					public void onSuccess(Object obj) {
						JSONArray signatures = (JSONArray) obj;
						objectSuccessCallback.onSuccess(signatures);						
					}

					@Override
					public void onFail(String error) {
						objectSuccessCallback.onFail(error);					
					}
	    		});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
				
		@SuppressWarnings("unchecked")
		private void signNormal(Transaction tx, JSONArray connected_scripts, ObjectSuccessCallback objectSuccessCallback) {
	    	Log.d("SharedCoin", "SharedCoin signNormal connected_scripts " + connected_scripts.toString());		

            JSONArray signatures = new JSONArray();
			try {
	            for (int index = 0; index < connected_scripts.size(); index++) {
	        		SharedCoin.setTimeout(1);		

	    	    	JSONObject connectedScriptStuff = (JSONObject) connected_scripts.get(index);

	    	    	

	    	    	/*
	    	    	BitcoinScript connected_script = (BitcoinScript) connectedScriptStuff.get("connected_script");
	            	if (connected_script == null) throw new Exception("Null connected script");
	    	    	//*/
	    	    	//*
	    	    	String connected_script_hex = (String) connectedScriptStuff.get("connected_script_hex");
	    			BitcoinScript connected_script = new BitcoinScript(Hex.decode(connected_script_hex.getBytes()));
	    	    	//*/
	    			
	    			
        			BigInteger tx_input_index = SharedCoin.getBigIntegerFromLong(connectedScriptStuff, "tx_input_index");
	                String base58PrivKey = (String) connectedScriptStuff.get("priv_to_use");
        	    	
	                byte[] signed_script = SharedCoin.signInput(this.sharedCoin.params, tx, tx_input_index.intValue(), base58PrivKey, connected_script, SigHash.ALL);
	                
	                if (signed_script != null) {
	        	    	JSONObject signature = new JSONObject();
	                	signature.put("tx_input_index", connectedScriptStuff.get("tx_input_index"));
		            	String signedScriptHex = new String(Hex.encode(signed_script));		            			            	
	                	signature.put("input_script", signedScriptHex);
	                	signature.put("offer_outpoint_index", connectedScriptStuff.get("offer_outpoint_index"));
	                	signatures.add(signature);
	                } else {
	        	    	throw new Exception("Unknown error signing transaction");
	                }
	            }
	            
		    	Log.d("SharedCoin", "SharedCoin signNormal signatures " + signatures.toString());		

	            objectSuccessCallback.onSuccess(signatures);
			} catch (Exception e) {
				objectSuccessCallback.onFail(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}
				
		public void submitInputScripts(JSONObject proposal, JSONArray input_scripts, final SharedCoinSuccessCallback sharedCoinSuccessCallback) {
			sharedCoin.setLastSignatureSubmitTime(new Date().getTime());
	    	Log.d("SharedCoin", "SharedCoin submit_signatures input_scripts " + input_scripts.toString());		
	    	Log.d("SharedCoin", "SharedCoin submit_signatures offer_id " + this.offer_id);		
	    	Log.d("SharedCoin", "SharedCoin submit_signatures proposal " + proposal.toString());	
	    	long proposalID = SharedCoin.getLongFromLong(proposal, "proposal_id");
	    	Log.d("SharedCoin", "SharedCoin submit_signatures proposal_id " + proposalID);		
	    	
	    	SharedCoin.submitSignatures(proposalID, this.offer_id, input_scripts, new ObjectSuccessCallback() {

				@Override
				public void onSuccess(Object obj) {
					JSONObject jsonObject = (JSONObject)obj;
					String status = (String) jsonObject.get("status");
					
			    	Log.d("SharedCoin", "SharedCoin submitInputScripts status " + status);		

			    	if (status.equals(STATUS_NOT_FOUND)) {
						sharedCoinSuccessCallback.onFail("Proposal Expired or Not Found");
					} else if (status.equals(STATUS_VERIFICATION_FAILED)) {
						sharedCoinSuccessCallback.onFail("Signature Verification Failed");
					} else if (status.equals(STATUS_COMPLETE)) {
						String txHash = (String) jsonObject.get("tx_hash");
						String tx = (String) jsonObject.get("tx");				
						sharedCoinSuccessCallback.onComplete(txHash, tx);
					} else if (status.equals(STATUS_SIGNATURES_ACCEPTED)) {
						sharedCoinSuccessCallback.onSuccess(jsonObject);
					} else {
						sharedCoinSuccessCallback.onFail("Unknown status " + status);
					}					
				}

				@Override
				public void onFail(String error) {
					sharedCoinSuccessCallback.onFail(error);	
				}
	    	});	
		}
	}
	

	
	public class Plan {
		public int n_stages = 0;
		public int c_stage = 0;
		public String address_seed = null;
		public int address_seen_n = 0;
		public List<String> generated_addresses = null;
		List<Offer> offers = new ArrayList<Offer>();
		public SharedCoin sharedCoin;

		public Plan(SharedCoin sharedCoin) {
			this.sharedCoin = sharedCoin;
			this.generated_addresses = new ArrayList<String>();
		}
		
		@SuppressLint("TrulyRandom")
		public String generateAddressFromSeed() {
	        if (this.address_seed == null) {
	    		SecureRandom random = new SecureRandom();
			    byte randomBytes[] = new byte[18];
			    random.nextBytes(randomBytes);
			    this.address_seed = new String(Hex.encode(randomBytes));
			    
			    Log.d("SharedCoin", "SharedCoin enerateAddressFromSeed1: " + SharedCoin.SEED_PREFIX + this.address_seed);

                List<String> additional_seeds = remoteWallet.getAdditionalSeeds();                
                for (String additional_seed : additional_seeds) Log.d("SharedCoin", "SharedCoin Saved Wallet additional_seed " + additional_seed);		
                Log.d("SharedCoin", "SharedCoin enerateAddressFromSeed additional_seeds.size()b4 " + additional_seeds.size());	
                
			    
                //wallet sync is called b4 execStage 0
			    remoteWallet.addAdditionalSeeds(new String(SharedCoin.SEED_PREFIX + this.address_seed));
	        }
	        
		    Log.d("SharedCoin", "SharedCoin enerateAddressFromSeed2: " + SharedCoin.SEED_PREFIX + this.address_seed);

	        String address = generateAddressFromCustomSeed(SharedCoin.SEED_PREFIX+this.address_seed, this.address_seen_n);
	        address_seen_n++;
	        return address;
		}	

		public String generateChangeAddress() {
			/*
			final ECKey key = remoteWallet.generateECKey();
	        final String address = key.toAddress(this.sharedCoin.params).toString();
			this.generated_addresses.add(address);
			Log.d("SharedCoin", "SharedCoin generateChangeAddress: address: " + address);		
			return address;
			//*/
			
			//*
			final ECKey key = remoteWallet.generateECKey();
	        final String address = key.toAddress(this.sharedCoin.params).toString();
			this.generated_addresses.add(address);
    		Log.d("SharedCoin", "SharedCoin generateChangeAddress addKeyToWallet " + address);	    						    			

			application.addKeyToWallet(key, address, null, 0, new AddAddressCallback() {
				public void onSavedAddress(String address) {
		             Log.d("SharedCoin", "SharedCoin addKeyToWallet onSavedAddress ");
				}

				public void onError(String reason) {
		             Log.d("SharedCoin", "SharedCoin addKeyToWallet onError ");
				}
			});
		
			return address;
			//*/
		}	
		
		
		public void constructRepetitions(Offer initial_offer, BigInteger[] fee_each_repetition, ObjectSuccessCallback objectSuccessCallback) {
			try {
	            
		        Log.d("SharedCoin", "SharedCoin constructRepetitions initial_offer " + initial_offer.getOffer().toString());		
	            Log.d("SharedCoin", "SharedCoin constructRepetitions fee_each_repetition " + Arrays.asList(fee_each_repetition));
	            
		        BigInteger totalValueInput = BigInteger.ZERO;
		        JSONArray offered_outpoints = initial_offer.getOfferedOutpoints();
		        for(int i = 0; i < offered_outpoints.size(); i++) {
		        	JSONObject dict = (JSONObject) offered_outpoints.get(i);
		        	totalValueInput = totalValueInput.add(SharedCoin.getIntegerFromIntegerString(dict, "value"));
		        }
	            
		        BigInteger totalValueLeftToConsume = totalValueInput;
		        BigInteger totalChangeValueLeftToConsume = BigInteger.ZERO;
		        for (int ii = 0; ii < this.n_stages-1; ++ii) {
		        	Offer offer = new Offer(this.sharedCoin);

		        	//Copy the inputs from the last offer
		    		if (ii == 0) {
		    	        JSONArray request_outputs = initial_offer.getRequestOutputs();
		    	        for(int i = 0; i < request_outputs.size(); i++) {
		    	        	JSONObject dict = (JSONObject) request_outputs.get(i);
		    	        	if (dict.containsKey("exclude_from_fee")) {
		    	        		request_outputs.remove(i);
		    	        		
		    	        		totalChangeValueLeftToConsume = SharedCoin.getIntegerFromIntegerString(dict, "value");
		    	    	        
		                        totalValueLeftToConsume = totalValueLeftToConsume.subtract(totalChangeValueLeftToConsume);
		                        break;
		    	        	}
		    	        }

		                //offer.offered_outpoints = initial_offer.offered_outpoints.slice(0);
		    	        offer.setOfferedOutpoints(offered_outpoints.clone());
		    	        
		    	        initial_offer.clearOfferedOutpoints();
		    		}
		    		
		            totalValueLeftToConsume = totalValueLeftToConsume.subtract(fee_each_repetition[ii]);
		            final double splitValues[] = new double[] {10, 5, 1, 0.5, 0.3, 0.1};
		            final int maxSplits = 8;

		    		SecureRandom random = new SecureRandom();
				    double rand = random.nextDouble();

				    int minSplits;
		            if (totalValueLeftToConsume.intValue() >= 0.2*SATOSHI) {
		            	minSplits = 2;
		                if (rand >= 0.5) {
		                    minSplits = 3;
		                }
		            } else {
		            	minSplits = 1;
		            }

		            BigInteger changeValue = BigInteger.ZERO;
		            int changePercent = 100;

		            if (totalChangeValueLeftToConsume.compareTo(BigInteger.ZERO) < 0) {
		    			throw new Exception("totalChangeValueLeftToConsume < 0");
		            } else if (totalChangeValueLeftToConsume.compareTo(BigInteger.ZERO) > 0) {
		                changeValue = totalChangeValueLeftToConsume.divide(BigInteger.valueOf(100)).multiply(BigInteger.valueOf(changePercent));
		            }
		            
		            Log.d("SharedCoin", "SharedCoin changeValue " + changeValue.toString());		

		            if (changeValue.compareTo(BigInteger.valueOf(getMinimumOutputValue())) <= 0
		            		|| totalChangeValueLeftToConsume.subtract(changeValue).compareTo(BigInteger.valueOf(getMinimumOutputValue())) <= 0) {
		                changeValue = totalChangeValueLeftToConsume;
		                totalChangeValueLeftToConsume = BigInteger.ZERO;
		            } else {
		                totalChangeValueLeftToConsume = totalChangeValueLeftToConsume.subtract(changeValue);
		            }

		            Log.d("SharedCoin", "SharedCoin totalChangeValueLeftToConsume " + totalChangeValueLeftToConsume);		

		            if (totalChangeValueLeftToConsume.compareTo(BigInteger.ZERO) < 0) {
		    			throw new Exception("totalChangeValueLeftToConsume < 0");
		            }

		            BigInteger totalValue = totalValueLeftToConsume.add(totalChangeValueLeftToConsume);
		            boolean outputsAdded = false;
		            for (int _i = 0; _i < 1000; ++_i) {
		                for (int j = 0; j < splitValues.length; j++) {
		                	int sK = j;
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions sK " + sK);		
			    	        double randDouble = random.nextDouble();
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions randDouble " + randDouble);		
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions splitValues[sK] " + splitValues[sK]);		
			    	        double variance = (splitValues[sK] / 100) * ((randDouble*30)-15);
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions variance " + variance);		

			    	        long tmp = Math.round((splitValues[sK] + variance) * SATOSHI);                
		                	BigInteger splitValue =  new BigInteger(Long.toString(tmp));

			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions totalValue " + totalValue);		
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions splitValue " + splitValue);		

		                	
		                	BigInteger[] valueAndRemainder = totalValue.divideAndRemainder(splitValue);
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions valueAndRemainder0 " + valueAndRemainder[0]);		
			    	        //Log.d("SharedCoin", "SharedCoin constructRepetitions valueAndRemainder1 " + valueAndRemainder[1]);		
		                    int quotient = valueAndRemainder[0].intValue();
		                    if (new BigInteger(Integer.toString(quotient)).compareTo(BigInteger.valueOf(getMaximumOfferNumberOfOutputs())) > 0) {                    
		                    //if (quotient > getMaximumOfferNumberOfOutputs()) {
		                        continue;
		                    }
		                    
		    	            //Log.d("SharedCoin", "SharedCoin quotient " + quotient);		
		    	            //Log.d("SharedCoin", "SharedCoin minSplits " + minSplits);		
		    	            //Log.d("SharedCoin", "SharedCoin maxSplits " + maxSplits);		

		                    if (quotient >= minSplits && quotient <= maxSplits) {
		                        if (valueAndRemainder[1].compareTo(BigInteger.ZERO) == 0 ||
		                        		valueAndRemainder[1].compareTo(BigInteger.valueOf(getMinimumOutputValue())) >= 0) {
		                            int[] remainderDivides = null;
		                            if (valueAndRemainder[1].compareTo(BigInteger.ZERO) > 0) {
		                                if (quotient <= 1) {
		                                    if (valueAndRemainder[1].compareTo(BigInteger.valueOf(getMinimumInputValue())) < 0 ||
		                                        valueAndRemainder[1].compareTo(BigInteger.valueOf(getMaximumOutputValue())) > 0) {
		                                        continue;
		                                    }

		                                    String new_address = this.generateAddressFromSeed();
		                                    Log.d("SharedCoin", "SharedCoin constructRepetitions new_address " + new_address);

		                            		BitcoinScript bitcoinScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(new_address));                            		
		                        		    String scriptHex = new String(Hex.encode(bitcoinScript.getProgram()));
		                                    offer.addRequestOutputs(valueAndRemainder[1].toString(), scriptHex);
		                                    
		                                } else {
				    	    	            Log.d("SharedCoin", "SharedCoin remainderDivides " + Arrays.asList(remainderDivides));
		                                    remainderDivides = divideUniformlyRandomly(valueAndRemainder[1].intValue(), quotient);
		                                }
		                            }

		    	    	            Log.d("SharedCoin", "SharedCoin remainderDivides " + Arrays.asList(remainderDivides));

		                            boolean withinRange = true;
		                            for (int iii  = 0; iii < quotient; ++iii) {

		                            	BigInteger value = splitValue;
		                            	if (remainderDivides != null && remainderDivides.length > iii) {
			                            	if (remainderDivides[iii] > 0) {
			                                    value = value.add(BigInteger.valueOf(remainderDivides[iii]));
			                                }
		                            	}
		                            	
		                                if (value.compareTo(BigInteger.valueOf(getMinimumInputValue())) < 0 ||
		                                    value.compareTo(BigInteger.valueOf(getMaximumOutputValue())) > 0) {
		                                    withinRange = false;
		                                    break;
		                                }
		                            }

		                            if (!withinRange) {
		                                continue;
		                            }

		                            for (int iii = 0; iii < quotient; ++iii) {
		                                String new_address = this.generateAddressFromSeed();

		                                BigInteger value = splitValue;
		                            	if (remainderDivides != null && remainderDivides.length > iii) {
			                                if (remainderDivides[iii] > 0) {
			                                    value = value.add(BigInteger.valueOf(remainderDivides[iii]));
			                                }
		                            	}
		                            	
		                        		BitcoinScript bitcoinScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(new_address));                            		
		                    		    String scriptHex = new String(Hex.encode(bitcoinScript.getProgram()));
		                                offer.addRequestOutputs(value.toString(), scriptHex);
		                            }

		                            outputsAdded = true;

		                            break;
		                        }
		                    }    
		                }
		                if (outputsAdded)
		                    break;
		            }

		            Log.d("SharedCoin", "SharedCoin constructRepetitions 6 ");		

		            if (!outputsAdded) {
		                String new_address = this.generateAddressFromSeed();

		        		BitcoinScript bitcoinScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(new_address));                            		
		    		    String scriptHex = new String(Hex.encode(bitcoinScript.getProgram()));
		                offer.addRequestOutputs(totalValue.toString(), scriptHex);
		            }
		            Log.d("SharedCoin", "SharedCoin constructRepetitions 7 ");		

		            if (changeValue.compareTo(BigInteger.ZERO) > 0) {
		            	String change_address = generateChangeAddress();

		                if (changeValue.compareTo(BigInteger.valueOf(getMinimumOutputValueExcludeFee())) < 0)
		                    throw new Exception("Change Value Too Small 0 (" + changeValue.toString() + "< " + getMinimumOutputValueExcludeFee() + ")");

		        		BitcoinScript bitcoinScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(change_address));                            		
		    		    String scriptHex = new String(Hex.encode(bitcoinScript.getProgram()));
		                offer.addRequestOutputs(changeValue.toString(), scriptHex, true);
		            }

		            Log.d("SharedCoin", "SharedCoin constructRepetitions 8 ");		

		            this.offers.add(offer);
		        }
		        
	            Log.d("SharedCoin", "SharedCoin totalChangeValueLeftToConsume " + totalChangeValueLeftToConsume);		

		        if (totalChangeValueLeftToConsume.compareTo(BigInteger.ZERO) > 0) {
		        	String change_address = generateChangeAddress();

		            if (totalChangeValueLeftToConsume.compareTo(BigInteger.valueOf(getMinimumOutputValueExcludeFee())) < 0)
		                throw new Exception("Change Value Too Small 1 (" + totalChangeValueLeftToConsume.toString() + " < " + getMinimumOutputValueExcludeFee()+ ")");

		    		BitcoinScript bitcoinScript = BitcoinScript.createSimpleOutBitoinScript(new BitcoinAddress(change_address));                            		
				    String scriptHex = new String(Hex.encode(bitcoinScript.getProgram()));
				    initial_offer.addRequestOutputs(totalChangeValueLeftToConsume.toString(), scriptHex, true);
		        }

		        this.offers.add(initial_offer);
	        	Log.d("SharedCoin", "SharedCoin this.offers.size() " + this.offers.size());		

	        	if (SHARED_COIN_DEBUG) {
	        		this.offers.clear();
	        		Offer offer1 = new Offer(this.sharedCoin);
	        		offer1.setOfferID(0L);
	        		offer1.addOfferedOutpoint("6e659f6cb8a160c55e65d589c85bdbaa9cd0b6bfb2e68939f5ad68ffa6fe9c84", 0, "12507377");
	        		offer1.addRequestOutputs("1380521", "76a914d363c62f93406334cd8aadae68f85d104d04e8c788ac");
	        		offer1.addRequestOutputs("8669479", "76a914998484121e9ea5af4fef79f5b55d036156f5e43e88ac");
	        		offer1.addRequestOutputs("2407377", "76a914931070cb85ecdc8ae1150d4a1128decd493cd49588ac", true);
	        		this.offers.add(offer1);
	        		Offer offer2 = new Offer(this.sharedCoin);
	        		offer2.setOfferID(0L);
	        		offer2.addRequestOutputs("10000000", "76a91472ef9da834b88115510dd35e808f9e0d0c67e9c488ac");
	        		this.offers.add(offer2);
	        	}
	        	
	        	
	        	//*
	        	//debug code
		        Log.d("SharedCoin", "SharedCoin initial_offer " + initial_offer.getOffer().toString());	
		        for (Offer offer : this.offers) {
		        	Log.d("SharedCoin", "SharedCoin self.offers " + offer.getOffer().toString());		
		        	JSONArray getRequestOutputs = offer.getRequestOutputs();
		        	BigInteger sum1 = BigInteger.ZERO;
		        	
		        	for (int i = 0; i < getRequestOutputs.size(); i ++) {
		        		JSONObject obj = (JSONObject) getRequestOutputs.get(i);
		        		BigInteger value = SharedCoin.getIntegerFromIntegerString(obj, "value");
			        	Log.d("SharedCoin", "SharedCoin RequestOutput value " + obj.get("value"));		
			        	sum1 = sum1.add(value);
		        	}
		        	sum1 = sum1.add(fee_each_repetition[0]);

		        	BigInteger sum2 = BigInteger.ZERO;
		        	JSONArray getOfferedOutpoints = offer.getOfferedOutpoints();
		        	for (int i = 0; i < getOfferedOutpoints.size(); i ++) {
		        		JSONObject obj = (JSONObject) getOfferedOutpoints.get(i);
		        		BigInteger value = SharedCoin.getIntegerFromIntegerString(obj, "value");
			        	Log.d("SharedCoin", "SharedCoin OfferedOutpoint value " + obj.get("value"));		
			        	sum2 = sum2.add(value);
		        	}
		        	
		        	Log.d("SharedCoin", "SharedCoin sum1 " + sum1);		
		        	Log.d("SharedCoin", "SharedCoin sum2 " + sum2);		
		        }
		        //*/
		        
		        objectSuccessCallback.onSuccess(this);
			} catch (Exception e) {
	            Log.d("SharedCoin", "SharedCoin constructRepetitions Exception e " + e.getLocalizedMessage());		
				objectSuccessCallback.onFail(e.getLocalizedMessage());
			}
		}
		
		
		
		private void _success(int ii, JSONArray outpoints_to_offer_next_stage, final ObjectSuccessCallback objectSuccessCallback) {
            ii++;
        	Log.d("SharedCoin", "SharedCoin Executing Stage_success ii " + ii);		
        	Log.d("SharedCoin", "SharedCoin Executing Stage_success this.n_stages " + this.n_stages);		
        	Log.d("SharedCoin", "SharedCoin Executing Stage_success this.c_stage " + this.c_stage);		

        	if (ii < this.n_stages) {
                //Connect the outputs created from the previous stage to the inputs to use this stage
            	this.offers.get(ii).setOfferedOutpoints(outpoints_to_offer_next_stage); 

            	Log.d("SharedCoin", "SharedCoin Executing Stage _success outpoints_to_offer_next_stage: " + outpoints_to_offer_next_stage.toString());		
            	Log.d("SharedCoin", "SharedCoin Executing Stage _success self.offers: " + this.offers.toArray().toString());		

                execStage(ii, objectSuccessCallback);
            } else if (ii == this.n_stages) {
            	Log.d("SharedCoin", "SharedCoin Executing Stage _success ii == self.n_stages");		
            	objectSuccessCallback.onSuccess(null);
            }
		}
		
		public void execStage(final int ii, final ObjectSuccessCallback objectSuccessCallback) {
            this.c_stage = ii;
            final Offer offerForThisStage = this.offers.get(ii);
        	Log.d("SharedCoin", "SharedCoin Executing Stage " + ii);		
        	this.executeOffer(offerForThisStage, new ObjectSuccessCallback() {

				@Override
				public void onSuccess(Object obj) {
					JSONArray outpoints_to_offer_next_stage = (JSONArray) obj;
					_success(ii, outpoints_to_offer_next_stage, objectSuccessCallback);
				}

				@Override
				public void onFail(final String error) {					
		        	Log.d("SharedCoin", "SharedCoin executeOffer onFail " + error);		
	        		SharedCoin.setTimeout(5000);		
	            	Log.d("SharedCoin", "SharedCoin executeOffer failed " + error);		
					Plan.this.executeOffer(offerForThisStage, new ObjectSuccessCallback() {

						@Override
						public void onSuccess(Object obj) {
							JSONArray outpoints_to_offer_next_stage = (JSONArray) obj;
							_success(ii, outpoints_to_offer_next_stage, objectSuccessCallback);									
						}

						@Override
						public void onFail(String error) {
							objectSuccessCallback.onFail(error);
						}
					});						
				}
        	});
		}
		
		public void execute(final ObjectSuccessCallback objectSuccessCallback) {
            List<String> additional_seeds = remoteWallet.getAdditionalSeeds();
            Log.d("SharedCoin", "SharedCoin execute additional_seeds.size() " + additional_seeds.size());
            final String seed = SharedCoin.SEED_PREFIX + this.address_seed;
            //double check to see if see seed exist in payload, it can fail to exist if payload gets reset b4 seed is synced.
            if (! additional_seeds.contains(seed)) {
            	Log.d("SharedCoin", "SharedCoin execute check to add seed ");
            	remoteWallet.addAdditionalSeeds(seed);
            }
        	
            //debug code, use to clear seed list so recoverSeeds is shorter, dont actually use in production
            //remoteWallet.clearAdditionalSeeds();
            
			this.sharedCoin.application.saveWallet(new SuccessCallback() {
				@Override
				public void onSuccess() {
					EventListeners.invokeWalletDidChange();
	            	Log.d("SharedCoin", "SharedCoin Saved Wallet ");		

                    List<String> additional_seeds = remoteWallet.getAdditionalSeeds();
                    
                    for (String additional_seed : additional_seeds)
                        Log.d("SharedCoin", "SharedCoin Saved Wallet additional_seed " + additional_seed);		

                    Log.d("SharedCoin", "SharedCoin Saved Wallet additional_seeds.size() " + additional_seeds.size());		

                    //Log.d("SharedCoin", "SharedCoin Saved Wallet additional_seeds " + additional_seeds.toString());		
	            	Log.d("SharedCoin", "SharedCoin Saved Wallet Plan.this.address_seed " + SharedCoin.SEED_PREFIX + Plan.this.address_seed);		

	            	boolean found = false;
	            	if (additional_seeds.contains(SharedCoin.SEED_PREFIX + Plan.this.address_seed)) {
		            	Log.d("SharedCoin", "SharedCoin Saved Wallet additional_seeds contains " + Plan.this.address_seed);
                        found = true;
                    }
                    
                    if (! found) {
    	            	Log.d("SharedCoin", "SharedCoin Address Seed not found even after wallet sync");		
                    	objectSuccessCallback.onFail("Address Seed Not Found");
                    } else {
    	            	Log.d("SharedCoin", "SharedCoin execStage ");		
                        execStage(0, objectSuccessCallback);
                    }
				}

				@Override
				public void onFail() {
					objectSuccessCallback.onFail("Error saving wallet");
				}
			});
			
		}
		
		public void executeOffer(final Offer offer, final ObjectSuccessCallback objectSuccessCallback) {
			offer.submit(new SharedCoinSuccessCallback() {
				@Override
				public void onSuccess(Object obj) {
		        	Log.d("SharedCoin", "SharedCoin Successfully Submitted Offer");		
					offer.pollForProposalID(new ObjectSuccessCallback() {
						
						@Override
						public void onSuccess(Object obj) {
							final BigInteger proposalID = (BigInteger) obj;
				        	Log.d("SharedCoin", "SharedCoin Proposal ID " + proposalID);		
							offer.getProposal(proposalID, new SharedCoinSuccessCallback() {

								@Override
								public void onSuccess(Object obj) {
						        	Log.d("SharedCoin", "SharedCoin Got Proposal");		
						        	final JSONObject proposal = (JSONObject)obj;
						        	offer.checkProposal(proposal, new ObjectSuccessCallback() {

										@Override
										public void onSuccess(Object obj) {
								        	Log.d("SharedCoin", "SharedCoin Proposal Looks Good");		
											final Transaction tx = (Transaction)obj;
											offer.signInputs(proposal, tx, new ObjectSuccessCallback() {

												@Override
												public void onSuccess(Object obj) {
										        	Log.d("SharedCoin", "SharedCoin Inputs Signed");		
										        	final JSONArray signatures = (JSONArray)obj;
													offer.submitInputScripts(proposal, signatures, new SharedCoinSuccessCallback() {

														@Override
														public void onSuccess(
																Object obj) {
												        	Log.d("SharedCoin", "SharedCoin Submitted Input Scripts");		
												        	pollForCompleted(proposalID, new SharedCoinSuccessCallback() {

																@Override
																public void onSuccess(
																		Object obj) {																	
														        	Log.d("SharedCoin", "SharedCoin pollForCompleted onSuccess");		
																}

																@Override
																public void onFail(
																		String error) {
														        	Log.d("SharedCoin", "SharedCoin pollForCompleted onFail " + error);		
																	objectSuccessCallback.onFail(error);																														
																}

																@Override
																public void onComplete(
																		String txHash,
																		String tx) {
														        	Log.d("SharedCoin", "SharedCoin pollForCompleted onComplete " + txHash);		
																	complete(offer, txHash, tx, objectSuccessCallback);																													
																}
												        		
												        	});					
														}

														@Override
														public void onFail(
																String error) {
															objectSuccessCallback.onFail(error);																														
														}

														@Override
														public void onComplete(
																String txHash,
																String tx) {
															complete(offer, txHash, tx, objectSuccessCallback);																		
														}
													});
												}

												@Override
												public void onFail(String error) {
													objectSuccessCallback.onFail(error);																														
												}
											});
										}

										@Override
										public void onFail(String error) {
											objectSuccessCallback.onFail(error);																		
										}					        		
						        	});
								}

								@Override
								public void onFail(String error) {
									objectSuccessCallback.onFail(error);								
								}

								@Override
								public void onComplete(String txHash, String tx) {
									complete(offer, txHash, tx, objectSuccessCallback);												
								}
							});					
						}

						@Override
						public void onFail(String error) {
							objectSuccessCallback.onFail(error);
						}					
					});
				}

				@Override
				public void onFail(String error) {
					objectSuccessCallback.onFail(error);
				}

				@Override
				public void onComplete(String txHash, String tx) {
					complete(offer, txHash, tx, objectSuccessCallback);				
				}
			});
		}
	}
	
	
    private static JSONObject getOfferID(final long offerID) {
    	
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("version", VERSION);
		params.put("method", "get_offer_id");
		params.put("offer_id", offerID);		
		params.put("format", "json");		

		try {
			if (! SHARED_COIN_DEBUG) {
				String response = WalletUtils.postURLWithParams(SHARED_COIN_ENDPOINT, params);				
		    	JSONObject obj = (JSONObject) new JSONParser().parse(response);
		    	Log.d("SharedCoin", "SharedCoin request get_offer_id " + obj.toString());		
				return obj;
			} else {
				return SharedCoin.getOfferIDDebugReturnObject();
			}			
		} catch (Exception e) {
        	Log.d("SharedCoin", "SharedCoin getOfferID Exception " + e.getLocalizedMessage());		
			e.printStackTrace();
		}
		return null;
    }
	
    private static void getProposalID(final BigInteger proposalID, final long offerID, ObjectSuccessCallback objectSuccessCallback) {
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("version", VERSION);
		params.put("method", "get_proposal_id");
		params.put("proposal_id", proposalID.longValue());		
		params.put("offer_id", offerID);		
		params.put("format", "json");		

		try {
			if (! SHARED_COIN_DEBUG) {
				String response = WalletUtils.postURLWithParams(SHARED_COIN_ENDPOINT, params);
		    	JSONObject obj = (JSONObject) new JSONParser().parse(response);
		    	Log.d("SharedCoin", "SharedCoin request get_proposal_id " + obj.toString());		
		    	objectSuccessCallback.onSuccess(obj);
			} else {
				objectSuccessCallback.onSuccess(SharedCoin.getProposalIDDebugReturnObject());
			}
		} catch (Exception e) {
        	Log.d("SharedCoin", "SharedCoin getProposalID Exception " + e.getLocalizedMessage());		
			objectSuccessCallback.onFail(e.getLocalizedMessage());
			e.printStackTrace();
		}		
	}
	
	private static JSONObject pollForProposalCompleted(final BigInteger proposalID) {
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("version", VERSION);
		params.put("method", "poll_for_proposal_completed");
		params.put("proposal_id", proposalID.longValue());		
		params.put("format", "json");		

		try {
			if (! SHARED_COIN_DEBUG) {
				String response = WalletUtils.postURLWithParams(SHARED_COIN_ENDPOINT, params);
		    	JSONObject obj = (JSONObject) new JSONParser().parse(response);
		    	Log.d("SharedCoin", "SharedCoin request poll_for_proposal_completed " + obj.toString());	
				return obj;			
			} else {	    	
				return SharedCoin.pollForProposalCompletedDebugReturnObject();
			}
		} catch (Exception e) {
        	Log.d("SharedCoin", "SharedCoin pollForProposalCompleted Exception " + e.getLocalizedMessage());		
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static JSONObject submitOffer(final JSONObject offer, final String token, final BigInteger feePercent, final String offerMaxAge) throws Exception {
		Map<Object, Object> params = new HashMap<Object, Object>();
    	Log.d("SharedCoin", "SharedCoin submitOffer offer.toString " + offer.toString());		
    	Log.d("SharedCoin", "SharedCoin submitOffer token " + token);		
    	
		params.put("version", VERSION);
		params.put("method", "submit_offer");
		params.put("token", token);		
		params.put("format", "json");		
		//if (feePercent != null) params.put("fee_percent", feePercent);	
		//if (offerMaxAge != null) params.put("offer_max_age", offerMaxAge);			
		params.put("offer", offer);	

		try {
			if (! SHARED_COIN_DEBUG) {
				String response = WalletUtils.postURLWithParams(SHARED_COIN_ENDPOINT, params);			
		    	Log.d("SharedCoin", "SharedCoin request submit_offer response " + response);		

				JSONObject obj = (JSONObject) new JSONParser().parse(response);
		    	Log.d("SharedCoin", "SharedCoin request submit_offer " + obj.toString());		
				return obj;
			} else {
				return SharedCoin.submitOfferDebugReturnObject();
			}			
		} catch (Exception e) {
        	Log.d("SharedCoin", "SharedCoin submitOffer Exception " + e.getLocalizedMessage());		
			e.printStackTrace();
		}
		return null;
	}

	private static void submitSignatures(final long proposalID, final long offerID, final JSONArray inputScripts, ObjectSuccessCallback objectSuccessCallback) {
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("version", VERSION);
		params.put("method", "submit_signatures");
		params.put("proposal_id", proposalID);		
		params.put("offer_id", offerID);		
		params.put("input_scripts", inputScripts);		
		params.put("format", "json");		

		try {
			if (! SHARED_COIN_DEBUG) {
				String response = WalletUtils.postURLWithParams(SHARED_COIN_ENDPOINT, params);
		    	JSONObject obj = (JSONObject) new JSONParser().parse(response);
		    	Log.d("SharedCoin", "SharedCoin request submit_signatures " + obj.toString());		
		    	objectSuccessCallback.onSuccess(obj);
			} else {
				objectSuccessCallback.onSuccess(SharedCoin.submitSignaturesDebugReturnObject());
			}
		} catch (Exception e) {
        	Log.d("SharedCoin", "SharedCoin submitSignatures Exception " + e.getLocalizedMessage());		
			objectSuccessCallback.onFail(e.getLocalizedMessage());
			e.printStackTrace();
		}		
	}
	
	public void getInfo() throws Exception {
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("version", VERSION);
		params.put("method", "get_info");
		params.put("format", "json");		
		String response = WalletUtils.postURLWithParams(SHARED_COIN_ENDPOINT, params);
		info = (JSONObject) new JSONParser().parse(response);
        Log.d("SharedCoin", "SharedCoin request get_info " + info.toString());
    }
	
	
	
	private static JSONObject getOfferIDDebugReturnObject() {
		int returnChoice = 1;
		JSONObject obj = new JSONObject();
		if (returnChoice == 1) {
			long proposal_id = 98237649241957L;
			
			obj.put("proposal_id", proposal_id);				
			obj.put("status", STATUS_ACTIVE_PROPOSAL);							
		} else {
			obj.put("status", STATUS_WAITING);							
		}
		
		return obj;
	}
	
	private static JSONObject getProposalIDDebugReturnObject() {
		String tx_hex = "010000000a15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d0100000000ffffffff78396f7aaf464e7a6f55d0512c3d84528eaf3f4bddfae11df469ee87bd09a9032100000000ffffffff92642cb19c9452224ab2094df8f8db0b5bb5a09196f400a7dfa9f47ce99281260d00000000ffffffff15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d0700000000ffffffff849cfea6ff68adf53989e6b2bfb6d09caadb5bc889d5655ec560a1b86c9f656e0000000000ffffffffade0e052ac3aae6440a836809381dfeabf76fbc01364a2a4ee0fabe0ae4afe6f0a00000000ffffffff15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d0500000000ffffffff4e046d1a59f0fdd2c916942d73f8194b18935939ac3b5f1c56b00d59335a41021200000000ffffffff77ffce18ab121eda835b84a1b7206ac84bfabfc28a2330105f4125ac706f98ae0000000000ffffffff05e9f0a1c8a5e8181aacc5d1971d97b43e05eb099ba7336e8d8c78919d7b4e2d0400000000ffffffff1196556e00000000001976a91435071d5261f1b99c815d12009e4fa14d7fa2488388acb0094600000000001976a914490d274fb93434bb9b2367779b77b153eb9c442488ac20505300000000001976a914fe850333caeaadba1e6682ecc9b9bae4c7cfd1e588aca9ca6f00000000001976a914969c3d4c0d4de45faba213264de3979b95497a1888acb0094600000000001976a914e2b59bc7a23d1ddefcdc4031287d2706d816a33288ac208c4900000000001976a914a09c2e8fd6b2e69ac309ace730d6f901c4bc1fcc88ac208c4900000000001976a914c59f457c4715631a973b8fb888cb4a77134e5baf88acf84a4e00000000001976a91416812cd6ebf94bd0299f16ba4c8b2f74611bcefd88ac0ec04f00000000001976a9145378df64160ad1f879c1643e6c128b8bf3a2168288ac81c56a00000000001976a914cffc87b6bb211801a3b78a12f906b505c1fd74ec88ac27498400000000001976a914998484121e9ea5af4fef79f5b55d036156f5e43e88acf1476e00000000001976a914f14799f957c6a2eadac288a7967c1744f5db91a788acb0cd4f00000000001976a9143f4782d7a3aeb8b0dd5487e502bd0392ff9443ea88aca9101500000000001976a914d363c62f93406334cd8aadae68f85d104d04e8c788acd80e4b00000000001976a9142dd0e6f44019949806fe15b2e49c637a97fc9dba88ac76841d00000000001976a9148234791e754096891b76e2f616b936430b750e9588acd1bb2400000000001976a914931070cb85ecdc8ae1150d4a1128decd493cd49588ac00000000";
		String connected_script = "76a9144b43401df52008c0076e4a9bb139aba9a72b365088ac";
		long proposal_id = 98237649241957L;
		long tx_input_index = 4L;
		long offer_outpoint_index = 0L;
		
		JSONObject p = new JSONObject();		
		p.put("tx", tx_hex);
		p.put("proposal_id", proposal_id);
		p.put("status", STATUS_SIGNATURES_NEEDED);

		JSONObject signature = new JSONObject();
		signature.put("connected_script", connected_script);
		signature.put("tx_input_index", tx_input_index);
		signature.put("offer_outpoint_index", offer_outpoint_index);
												
		JSONArray signature_requests = new JSONArray();
		signature_requests.add(signature);
    	p.put("signature_requests", signature_requests);
		return p;
	}
	
	

	private static JSONObject pollForProposalCompletedDebugReturnObject() {
		int returnChoice = 3;
		
		JSONObject obj = new JSONObject();
		if (returnChoice == 1) {
			obj.put("status", STATUS_WAITING);							
		} else if (returnChoice == 2) {
			obj.put("status", STATUS_NOT_FOUND);							
		} else if (returnChoice == 3) {
			String tx = "010000000a15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d0100000000ffffffff78396f7aaf464e7a6f55d0512c3d84528eaf3f4bddfae11df469ee87bd09a9032100000000ffffffff92642cb19c9452224ab2094df8f8db0b5bb5a09196f400a7dfa9f47ce99281260d00000000ffffffff15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d0700000000ffffffff849cfea6ff68adf53989e6b2bfb6d09caadb5bc889d5655ec560a1b86c9f656e0000000000ffffffffade0e052ac3aae6440a836809381dfeabf76fbc01364a2a4ee0fabe0ae4afe6f0a00000000ffffffff15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d0500000000ffffffff4e046d1a59f0fdd2c916942d73f8194b18935939ac3b5f1c56b00d59335a41021200000000ffffffff77ffce18ab121eda835b84a1b7206ac84bfabfc28a2330105f4125ac706f98ae0000000000ffffffff05e9f0a1c8a5e8181aacc5d1971d97b43e05eb099ba7336e8d8c78919d7b4e2d0400000000ffffffff1196556e00000000001976a91435071d5261f1b99c815d12009e4fa14d7fa2488388acb0094600000000001976a914490d274fb93434bb9b2367779b77b153eb9c442488ac20505300000000001976a914fe850333caeaadba1e6682ecc9b9bae4c7cfd1e588aca9ca6f00000000001976a914969c3d4c0d4de45faba213264de3979b95497a1888acb0094600000000001976a914e2b59bc7a23d1ddefcdc4031287d2706d816a33288ac208c4900000000001976a914a09c2e8fd6b2e69ac309ace730d6f901c4bc1fcc88ac208c4900000000001976a914c59f457c4715631a973b8fb888cb4a77134e5baf88acf84a4e00000000001976a91416812cd6ebf94bd0299f16ba4c8b2f74611bcefd88ac0ec04f00000000001976a9145378df64160ad1f879c1643e6c128b8bf3a2168288ac81c56a00000000001976a914cffc87b6bb211801a3b78a12f906b505c1fd74ec88ac27498400000000001976a914998484121e9ea5af4fef79f5b55d036156f5e43e88acf1476e00000000001976a914f14799f957c6a2eadac288a7967c1744f5db91a788acb0cd4f00000000001976a9143f4782d7a3aeb8b0dd5487e502bd0392ff9443ea88aca9101500000000001976a914d363c62f93406334cd8aadae68f85d104d04e8c788acd80e4b00000000001976a9142dd0e6f44019949806fe15b2e49c637a97fc9dba88ac76841d00000000001976a9148234791e754096891b76e2f616b936430b750e9588acd1bb2400000000001976a914931070cb85ecdc8ae1150d4a1128decd493cd49588ac00000000";			
			String tx_hash = "6e659f6cb8a160c55e65d589c85bdbaa9cd0b6bfb2e68939f5ad68ffa6fe9c84";
			
			obj.put("tx", tx);
			obj.put("tx_hash", tx_hash);
			obj.put("status", STATUS_COMPLETE);							
		} else {
			obj.put("status", "unkown status test");							
		}
		return obj;
	}

	private static JSONObject submitOfferDebugReturnObject() {
		long offer_id = 85839170277513L;
		
		JSONObject obj = new JSONObject();
		obj.put("offer_id", offer_id);
		return obj;
	}
	
	private static JSONObject submitSignaturesDebugReturnObject() {
//		int returnChoice = 3;
		int returnChoice = 4;
		
		JSONObject obj = new JSONObject();				
		if (returnChoice == 1) {
			obj.put("status", STATUS_NOT_FOUND);			
		} else if (returnChoice == 2) {
			obj.put("status", STATUS_VERIFICATION_FAILED);							
		} else if (returnChoice == 3) {
			String tx_hash = "62d668e2c4587808e6b68aa761d64190b4c57167f6979bed454a0aa16cee0d96";
			String tx = "010000000a15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d010000006b483045022100b2f98f8d86efd4f47ee21ce64de531ef9961a9570d9406b87a3ab34474a75df402200b3503f6452973115ce92047a79cc4050d997e5fc4a3ec96bb05b88b2159227f0121030583fd5e8891a280b678c9f949d436766c723d5b14e93989feadb289992ac9f9ffffffff78396f7aaf464e7a6f55d0512c3d84528eaf3f4bddfae11df469ee87bd09a903210000008c493046022100d1a3dcf3f3b70777617dd2b64f448e90900766c080cdc488adb8356a0710d795022100cceab90ce2b7d50facdeba0fd07fc2cbe8822836351fe058874457b1238de3810141041b7acbadaa9ef5df75d0772d260de67db2f6432ada05ac758775b1c252e6b585f5c9a30783f75cf5e27e51067af034485849d37fea7a2e73e0dee478df1890a2ffffffff92642cb19c9452224ab2094df8f8db0b5bb5a09196f400a7dfa9f47ce99281260d0000008b4830450221008ef3aa6195e2e43583e46e1cf753583a192f665b8453cbccec7c4214bdfdf2a7022077b173d3639c37f1af6389512ea83e764e5c2fce4c741989eee60d71e033522b0141046f9c23459694d53445c4a4aa3131537347e5bd4080ad7a5de77e5d6b4246464eb74c2ccce9d07436fba2d0f4846c1aa6c7a89eb75d577bb24bdd6af334853f20ffffffff15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d070000008b48304502205acf3ba874d7a06d604cfcc6cdb64efa30f0fbe18fb0de54c608ea5fff4f418b022100bfffa4ee2f9f5f556dcf5fcdb188f9b717630893d43d74ecbdc3d5fcbc29c605014104eb6a9068f79818181f76a2b33b3d318ce5342deaf4aa1623cbcbec079945a4a495d2a60e8d6c875a29fbf405245368578fa833d7774daef25223ce300710cf0dffffffff849cfea6ff68adf53989e6b2bfb6d09caadb5bc889d5655ec560a1b86c9f656e000000008c493046022100b89a97ca74cfb4bd315a02c991d3491bfd7e50248f877fbced95712f22449643022100a65228bf6eed45895a309ec64e8e6e0f9d7e023c6779bd7e57239f8f2c6f94ee01410450eac3564aaf354e031721236eae4281ad9f36e2dc55c47249356cf347d01bc2ae945356e619994029c7a7d1876e7b424e6fa8b39b37a5259e3d976cd51cb798ffffffffade0e052ac3aae6440a836809381dfeabf76fbc01364a2a4ee0fabe0ae4afe6f0a0000006c49304602210096ba2fdcb2df9b15e7e33d7f2ea1cb8ce480935bd9e2220fc421850a25363e92022100b658bb2857393dfa8ab2fafe84a1c430f8b12b7c54801d8a29d844398f70a96501210223896a55142c179362e5d8039773265677371188e492a4367d9c11a1a5ff805fffffffff15714bbf9daf68562d5fa08bf0307fc4379393bf2c4c9e2a25b9612a6b478c7d05000000694630430220598ea8d6ef4ae6ea0c9d4a60f93daed4142787f01ac7f85979cdb8588b306ff8021f1eaefd2fe59ed5f35846654166cb63ddde211e9795ec2b3549cb680392262e01210212aeaca10ad318a08fa44e04e0c216ac7b5767bc75b4e24bffb4bc390f6fa57affffffff4e046d1a59f0fdd2c916942d73f8194b18935939ac3b5f1c56b00d59335a4102120000006a4730440220251e873c8cb3886d28727653022c46041973ad311413bc5f73a1533979514b30022062683914a5e5329622ea11f23a2d3d4becd505024203fda2449838eb1f6e49910121027f71bb43eeff86d934dc6a38ac1cc168b16f8295702d4da97d3f037223ae5b47ffffffff77ffce18ab121eda835b84a1b7206ac84bfabfc28a2330105f4125ac706f98ae000000008b483045022077bdab615964e2803cc1aa6b87d8c1b673da3703ee77c667fb8ea1534a594fe80221008c1214557b103bfe2dc2538c24e968ed36bbb44b49f5305d845a28e6736025d8014104e492997da7ad25a7a06cdc0a11bfccedef08d149de6a97bb2ab456ca305338bbba9d5986aa33ccd6670e4cbb7edd46b1ebbdaced18c30192d20812cbee01bf20ffffffff05e9f0a1c8a5e8181aacc5d1971d97b43e05eb099ba7336e8d8c78919d7b4e2d040000008b483045022044030a58a9974b41695ed71de9b9c688463ea4d595ca2b433862a4387d387d45022100eb6649762fa891aaa584eb6cc53bd5b6ed75ae63e57ee0ab1a2d1cc2174abcf5014104aaee7c7b62ad2f83d3a0b6c09f348090eba218958eed7910919ab019e0a7803d88b337fb9fa98febeee94698d5dc6c9c2b3dc2d5afba9a4197b6980cca35ffe3ffffffff1196556e00000000001976a91435071d5261f1b99c815d12009e4fa14d7fa2488388acb0094600000000001976a914490d274fb93434bb9b2367779b77b153eb9c442488ac20505300000000001976a914fe850333caeaadba1e6682ecc9b9bae4c7cfd1e588aca9ca6f00000000001976a914969c3d4c0d4de45faba213264de3979b95497a1888acb0094600000000001976a914e2b59bc7a23d1ddefcdc4031287d2706d816a33288ac208c4900000000001976a914a09c2e8fd6b2e69ac309ace730d6f901c4bc1fcc88ac208c4900000000001976a914c59f457c4715631a973b8fb888cb4a77134e5baf88acf84a4e00000000001976a91416812cd6ebf94bd0299f16ba4c8b2f74611bcefd88ac0ec04f00000000001976a9145378df64160ad1f879c1643e6c128b8bf3a2168288ac81c56a00000000001976a914cffc87b6bb211801a3b78a12f906b505c1fd74ec88ac27498400000000001976a914998484121e9ea5af4fef79f5b55d036156f5e43e88acf1476e00000000001976a914f14799f957c6a2eadac288a7967c1744f5db91a788acb0cd4f00000000001976a9143f4782d7a3aeb8b0dd5487e502bd0392ff9443ea88aca9101500000000001976a914d363c62f93406334cd8aadae68f85d104d04e8c788acd80e4b00000000001976a9142dd0e6f44019949806fe15b2e49c637a97fc9dba88ac76841d00000000001976a9148234791e754096891b76e2f616b936430b750e9588acd1bb2400000000001976a914931070cb85ecdc8ae1150d4a1128decd493cd49588ac00000000";

			obj.put("status", STATUS_COMPLETE);							
			obj.put("tx_hash", tx_hash);							
			obj.put("tx", tx);												
		} else if (returnChoice == 4) {
			obj.put("status", STATUS_SIGNATURES_ACCEPTED);							
		} else {
			obj.put("status", "unkown status test");							
		}
		return obj;
	}
}
