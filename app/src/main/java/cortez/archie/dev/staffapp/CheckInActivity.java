package cortez.archie.dev.staffapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
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

import com.google.gson.Gson;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cortez.archie.dev.staffapp.models.Center;
import cortez.archie.dev.staffapp.models.CheckIn;
import cortez.archie.dev.staffapp.models.MemberInfo;

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
    private Gson gsonParser;

    List<CheckIn> unsentCheckIns = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        gsonParser = new Gson();

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
        ab.setDisplayHomeAsUpEnabled(true);

        setupSearch();
        readMembers();
        setupListView();
        setupSms();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.push_unsent_menu) {
            return true;
        }

        if (item.getItemId() == R.id.clear_chkins_menu) {
            deleteFile(MainActivity.FILENAME_UNSENT_CHECKINS);
            loadUnsentCheckIns();
            return true;
        }

        return false;
    }

    private void loadUnsentCheckIns() {
        try {
            FileInputStream file = openFileInput(MainActivity.FILENAME_UNSENT_CHECKINS);
            InputStreamReader reader = new InputStreamReader(file);

            reader.close();
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
                    String uri = bundle.getString("uri");

                    Cursor rslt = getContentResolver().query(Uri.parse(uri), null, null, null, null);
                    rslt.moveToFirst();
                    String body = rslt.getString(rslt.getColumnIndexOrThrow("body"));
                    CheckIn checkIn = gsonParser.fromJson(body, CheckIn.class);
                    if (checkIn != null && unsentCheckIns.contains(checkIn) == false) {
                        unsentCheckIns.add(checkIn);
                    }
                }
                hideProgress();
            }
        };
        registerReceiver(receiver, new IntentFilter(INTENT_FILTER_SENT));
    }

    @Override
    protected void onPause() {
        String json = gsonParser.toJson(unsentCheckIns);
        deleteFile(MainActivity.FILENAME_UNSENT_CHECKINS);
        try {
            FileOutputStream file = openFileOutput(MainActivity.FILENAME_UNSENT_CHECKINS, MODE_APPEND);
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onPause();
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

                                PendingIntent sentSms = PendingIntent.getBroadcast(CheckInActivity.this,
                                        0, new Intent(INTENT_FILTER_SENT), 0);
                                String message = String.format("{\"id\":%d, \"scope\":\"self\", \"status\":\"%s\"}",
                                        person.getId(), status);
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
            Gson gson = new Gson();
            InputStreamReader reader = new InputStreamReader(evacuation);
            center = gson.fromJson(reader, Center.class);
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
}
