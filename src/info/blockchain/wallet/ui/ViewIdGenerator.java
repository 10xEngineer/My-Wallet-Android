package info.blockchain.wallet.ui;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

import java.util.concurrent.atomic.AtomicInteger;

public class ViewIdGenerator {
    private static final AtomicInteger nextId = new AtomicInteger(1);

    @SuppressLint("NewApi")
    public static int generateViewId() {

        if (Build.VERSION.SDK_INT < 17) {
            for (;;) {
                final int res = nextId.get();
                int newVal = res + 1;
                if(newVal > 0x00FFFFFF) {
                    newVal = 1;
                }
                if(nextId.compareAndSet(res, newVal)) {
                    return res;
                }
            }
        } else {
            return View.generateViewId();
        }

    }
}
