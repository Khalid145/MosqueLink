package com.khalid.mosquelink;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.khalid.mosquelink.object.Mosque;
import com.khalid.mosquelink.util.JsonParser;
import com.khalid.mosquelink.util.Server;
import com.khalid.mosquelink.window.MosqueDetailWindow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

public class MainFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {


    private static final String TAG = MainActivity.class.getSimpleName();

    SplashActivity splashActivity = new SplashActivity();
    JSONArray allMosqueTransferred;
    JSONObject jsonObject;

    EditText mEnteredLocation;

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters


    TextView tvMosqueName, tvAddress;

    static String rawAddress;

    Mosque mosque = new Mosque();

    static double lat;
    static double lng;

    GoogleMap mGoogleMap;
    MapView mMapView;
    View mView;
    UiSettings mUiSettings;

    Marker marker;

    ProgressDialog progressDialog;


    // JSON Node names
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_Coordinates = "mosques";
    private static final String TAG_ID = "id";
    private static final String TAG_MOSQUE_NAME = "mosque_name";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";

    static int firstTime;

    ProgressDialog pd;

    String address;
    String city;
    String postalCode;
    TextView tvMoreInfo;



    public MainFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main, container, false);
        return mView;
    }

    public void showProgressDialog() {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Loading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AdView adView = mView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        mEnteredLocation = (EditText) mView.findViewById(R.id.enteredLocation);
        mEnteredLocation.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);

                    searchLocation();
                    return true;
                }
                return false;
            }
        });

        if (googleServicesAvailable()) {
            firstTime = 0;
            threadA.start();
            initMap();
        }
    }

    Thread threadA = new Thread(new Runnable(){
        public void run(){
            buildGoogleApiClient();
            createLocationRequest();
        }
    }, "Thread A");

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance().getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(getActivity());
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(getActivity(), isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(getActivity(), "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;

    }

    private void initMap() {
        showProgressDialog();
        mMapView = mView.findViewById(R.id.map);
        mMapView.onCreate(null);
        mMapView.onResume();
        mMapView.getMapAsync(this);
        progressDialog.dismiss();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        try {
            MapsInitializer.initialize(getContext());
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        mGoogleMap = googleMap;
        //mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.my_map_style));
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mGoogleMap.setMyLocationEnabled(true);
        mUiSettings = mGoogleMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(53.471305, -2.243249), 5.0f));


        Toast.makeText(getActivity(), "Trying to retrieve your location.....", Toast.LENGTH_LONG).show();
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getActivity(), "Unable to retrieve your location. Please search manually.", Toast.LENGTH_LONG).show();
            addMosqueMarkers();
            return;
        }

        addMosqueMarkers();
    }

    public void addMosqueMarkers() {

        allMosqueTransferred = splashActivity.getAllMosques();

        if (allMosqueTransferred != null) {
            for (int i = 0; i < allMosqueTransferred.length(); i++) {
                try {
                    jsonObject = allMosqueTransferred.getJSONObject(i);
                    mosque.setId(Integer.parseInt(jsonObject.getString(TAG_ID)));

                    String cleanMosqueName = jsonObject.getString(TAG_MOSQUE_NAME);

                    String uncleanMosqueName = cleanMosqueName;
                    String searchBracket = "(";

                    if ( uncleanMosqueName.toLowerCase().indexOf(searchBracket.toLowerCase()) != -1 ) {

                        cleanMosqueName = cleanMosqueName.replaceAll("\\(.*?\\) ?", "");

                    } else {

                    }

                    mosque.setMosque_name(cleanMosqueName);
                    mosque.setLatitude(jsonObject.getDouble(TAG_LATITUDE));
                    mosque.setLongitude(jsonObject.getDouble(TAG_LONGITUDE));

                    mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(mosque.getLatitude(), mosque.getLongitude()))
                            .title(mosque.getMosque_name())
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mosque_icon))
                            .snippet(String.valueOf(mosque.getId())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //Handle case of no products or failure
        }
        completeMapReady();
    }

    public void searchLocation(){
        try {
            geoLocate(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void completeMapReady() {

        ImageView searchButton = mView.findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        mGoogleMap.setOnInfoWindowClickListener(this);

        if (mGoogleMap != null) {
            mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(final Marker marker) {

                    String id = marker.getSnippet();
                    View v = getActivity().getLayoutInflater().inflate(R.layout.info_window, null);

                    if (id.equals("searched")){
                        v = getActivity().getLayoutInflater().inflate(R.layout.searched_info_window, null);
                    }

                    pd = new ProgressDialog(getActivity());
                    pd.setMessage("Loading...");
                    //pd.show();

                    tvAddress =  v.findViewById(R.id.tv_address);
                    tvMosqueName = v.findViewById(R.id.tv_mosque_name);
                    tvMoreInfo = v.findViewById(R.id.tv_moreInfo);


                    final Handler handler = new Handler(){
                        @Override
                        public void handleMessage(Message msg) {

                            pd.dismiss();
                        }
                    };



                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            LatLng ll = marker.getPosition();

                            Geocoder geocoder;
                            List<Address> addresses = null;
                            geocoder = new Geocoder(getActivity(), Locale.getDefault());

                            try {
                                addresses = geocoder.getFromLocation(ll.latitude, ll.longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            try {
                                address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                                city = addresses.get(0).getLocality();
                                postalCode = addresses.get(0).getPostalCode();

                                handler.sendEmptyMessage(0);
                            }catch (NullPointerException | IndexOutOfBoundsException e){
                                e.printStackTrace();
                            }
                        }
                    };

                    r.run();

                    tvMosqueName.setText(marker.getTitle());

                    if (marker.getSnippet().equals("searched")) {
                        tvMoreInfo.setText("");


                    } else {
                        tvMoreInfo.setText("Click for more information.");


                    }
                    rawAddress = address + ", " + city + ", " + postalCode;

                    tvAddress.setText("Address: \n" + address + ", " + city + ", " + postalCode + ".");

                    return v;
                }
            });
        }

    }

    private void goToLocationZoom(double lat, double lng, float zoom){
        LatLng ll = new LatLng(lat,lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,zoom);
        mGoogleMap.animateCamera(update);
    }


    public void geoLocate(View.OnClickListener view) throws IOException {

        if(marker != null){
            marker.remove();
        }

        String location = mEnteredLocation.getText().toString();


        Geocoder gc = new Geocoder(getActivity());
        List<Address> list = gc.getFromLocationName(location, 1);
        Address address = list.get(0);

        lat = address.getLatitude();
        lng = address.getLongitude();
        goToLocationZoom(lat,lng,15);

        mGoogleMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title(location)
                .icon(BitmapDescriptorFactory.defaultMarker())
                .snippet("searched"));
    }


    @Override
    public void onInfoWindowClick(Marker marker) {

        String id = marker.getSnippet();

        if (id.equals("searched")){

        }else {


            mosque.setId(Integer.parseInt(id));
            mosque.setMosque_name(tvMosqueName.getText().toString());
            mosque.setAddress(tvAddress.getText().toString());

            String lat, lng;


            lat = String.valueOf(marker.getPosition().latitude);
            lng = String.valueOf(marker.getPosition().longitude);


            Intent in = new Intent(getActivity(), MosqueDetailWindow.class);
            in.putExtra("id", id);
            in.putExtra("mosque_name", tvMosqueName.getText().toString());
            in.putExtra("address", rawAddress);
            in.putExtra("lat", lat);
            in.putExtra("lng", lng);

            startActivity(in);
        }
    }





    @Override
    public void onStart() {
        super.onStart();
        if(firstTime == 0) {
            firstTime = 1;
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        checkPlayServices();

        // Resuming the periodic location updates
        try {
            if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
                startLocationUpdates();
                togglePeriodicLocationUpdates();
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        firstTime = 1;
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (firstTime == 0) {
            firstTime = 1;
            stopLocationUpdates();
        }
    }


    /**
     * Method to display the location on UI
     * */
    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();
            goToLocationZoom(latitude,longitude,15);

        } else {
            System.out.println("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }

    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text

            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }


    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(MainFragment.this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     * */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(getContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(),
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getActivity(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     * */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (LocationListener) this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayLocation();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Toast.makeText(getActivity(), "Location changed!",
                Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        displayLocation();
    }


    @Override
    public void onClick(View view) {

    }
}
