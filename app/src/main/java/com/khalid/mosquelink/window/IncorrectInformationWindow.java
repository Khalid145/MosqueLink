package com.khalid.mosquelink.window;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.khalid.mosquelink.R;
import com.khalid.mosquelink.util.Server;
import com.kosalgeek.genasync12.AsyncResponse;
import com.kosalgeek.genasync12.PostResponseAsyncTask;

import java.util.HashMap;

public class IncorrectInformationWindow extends Activity {

    TextView tvMosqueName;

    String mosqueName, mosqueID, device_ip, device_unique_id;

    EditText et_name, et_context;

    String name, context;

    Button sendBtn;

    Server server = new Server();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.incorrect_information_window);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        tvMosqueName = findViewById(R.id.tv_mosque_name);
        et_name =  findViewById(R.id.et_name);
        et_context = findViewById(R.id.et_context);

        Intent intent = getIntent();

        mosqueID=(intent.getStringExtra("mosque_id"));

        mosqueName=(intent.getStringExtra("mosque_name"));
        tvMosqueName.setText("Mosque: " + mosqueName);

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

        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendIncorrectInformation();
            }
        });


    }

    private void sendIncorrectInformation() {
        name = et_name.getText().toString();
        context = et_context.getText().toString();

        if (et_name.getText().toString().trim().equals("")) {
            et_name.setError("Name is required!");

        }
        if (et_context.getText().toString().trim().equals("")) {
            et_context.setError("This is required!");

        } else {

            HashMap postData = new HashMap();

            postData.put("mosqueID", mosqueID);
            postData.put("name", name);
            postData.put("context", context);


            PostResponseAsyncTask task1 = new PostResponseAsyncTask(IncorrectInformationWindow.this, postData, new AsyncResponse() {
                @Override
                public void processFinish(String s) {
                    if (s.contains("success")) {
                        Toast.makeText(IncorrectInformationWindow.this, "Thank You for the corrections. We will look into it.", Toast.LENGTH_LONG).show();
                        finish();

                    } else {
                        Toast.makeText(IncorrectInformationWindow.this, "Something went wrong! Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });


            task1.execute(server.getSendIncorrectInformation());
        }
    }
}
