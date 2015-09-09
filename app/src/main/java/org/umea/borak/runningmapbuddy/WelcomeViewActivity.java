package org.umea.borak.runningmapbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * This is the starting activity which displays a welcome screen to the user.
 */
public class WelcomeViewActivity extends AppCompatActivity {

    private static final String TAG = "WelcomeViewActivity";
    private Button btnStart, btnOverview;

    /**
     * Initializes the UI and displays the welcome layout (R.layout.activity_welcome).
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        btnStart = (Button) findViewById(R.id.welcome_button);
        btnOverview = (Button) findViewById(R.id.welcome_button2);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewMapActivity();
            }
        });
        btnOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startListActivity();
            }
        });
    }

    /**
     * Creates a new session by redirecting to the MapActivity.
     */
    private void startNewMapActivity() {
        Log.d(TAG, ": startNewMapActivity()");
        Intent intent = new Intent(WelcomeViewActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    /**
     * Redirects the user to review previous sessions in a list view.
     */
    private void startListActivity() {
        Log.d(TAG, ": startListActivity()");
        Intent intent = new Intent(WelcomeViewActivity.this, SessionListViewActivity.class);
        startActivity(intent);
    }
}
