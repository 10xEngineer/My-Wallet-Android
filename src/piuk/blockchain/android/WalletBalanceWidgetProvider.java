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

import info.blockchain.wallet.ui.BlockchainUtil;
import info.blockchain.wallet.ui.MainActivity;
import info.blockchain.wallet.ui.ObjectSuccessCallback;
import info.blockchain.wallet.ui.PinEntryActivity;

import java.math.BigInteger;
import java.util.List;

import org.json.simple.JSONObject;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.WalletActivity;

/**
 * @author Andreas Schildbach
 */
public class WalletBalanceWidgetProvider extends AppWidgetProvider {
	final public static String ACTION_WIDGET_MERCHANT_DIRECTORY ="piuk.blockchain.android.intent.action.ACTION_WIDGET_MERCHANT_DIRECTORY";
	final public static String ACTION_WIDGET_SCAN_RECEIVING ="piuk.blockchain.android.intent.action.ACTION_WIDGET_SCAN_RECEIVING";
	final public static String ACTION_WIDGET_REFRESH_BALANCE ="piuk.blockchain.android.intent.action.ACTION_WIDGET_REFRESH_BALANCE";
	final public static String ACTION_WIDGET_BALANCE_SCREEN ="piuk.blockchain.android.intent.action.ACTION_WIDGET_BALANCE_SCREEN";

	private BigInteger balance = BigInteger.ZERO;
	
	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.wallet_balance_widget_content);
		updateBalance(context, views);
		updateRemoteViews(context, appWidgetIds, views, balance);
	}
	
	public static void updateRemoteViews(final Context context, final int[] appWidgetIds, RemoteViews views, BigInteger balance) {
		for (int i = 0; i < appWidgetIds.length; i++) {
			final int appWidgetId = appWidgetIds[i];
			updateViewItems(context, views, balance);
			AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);
		}
	}
	
	public static void updateViewItems(Context context, RemoteViews views, BigInteger balance) {
		views.setTextViewText(R.id.widget_wallet_balance, BlockchainUtil.formatBitcoin(balance));
		registerButtons(context, views);
	}
	
	public static void registerButtons(Context context, RemoteViews remoteViews) {
		remoteViews.setOnClickPendingIntent(R.id.scan_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_SCAN_RECEIVING));
		
		remoteViews.setOnClickPendingIntent(R.id.refresh_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_REFRESH_BALANCE));

		remoteViews.setOnClickPendingIntent(R.id.merchant_directory_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_MERCHANT_DIRECTORY));

		remoteViews.setOnClickPendingIntent(R.id.logo_button,
                buildButtonPendingIntent(context, ACTION_WIDGET_BALANCE_SCREEN));

	}
	
	public void updateBalance(final Context context, final RemoteViews remoteViews) {
		try {
			final WalletApplication application = (WalletApplication) context.getApplicationContext();

			if (application.getRemoteWallet() == null) {
//				balance = BigInteger.ZERO;				
				final List<String> activeAddresses = application.getSharedPrefsActiveAddresses();
				if (activeAddresses != null) {
					application.getBalances(activeAddresses.toArray(new String[activeAddresses.size()]), false, new ObjectSuccessCallback() {
						@Override
						public void onSuccess(Object obj) {
							long totalBalance = 0;
							JSONObject results = (JSONObject) obj;
							for (final String address : activeAddresses) {
						        JSONObject addressDict = (JSONObject) results.get(address);
								totalBalance += (Long) addressDict.get("final_balance");
							}
							
							updateViewItems(context, remoteViews, BigInteger.valueOf(totalBalance));
							WalletBalanceWidgetProvider.pushWidgetUpdate(context.getApplicationContext(), remoteViews);
						}

						@Override
						public void onFail(String error) {
						}
					});
				}
			} else {
				balance = application.getRemoteWallet().getFinal_balance();
				remoteViews.setTextViewText(R.id.widget_wallet_balance, BlockchainUtil.formatBitcoin(balance));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public static PendingIntent buildButtonPendingIntent(Context context, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

	public static void pushWidgetUpdate(Context context, RemoteViews remoteViews) {
		ComponentName myWidget = new ComponentName(context,	WalletBalanceWidgetProvider.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(myWidget, remoteViews);
	}

	@Override
	public void onReceive(Context context, Intent intent) {		
		String action = intent.getAction();

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.wallet_balance_widget_content);

		if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_MERCHANT_DIRECTORY)) {
			WalletApplication application = (WalletApplication)context.getApplicationContext();
			boolean isPassPinScreen = application.getIsPassedPinScreen();
			final Intent navigateIntent;
			if (isPassPinScreen) {
				navigateIntent = new Intent(context, MainActivity.class);
			} else {
				if (application.isGeoEnabled()) {
					navigateIntent = new Intent(context, info.blockchain.merchant.directory.MapActivity.class);					
				} else {
					navigateIntent = new Intent(context, PinEntryActivity.class);					
				}
			}
			
            navigateIntent.putExtra("navigateTo", "merchantDirectory");            
            remoteViews.setOnClickPendingIntent(R.id.widget_frame,
                            PendingIntent.getActivity(context, 0, navigateIntent, 0));            
            navigateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(navigateIntent);
            
		} else if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_SCAN_RECEIVING)) {
			boolean isPassPinScreen = ((WalletApplication)context.getApplicationContext()).getIsPassedPinScreen();
			boolean isScanning = ((WalletApplication)context.getApplicationContext()).getIsScanning();
			if (! isScanning) {
				final Intent navigateIntent;
				if (isPassPinScreen) {
					navigateIntent = new Intent(context, MainActivity.class);
				} else {
					navigateIntent = new Intent(context, PinEntryActivity.class);
				}
				
	            navigateIntent.putExtra("navigateTo", "scanReceiving");            
	            remoteViews.setOnClickPendingIntent(R.id.widget_frame,
	                            PendingIntent.getActivity(context, 0, navigateIntent, 0));            
	            navigateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            context.startActivity(navigateIntent);     
			}     
		} else if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_BALANCE_SCREEN)) {
			boolean isPassPinScreen = ((WalletApplication)context.getApplicationContext()).getIsPassedPinScreen();
			final Intent navigateIntent;
			if (isPassPinScreen) {
				navigateIntent = new Intent(context, MainActivity.class);
			} else {
				navigateIntent = new Intent(context, PinEntryActivity.class);
			}
			
            remoteViews.setOnClickPendingIntent(R.id.widget_frame,
                            PendingIntent.getActivity(context, 0, navigateIntent, 0));            
            navigateIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(navigateIntent);    		
		} else if (action.equals(WalletBalanceWidgetProvider.ACTION_WIDGET_REFRESH_BALANCE)) {
			updateBalance(context, remoteViews);
		} else if (action.equals(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_DISABLED)) {
			updateBalance(context, remoteViews);
		} else if (action.equals(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			updateBalance(context, remoteViews);
		} else {
			updateBalance(context, remoteViews);
		}

		// re-registering for click listener
		updateViewItems(context, remoteViews, balance);
		WalletBalanceWidgetProvider.pushWidgetUpdate(context.getApplicationContext(), remoteViews);
	}
}
