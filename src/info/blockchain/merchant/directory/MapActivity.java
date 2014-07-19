package info.blockchain.merchant.directory;

import info.blockchain.wallet.ui.OnSwipeTouchListener;
import info.blockchain.wallet.ui.TypefaceUtil;

import java.text.DecimalFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.text.util.Linkify;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.net.Uri;
import android.util.Log;

import piuk.blockchain.android.R;
import piuk.blockchain.android.util.WalletUtils;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLngBounds;
//import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
//import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;

public class MapActivity extends Activity implements LocationListener	{

	private GoogleMap map = null;
	private LocationManager locationManager = null;
	private String provider = null;
    private Location location = null;
    private Location currLocation = null;
	private static final long MIN_TIME = 400;
	private static final float MIN_DISTANCE = 1000;
//	private Marker mSelf = null;
	
	private static float Z00M_LEVEL_DEFAULT = 13.0f;
	private static float Z00M_LEVEL_CLOSE = 18.0f;
	private static float Z00M_LEVEL_FAR = 10.0f;
	private float saveZ00mLevel = Z00M_LEVEL_DEFAULT;
	
	private int color_category_selected = 0xffFFFFFF;
    private int color_category_unselected = 0xffF1F1F1;
    
	private int color_cafe_selected = 0xffc12a0c;
	private int color_drink_selected = 0xffb65db1;
	private int color_eat_selected = 0xfffd7308;
	private int color_spend_selected = 0xff5592ae;
	private int color_atm_selected = 0xff4dad5c;

    private ImageView imgCafe = null;
    private LinearLayout layoutCafe = null;
    private LinearLayout dividerCafe = null;
    private ImageView imgDrink = null;
    private LinearLayout layoutDrink = null;
    private LinearLayout dividerDrink = null;
    private ImageView imgEat = null;
    private LinearLayout layoutEat = null;
    private LinearLayout dividerEat = null;
    private ImageView imgSpend = null;
    private LinearLayout layoutSpend = null;
    private LinearLayout dividerSpend = null;
    private ImageView imgATM = null;
    private LinearLayout layoutATM = null;
    private LinearLayout dividerATM = null;
    
    private TextView tvName = null;
    private TextView tvAddress = null;
//    private TextView tvCity = null;
    private TextView tvTel = null;
    private TextView tvWeb = null;
    private TextView tvDesc = null;

    private boolean cafeSelected = true;
    private boolean drinkSelected = true;
    private boolean eatSelected = true;
    private boolean spendSelected = true;
    private boolean atmSelected = true;

	private ProgressDialog progress = null;
	
	private HashMap<String,BTCBusiness> markerValues = null;
    private LatLngBounds bounds = null;
	
	private String strJSONData = null;
	public static ArrayList<BTCBusiness> btcb = null;
	
	private LinearLayout infoLayout = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ActionBar actionBar = getActionBar();
        actionBar.hide();
//        actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayOptions(actionBar.getDisplayOptions() ^ ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setLogo(R.drawable.masthead);
        actionBar.setHomeButtonEnabled(false);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF1B8AC7")));
        actionBar.show();

    	markerValues = new HashMap<String,BTCBusiness>();
    	btcb = new ArrayList<BTCBusiness>();

		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
		
		infoLayout = ((LinearLayout)findViewById(R.id.info));
		infoLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
		    public void onSwipeBottom() {
            	if(infoLayout.getVisibility() == View.VISIBLE) {
            		infoLayout.setVisibility(View.GONE);
            		map.animateCamera(CameraUpdateFactory.zoomTo(saveZ00mLevel));
            	}
		    }
		});
		infoLayout.setVisibility(View.GONE);
		
        tvName = (TextView)findViewById(R.id.tv_name);
        tvName.setTypeface(TypefaceUtil.getInstance(this).getGravityLightTypeface());
        tvAddress = (TextView)findViewById(R.id.tv_address);
        tvAddress.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
//        tvCity = (TextView)findViewById(R.id.tv_city);
//        tvCity.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
        tvTel = (TextView)findViewById(R.id.tv_tel);
        tvTel.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
        tvWeb = (TextView)findViewById(R.id.tv_web);
        tvWeb.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());
        tvDesc = (TextView)findViewById(R.id.tv_desc);
        tvDesc.setTypeface(TypefaceUtil.getInstance(this).getRobotoTypeface());

		map = ((MapFragment)getFragmentManager().findFragmentById(R.id.map)).getMap();
		map.setMyLocationEnabled(true);
        map.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
            	
                if(marker == null) {
                	return true;
                }

                if(markerValues == null || markerValues.size() < 1) {
                	return true;
                }
                
                ((LinearLayout)findViewById(R.id.row_call)).setVisibility(View.VISIBLE);
                ((LinearLayout)findViewById(R.id.row_web)).setVisibility(View.VISIBLE);

                LatLng latLng = marker.getPosition();
                                
                BTCBusiness b = markerValues.get(marker.getId());

                //
                // launch via intent: waze://?ll=<lat>,<lon>&navigate=yes
                //
                // Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=New+York+NY));
                // startActivity(i);
                //
				String url = "http://maps.google.com/?saddr=" +
	 	     	    	currLocation.getLatitude() + "," + currLocation.getLongitude() +
	 	     	     	"&daddr=" + markerValues.get(marker.getId()).lat + "," + markerValues.get(marker.getId()).lon;
                tvAddress.setText(Html.fromHtml("<a href=\"" + url + "\">" + b.address + ", " + b.city + " " + b.pcode + "</a>"));
                tvAddress.setMovementMethod(LinkMovementMethod.getInstance());

                if(b.tel != null && b.tel.length() > 0)	{
                    tvTel.setText(b.tel);
                    Linkify.addLinks(tvTel, Linkify.PHONE_NUMBERS);
                }
                else	{
                    ((LinearLayout)findViewById(R.id.row_call)).setVisibility(View.GONE);
                }
                
                if(b.web != null && b.web.length() > 0)	{
                    tvWeb.setText(b.web);
                    Linkify.addLinks(tvWeb, Linkify.WEB_URLS);
                }
                else	{
                    ((LinearLayout)findViewById(R.id.row_web)).setVisibility(View.GONE);
                }

                tvDesc.setText(b.desc);

                Double distance = Double.parseDouble(markerValues.get(marker.getId()).distance);
                String strDistance = null;
                if(distance < 1.0) {
                	distance *= 1000;
                	DecimalFormat df = new DecimalFormat("###");
                	strDistance = df.format(distance) + " meters";
                }
                else {
                	DecimalFormat df = new DecimalFormat("#####.#");
                	strDistance = df.format(distance) + " km";
                }

                tvName.setText(b.name);
    			switch(Integer.parseInt(b.hc)) {
				case BTCBusiness.HEADING_CAFE:
					tvName.setTextColor(color_cafe_selected);
					break;
				case BTCBusiness.HEADING_BAR:
					tvName.setTextColor(color_drink_selected);
					break;
				case BTCBusiness.HEADING_RESTAURANT:
					tvName.setTextColor(color_eat_selected);
					break;
				case BTCBusiness.HEADING_SPEND:
					tvName.setTextColor(color_spend_selected);
					break;
				case BTCBusiness.HEADING_ATM:
					tvName.setTextColor(color_atm_selected);
					break;
				default:
					tvName.setTextColor(color_cafe_selected);
					break;
				}

     			infoLayout.setVisibility(View.VISIBLE);
     			
     			saveZ00mLevel = map.getCameraPosition().zoom;
     			if(map.getCameraPosition().zoom < Z00M_LEVEL_CLOSE) {
         			map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), Z00M_LEVEL_CLOSE));
     			}
     			else {
         			map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), map.getCameraPosition().zoom));
     			}

            	return true;
            }
        });
        /*
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (BTCBusiness b : btcb)
                {
                    builder.include(new LatLng(Double.parseDouble(b.lat), Double.parseDouble(b.lon)));
                }
                bounds = builder.build();
                int padding = 30; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                map.moveCamera(cu);
                map.animateCamera(cu);
            }
        });
        */

	    imgCafe = ((ImageView)findViewById(R.id.cafe));
	    layoutCafe = ((LinearLayout)findViewById(R.id.layout_cafe));
	    dividerCafe = ((LinearLayout)findViewById(R.id.divider_cafe));
	    imgDrink = ((ImageView)findViewById(R.id.drink));
	    layoutDrink = ((LinearLayout)findViewById(R.id.layout_drink));
	    dividerDrink = ((LinearLayout)findViewById(R.id.divider_drink));
	    imgEat = ((ImageView)findViewById(R.id.eat));
	    layoutEat = ((LinearLayout)findViewById(R.id.layout_eat));
	    dividerEat = ((LinearLayout)findViewById(R.id.divider_eat));
	    imgSpend = ((ImageView)findViewById(R.id.spend));
	    layoutSpend = ((LinearLayout)findViewById(R.id.layout_spend));
	    dividerSpend = ((LinearLayout)findViewById(R.id.divider_spend));
	    imgATM = ((ImageView)findViewById(R.id.atm));
	    layoutATM = ((LinearLayout)findViewById(R.id.layout_atm));
	    dividerATM = ((LinearLayout)findViewById(R.id.divider_atm));
	    imgCafe.setBackgroundColor(color_category_selected);
	    layoutCafe.setBackgroundColor(color_category_selected);
	    dividerCafe.setBackgroundColor(color_cafe_selected);
	    imgDrink.setBackgroundColor(color_category_selected);
	    layoutDrink.setBackgroundColor(color_category_selected);
	    dividerDrink.setBackgroundColor(color_drink_selected);
	    imgEat.setBackgroundColor(color_category_selected);
	    layoutEat.setBackgroundColor(color_category_selected);
	    dividerEat.setBackgroundColor(color_eat_selected);
	    imgSpend.setBackgroundColor(color_category_selected);
	    layoutSpend.setBackgroundColor(color_category_selected);
	    dividerSpend.setBackgroundColor(color_spend_selected);
	    imgATM.setBackgroundColor(color_category_selected);
	    layoutATM.setBackgroundColor(color_category_selected);
	    dividerATM.setBackgroundColor(color_atm_selected);
	    
        layoutCafe.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgCafe.setImageResource(cafeSelected ? R.drawable.marker_cafe_off : R.drawable.marker_cafe);
            	dividerCafe.setBackgroundColor(cafeSelected ? color_category_unselected : color_cafe_selected);
            	cafeSelected = cafeSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutDrink.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgDrink.setImageResource(drinkSelected ? R.drawable.marker_drink_off : R.drawable.marker_drink);
            	dividerDrink.setBackgroundColor(drinkSelected ? color_category_unselected : color_drink_selected);
            	drinkSelected = drinkSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutEat.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgEat.setImageResource(eatSelected ? R.drawable.marker_eat_off : R.drawable.marker_eat);
            	dividerEat.setBackgroundColor(eatSelected ? color_category_unselected : color_eat_selected);
            	eatSelected = eatSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutSpend.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgSpend.setImageResource(spendSelected ? R.drawable.marker_spend_off : R.drawable.marker_spend);
            	dividerSpend.setBackgroundColor(spendSelected ? color_category_unselected : color_spend_selected);
            	spendSelected = spendSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

        layoutATM.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
            	imgATM.setImageResource(atmSelected ? R.drawable.marker_atm_off : R.drawable.marker_atm);
            	dividerATM.setBackgroundColor(atmSelected ? color_category_unselected : color_atm_selected);
            	atmSelected = atmSelected ? false : true;
            	drawData(false);
                return false;
            }
        });

		currLocation = new Location(LocationManager.NETWORK_PROVIDER);
        Location lastKnownByGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location lastKnownByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if(lastKnownByGps == null && lastKnownByNetwork == null) {
    		currLocation.setLatitude(0.0);
    		currLocation.setLongitude(0.0);
        }
        else if(lastKnownByGps != null && lastKnownByNetwork == null) {
        	currLocation  = lastKnownByGps;
        }
        else if(lastKnownByGps == null && lastKnownByNetwork != null) {
        	currLocation  = lastKnownByNetwork;
        }
        else {
        	currLocation = (lastKnownByGps.getAccuracy() <= lastKnownByNetwork.getAccuracy()) ? lastKnownByGps : lastKnownByNetwork;
        }

		map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), Z00M_LEVEL_DEFAULT));
		drawData(true);
	}

	@Override
	public void onLocationChanged(Location location) {

		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

		currLocation = location;
		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, map.getCameraPosition().zoom);
		map.animateCamera(cameraUpdate);
		locationManager.removeUpdates(this);
		
		drawData(true);
		setProperZoomLevel(latLng, 7, 1);

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onProviderDisabled(String provider) { }

    @Override
    public void onResume() {
    	super.onResume();

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.dir_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
    	case R.id.list_view:
//    		if(Double.parseDouble(MapActivity.btcb.get(0).distance) < 50.0) {
        		doListView();
//    		}
//    		else {
//		        Toast.makeText(MapActivity.this, "There are no nearby Bitcoin merchants to list.", Toast.LENGTH_SHORT).show();
//    		}
    		return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if(keyCode == KeyEvent.KEYCODE_BACK) {
        	if(infoLayout.getVisibility() == View.VISIBLE) {
        		infoLayout.setVisibility(View.GONE);
        	}
        	else {
        		finish();
        	}
        }

        return false;
    }

    private void drawData(final boolean fetch) {
    	
		map.clear();

		final Handler handler = new Handler(Looper.getMainLooper());

		new Thread(new Runnable() {
			@Override
			public void run() {
				
				Looper.prepare();

				try {
					if(fetch) {
						final String url = "http://192.64.115.86/cgi-bin/btcd.pl?ULAT=" + currLocation.getLatitude() + "&ULON=" + currLocation.getLongitude() + "&D=40000&K=1";
//	         			Log.d("BlockchainMerchantDirectory", url);
	         			strJSONData = WalletUtils.getURL(url);
//	         			Log.d("BlockchainMerchantDirectory", strJSONData);
					}

					handler.post(new Runnable() {
						@Override
						public void run() {

							try {
								ArrayList<BTCBusiness> tmp = null; 
								tmp = ParseData.parse(strJSONData);
								if(tmp != null && tmp.size() > 0) {
									btcb = tmp;
								}
								else {
									btcb = null;
								}
								
//								btcb = ParseData.parse(strJSONData);
								
								if(btcb != null && btcb.size() > 0) {
				         			Log.d("BlockchainMerchantDirectory", "list size=" + btcb.size());
									
//									markerValues.clear();
									
									BTCBusiness b = null;

				         			for(int i = 0; i < btcb.size(); i++) {
				         				
				         				b = btcb.get(i);

				            			BitmapDescriptor bmd = null;
				            			
				            			switch(Integer.parseInt(b.hc)) {
				            				case BTCBusiness.HEADING_CAFE:
				            					if(cafeSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_BAR:
				            					if(drinkSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_drink_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_drink);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_RESTAURANT:
				            					if(eatSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_eat_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_eat);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_SPEND:
				            					if(spendSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_spend_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_spend);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				case BTCBusiness.HEADING_ATM:
				            					if(atmSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_atm_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_atm);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				default:
				            					if(cafeSelected) {
					            					bmd = b.flag.equals("1") ? BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe_featured) : BitmapDescriptorFactory.fromResource(R.drawable.marker_cafe);
				            					}
				            					else {
					            					bmd = null;
				            					}
				            					break;
				            				}
				            			
				            			if(bmd != null) {
					         				Marker marker = map.addMarker(new MarkerOptions()
					         		        .position(new LatLng(Double.parseDouble(b.lat), Double.parseDouble(b.lon)))
					         		        .icon(bmd));
					         				
					         				markerValues.put(marker.getId(), b);
				            			}

				         			}
									
								}

							} catch (Exception e) {
								e.printStackTrace();
							}

							setProperZoomLevel(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), 40000, 1);

						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Looper.loop();

			}
		}).start();
	}

	void setProperZoomLevel(LatLng loc, int radius, int nbPoi) {

        float currentZoomLevel = 21;
        int currentFoundPoi = 0;
        LatLngBounds bounds = null;
        List<LatLng> found = new ArrayList<LatLng>();
        Location location = new Location("");
        location.setLatitude(loc.latitude);
        location.setLongitude(loc.longitude);
        
        boolean continueZooming = true;
        boolean continueSearchingInsideRadius = true;

        while (continueZooming) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, currentZoomLevel--));
            bounds = map.getProjection().getVisibleRegion().latLngBounds;
            Location swLoc = new Location("");
            swLoc.setLatitude(bounds.southwest.latitude);
            swLoc.setLongitude(bounds.southwest.longitude);
            continueSearchingInsideRadius = (Math.round(location.distanceTo(swLoc) / 100) > radius) ? false : true;
            
            for (BTCBusiness b : btcb) {
            	
            	LatLng pos = new LatLng(Double.parseDouble(b.lat), Double.parseDouble(b.lon));

            	if (bounds.contains(pos)) {
                    if (!found.contains(pos)) {
                        currentFoundPoi++;
                        found.add(pos);
//                		Toast.makeText(MapActivity.this, "Found position", Toast.LENGTH_SHORT).show();
                    }
                }

                if (continueSearchingInsideRadius) {
                    if (currentFoundPoi > nbPoi) {
                    	continueZooming = false;
                        break;
                    }
                }
                else if (currentFoundPoi > 0) {
                	continueZooming = false;
                    break;
                }
                else if (currentZoomLevel < 3) {
                	continueZooming = false;
                    break;
                }

            }
            continueZooming = ((currentZoomLevel > 0) && continueZooming) ? true : false;

        }
    }

    private void doListView() {

		boolean doList = false;

    	for(int i = 0; i < btcb.size(); i++) {
			if(Double.parseDouble(btcb.get(i).distance) < 15.0) {
				doList = true;
				break;
			}
		}

    	if(doList) {
        	Intent intent = new Intent(MapActivity.this, ListActivity.class);
        	intent.putExtra("ULAT", Double.toString(currLocation.getLatitude()));
        	intent.putExtra("ULON", Double.toString(currLocation.getLongitude()));
    		startActivity(intent);
    	}
    	else {
 			Toast.makeText(MapActivity.this, "There are no Bitcoin businesses within range for listing.", Toast.LENGTH_LONG).show();
    	}
    }

}
