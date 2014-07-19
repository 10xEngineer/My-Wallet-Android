package info.blockchain.wallet.ui;
 
public class TimeOutUtil {
	
    private static long lastPin = 0L;
    private static TimeOutUtil instance = null;

	private TimeOutUtil() { ; }
	
	public static TimeOutUtil getInstance() {
		
		if(instance == null) {
			instance = new TimeOutUtil();
		}
		
		return instance;
	}

	public void updatePin() {
		lastPin = System.currentTimeMillis();
	}

	public boolean isTimedOut() {
		return (System.currentTimeMillis() - lastPin) > (1000 * 60 * 5);
	}

}
