package com.khalid.mosquelink.window;

        import android.Manifest;
        import android.app.Activity;
        import android.app.ProgressDialog;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.support.v4.app.ActivityCompat;
        import android.util.DisplayMetrics;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageView;
        import android.widget.TextView;

        import com.google.android.gms.ads.AdRequest;
        import com.google.android.gms.ads.AdView;
        import com.google.android.gms.maps.CameraUpdate;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.MapView;
        import com.google.android.gms.maps.MapsInitializer;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.maps.model.MarkerOptions;
        import com.khalid.mosquelink.object.Mosque;
        import com.khalid.mosquelink.util.Server;
        import com.khalid.mosquelink.R;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.util.Locale;

/**
 * Created by khalid on 16/03/2017.
 */

public class MosqueDetailWindow extends Activity implements OnMapReadyCallback {

    TextView tvMosqueName, tvAddress, tvFollowing, tvGender, tvCapacity, tvManagement, tvTelephone, tvWebsite;

    ImageView ivPhone, ivWebsite;

    Button directionBtn, incorrectBtn;

    JSONObject jsonObject;
    JSONArray jsonArray;
    String json_string;

    double lat, lng;

    Mosque mosque = new Mosque();
    Server server = new Server();

    GoogleMap mGoogleMap;
    MapView mMapView;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mosque_detail_window);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        tvMosqueName = findViewById(R.id.tv_mosque_name);
        tvAddress = findViewById(R.id.tv_address);
        tvFollowing = findViewById(R.id.tv_following);
        tvGender = findViewById(R.id.tv_gender);
        tvCapacity = findViewById(R.id.tv_capacity);
        tvManagement = findViewById(R.id.tv_management);
        tvTelephone = findViewById(R.id.tv_telephone);
        tvWebsite = findViewById(R.id.tv_website);

        ivPhone = findViewById(R.id.ivPhone);
        ivWebsite =  findViewById(R.id.ivWebsite);

        mMapView = findViewById(R.id.map);

        initMap();

        Intent intent = getIntent();
        mosque.setId(Integer.parseInt(intent.getStringExtra("id")));
        mosque.setMosque_name(intent.getStringExtra("mosque_name"));
        mosque.setAddress(intent.getStringExtra("address"));
        lat = Double.parseDouble((intent.getStringExtra("lat")));
        lng = Double.parseDouble((intent.getStringExtra("lng")));


        ivPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callNumber(tvTelephone.getText().toString());
            }
        });

        tvTelephone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callNumber(tvTelephone.getText().toString());
            }
        });

        ivWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visitWebsite();
            }
        });

        tvWebsite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                visitWebsite();
            }
        });

        directionBtn = findViewById(R.id.directionBtn);
        directionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                LatLng position = marker.getPosition();
                String uri = String.format(Locale.ENGLISH, "geo:0,0?q="+position.latitude+","+position.longitude);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                progressDialog.dismiss();
                startActivity(intent);
            }
        });

        incorrectBtn = findViewById(R.id.incorrectBtn);
        incorrectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(MosqueDetailWindow.this,IncorrectInformationWindow.class);

                String mosque_id = String.valueOf(mosque.getId());
                in.putExtra("mosque_id", mosque_id);
                in.putExtra("mosque_name", mosque.getMosque_name());
                startActivity(in);
            }
        });


        getJSON();

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((width), (height));

        Button closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public void callNumber(final String phoneNumber){
        if (tvTelephone.getText().toString().equals("Unspecified")){

        }else{
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phoneNumber, null)));
        }
    }

    public void visitWebsite(){
        if(tvWebsite.getText().toString().equals("Unavailable")) {


        }else{
            Uri uri = Uri.parse("http://" + tvWebsite.getText().toString()); // missing 'http://' will cause crashed
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    public void showProgressDialog(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    public void getJSON() {
        new BackgroundTask().execute();
    }

    class BackgroundTask extends AsyncTask<Void, Void, String> {

        String json_url, JSON_STRING;

        @Override
        protected void onPreExecute() {
            json_url = server.getRetrieveMosqueDetail() + "?id=" + mosque.getId();
            showProgressDialog();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(json_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                while ((JSON_STRING = bufferedReader.readLine()) != null) {
                    stringBuilder.append(JSON_STRING + "\n");


                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return stringBuilder.toString().trim();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                System.out.println(result);
                json_string = result;
                parseJson();
            }catch (NullPointerException e){
                e.printStackTrace();

            }

        }


    }

    public void parseJson() {
        try {
            jsonObject = new JSONObject(json_string);
            jsonArray = jsonObject.getJSONArray("server_response");
            int count = 0;
            while (count < jsonArray.length()) {
                JSONObject JO = jsonArray.getJSONObject(count);

                String cleanUrl = (JO.getString("website_url"));


                String uncleanWebsiteUrlGoogle = cleanUrl;
                String searchGoogle = "google";

                if ( uncleanWebsiteUrlGoogle.toLowerCase().indexOf(searchGoogle.toLowerCase()) != -1 ) {

                    cleanUrl = "Unavailable";

                } else {



                }

                mosque.setWebsite_url(cleanUrl);
                mosque.setFollowing(JO.getString("following"));
                mosque.setGender(JO.getString("gender"));
                mosque.setCapacity(JO.getString("capacity"));
                mosque.setTelephone(JO.getString("telephone"));
                mosque.setManagement(JO.getString("management"));


                tvMosqueName.setText(mosque.getMosque_name());
                tvAddress.setText(mosque.getAddress());

                String websiteUrl = mosque.getWebsite_url();
                System.out.println("KKKKKKKKKKKKKKKKK: " + websiteUrl);

                mosque.setWebsite_url(websiteUrl.replaceAll("^http://", ""));
                //mosque.setWebsite_url(websiteUrl.replaceAll("https://", ""));

                tvWebsite.setText(mosque.getWebsite_url());
                tvFollowing.setText(mosque.getFollowing());
                tvGender.setText(mosque.getGender());
                tvCapacity.setText("" + mosque.getCapacity());
                tvTelephone.setText("" + mosque.getTelephone());
                tvManagement.setText(mosque.getManagement());

                count++;

                if (!tvWebsite.getText().toString().equals("Unavailable")){
                    ivWebsite.setVisibility(View.VISIBLE);
                }

                if (!tvTelephone.getText().toString().equals("Unspecified")){
                    ivPhone.setVisibility(View.VISIBLE);
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        progressDialog.dismiss();
    }

    private void initMap() {

        mMapView.onCreate(null);
        mMapView.onResume();
        mMapView.getMapAsync(this);
    }

    Marker marker;

    @Override
    public void onMapReady(GoogleMap googleMap) {

        MapsInitializer.initialize(this);
        mGoogleMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        goToLocationZoom(lat,lng,17);
        setMarker(mosque.getMosque_name(),lat,lng);
    }

    private void goToLocationZoom(double lat, double lng, float zoom){
        LatLng ll = new LatLng(lat,lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll,zoom);
        mGoogleMap.moveCamera(update);
    }

    private void setMarker(String locality, double lat, double lng) {
        if (marker != null) {
            marker.remove();
        }

        MarkerOptions options = new MarkerOptions()
                .title(locality)
                .position(new LatLng(lat, lng));
        marker = mGoogleMap.addMarker(options);
    }


}


