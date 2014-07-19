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

package piuk.blockchain.android.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import android.util.Pair;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.DumpedPrivateKey;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.params.MainNetParams;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import piuk.blockchain.android.Hash;
import piuk.blockchain.android.MyWallet;
import piuk.blockchain.android.Constants;

import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.crypto.generators.SCrypt;
import org.spongycastle.util.encoders.Hex;

/**
 * @author Andreas Schildbach
 */
public class WalletUtils {
	public final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

	private static final int DefaultRequestRetry = 2;
	private static final int DefaultRequestTimeout = 60000;

	public static ECKey parsePrivateKey(String format, String contents, String password) throws Exception { 
		if (format.equals("sipa") || format.equals("compsipa")) {
			DumpedPrivateKey pk = new DumpedPrivateKey(MainNetParams.get(), contents);
			return pk.getKey();
		} else if (format.equals("base58")) {
			return MyWallet.decodeBase58PK(contents);
		} else if (format.equals("base64")) {
			return MyWallet.decodeBase64PK(contents);
		} else if (format.equals("hex")) {
			return MyWallet.decodeHexPK(contents);
		}else if (format.equals("bip38")) {
			return parseBIP38 (contents, password);
		} else {
			throw new Exception("Unable to handle format " + format);
		}
	}

	public static String postURLWithParams(String request, Map<Object, Object> params) throws Exception {
		StringBuffer urlParameters = new StringBuffer();
		for (Entry<Object, Object> entry : params.entrySet())  {
			urlParameters.append(entry.getKey() + "=" + URLEncoder.encode(entry.getValue().toString(), "UTF-8") + "&");
		}
		
		return postURL(request, urlParameters.toString());
	}

	public static String postURL(String request, Map<Object, Object> params) throws Exception {
		StringBuffer urlParameters = new StringBuffer();
		for (Entry<Object, Object> entry : params.entrySet())  {
			urlParameters.append(entry.getKey() + "=" + URLEncoder.encode(entry.getValue().toString(), "UTF-8") + "=");
		}
		
		return postURL(request, urlParameters.toString());
	}
	
	public static String postURL(String request, String urlParameters) throws Exception {
		String error = null;

		for (int ii = 0; ii < DefaultRequestRetry; ++ii) {
			URL url = new URL(request);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			try {
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("Accept", "application/json");
				connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

				connection.setUseCaches (false);

				connection.setConnectTimeout(DefaultRequestTimeout);
				connection.setReadTimeout(DefaultRequestTimeout);

				connection.connect();

				DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
				wr.writeBytes(urlParameters);
				wr.flush();
				wr.close();

				connection.setInstanceFollowRedirects(false);

				if (connection.getResponseCode() == 200)
					return IOUtils.toString(connection.getInputStream(), "UTF-8");
				else
					error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
				
				Thread.sleep(5000);
			} finally {
				connection.disconnect();
			}
		}

		throw new Exception("Inavlid Response " + error);
	}

	public static String getURL(String URL) throws Exception {
		URL url = new URL(URL);

		String error = null;
		
		for (int ii = 0; ii < DefaultRequestRetry; ++ii) {

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			try {
				connection.setRequestMethod("GET");
				connection.setRequestProperty("charset", "utf-8");
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

				connection.setConnectTimeout(DefaultRequestTimeout);
				connection.setReadTimeout(DefaultRequestTimeout);

				connection.setInstanceFollowRedirects(false);

				connection.connect();

				if (connection.getResponseCode() == 200)
					return IOUtils.toString(connection.getInputStream(), "UTF-8");
				else
					error = IOUtils.toString(connection.getErrorStream(), "UTF-8");
				
				Thread.sleep(5000);
			} finally {
				connection.disconnect();
			}
		}
		
		return error;
	}

	public static String detectPrivateKeyFormat(String key) throws Exception {
		// 51 characters base58, always starts with a '5'
		if (key.matches("^5[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{50}$"))
			return "sipa";

		if (key.matches("^[LK][123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{51}$"))
			return "compsipa";

		// 52 characters base58
		if (key.matches("^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{44}$") || key.matches("^[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{43}$"))
			return "base58";

		if (key.matches("^[A-Fa-f0-9]{64}$"))
			return "hex";

		if (key.matches("^[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789=+]{44}$"))
			return "base64";

		if (key.matches("^6P[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{56}$"))
			return "bip38";

		if (key.matches("^S[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{21}$") ||
				key.matches("^S[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{25}$") ||
				key.matches("^S[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{29}$") ||
				key.matches("^S[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{30}$")) {

			byte[] testBytes = SHA256(key + "?").getBytes();

			if (testBytes[0] == 0x00 || testBytes[0] == 0x01)
				return "mini";
		}

		throw new Exception("Unknown Key Format");
	}



	public static String SHA256Hex(String str) {
		try {
			return new String(Hex.encode(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}

	public static Hash SHA256(String str) {
		try {
			return new Hash(MessageDigest.getInstance("SHA-256").digest(str.getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();

			return null;
		}
	}


	public static Bitmap getQRCodeBitmap(final String url, final int size) {
		try {
			final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
			hints.put(EncodeHintType.MARGIN, 2);

			final BitMatrix result = QR_CODE_WRITER.encode(url,
					BarcodeFormat.QR_CODE, size, size, hints);

			final int width = result.getWidth();
			final int height = result.getHeight();
			final int[] pixels = new int[width * height];

			for (int y = 0; y < height; y++) {
				final int offset = y * width;
				for (int x = 0; x < width; x++) {
					pixels[offset + x] = result.get(x, y) ? Color.BLACK
							: Color.TRANSPARENT;
				}
			}

			final Bitmap bitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
			return bitmap;
		} catch (final WriterException x) {
			x.printStackTrace();
			return null;
		}
	}

	public static Editable formatAddress(final Address address,
			final int groupSize, final int lineSize) {
		return formatAddress(address.toString(), groupSize, lineSize);
	}

	public static Editable formatAddress(final String address,
			final int groupSize, final int lineSize) {
		final SpannableStringBuilder builder = new SpannableStringBuilder();

		final int len = address.length();
		for (int i = 0; i < len; i += groupSize) {
			final int end = i + groupSize;
			final String part = address.substring(i, end < len ? end : len);

			builder.append(part);
			builder.setSpan(new TypefaceSpan("monospace"), builder.length()
					- part.length(), builder.length(),
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			if (end < len) {
				final boolean endOfLine = end % lineSize == 0;
				builder.append(endOfLine ? "\n" : Constants.THIN_SPACE);
			}
		}

		return builder;
	}

	public static String formatValue(final BigInteger value) {
		return formatValue(value, "", "-");
	}

	public static String formatValue(final BigInteger value,
			final String plusSign, final String minusSign) {
		final boolean negative = value.compareTo(BigInteger.ZERO) < 0;
		final BigInteger absValue = value.abs();

		final String sign = negative ? minusSign : plusSign;

		final int coins = absValue.divide(Utils.COIN).intValue();
		final int cents = absValue.remainder(Utils.COIN).intValue();

		if (cents % 1000000 == 0)
			return String.format("%s%d.%02d", sign, coins, cents / 1000000);
		else if (cents % 10000 == 0)
			return String.format("%s%d.%04d", sign, coins, cents / 10000);
		else
			return String.format("%s%d.%08d", sign, coins, cents);
	}

	public static byte[] hash (byte[] data, int offset, int len)
	{
		try
		{
			MessageDigest a = MessageDigest.getInstance ("SHA-256");
			a.update (data, offset, len);
			return a.digest (a.digest ());
		}
		catch ( NoSuchAlgorithmException e )
		{
			throw new RuntimeException (e);
		}
	}

	public static byte[] hash (byte[] data)
	{
		return hash (data, 0, data.length);
	}

	public static ECKey parseBIP38 (String input, String password) throws Exception
	{
		byte[] store = Base58.decode(input);

		if ( store.length != 43 )
		{
			throw new Exception ("invalid key length for BIP38");
		}
		boolean ec = false;
		boolean compressed = false;
		boolean hasLot = false;
		if ( (store[1] & 0xff) == 0x42 )
		{
			if ( (store[2] & 0xff) == 0xc0 )
			{
				// non-EC-multiplied keys without compression (prefix 6PR)
			}
			else if ( (store[2] & 0xff) == 0xe0 )
			{
				// non-EC-multiplied keys with compression (prefix 6PY)
				compressed = true;
			}
			else
			{
				throw new Exception ("invalid key");
			}
		}
		else if ( (store[1] & 0xff) == 0x43 )
		{
			// EC-multiplied keys without compression (prefix 6Pf)
			// EC-multiplied keys with compression (prefix 6Pn)
			ec = true;
			compressed = (store[2] & 0x20) != 0;
			hasLot = (store[2] & 0x04) != 0;
			if ( (store[2] & 0x24) != store[2] )
			{
				throw new Exception ("invalid key");
			}
		}
		else
		{
			throw new Exception ("invalid key");
		}

		byte[] checksum = new byte[4];
		System.arraycopy (store, store.length - 4, checksum, 0, 4);
		byte[] ekey = new byte[store.length - 4];
		System.arraycopy (store, 0, ekey, 0, store.length - 4);
		byte[] hash = hash (ekey);
		for ( int i = 0; i < 4; ++i )
		{
			if ( hash[i] != checksum[i] )
			{
				throw new Exception ("checksum mismatch");
			}
		}

		if ( ec == false )
		{
			return parseBIP38NoEC (store, password, compressed);
		}
		else
		{
			return parseBIP38EC (store, password, compressed, hasLot);
		}
	}

	private static ECKey parseBIP38NoEC (byte[] store, String passphrase, boolean compressed) throws Exception
	{
		byte[] addressHash = new byte[4];
		System.arraycopy (store, 3, addressHash, 0, 4);
		byte[] derived = SCrypt.generate (passphrase.getBytes ("UTF-8"), addressHash, 16384, 8, 8, 64);
		byte[] key = new byte[32];
		System.arraycopy (derived, 32, key, 0, 32);
		SecretKeySpec keyspec = new SecretKeySpec (key, "AES");
		Cipher cipher = Cipher.getInstance ("AES/ECB/NoPadding", "BC");
		cipher.init (Cipher.DECRYPT_MODE, keyspec);
		byte[] decrypted = cipher.doFinal (store, 7, 32);
		for ( int i = 0; i < 32; ++i )
		{
			decrypted[i] ^= derived[i];
		}

		byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], decrypted);

		ECKey kp = new ECKey (new BigInteger(appendZeroByte));

		String address = null;
		if (compressed) {
			address = kp.toAddressCompressed(MainNetParams.get()).toString();
		} else {
			address = kp.toAddressUnCompressed(MainNetParams.get()).toString();
		}

		byte[] acs = hash (address.toString().getBytes ("US-ASCII"));
		byte[] check = new byte[4];
		System.arraycopy (acs, 0, check, 0, 4);
		if ( !Arrays.equals (check, addressHash) )
		{
			throw new Exception ("failed to decrpyt");
		}
		return kp;
	}

	private static ECKey parseBIP38EC (byte[] store, String passphrase, boolean compressed, boolean hasLot) throws Exception
	{
		byte[] addressHash = new byte[4];
		System.arraycopy (store, 3, addressHash, 0, 4);

		byte[] ownentropy = new byte[8];
		System.arraycopy (store, 7, ownentropy, 0, 8);

		byte[] ownersalt = ownentropy;
		if ( hasLot )
		{
			ownersalt = new byte[4];
			System.arraycopy (ownentropy, 0, ownersalt, 0, 4);
		}

		byte[] passfactor = SCrypt.generate (passphrase.getBytes ("UTF-8"), ownersalt, 16384, 8, 8, 32);
		if ( hasLot )
		{
			byte[] tmp = new byte[40];
			System.arraycopy (passfactor, 0, tmp, 0, 32);
			System.arraycopy (ownentropy, 0, tmp, 32, 8);
			passfactor = hash (tmp);
		}

		byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], passfactor);

		ECKey kp = new ECKey (new BigInteger(appendZeroByte));

		byte[] salt = new byte[12];
		System.arraycopy (store, 3, salt, 0, 12);
		byte[] derived = SCrypt.generate (kp.getPubKeyCompressed(), salt, 1024, 1, 1, 64);
		byte[] aeskey = new byte[32];
		System.arraycopy (derived, 32, aeskey, 0, 32);

		SecretKeySpec keyspec = new SecretKeySpec (aeskey, "AES");
		Cipher cipher = Cipher.getInstance ("AES/ECB/NoPadding", "BC");
		cipher.init (Cipher.DECRYPT_MODE, keyspec);

		byte[] encrypted = new byte[16];
		System.arraycopy (store, 23, encrypted, 0, 16);
		byte[] decrypted2 = cipher.doFinal (encrypted);
		for ( int i = 0; i < 16; ++i )
		{
			decrypted2[i] ^= derived[i + 16];
		}

		System.arraycopy (store, 15, encrypted, 0, 8);
		System.arraycopy (decrypted2, 0, encrypted, 8, 8);
		byte[] decrypted1 = cipher.doFinal (encrypted);
		for ( int i = 0; i < 16; ++i )
		{
			decrypted1[i] ^= derived[i];
		}

		byte[] seed = new byte[24];
		System.arraycopy (decrypted1, 0, seed, 0, 16);
		System.arraycopy (decrypted2, 8, seed, 16, 8);
		BigInteger priv =
				new BigInteger (1, passfactor).multiply (new BigInteger (1, hash (seed))).remainder (SECNamedCurves.getByName ("secp256k1").getN ());

		kp = new ECKey (priv);

		String address = null;
		if (compressed) {
			address = kp.toAddressCompressed(MainNetParams.get()).toString();
		} else {
			address = kp.toAddressUnCompressed(MainNetParams.get()).toString();
		}

		byte[] acs = hash (address.getBytes ("US-ASCII"));
		byte[] check = new byte[4];
		System.arraycopy (acs, 0, check, 0, 4);
		if ( !Arrays.equals (check, addressHash) )
		{
			throw new Exception ("failed to decrpyt");
		}
		return kp;
	}

}
