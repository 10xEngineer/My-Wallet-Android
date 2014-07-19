package piuk.blockchain.android;

import com.google.bitcoin.core.Sha256Hash;

public class MyBlock {
	public int height;
	public long time;
	public Sha256Hash hash;
	public int blockIndex;

	public long getTime() {
		return time;
	}
	
	public int getHeight() {
		return height;
	}

	public Sha256Hash getHash() {
		return hash;
	}
}