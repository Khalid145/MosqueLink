package com.khalid.mosquelink.window;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.khalid.mosquelink.R;
import com.khalid.mosquelink.util.Server;
import com.kosalgeek.genasync12.AsyncResponse;
import com.kosalgeek.genasync12.PostResponseAsyncTask;

import java.util.HashMap;

/**
 * Created by khalid on 16/03/2017.
 */

public class AddMosqueWindow extends Activity {

    EditText etMosqueName, etDoorNumber, etStreetName, etPostcode, etCity, etCapacity, etGender, etFollowing,
            etManagement, etTelephone, etWebsite;

    String mosqueName, doorNumber, streetName, postcode, city, capacity, gender, following, management, telephone,
            website, device_ip, device_unique_id;

    Server server = new Server();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_mosque_window);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        etMosqueName = findViewById(R.id.et_mosque_name);
        etDoorNumber = findViewById(R.id.et_doorNumber);
        etStreetName = findViewById(R.id.et_streetName);
        etPostcode = findViewById(R.id.et_postcode);
        etCity = findViewById(R.id.et_city);
        etCapacity = findViewById(R.id.et_capacity);
        etGender = findViewById(R.id.et_gender);
        etFollowing = findViewById(R.id.et_following);
        etManagement = findViewById(R.id.et_management);
        etTelephone = findViewById(R.id.et_telephone);
        etWebsite = findViewById(R.id.et_website);

        Button sendBtn = findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMosqueSuggestion();
            }
        });

        Button closeBtn = findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void sendMosqueSuggestion() {

        mosqueName = etMosqueName.getText().toString();
        doorNumber = etDoorNumber.getText().toString();
        streetName = etStreetName.getText().toString();
        postcode = etPostcode.getText().toString();
        city = etCity.getText().toString();
        capacity = etCapacity.getText().toString();
        gender = etGender.getText().toString();
        following = etFollowing.getText().toString();
        management = etManagement.getText().toString();
        telephone = etTelephone.getText().toString();
        website = etWebsite.getText().toString();

        if (etMosqueName.getText().toString().trim().equals("")){
            etMosqueName.setError("Mosque name is required!");

        } if (etStreetName.getText().toString().trim().equals("")) {
            etStreetName.setError("Street name is required!");

        } if(etPostcode.getText().toString().trim().equals("")) {
            etPostcode.setError("Postcode is required!");

        } if(etCity.getText().toString().trim().equals("")) {
            etCity.setError("City is required!");

        }else {

            if (website.equals("")){
                website = " ";
            }
            if (doorNumber.equals("")){
                doorNumber = " ";
            }
            if (capacity.equals("")){
                capacity = " ";
            }
            if (gender.equals("")){
                gender = " ";
            }
            if (following.equals("")){
                following = " ";
            }
            if (management.equals("")){
                management = " ";
            }
            if (telephone.equals("")){
                telephone = " ";
            }


            HashMap postData = new HashMap();

            postData.put("txtMosqueName", mosqueName);
            postData.put("txtDoorNumber", doorNumber);
            postData.put("txtStreetName", streetName);
            postData.put("txtPostcode", postcode);
            postData.put("txtCity", city);
            postData.put("txtCapacity", capacity);
            postData.put("txtGender", gender);
            postData.put("txtFollowing", following);
            postData.put("txtManagement", management);
            postData.put("txtTelephone", telephone);
            postData.put("txtWebsite", website);

            PostResponseAsyncTask task1 = new PostResponseAsyncTask(AddMosqueWindow.this, postData, new AsyncResponse() {
                @Override
                public void processFinish(String s) {
                    if (s.contains("success")) {
                        Toast.makeText(AddMosqueWindow.this, "Thank You. Your suggestion is under review.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(AddMosqueWindow.this, "Something went wrong! Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });

            task1.execute(server.getSendMosqueSuggestion());


        }
    }
}

