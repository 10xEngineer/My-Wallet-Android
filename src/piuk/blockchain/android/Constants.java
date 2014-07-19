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

import java.io.File;
import java.math.BigInteger;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.format.DateUtils;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.params.MainNetParams;

import piuk.blockchain.android.R;

/**
 * @author Andreas Schildbach
 */
public class Constants {
	public static final boolean TEST = false; // replace protected

	public static final NetworkParameters NETWORK_PARAMETERS = TEST ? NetworkParameters
			.testNet() : MainNetParams.get();
			
			public static final String WALLET_FILENAME_PROTOBUF = "wallet-protobuf";

			public static final String PREFS_KEY_BEST_CHAIN_HEIGHT_EVER = "best_chain_height_ever";
			public static final String EARLIEST_KEY_TIME = "earliest_key_time";
			public static final long REBROADCAST_TIME = 60000;
 
			public static final String BLOCKCHAIN_DOMAIN = "blockchain.info";
			
			static final String WALLET_FILENAME = "wallet.aes.json";

			static final String MULTIADDR_FILENAME = "multiaddr.cache.json";
			public static final long BLOCKCHAIN_UPTODATE_THRESHOLD_MS = DateUtils.HOUR_IN_MILLIS;

			public static final String CHECKPOINTS_FILENAME = "checkpoints";

			static final String EXCEPTION_LOG = "exception.log";
 
			static final boolean isAmazon = false;

			public static final String SENDER_ID = "789483738880";
 
			public static final String DISPLAY_MESSAGE_ACTION = "piuk.blockchain.android.DISPLAY_MESSAGE";

			public static final String TITLE = "title";
			public static final String BODY = "body";

			public final static long MultiAddrTimeThreshold = 10000; // 10 seconds
			public final static long NetworkErrorDisplayThreshold = 60000; // 60 seconds
 
			public final static long COIN = 100000000;

			public static final File EXTERNAL_WALLET_BACKUP_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

			private static final String WALLET_KEY_BACKUP_BASE58_PROD = "key-backup-base58";
			private static final String WALLET_KEY_BACKUP_BASE58_TEST = "key-backup-base58-testnet";
			public static final String WALLET_KEY_BACKUP_BASE58 = Constants.TEST ? WALLET_KEY_BACKUP_BASE58_TEST
					: WALLET_KEY_BACKUP_BASE58_PROD;

			private static final int WALLET_MODE_PROD = Context.MODE_PRIVATE;
			private static final int WALLET_MODE_TEST = Context.MODE_WORLD_READABLE
					| Context.MODE_WORLD_WRITEABLE;
			public static final int WALLET_MODE = Constants.TEST ? WALLET_MODE_TEST
					: WALLET_MODE_PROD;

			private static final String WALLET_KEY_BACKUP_SNAPSHOT_PROD = "key-backup-snapshot";
			private static final String WALLET_KEY_BACKUP_SNAPSHOT_TEST = "key-backup-snapshot-testnet";
			public static final String WALLET_KEY_BACKUP_SNAPSHOT = Constants.TEST ? WALLET_KEY_BACKUP_SNAPSHOT_TEST
					: WALLET_KEY_BACKUP_SNAPSHOT_PROD;

			private static final String BLOCKCHAIN_SNAPSHOT_FILENAME_PROD = "blockchain-snapshot.jpg";
			private static final String BLOCKCHAIN_SNAPSHOT_FILENAME_TEST = "blockchain-snapshot-testnet.jpg";
			public static final String BLOCKCHAIN_SNAPSHOT_FILENAME = Constants.TEST ? BLOCKCHAIN_SNAPSHOT_FILENAME_TEST
					: BLOCKCHAIN_SNAPSHOT_FILENAME_PROD;

			private static final String BLOCKCHAIN_FILENAME_PROD = "blockchain";
			private static final String BLOCKCHAIN_FILENAME_TEST = "blockchain-testnet";
			public static final String BLOCKCHAIN_FILENAME = TEST ? BLOCKCHAIN_FILENAME_TEST
					: BLOCKCHAIN_FILENAME_PROD;

			public static final String PEER_DISCOVERY_IRC_CHANNEL_PROD = "#bitcoin";
			public static final String PEER_DISCOVERY_IRC_CHANNEL_TEST = "#bitcoinTEST";

			private static final String PACKAGE_NAME_PROD = "piuk.blockchain.android";
			private static final String PACKAGE_NAME_TEST = "piuk.blockchain.android"
					+ '_' + "test"; // replace protected
			public static final String PACKAGE_NAME = TEST ? PACKAGE_NAME_TEST
					: PACKAGE_NAME_PROD;
 
			public static final int APP_ICON_RESID = R.drawable.app_icon;

			public static final String MIMETYPE_TRANSACTION = "application/x-btctx";

			public static final int MAX_CONNECTED_PEERS = 6;
			public static final String USER_AGENT = "Blockchain";
			public static final String DEFAULT_EXCHANGE_CURRENCY = "USD";
			public static final int WALLET_OPERATION_STACK_SIZE = 256 * 1024;
			public static final int BLOCKCHAIN_DOWNLOAD_THRESHOLD_MS = 5000;
			public static final int BLOCKCHAIN_STATE_BROADCAST_THROTTLE_MS = 1500;
			public static final int BLOCKCHAIN_UPTODATE_THRESHOLD_HOURS = 1;
			public static final int SHUTDOWN_REMOVE_NOTIFICATION_DELAY = 2000;

			public static final String CURRENCY_CODE_BITCOIN = "BTC";
			public static final String THIN_SPACE = "\u2009";
			public static final String CURRENCY_PLUS_SIGN = "+" + THIN_SPACE;
			public static final String CURRENCY_MINUS_SIGN = "-" + THIN_SPACE;
			public static final int ADDRESS_FORMAT_LINE_SIZE = 12;

			public static final String DISCLAIMER = "http://"+Constants.BLOCKCHAIN_DOMAIN+"/disclaimer";
			public static final String PRIVACY_POLICY = "http://"+Constants.BLOCKCHAIN_DOMAIN+"/privacy";

			public static final String LICENSE_URL = "http://www.gnu.org/licenses/gpl-3.0.txt";
			public static final String SOURCE_URL = "https://github.com/blockchain/My-Wallet-Android/";
			public static final String CREDITS_BITCOINJ_URL = "http://code.google.com/p/bitcoinj/";
			public static final String CREDITS_ZXING_URL = "http://code.google.com/p/zxing/";
			public static final String CREDITS_BITCON_WALLET_ANDROID = "http://code.google.com/p/bitcoin-wallet/";
			public static final String AUTHOR_TWITTER_URL = "http://twitter.com/android_bitcoin";
			public static final String AUTHOR_GOOGLEPLUS_URL = "https://profiles.google.com/andreas.schildbach";
			public static final String MARKET_APP_URL = "market://details?id=%s";
			public static final String WEBMARKET_APP_URL = isAmazon ? "http://www.amazon.com/gp/mas/dl/android?p=%s"
					: "https://play.google.com/store/apps/details?id=%s";

			public static final Intent INTENT_QR_SCANNER = new Intent(
					"com.google.zxing.client.android.SCAN").putExtra("SCAN_MODE",
							"QR_CODE_MODE");
			public static final String PACKAGE_NAME_ZXING = "com.google.zxing.client.android";

			public static final String PREFS_KEY = "general";
			public static final String PREFS_KEY_LAST_VERSION = "last_version";
			public static final String PREFS_KEY_AUTOSYNC = "autosync";
			public static final String PREFS_KEY_SELECTED_ADDRESS = "selected_address";
			public static final String PREFS_KEY_EXCHANGE_CURRENCY = "exchange_currency";
			public static final String PREFS_KEY_TRUSTED_PEER = "trusted_peer";
			public static final String PREFS_KEY_INITIATE_RESET = "initiate_reset";

			public static final BigInteger FEE_THRESHOLD_MIN = Utils
					.toNanoCoins("0.01");
}
