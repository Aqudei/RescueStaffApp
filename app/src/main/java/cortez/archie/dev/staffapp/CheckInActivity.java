package cortez.archie.dev.staffapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cortez.archie.dev.staffapp.models.Center;
import cortez.archie.dev.staffapp.models.CheckIn;
import cortez.archie.dev.staffapp.models.MemberInfo;
import cortez.archie.dev.staffapp.services.RescueAndroidService;
import cortez.archie.dev.staffapp.services.RescueService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CheckInActivity extends AppCompatActivity {

    private static final String INTENT_FILTER_SENT = "SENT";
    private Center center;
    private ListView peopleListView;
    private SearchView search;
    private ArrayAdapter<MemberInfo> adapter;
    private RadioButton radioSafe;
    private RadioButton radioInjured;
    private RadioButton radioMissing;
    private RadioButton radioDead;
    private ProgressBar progressSending;
    private Gson gsonParser = new Gson();
    private List<CheckIn> notSentList = new ArrayList<>();

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        peopleListView = (ListView) findViewById(R.id.listViewPeople);

        radioSafe = (RadioButton) findViewById(R.id.radioButtonSafe);
        radioInjured = (RadioButton) findViewById(R.id.radioButtonInjured);
        radioMissing = (RadioButton) findViewById(R.id.radioButtonMissing);
        radioDead = (RadioButton) findViewById(R.id.radioButtonDead);
        progressSending = (ProgressBar) findViewById(R.id.progressBarSending);
        progressSending.setVisibility(View.GONE);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayShowHomeEnabled(true);

        readNotSentCheckIns();
        setupSearch();
        readMembers();
        setupListView();
        setupSms();
    }

    private void readNotSentCheckIns() {
        try {
            FileInputStream file = openFileInput(MainActivity.FILENAME_NOTSENT_CHECK_INS);
            InputStreamReader reader = new InputStreamReader(file);
            CheckIn[] checkIns = gsonParser.fromJson(reader, CheckIn[].class);
            notSentList.addAll(Arrays.asList(checkIns));
            reader.close();
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    class PushUnsentAsyncTask extends AsyncTask<Void, Void, Void> {

        private boolean withErrors = false;

        @Override
        protected Void doInBackground(Void... params) {
            Log.d("PUSH", "Pushing unsent messages");
            String server_ip = sharedPreferences.getString("server_ip", "");
            String server_port = sharedPreferences.getString("server_port", "8000");

            if (TextUtils.isEmpty(server_ip)) {
                Log.d("PUSH", "No server IP found.");
                return null;
            }

            String remoteUrl = String.format("http://%s:%s/api/", server_ip, server_port);
            Log.d("PUSH", "Request url is " + remoteUrl);
            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(remoteUrl)
                    .build();

            RescueService rescueService = retrofit.create(RescueService.class);
            for (CheckIn chkIn : notSentList) {
                Log.d("PUSH", "Uploading unsent with id: " + chkIn.getId());
                Call<ResponseBody> response = rescueService.uploadOneCheckin("" + chkIn.getId(), chkIn);
                try {
                    Response<ResponseBody> rslt = response.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                    withErrors = true;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            notSentList.clear();
            deleteFile(MainActivity.FILENAME_NOTSENT_CHECK_INS);
            hideProgress();
            if (!withErrors)
                Toast.makeText(CheckInActivity.this, "Operation Done", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(CheckInActivity.this, "Operation Done with Errors!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clear_chkins_menu) {
            deleteFile(MainActivity.FILENAME_NOTSENT_CHECK_INS);
            notSentList.clear();
            return true;
        } else if (item.getItemId() == R.id.push_unsent_menu) {
            showProgress();
            new PushUnsentAsyncTask().execute();
            return true;
        }

        return false;
    }

    private void showProgress() {
        progressSending.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        progressSending.setVisibility(View.GONE);
    }

    private void setupSms() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getResultCode() == RESULT_OK) {

                } else {

                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        String body = bundle.getString("sms_body", "");
                        if (TextUtils.isEmpty(body) == false) {
                            Log.d("PUSH", "Message not sent, adding to unsent list: " + body);
                            CheckIn chkIn = gsonParser.fromJson(body, CheckIn.class);
                            if (chkIn != null && chkIn.getId() != -1) {
                                if (notSentList.contains(chkIn) == false)
                                    notSentList.add(chkIn);
                            }
                        }
                    }


                }
                hideProgress();
            }
        };
        registerReceiver(receiver, new IntentFilter(INTENT_FILTER_SENT));
    }

    private void setupListView() {
        if (center != null) {
            adapter = new ArrayAdapter<MemberInfo>(this, android.R.layout.simple_list_item_1, center.getMembers());
            peopleListView.setAdapter(adapter);

            peopleListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final MemberInfo person = adapter.getItem(position);

                    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == DialogInterface.BUTTON_POSITIVE) {

                                String status = "";
                                if (radioSafe.isChecked()) {
                                    status = "safe";
                                }

                                if (radioInjured.isChecked()) {
                                    status = "injured";
                                }

                                if (radioMissing.isChecked()) {
                                    status = "missing";
                                }

                                if (radioDead.isChecked()) {
                                    status = "dead";
                                }

                                String contact = center.getInChargeCellphone();
                                if (TextUtils.isEmpty(contact)) {
                                    new AlertDialog.Builder(CheckInActivity.this)
                                            .setTitle("Error")
                                            .setMessage("Contact Number not found on " + center.getCenterName())
                                            .show();
                                    return;
                                }


                                String message = String.format("{\"id\":%d, \"scope\":\"self\", \"status\":\"%s\"}",
                                        person.getId(), status);
                                Intent intent = new Intent(INTENT_FILTER_SENT);
                                intent.putExtra("sms_body", message);
                                PendingIntent sentSms = PendingIntent.getBroadcast(CheckInActivity.this,
                                        0, intent, 0);
                                showProgress();
                                SmsManager.getDefault().sendTextMessage(contact, null,
                                        message, sentSms, null);
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(CheckInActivity.this)
                            .setTitle("Confirm Action")
                            .setMessage("Do you want to check in " + person.getFullName() + "?")
                            .setPositiveButton("Yes", dialogListener)
                            .setNegativeButton("No", dialogListener);
                    builder.show();
                }
            });
        }
    }

    private void readMembers() {
        try {
            FileInputStream evacuation = openFileInput(MainActivity.FILENAME_CENTER_INFO);

            InputStreamReader reader = new InputStreamReader(evacuation);
            center = gsonParser.fromJson(reader, Center.class);
            reader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupSearch() {
        search = (SearchView) findViewById(R.id.searchView);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.getFilter().filter(newText);
                }
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        deleteFile(MainActivity.FILENAME_NOTSENT_CHECK_INS);
        try {
            FileOutputStream file = openFileOutput(MainActivity.FILENAME_NOTSENT_CHECK_INS, MODE_APPEND);
            String json = gsonParser.toJson(notSentList);
            file.write(json.getBytes());
            file.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
