package cortez.archie.dev.staffapp;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.StringSearch;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
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
import java.io.IOException;
import java.io.InputStreamReader;

import cortez.archie.dev.staffapp.models.Center;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        peopleListView = (ListView) findViewById(R.id.listViewPeople);

        radioSafe = (RadioButton) findViewById(R.id.radioButtonSafe);
        radioInjured = (RadioButton) findViewById(R.id.radioButtonInjured);
        radioMissing = (RadioButton) findViewById(R.id.radioButtonMissing);
        radioDead = (RadioButton) findViewById(R.id.radioButtonDead);
        progressSending = (ProgressBar) findViewById(R.id.progressBarSending);
        progressSending.setVisibility(View.GONE);

        setupSearch();
        readMembers();
        setupListView();
        setupSms();
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

                                PendingIntent sentSms = PendingIntent.getBroadcast(CheckInActivity.this,
                                        0, new Intent(INTENT_FILTER_SENT), 0);
                                String message = String.format("\"id\":%d, \"scope\":\"self\", \"status\":\"%s\"",
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
