package cortez.archie.dev.staffapp.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import cortez.archie.dev.staffapp.models.CheckIn;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by _develop on 20/10/2017.
 */

public class RescueAndroidService extends IntentService {

    private RescueService rescueService;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     *
     */
    public RescueAndroidService() {
        super("RescueAndroidService");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String server_ip = sharedPreferences.getString("server_ip", "");
        String server_port = sharedPreferences.getString("server_port", "8000");

        if (server_ip.compareToIgnoreCase("") == 0)
            return;

        String remoteUrl = String.format("http://%s:%s/api/", server_ip, server_port);
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(remoteUrl)
                .build();

        rescueService = retrofit.create(RescueService.class);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle b = intent.getExtras();
        String messages = b.getString("messages");
        Gson gson = new Gson();
        CheckIn[] chkIns = gson.fromJson(messages, CheckIn[].class);

        for (CheckIn chkIn : chkIns) {
            rescueService.uploadOneCheckin("" + chkIn.getId(), chkIn);
        }
    }
}
