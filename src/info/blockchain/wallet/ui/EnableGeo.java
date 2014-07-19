package info.blockchain.wallet.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.Settings;
 
public class EnableGeo {

	public static void displayGPSPrompt(final Activity activity) {

    	final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        
    	final String action = Settings.ACTION_LOCATION_SOURCE_SETTINGS;
    
        final String message = "Enable either GPS or any other location"
            + " service to find current location.  Click OK to go to"
            + " location services settings to let you do so.";
 
        builder.setMessage(message)
            .setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int id) {
                        activity.startActivity(new Intent(action));
                        d.dismiss();
                    }
            })
            .setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int id) {
                        d.cancel();
                    }
            });

        builder.create().show();
    }
}
