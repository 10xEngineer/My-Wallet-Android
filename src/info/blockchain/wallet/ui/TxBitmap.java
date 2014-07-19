package info.blockchain.wallet.ui;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Matrix;
import android.util.Log;
import android.content.res.Resources;
//import android.util.Log;

public class TxBitmap {
	
	public static int SENDING = 1;
	public static int RECEIVING = 2;
	
	private static int VERTICAL_FLIP = 1;
	private static int HORIZONTAL_FLIP = 2;
	
	private static TxBitmap instance = null;
	
	private static Context context = null;
	
	private static float REG_RES = 2.0f;
	
	private List<Map.Entry<String, String>> addressValueEntryList;	

	public TxBitmap() { ; }
	
	public TxBitmap(Context ctx, List<Map.Entry<String, String>> addressValueEntryList) {
		this.setAddressValueEntryList(addressValueEntryList);

		context = ctx;
		}

	public static TxBitmap getInstance(Context ctx) {
		
		context = ctx;
		
		if(instance == null) {
			instance = new TxBitmap();
		}
		
		return instance;
	}

    public Bitmap createArrowsBitmap(int width, int type, int branches) {

    	if(type == SENDING) {
    		return tx_arrows(width, type, branches);
    	}
    	else {
    		return flip(tx_arrows(width, type, branches), HORIZONTAL_FLIP);
 	    }

    }

    public Bitmap createListBitmap(int width) {

		Resources resources = context.getResources();
		float scale = resources.getDisplayMetrics().density;
		if(scale > REG_RES) {
			width += 160;
		}

		return tx_list(width);

    }

    private Bitmap flip(Bitmap src, int type) {

    	Matrix matrix = new Matrix();

    	// vertical
    	if(type == VERTICAL_FLIP) {
    		// y = y * -1
    		matrix.preScale(1.0f, -1.0f);
    	}
    	// horizontal
 	    else {
    	   // x = x * -1
    	   matrix.preScale(-1.0f, 1.0f);
 	    }

    	return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    @SuppressLint("NewApi")
	private Bitmap tx_list(int width) {

		Resources resources = context.getResources();
		float scale = resources.getDisplayMetrics().density;

    	float fX = 10.0f;
    	float fY = (scale <= REG_RES) ? 35.0f : 42.0f;
		float vOffset = (scale <= REG_RES) ? 80.0f : 88.0f;	// down step
		float vOffsetAmount = (scale <= REG_RES) ? 26.0f : 40.0f;
		float vXtraOffset = (scale <= REG_RES) ? 1.0f : 8.0f;
		float szAddress = 15;
		float szAmount = 12;
		int bottomPadding = (scale <= REG_RES) ? 10 : 20;

		final int branches = addressValueEntryList.size();
//    	Bitmap bm = Bitmap.createBitmap(resources.getDisplayMetrics(), width, ((int)(vOffset * branches)) + bottomPadding, Config.ARGB_8888);
    	Bitmap bm = Bitmap.createBitmap(width, ((int)(vOffset * branches)) + bottomPadding, Config.ARGB_8888);
    	Canvas canvas = new Canvas(bm);

    	Paint paintAddressLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    	paintAddressLabel.setColor(0xff87cefa);
		paintAddressLabel.setTextSize((int)(szAddress * scale + 0.5f));
		paintAddressLabel.setTypeface(TypefaceUtil.getInstance(context).getRobotoTypeface());

    	Paint paintAddressBTC = new Paint(Paint.ANTI_ALIAS_FLAG);
    	paintAddressBTC.setColor(Color.GRAY);
		paintAddressBTC.setTextSize((int)(szAddress * scale + 0.5f));
		paintAddressBTC.setTypeface(TypefaceUtil.getInstance(context).getRobotoTypeface());

    	Paint paintAmount = new Paint(Paint.ANTI_ALIAS_FLAG);
    	paintAmount.setColor(Color.GRAY);
		paintAmount.setTextSize((int)(szAmount * scale + 0.5f));
		paintAddressLabel.setTypeface(TypefaceUtil.getInstance(context).getRobotoBoldTypeface());

		for(int i = 0; i < branches; i++) {
			Map.Entry<String, String> addressValueEntry = addressValueEntryList.get(i);			
			String addressLabel = addressValueEntry.getKey();
			String addressLabelConcat;
			if (scale <= REG_RES) {
				addressLabelConcat = BlockchainUtil.formatAddress(addressLabel, 7);
			} else {
				addressLabelConcat = BlockchainUtil.formatAddress(addressLabel, 15);
			}

			canvas.drawText(addressLabelConcat, fX, fY + (vOffset * i) + vXtraOffset, paintAddressLabel);
				
    		if(branches > 1) {
				canvas.drawText(addressValueEntry.getValue(), fX, fY + (vOffset * i) + vOffsetAmount + vXtraOffset, paintAmount);
    		}
		}

    	return bm;
    }

    @SuppressLint("NewApi")
	private Bitmap tx_arrows(int width, int type, int branches) {

		Resources resources = context.getResources();
		float scale = resources.getDisplayMetrics().density;

    	float fStroke = 6.0f;
    	float fRadius = 8.0f;	// filled circle at start of line
    	float fX = 15.0f;
    	float fY = (scale <= REG_RES) ? 30.0f : 36.0f;
		float hOffset = 20.0f;	// indent
		float vOffset = (scale <= REG_RES) ? 80.0f : 88.0f;	// down step
		int bottomPadding = (scale <= REG_RES) ? 10 : 20;

//    	Bitmap bm = Bitmap.createBitmap(resources.getDisplayMetrics(), width, ((int)(vOffset * branches)) + bottomPadding, Config.ARGB_8888);
    	Bitmap bm = Bitmap.createBitmap(width, ((int)(vOffset * branches)) + bottomPadding, Config.ARGB_8888);
    	Canvas canvas = new Canvas(bm);
    	
    	Paint paint = new Paint();
    	paint.setColor(type == SENDING ? BlockchainUtil.BLOCKCHAIN_RED : BlockchainUtil.BLOCKCHAIN_GREEN);
    	paint.setStrokeWidth(fStroke);
    	canvas.drawCircle(fX, fY, fRadius, paint);
    	canvas.drawLine(fX, fY, canvas.getWidth() - fX, fY, paint);
		doArrowhead(canvas, fX, fY, canvas.getWidth() - ((fX - 10.0f) - 2.0f), fY, type);
		
    	if(branches > 1) {
    		int remaining_branches = branches - 1;
    		for(int i = 0; i < remaining_branches; i++) {
            	canvas.drawLine(fX + hOffset, fY, fX + hOffset, fY + (vOffset * (i + 1)), paint);
            	canvas.drawLine(fX + hOffset, fY + (vOffset * (i + 1)), canvas.getWidth() - (fX - 10.0f), fY + (vOffset * (i + 1)), paint);
            	doArrowhead(canvas, fX + hOffset, fY + (vOffset * (i + 1)), canvas.getWidth() - ((fX - 10.0f) - 2.0f), fY + (vOffset * (i + 1)), type);
    		}
    	}

    	return bm;
    }

    private void doArrowhead(Canvas canvas, float x0, float y0, float x1, float y1, int type) {
    	
    	Paint paint = new Paint();
    	paint.setColor(type == SENDING ? BlockchainUtil.BLOCKCHAIN_RED : BlockchainUtil.BLOCKCHAIN_GREEN);
        paint.setStyle(Paint.Style.FILL);

        float deltaX = x1 - x0;
        float deltaY = y1 - y0;
        float frac = 0.065f;	// arrow head size

        float point_x_1 = x0 + ((1.0f - frac) * deltaX + frac * deltaY);
        float point_y_1 = y0 + ((1.0f - frac) * deltaY - frac * deltaX);

        float point_x_2 = x1;
        float point_y_2 = y1;

        float point_x_3 = x0 + ((1.0f - frac) * deltaX - frac * deltaY);
        float point_y_3 = y0 + ((1.0f - frac) * deltaY + frac * deltaX);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);

        path.moveTo(point_x_1, point_y_1);
        path.lineTo(point_x_2, point_y_2);
        path.lineTo(point_x_3, point_y_3);
        path.lineTo(point_x_1, point_y_1);
        path.lineTo(point_x_1, point_y_1);
        path.close();

        canvas.drawPath(path, paint);
    }

    public List<Map.Entry<String, String>> getAddressValueEntryList() {
		return addressValueEntryList;
	}

    public void setAddressValueEntryList(List<Map.Entry<String, String>> addressValueEntryList) {
		this.addressValueEntryList = addressValueEntryList;
	}

}
