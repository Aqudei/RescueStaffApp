package cortez.archie.dev.staffapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import cortez.archie.dev.staffapp.models.Center;
import cortez.archie.dev.staffapp.services.RescueService;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    public static final String FILENAME_CENTER_INFO = "center.json";
    public static final String FILENAME_UNSENT_CHECKINS = "unsent_check_ins.json";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 5;
    private static final int MY_PERMISSIONS_REQUEST_READ_SMS = 6;

    private SharedPreferences sharedPreferences;
    private Gson gsonParser;
    private TextView textViewCenterAddress;
    private TextView textViewCenterName;
    private LinearLayout progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = (LinearLayout) findViewById(R.id.linearLayoutProgress);
        hideProgress();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        gsonParser = new Gson();

        textViewCenterName = (TextView) findViewById(R.id.textViewCenterName);
        textViewCenterAddress = (TextView) findViewById(R.id.textViewCenterAddress);

        readCenterInfo();

        requestSendSmsPermission();
        requestReadSmsPermission();
    }

    private void requestReadSmsPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_SMS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Permission Request")
                        .setMessage("This app needs to read sms to monitor send/unsent check ins.")
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_SMS},
                                        MY_PERMISSIONS_REQUEST_READ_SMS);
                            }
                        });

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_SMS},
                        MY_PERMISSIONS_REQUEST_READ_SMS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }
    private void requestSendSmsPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.SEND_SMS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Permission Request")
                        .setMessage("This app needs to send sms to notify server about a person's status.")
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.SEND_SMS},
                                        MY_PERMISSIONS_REQUEST_SEND_SMS);
                            }
                        });

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.SEND_SMS},
                        MY_PERMISSIONS_REQUEST_SEND_SMS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void readCenterInfo() {
        try {
            FileInputStream fileInput = openFileInput(FILENAME_CENTER_INFO);
            InputStreamReader reader = new InputStreamReader(fileInput);
            Center center = gsonParser.fromJson(reader, Center.class);
            updateCenterDisplay(center);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updateCenterDisplay(Center center) {
        if (center != null) {
            textViewCenterAddress.setText(center.getAddress());
            textViewCenterName.setText(center.getCenterName());
        }
    }

    public void openSettings(View view) {
        Intent settingIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingIntent);
    }

    public void openCheckIns(View view) {
        Intent checkInActivity = new Intent(this, CheckInActivity.class);
        startActivity(checkInActivity);
    }

    class SyncTask extends AsyncTask<Void, Void, Center> {


        @Override
        protected Center doInBackground(Void... params) {

            String evacuation_id = sharedPreferences.getString("evacuation_id", "");
            String server_ip = sharedPreferences.getString("server_ip", "");
            String server_port = sharedPreferences.getString("server_port", "8000");

            if (server_ip.compareToIgnoreCase("") == 0
                    || evacuation_id.compareToIgnoreCase("") == 0)
                return null;

            String remoteUrl = String.format("http://%s:%s/api/", server_ip, server_port);
            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(remoteUrl)
                    .build();

            RescueService rescueService = retrofit.create(RescueService.class);
            Call<Center> call = rescueService.getCenterInfo(evacuation_id);

            try {
                Center center = call.execute().body();
                if (center != null) {
                    String json = gsonParser.toJson(center);
                    try {
                        deleteFile(FILENAME_CENTER_INFO);
                        FileOutputStream fileout = openFileOutput(FILENAME_CENTER_INFO, MODE_APPEND);
                        fileout.write(json.getBytes());
                        fileout.close();
                        return center;
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Center center) {
            super.onPostExecute(center);
            updateCenterDisplay(center);
            hideProgress();
        }
    }

    public void doSync(View view) {
        showProgress();
        new SyncTask().execute();
    }

    public void showProgress() {
        progress.setVisibility(View.VISIBLE);
    }

    public void hideProgress() {
        progress.setVisibility(View.INVISIBLE);
    }
}
