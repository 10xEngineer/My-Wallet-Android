package info.blockchain.wallet.ui;

import java.util.Arrays;
import java.util.Map;

import piuk.blockchain.android.MyRemoteWallet;
import piuk.blockchain.android.WalletApplication;
import piuk.blockchain.android.R;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
//import android.util.Log;

public class QRActivity extends Activity	{

	private ImageView ivQR = null;
	private TextView tvBTCAddress = null;
	private TextView tvBTCLabel = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	    setContentView(R.layout.layout_qr_dialog);

	    String btcAddress = null;
        Bundle extras = getIntent().getExtras();
        if(extras != null)	{
        	btcAddress = extras.getString("BTC_ADDRESS");
        }

    	Bitmap bm = generateQRCode(btcAddress);
	    ivQR = (ImageView)findViewById(R.id.qr);
		ivQR.setImageBitmap(bm);
	    tvBTCAddress = (TextView)findViewById(R.id.btc_address);
	    tvBTCAddress.setText(btcAddress);
	    tvBTCLabel = (TextView)findViewById(R.id.btc_label);
	    
		MyRemoteWallet wallet = WalletUtil.getInstance(this, this).getRemoteWallet();
		Map<String,String> labels = wallet.getLabelMap();
        String label = labels.get(btcAddress);
        if (label != null) {
    	    tvBTCLabel.setText(label);
        }
        else {
    	    tvBTCLabel.setVisibility(View.GONE);
        }

    }

    private Bitmap generateQRCode(String uri) {

    	final float REG_RES = 2.0f;
		Resources resources = getResources();
		float scale = resources.getDisplayMetrics().density;

        Bitmap bitmap = null;
        int qrCodeDimension = (scale <= REG_RES) ? 440 : 880;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

    	try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    	
    	return bitmap;
    }

}
