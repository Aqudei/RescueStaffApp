package cortez.archie.dev.staffapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import cortez.archie.dev.staffapp.models.Center;
import cortez.archie.dev.staffapp.services.RescueService;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String FILENAME_CENTER_INFO = "center.json";
    private static final String FILENAME_CHECK_INS = "check_ins.json";

    private SharedPreferences sharedPreferences;
    private Gson gsonParser;
    private TextView textViewCenterAddress;
    private TextView textViewCenterName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        gsonParser = new Gson();

        textViewCenterName = (TextView) findViewById(R.id.textViewCenterName);
        textViewCenterAddress = (TextView) findViewById(R.id.textViewCenterAddress);

        readCenterInfo();
    }

    private void readCenterInfo() {
        try {
            FileInputStream fileInput = openFileInput(FILENAME_CENTER_INFO);
            InputStreamReader reader = new InputStreamReader(fileInput);
            Center center = gsonParser.fromJson(reader, Center.class);
            updateCenterDispaly(center);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void updateCenterDispaly(Center center) {
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
            updateCenterDispaly(center);
        }
    }

    public void doSync(View view) {
        new SyncTask().execute();
    }
}
