package com.freestyletech.challenger;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.actions.SearchIntents;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends FragmentActivity
    implements GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;

    public static final String TAG = "Challenger";

    private static final int REQUEST_OAUTH = 1001;
    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private int monthly;
    private int current;
    private TextView currentText;
    private EditText monthlyText;
    private TextView neededText;
    private TextView neededDailyText;
    private TextView daysText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        monthlyText = (EditText) findViewById(R.id.monthly);
        currentText = (TextView) findViewById(R.id.current);
        neededText = (TextView) findViewById(R.id.needed);
        neededDailyText = (TextView) findViewById(R.id.neededDaily);
        daysText = (TextView) findViewById(R.id.days);

        // When permissions are revoked the app is restarted so onCreate is sufficient to check for
        // permissions core to the Activity's functionality.
        if (!checkPermissions()) {
            requestPermissions();
        }

        // Create a Google Fit Client instance with default user account.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
                .useDefaultAccount()
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .build();

        Log.i(TAG, "Connection built.");
        Log.i(TAG, "Google API connection status: " + mGoogleApiClient.isConnected());

        updateCurrent();

        // Did we get here from a search? If so, assume it was voice and speak the requested info
        Intent intent = getIntent();
        if (SearchIntents.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i(TAG, "Received Search Query: " + query);
        }

        monthlyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String value = editable.toString();
                if (value.length() > 0) {
                    monthly = Integer.parseInt(value);
                }
                updateStatus();
            }
        });

        currentText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateStatus();
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Google Play services connection failed. Cause: " +
                result.toString());
        Snackbar.make(
                MainActivity.this.findViewById(R.id.main_activity_view),
                "Exception while connecting to Google Play services: " +
                        result.getErrorMessage(),
                Snackbar.LENGTH_INDEFINITE).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (SearchIntents.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i(TAG, "Received Search Query: " + query);
        }
    }

    private void updateCurrent() {
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        long startTime = cal.getTimeInMillis();

        cal.setTime(new Date());
        long endTime = cal.getTimeInMillis();

        Log.i(TAG, "read data");
        Log.i(TAG, "Permission: " + checkPermissions());

        final DataSource dataSource = new DataSource.Builder()
                .setAppPackageName("com.endomondo.android")
                .setDataType(DataType.TYPE_DISTANCE_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .build();

        final DataReadRequest readRequest = new DataReadRequest.Builder()
                .enableServerQueries()
                .aggregate(dataSource, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(365, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        Fitness.HistoryApi
                .readData(mGoogleApiClient, readRequest)
                .setResultCallback(new ResultCallback<DataReadResult>() {
                    @Override
                    public void onResult(DataReadResult result) {
                        Log.i(TAG, "Received result");
                        if (result.getStatus().isSuccess()) {
                            // There should be only one Bucket containing one DataSet and one DataPoint
                            for (Bucket bucket : result.getBuckets()) {
                                List<DataSet> dataSets = bucket.getDataSets();
                                for (DataSet dataSet : dataSets) {
                                    for (DataPoint dp : dataSet.getDataPoints()) {
                                        current = (int) metersToMiles(dp.getValue(Field.FIELD_DISTANCE).asFloat());
                                        currentText.setText(String.format(Locale.getDefault(), "%d", current));
                                        updateStatus();
                                    }
                                }
                            }
                        } else handleReadFailure(result);
                    }
                });
    }

    private void handleReadFailure(DataReadResult result) {
        if (result.getStatus().hasResolution()) {
            try {
                result.getStatus().startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Failed to resolve read failure: " + e.toString());
            }
        }
        else Log.e(TAG, "Unresolvable read failure: " + result.getStatus().toString());
    }

    private void updateStatus() {
        // don't bother if monthly is not yet set
        if (monthly <= 0) return;

        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        float daily = ((float) monthly) / daysInMonth;

        float monthEndTarget = (month + 1) * monthly;

        float neededThisMonth = monthEndTarget - current;
        neededText.setText(String.format(Locale.getDefault(), "%d", (int) (neededThisMonth + 0.5)));

        int daysRemaining = daysInMonth - day;
        float neededDaily = (daysRemaining > 0) ? (neededThisMonth) / daysRemaining : neededThisMonth;
        neededDailyText.setText(String.format(Locale.getDefault(), "%.2f", neededDaily));

        float currentTarget = (month * monthly) + (day * daily);
        int daysFromPace = (int) (((currentTarget - current) / daily) + 0.5);
        daysFromPace = -1 * daysFromPace;

        daysText.setText(String.format(Locale.getDefault(), "%d", daysFromPace));
    }

    private float metersToMiles(float meters) {
        return ((float) (meters / 1609.344));
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    this.getCurrentFocus(),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
