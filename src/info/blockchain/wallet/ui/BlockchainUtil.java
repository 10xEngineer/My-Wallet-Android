package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.text.DecimalFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import com.google.bitcoin.core.Utils;

import piuk.blockchain.android.util.WalletUtils;

public class BlockchainUtil {

    private static BlockchainUtil instance = null;

    public static int BLOCKCHAIN_RED = 0xFFd17d7d;
    public static int BLOCKCHAIN_GREEN = 0xFF6daf50;
    
    public static String ZEROBLOCK_PACKAGE = "com.phlint.android.zeroblock";
    public static String MERCHANT_DIRECTORY_PACKAGE = "info.blockchain.merchant.directory";

//    public static String BLOCKCHAIN_DONATE = "1JArS6jzE3AJ9sZ3aFij1BmTcpFGgN86hA";
    public static String BLOCKCHAIN_DONATE = "Add New Address";
    public static String BLOCKCHAIN_DONATE2 = "Address Book Empty";

    private static double BTC_RATE = 635.0;

	private BlockchainUtil() { ; }

	public static BlockchainUtil getInstance(Context ctx) {
		
		if(instance == null) {
			instance = new BlockchainUtil();
		}
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String strCurrencyCode = prefs.getString("ccurrency", "USD");
		BTC_RATE = CurrencyExchange.getInstance(ctx).getCurrencyPrice(strCurrencyCode);

		return instance;
	}

	public static String BTC2Fiat(String btc)	{
		double val = 0.0;
		
		try	{
			val = Double.parseDouble(btc);
		}
		catch(NumberFormatException nfe)	{
			val = 0.0;
		}

		DecimalFormat df = new DecimalFormat("######0.00");
		return df.format(BTC2Fiat(val));
	}

	public static String Fiat2BTC(String fiat)	{
		double val = 0.0;
		
		try	{
			val = Double.parseDouble(fiat);
		}
		catch(NumberFormatException nfe)	{
			val = 0.0;
		}

        DecimalFormat df = new DecimalFormat("####0.0000");
		return df.format(Fiat2BTC(val));
	}

	public static double BTC2Fiat(double btc)	{
		return btc * BTC_RATE;
	}

	public static double Fiat2BTC(double fiat)	{
		return fiat / BTC_RATE;
	}

	public static void updateRate(Context context, String currency) {
		BTC_RATE = CurrencyExchange.getInstance(context).getCurrencyPrice(currency);
	}

	public static String formatBitcoin(BigInteger value) {
        DecimalFormat df = new DecimalFormat("####0.0000");
		return df.format(Double.parseDouble(WalletUtils.formatValue(value)));
	}

	public static String formatAddress(String address, int charactersToDisplay) {
		if (address.length() > charactersToDisplay+1)
			return address.substring(0,charactersToDisplay) + "...";
		else
			return address;
	}

	public static BigInteger bitcoinAmountStringToBigInteger(String amount) {
		if (isValidAmount(amount))
			return Utils.toNanoCoins(amount);
		else
			return null;
	}
	
	private static boolean isValidAmount(String amount) {
		try {
			if (amount.length() > 0) {
				final BigInteger nanoCoins = Utils.toNanoCoins(amount);
				if (nanoCoins.signum() >= 0)
					return true;
			}
		} catch (final Exception x) {
			;
		}

		return false;
	}
}
