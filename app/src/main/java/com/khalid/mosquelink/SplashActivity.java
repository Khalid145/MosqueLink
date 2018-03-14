package com.khalid.mosquelink;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.khalid.mosquelink.util.JsonParser;
import com.khalid.mosquelink.util.Server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    double longitudeBest, latitudeBest;

    int isNotAvailable = 0;

    LocationManager locationManager;

    Server server = new Server();
    JsonParser jParser = new JsonParser();
    JSONObject jsonObject;
    static JSONArray allMosques;

    ProgressBar loadingProgressBar;
    TextView loadingTextView, completedTextView;
    ImageView tickImageView;

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_Coordinates = "mosques";

    private static int SPLASH_TIME_OUT = 2000;

    public static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        MobileAds.initialize(this, "ca-app-pub-4464620464028773~9960790436");

        checkLocation();

        loadingProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        loadingTextView = (TextView) findViewById(R.id.loadingTextView);
        completedTextView = (TextView) findViewById(R.id.completedTextView);
        tickImageView = (ImageView) findViewById(R.id.tickImageView);

        if (googleServicesAvailable()) {
            checkServicesEnabled();
            if (isNotAvailable == 2) {
                System.out.println();
                showAlert();
            } else {

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        int permissionCheck = ContextCompat.checkSelfPermission(SplashActivity.this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION);
                        if(permissionCheck == -1) {
                            if (ContextCompat.checkSelfPermission(SplashActivity.this,
                                    Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(SplashActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                            }
                        }else {

                            new retrieveMosqueLocations().execute();
                        }
                    }
                }, SPLASH_TIME_OUT);


            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Google Services Not Found")
                    .setMessage("Make sure Google Services is installed and up to date.")
                    .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent i = getPackageManager()
                                    .getLaunchIntentForPackage(getPackageName());
                            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(i);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the task you need to do.
                    System.out.println("YEEEEEEEEEES");
                    new retrieveMosqueLocations().execute();
                } else {
                    System.out.println("NOOOOOOOOOOO");

                        new AlertDialog.Builder(SplashActivity.this)
                                .setTitle("Location Permission Denied.")
                                .setMessage("Location permission must be allowed to continue using MosqueLink.")
                                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent i = SplashActivity.this.getPackageManager()
                                                .getLaunchIntentForPackage(SplashActivity.this.getPackageName() );
                                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(i);
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setCancelable(false)
                                .show();


                }
                return;
            }
        }
    }

    private void checkLocation(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String provider = locationManager.getBestProvider(criteria, true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(provider, 2 * 60 * 1000, 10, locationListenerBest);
    }


    private final LocationListener locationListenerBest = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeBest = location.getLongitude();
            latitudeBest = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


        public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance().getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(getApplication());
        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            // Dialog dialog = api.getErrorDialog(SplashActivity.this, isAvailable, 0);
            //dialog.show();
        } else {
            Toast.makeText(getApplication(), "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;

    }

    public void checkServicesEnabled() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if ((info == null || !info.isConnected() || !info.isAvailable())) {

            network_enabled = false;
            System.out.println("Off");
        } else {
            network_enabled = true;
            System.out.println("On");
        }


        if (!network_enabled || !gps_enabled) {
            isNotAvailable = 2;
        }

    }

    public void showAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Disabled features required")
                .setMessage("Make sure both location services and your internet connection is enabled.")
                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = getPackageManager()
                                .getLaunchIntentForPackage(getPackageName());
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }


    private class retrieveMosqueLocations extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingProgressBar.setVisibility(View.VISIBLE);
            loadingTextView.setVisibility(View.VISIBLE);

        }

        protected JSONArray doInBackground(Void... args) {

            List params = new ArrayList();

            // getting JSON string from URL
            String all_mosque_location = server.getRetrieveMosqueLocations();
            JSONObject json = jParser.makeHttpRequest(all_mosque_location, "GET", params);

            // Check your log cat for JSON reponse
            try {
                try {
                    // Checking for SUCCESS TAG
                    int success = json.getInt(TAG_SUCCESS);

                    if (success == 1) {
                        // products found
                        // Getting Array of Products
                        return json.getJSONArray(TAG_Coordinates);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (NullPointerException e) {
                e.printStackTrace();

            }
            return null;

        }

        protected void onPostExecute(JSONArray products) {
            setAllMosques(products);
            if (products != null) {
                for (int i = 0; i < products.length(); i++) {
                    try {
                        jsonObject = products.getJSONObject(i);
                        System.out.println("KKKKKKKKKKKKKKK: " + jsonObject);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                loadingTextView.setVisibility(View.INVISIBLE);


                completedTextView.setVisibility(View.VISIBLE);


            } else {
                //Handle case of no products or failure
            }
            nextActivity();
        }
    }

    public void nextActivity(){
        Intent in = new Intent(SplashActivity.this,MainActivity.class);
        startActivity(in);
        finish();
    }


    public static JSONArray getAllMosques() {
        return allMosques;
    }

    public static void setAllMosques(JSONArray allMosques) {
        SplashActivity.allMosques = allMosques;
    }
}
