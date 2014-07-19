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

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
import com.google.bitcoin.core.Utils;

import java.io.Serializable;

public class BitcoinAddress implements Serializable {
	private static final long serialVersionUID = 1L;
	private final Hash hash160;
	private final short version;
	private transient String _toStringCache;

	public BitcoinAddress(String humanReadable) throws AddressFormatException {
		byte[] tmp = Base58.decodeChecked(humanReadable);

		version = (short) (tmp[0] & 0xFF);

		byte[] bytes = new byte[tmp.length - 1];

		System.arraycopy(tmp, 1, bytes, 0, tmp.length - 1);

		this.hash160 = new Hash(bytes);
	}

	@Override
	public String toString() {
		if (_toStringCache == null) {
			int length = hash160.getBytes().length;

			byte[] addressBytes = new byte[1 + length + 4];

			addressBytes[0] = (byte) version;

			System.arraycopy(hash160.getBytes(), 0, addressBytes, 1, length);
			byte[] check = Utils.doubleDigest(addressBytes, 0, length + 1);
			System.arraycopy(check, 0, addressBytes, length + 1, 4);

			_toStringCache = Base58.encode(addressBytes);
		}

		return _toStringCache;
	}

	// from pubKey
	public BitcoinAddress(byte[] pubKey) {
		this.hash160 = new Hash(Utils.sha256hash160(pubKey));
		this.version = 0;
	}

	public short getVersion() {
		return version;
	}

	public short getDBType() {
		if (version == 5)
			return -2;
		else
			return 0;
	}

	public BitcoinAddress(Hash hash, short version)
			throws AddressFormatException {
		this.hash160 = hash;
		this.version = version;
	}

	public Hash getHash160() {
		return hash160;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hash160 == null) ? 0 : hash160.hashCode());
		result = prime * result + version;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		BitcoinAddress other = (BitcoinAddress) obj;
		if (hash160 == null) {
			if (other.hash160 != null)
				return false;
		} else if (!hash160.equals(other.hash160))
			return false;
		if (version != other.version)
			return false;
		return true;
	}
}