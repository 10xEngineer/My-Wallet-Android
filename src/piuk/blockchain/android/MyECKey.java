package piuk.blockchain.android;

import com.google.bitcoin.core.*;

import java.math.BigInteger;

public class MyECKey extends ECKey {
	private static final long serialVersionUID = 1L;
	protected final String addr;
	protected final String base58;
	protected final String sharedKey;
	protected final String password;
	protected final int iterations;
	protected final boolean encrypted;

	private int tag;
	private String label;
	private ECKey _internalKey;
	private Address address;

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public MyECKey(String addr, String base58, MyWallet wallet) throws Exception {
		super((BigInteger)null, null);

		this.addr = addr;
		this.base58 = base58;
		this.sharedKey = wallet.getSharedKey();
		this.password = wallet.getTemporySecondPassword();
		this.iterations = wallet.getDoubleEncryptionPbkdf2Iterations();
		this.encrypted = wallet.isDoubleEncrypted();
		
		if (encrypted && password == null)
			throw new Exception("Cannot decrypted PK without second password");
	}

	private ECKey getInternalKey() {
				
		if (_internalKey == null) {
			try {
				if (this.encrypted) {
					this._internalKey = MyWallet.decodeBase58PK(MyWallet.decryptPK(base58, sharedKey, password, iterations));
				} else {
					this._internalKey = MyWallet.decodeBase58PK(base58);
				}	
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return _internalKey;
	}

	@Override
	public DumpedPrivateKey getPrivateKeyEncoded(NetworkParameters params) {
		return getInternalKey().getPrivateKeyEncoded(params);
	}

	@Override
	public boolean verify(byte[] data, byte[] signature) {
		return getInternalKey().verify(data, signature);
	}

	@Override
	public ECDSASignature sign(Sha256Hash input) {
		return getInternalKey().sign(input);
	}

	@Override
	public byte[] getPubKey() {
		return getInternalKey().getPubKey();
	}

	@Override
	public byte[] toASN1() {
		return getInternalKey().toASN1();
	}

	@Override
	public byte[] getPrivKeyBytes() {
		return getInternalKey().getPrivKeyBytes();
	}

	/** Gets the hash160 form of the public key (as seen in addresses). */
	public byte[] getPubKeyHash() {
		return getInternalKey().getPubKeyHash();
	}

	/** Gets the hash160 form of the public key (as seen in addresses). */
	public byte[] getCompressedPubKeyHash() {
		return getInternalKey().getPubKeyHash();
		//return getInternalKey().getCompressedPubKeyHash();
	}

	/**
	 * Gets the raw public key value. This appears in transaction scriptSigs. Note that this is <b>not</b> the same
	 * as the pubKeyHash/address.
	 */
	public byte[] getPubKeyCompressed() {
		return getInternalKey().getPubKey();
		//return getInternalKey().getPubKeyCompressed();
	}

	@Override
	public Address toAddress(NetworkParameters params) {
		try {
			if (address == null)
				address = new Address(params, addr);
			
			return address;
		} catch (AddressFormatException e) {
			e.printStackTrace();
		}

		return null;
	}

	public Address toAddressCompressed(NetworkParameters params) {
		try {
			if (address == null)
				address = new Address(params, addr);
			
			return address;
		} catch (AddressFormatException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return getInternalKey().toString();
	}

	@Override
	public String toStringWithPrivate() {
		return getInternalKey().toStringWithPrivate();
	}


	@Override
	public boolean equals(Object o) {
		if (o instanceof MyECKey)
			return this.base58.equals(((MyECKey)o).base58);
		else if (o instanceof ECKey)
			return this.getInternalKey().equals(o);

		return false;
	}

	@Override
	public int hashCode() {
		return this.getInternalKey().hashCode();
	}
}