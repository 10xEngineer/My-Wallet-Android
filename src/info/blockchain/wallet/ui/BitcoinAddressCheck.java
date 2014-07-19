package info.blockchain.wallet.ui;

import piuk.blockchain.android.R;

import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.uri.BitcoinURI;
import com.google.bitcoin.uri.BitcoinURIParseException;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.WrongNetworkException;

import android.util.Log;

public class BitcoinAddressCheck {
	
	private BitcoinAddressCheck() { ; }

	public static String validate(final String btcaddress) {
		
		if(isValidAddress(btcaddress)) {
			return btcaddress;
		}
		else {
			String address = clean(btcaddress);
			if(address != null) {
				return address;
			}
			else {
				return null;
			}
		}
	}

	private static String clean(final String btcaddress) {
		
		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(btcaddress);
			ret = uri.getAddress().toString();
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}
		
		return ret;
	}

	public static boolean isUri(final String s) {

		boolean ret = false;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(s);
			ret = true;
		}
		catch(BitcoinURIParseException bupe) {
			ret = false;
		}
		
		return ret;
	}

	public static String getUri(final String s) {

		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(s);
			ret = uri.toString();
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}
		
		return ret;
	}

	public static String getAddress(final String s) {

		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(s);
			ret = uri.getAddress().toString();
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}

		return ret;
	}

	public static String getAmount(final String s) {

		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(s);
			if(uri.getAmount() != null) {
				ret = uri.getAmount().toString();
			}
			else {
				ret = "0.0000";
			}
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}

		return ret;
	}

	public static boolean isValidAddress(final String btcaddress) {

		boolean ret = false;
		Address address = null;
		
		try {
			address = new Address(MainNetParams.get(), btcaddress);
			if(address != null) {
				ret = true;
			}
		}
		catch(WrongNetworkException wne) {
			ret = false;
		}
		catch(AddressFormatException afe) {
			ret = false;
		}

		return ret;
	}

}
