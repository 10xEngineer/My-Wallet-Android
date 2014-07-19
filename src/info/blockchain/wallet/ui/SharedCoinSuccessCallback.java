package info.blockchain.wallet.ui;

public interface SharedCoinSuccessCallback extends ObjectSuccessCallback {
	public void onComplete(String txHash, String tx);
}
