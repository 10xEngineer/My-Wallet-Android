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

package piuk.blockchain.android.service;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.EventListeners;
import piuk.blockchain.android.MyTransactionOutput;
import piuk.blockchain.android.Constants;
import piuk.blockchain.android.R;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.R.drawable;
import piuk.blockchain.android.R.raw;
import piuk.blockchain.android.R.string;
import piuk.blockchain.android.ui.WalletActivity;
import piuk.blockchain.android.util.WalletUtils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;


public class WebsocketService extends android.app.Service
{
	private WalletApplication application;

	private WebSocketHandler webSocketHandler;

	Timer timer = new Timer();

	private final Handler handler = new Handler();

	private NotificationManager nm;
	private static final int NOTIFICATION_ID_CONNECTED = 0;
	private static final int NOTIFICATION_ID_COINS_RECEIVED = 1;
	private static final int NOTIFICATION_ID_COINS_SENT = 3;
	public static boolean isRunning = false;
	
	private final EventListeners.EventListener walletEventListener = new EventListeners.EventListener() {
		@Override
		public String getDescription() {
			return "Websocket Service Listener";
		}

		@Override
		public void onWalletDidChange() {
			try {
				notifyWidgets();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};

		@Override
		public void onCoinsSent(final Transaction tx, final long result)
		{
			System.out.println("onCoinsSent()");

			try {
				final MyTransactionOutput output = (MyTransactionOutput) tx.getOutputs().get(0);
				final Address to = output.getToAddress();

				notifyCoinsSent(to, BigInteger.valueOf(result).abs());

				notifyWidgets();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onCoinsReceived(final Transaction tx, final long result)
		{
			try {
				System.out.println("onCoinsReceived()");

				if (tx.getInputs() == null || tx.getInputs().size() == 0) {
					notifyCoinbaseReceived(BigInteger.valueOf(result));
				} else {
					final TransactionInput input = tx.getInputs().get(0);

					final Address from = input.getFromAddress();

					handler.post(new Runnable()
					{
						public void run()
						{
							notifyCoinsReceived(from, BigInteger.valueOf(result));

							notifyWidgets();

						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private void notifyCoinsSent(final Address to, final BigInteger amount)
	{
		System.out.println("Notify ");

		BigInteger notificationAccumulatedAmount = BigInteger.ZERO;

		notificationAccumulatedAmount = notificationAccumulatedAmount.add(amount);

		final List<Address> notificationAddresses = new LinkedList<Address>();

		if (to != null && !notificationAddresses.contains(to))
			notificationAddresses.add(to);

		final String tickerMsg = getString(R.string.notification_coins_sent_msg, WalletUtils.formatValue(amount))
				+ (Constants.TEST ? " [testnet]" : "");

		final String msg = getString(R.string.notification_coins_sent_msg, WalletUtils.formatValue(notificationAccumulatedAmount))
				+ (Constants.TEST ? " [testnet]" : "");

		final StringBuilder text = new StringBuilder();
		for (final Address address : notificationAddresses)
		{
			if (text.length() > 0)
				text.append(", ");
			text.append(address.toString());
		}

		if (text.length() == 0)
			text.append("unknown");

		text.insert(0, "To ");

		final Notification notification = new Notification(R.drawable.stat_notify_received, tickerMsg, System.currentTimeMillis());
		Intent notificationIntent = new Intent(WebsocketService.this, info.blockchain.wallet.ui.PinEntryActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(WebsocketService.this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(WebsocketService.this, msg, text, intent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = 0;
		notification.sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert);
		nm.notify(NOTIFICATION_ID_COINS_SENT, notification);

		Toast.makeText(application, tickerMsg, Toast.LENGTH_LONG).show();
	}


	private void notifyCoinbaseReceived(final BigInteger amount) {

		final Notification notification = new Notification(R.drawable.stat_notify_received, "Newly Generated Coins", System.currentTimeMillis());

		final String msg = getString(R.string.notification_coins_received_msg, WalletUtils.formatValue(amount));

		notification.setLatestEventInfo(WebsocketService.this, msg, "Newly Generated Coins",
				PendingIntent.getActivity(WebsocketService.this, 0, new Intent(WebsocketService.this, WalletActivity.class), 0));

		notification.number = 0;
		notification.sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert);

		nm.notify(NOTIFICATION_ID_COINS_RECEIVED, notification);

		Toast.makeText(application, "Newly Generated Coins Receive", Toast.LENGTH_LONG).show();

	}

	private void notifyCoinsReceived(final Address from, final BigInteger amount)
	{
		System.out.println("Notify ");

		BigInteger notificationAccumulatedAmount = BigInteger.ZERO;

		notificationAccumulatedAmount = notificationAccumulatedAmount.add(amount);

		final List<Address> notificationAddresses = new LinkedList<Address>();

		if (from != null && !notificationAddresses.contains(from))
			notificationAddresses.add(from);

		final String tickerMsg = getString(R.string.notification_coins_received_msg, WalletUtils.formatValue(amount))
				+ (Constants.TEST ? " [testnet]" : "");

		final String msg = getString(R.string.notification_coins_received_msg, WalletUtils.formatValue(notificationAccumulatedAmount))
				+ (Constants.TEST ? " [testnet]" : "");

		final StringBuilder text = new StringBuilder();
		for (final Address address : notificationAddresses)
		{
			if (text.length() > 0)
				text.append(", ");
			text.append(address.toString());
		}

		if (text.length() == 0)
			text.append("unknown");

		text.insert(0, "From ");

		final Notification notification = new Notification(R.drawable.stat_notify_received, tickerMsg, System.currentTimeMillis());
		Intent notificationIntent = new Intent(WebsocketService.this, info.blockchain.wallet.ui.PinEntryActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent intent = PendingIntent.getActivity(WebsocketService.this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(WebsocketService.this, msg, text, intent);
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = 0;
		notification.sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.alert);
		nm.notify(NOTIFICATION_ID_COINS_RECEIVED, notification);

		Toast.makeText(application, tickerMsg, Toast.LENGTH_LONG).show();
	}


	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(final Context context, final Intent intent)
		{
			final String action = intent.getAction();

			handler.post(new Runnable() {
				public void run() {
					if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
					{
						boolean hasConnectivity = !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
						
						if (hasConnectivity) {
							connectToWebsocketIfNotConnected();
						}
					}	
				}
			});
		}
	};

	public class LocalBinder extends Binder
	{
		public WebsocketService getService()
		{
			return WebsocketService.this;
		}
	}

	private final IBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(final Intent intent)
	{
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		isRunning = true;
		
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		application = (WalletApplication) getApplication();

		EventListeners.addEventListener(walletEventListener);

		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(broadcastReceiver, intentFilter);

		webSocketHandler = new WebSocketHandler(application);

		connectToWebsocketIfNotConnected();

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				handler.post(new Runnable() {
					@Override
					public void run() {
						connectToWebsocketIfNotConnected();
					}
				});
			}

		}, 10000, 20000);
	}

	public void connectToWebsocketIfNotConnected()
	{
		try {
			if (!webSocketHandler.isConnected()) {
				webSocketHandler.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			webSocketHandler.stop(); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroy()
	{
		isRunning = false;
		
		EventListeners.removeEventListener(walletEventListener);

		stop();

		unregisterReceiver(broadcastReceiver);

		handler.removeCallbacksAndMessages(null);

		handler.postDelayed(new Runnable()
		{
			public void run()
			{
				nm.cancel(NOTIFICATION_ID_CONNECTED);
			}
		}, Constants.SHUTDOWN_REMOVE_NOTIFICATION_DELAY);

		super.onDestroy();
	}

	public void notifyWidgets()
	{
		final Context context = getApplicationContext();

		// notify widgets
		final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		for (final AppWidgetProviderInfo providerInfo : appWidgetManager.getInstalledProviders())
		{
			// limit to own widgets
			if (providerInfo.provider.getPackageName().equals(context.getPackageName()))
			{
				final Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
				intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetManager.getAppWidgetIds(providerInfo.provider));
				context.sendBroadcast(intent);
			}
		}
	}
}
