/*
application.getRemoteWallet() * Copyright 2011-2012 the original author or authors.
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

package piuk.blockchain.android.service;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;
import org.spongycastle.util.encoders.Hex;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyBlock;
import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.MyTransaction;
import piuk.blockchain.android.MyTransactionConfidence;
import piuk.blockchain.android.MyTransactionInput;
import piuk.blockchain.android.MyTransactionOutput;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.WalletApplication;

import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

import de.tavendo.autobahn.WebSocketConnection;

public class WebSocketHandler {
	final static String URL = "ws://ws."+Constants.BLOCKCHAIN_DOMAIN+"/inv";
	int nfailures = 0;
	static WalletApplication application;
	boolean isRunning = true;
	long lastConnectAttempt = 0;
	private final WebSocketConnection mConnection = new WebSocketConnection();

	public int getBestChainHeight() {
		return getChainHead().getHeight();
	}

	public MyBlock getChainHead() {

		if (application.getRemoteWallet() == null)
			return null;

		return application.getRemoteWallet().getLatestBlock();
	}

	final private EventListeners.EventListener walletEventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Websocket Listener";
		}

		@Override
		public void onWalletDidChange() {
			try {

				if (isRunning) {
					start();
				} else if (isConnected()) {
					// Disconnect and reconnect
					// To resubscribe
					subscribe();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	public WebSocketHandler(WalletApplication application) {
		this.application = application;
	}

	public void send(String message) {
		try {
			if (mConnection.isConnected())
				mConnection.sendTextMessage(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public synchronized void subscribe() {
		if (application.getRemoteWallet() == null)
			return;

		System.out.println("Websocket subscribe");

		send("{\"op\":\"blocks_sub\"}");

		send("{\"op\":\"wallet_sub\",\"guid\":\""+ application.getRemoteWallet().getGUID() + "\"}");

		String[] active = application.getRemoteWallet().getActiveAddresses();
		for (String address : active) {
			send("{\"op\":\"addr_sub\", \"addr\":\""+ address + "\"}");
		}
	}


	public boolean isConnected() {
		return  mConnection != null && mConnection.isConnected();
	}

	public void stop() {
		this.isRunning = false;

		if (mConnection.isConnected())
			mConnection.disconnect();

		EventListeners.removeEventListener(walletEventListener);
	}

	public void connect() throws URISyntaxException, InterruptedException {

		final WebSocketHandler handler = this;

		if (application.getRemoteWallet() == null)
			return;

		try {
			mConnection.connect(URL, new de.tavendo.autobahn.WebSocketHandler() {			 
				@Override
				public void onOpen() {
					handler.subscribe();

					handler.nfailures = 0;				
				}

				@Override
				public void onTextMessage(String message) {
					if (application.getRemoteWallet() == null)
						return;

					MyRemoteWallet wallet = application.getRemoteWallet();

					try {
						Map<String, Object> top = (Map<String, Object>) JSONValue.parse(message);

						if (top == null)
							return;

						String op = (String) top.get("op");

						if (op.equals("block")) {
							Map<String, Object> x = (Map<String, Object>) top.get("x");

							if (x == null)
								return;

							Sha256Hash hash = new Sha256Hash(Hex.decode((String) x
									.get("hash")));
							int blockIndex = ((Number) x.get("blockIndex")).intValue();
							int blockHeight = ((Number) x.get("height")).intValue();
							long time = ((Number) x.get("time")).longValue();

							MyBlock block = new MyBlock();

							block.height = blockHeight;
							block.hash = hash;
							block.blockIndex = blockIndex;
							block.time = time;


							if (application.getRemoteWallet() != null) {					
								application.getRemoteWallet().setLatestBlock(block);
							}

							List<MyTransaction> transactions = wallet.getMyTransactions();
							List<Number> txIndexes = (List<Number>) x.get("txIndexes");
							for (Number txIndex : txIndexes) {
								for (MyTransaction tx : transactions) {

									MyTransactionConfidence confidence = (MyTransactionConfidence) tx
											.getConfidence();

									if (tx.txIndex == txIndex.intValue()
											&& confidence.height != blockHeight) {
										confidence.height = blockHeight;
										//confidence.runListeners();
									}
								}
							}

						} else if (op.equals("utx")) {
							Map<String, Object> x = (Map<String, Object>) top.get("x");

							MyTransaction tx = MyTransaction.fromJSONDict(x);

							if (wallet.prependTransaction(tx)) {
								BigInteger result = BigInteger.ZERO;

								for (TransactionInput input : tx.getInputs()) {
									// if the input is from me subtract the value
									MyTransactionInput myinput = (MyTransactionInput) input;

									if (wallet.isAddressMine(input.getFromAddress()
											.toString())) {
										result = result.subtract(myinput.value);

										wallet.setFinal_balance(wallet.getFinal_balance()
												.subtract(myinput.value));
										wallet.setTotal_sent(wallet.getTotal_sent()
												.add(myinput.value));
									}
								}

								for (TransactionOutput output : tx.getOutputs()) {
									// if the input is from me subtract the value
									MyTransactionOutput myoutput = (MyTransactionOutput) output;

									if (wallet.isAddressMine(myoutput.getToAddress()
											.toString())) {
										result = result.add(myoutput.getValue());

										wallet.setFinal_balance(wallet.getFinal_balance().add(myoutput
												.getValue()));
										wallet.setTotal_received(wallet.getTotal_sent().add(myoutput
												.getValue()));
									}
								}

								tx.result = result;
								
								if (result.compareTo(BigInteger.ZERO) >= 0) {
									EventListeners.invokeOnCoinsReceived(tx, result.longValue());
								} else {
									EventListeners.invokeOnCoinsSent(tx, result.longValue());
								}
							}
						} else if (op.equals("on_change")) {
							String newChecksum = (String) top.get("checksum");
							String oldChecksum = wallet.getChecksum();

							System.out.println("On change " + newChecksum + " " + oldChecksum);

							if (!newChecksum.equals(oldChecksum)) {
								try {
									application.checkIfWalletHasUpdatedAndFetchTransactions(application.getRemoteWallet().getTemporyPassword());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onClose(int code, String reason) {
					++handler.nfailures;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();

			++handler.nfailures;
		}

		lastConnectAttempt = System.currentTimeMillis();

		System.out.println("WebSocket connect()");

		EventListeners.addEventListener(walletEventListener);
	}

	public void start() {

		if (lastConnectAttempt > System.currentTimeMillis()-30000)
			return; 

		this.isRunning = true;

		try {
			stop();

			connect();

		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
