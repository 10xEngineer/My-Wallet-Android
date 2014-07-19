package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.HashMap;

public class CustomSend {
	
	private HashMap<String, BigInteger> sendingAddresses;
	private BigInteger fee = null;
	private String changeAddress = null;

	public CustomSend() {
		sendingAddresses = new HashMap<String, BigInteger>();
	}

	public void addSendingAddress(String address, BigInteger amount) {
		sendingAddresses.put(address, amount);
	}
	
	public void setSendingAddresses(HashMap<String, BigInteger> addresses) {
		sendingAddresses = addresses;
	}

	public HashMap<String, BigInteger> getSendingAddresses() {
		return sendingAddresses;
	}

	public void setFee(BigInteger fee) {
		this.fee = fee;
	}

	public BigInteger getFee() {
		return fee;
	}

	public void setChangeAddress(String address) {
		changeAddress = address;
	}

	public String getChangeAddress() {
		return changeAddress;
	}

}
