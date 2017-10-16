package cortez.archie.dev.staffapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openSettings(View view) {
        Intent settingIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingIntent);
    }

    public void openCheckIns(View view) {
        Intent checkInActivity = new Intent(this, CheckInActivity.class);
        startActivity(checkInActivity);
    }
}
