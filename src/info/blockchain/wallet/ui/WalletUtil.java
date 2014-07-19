package info.blockchain.wallet.ui;

import android.content.Context;
import android.app.Activity;

import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.util.WalletUtils;

public class WalletUtil {

    private static WalletUtil instance = null;
    private static Context context = null;
    private static Activity activity = null;
	private static WalletApplication application = null;
	private static MyRemoteWallet remoteWallet = null;

	private WalletUtil() { ; }

	public static WalletUtil getInstance(Context ctx, Activity act) {
		
		if(instance == null) {
			context = ctx;
			activity = act;
			
			application = (WalletApplication)activity.getApplication();
			remoteWallet = application.getRemoteWallet();

			instance = new WalletUtil();
		}
		
		return instance;
	}

	public static WalletUtil getRefreshedInstance(Context ctx, Activity act) {
		
		context = ctx;
		activity = act;
		
		application = (WalletApplication)activity.getApplication();
		remoteWallet = application.getRemoteWallet();

		instance = new WalletUtil();
		
		return instance;
	}

	public WalletApplication getWalletApplication() {
		return application;
	}

	public MyRemoteWallet getRemoteWallet() {
		return remoteWallet;
	}

}
