package com.khalid.mosquelink.window;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
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


public class FeedbackWindow extends Activity {

    EditText etSubject, etName, etEmail, etFeedback;

    String subject, name, email, feedback;

    WifiManager wm;

    Server server = new Server();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_window);

         wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        etSubject = findViewById(R.id.et_subject);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_senderEmail);
        etFeedback = findViewById(R.id.et_feedback);


        Button sendBtn = findViewById(R.id.btn_send);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFeedback();
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


    private void sendFeedback() {
        subject = etSubject.getText().toString();
        name = etName.getText().toString();
        email = etEmail.getText().toString();
        feedback = etFeedback.getText().toString();

        System.out.println("TTTTT: " + name);
        System.out.println("HHHHH: " + email);

        if (etSubject.getText().toString().trim().equals("")){
            etSubject.setError("Subject is required!");

        } if (etName.getText().toString().trim().equals("")) {
            etName.setError("Name is required!");

        } if(etEmail.getText().toString().trim().equals("")) {
            etEmail.setError("Email is required!");

        } if(etFeedback.getText().toString().trim().equals("")) {
            etFeedback.setError("Feedback is required!");

        }else {

            HashMap postData = new HashMap();

            postData.put("txtSubject", subject);
            postData.put("txtName", name);
            postData.put("txtEmail", email);
            postData.put("txtFeedback", feedback);


            PostResponseAsyncTask task1 = new PostResponseAsyncTask(FeedbackWindow.this, postData, new AsyncResponse() {
                @Override
                public void processFinish(String s) {
                    if (s.contains("success")) {
                        Toast.makeText(FeedbackWindow.this, "Thank You for the feedback.", Toast.LENGTH_LONG).show();
                        finish();

                    } else {
                        Toast.makeText(FeedbackWindow.this, "Something went wrong! Please try again.", Toast.LENGTH_LONG).show();
                    }
                }
            });
            task1.execute(server.getSendFeedback());


        }
    }
}
