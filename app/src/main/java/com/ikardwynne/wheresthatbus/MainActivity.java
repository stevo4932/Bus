package com.ikardwynne.wheresthatbus;


import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;



/*
  TODO: attach parse db helper.
  TODO: seems to be sending multiple notifications on waiting of bus.
  TODO: shared Preferences.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
                                                      OnConnectionFailedListener, StartFragment.Callbacks,
                                                      NotificationFragment.NotificationCallbacks{

    private final static String TAG = "MainActivity";
    //Pending intent used when fetching activity.
    private PendingIntent mPendingIntent;


    //The Google client
    private GoogleApiClient mClient;

    //parse db helper

    //constants for detection interval.
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int DETECTION_INTERVAL_SECONDS = 13;
    private static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    //Used for the bus locations.
    private String selectedBus;
    private boolean updateBus;
    private static final String SELECTED_BUS = "selectedbus";

    MapViewFragment mapFrag;

    //Notification action string.
    private String action;
    private static final String ACTION = "action";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set variables
        mPendingIntent = null;

        //start google play services.
        startUpdates();

        if(action == null)
            action = getIntent().getStringExtra("activity");

        if(action != null) {
            Log.i(TAG, action);

            Bundle b = new Bundle();
            b.putString("activity", action);
            NotificationFragment nFrag = new NotificationFragment();
            nFrag.setArguments(b);
            //start the notification fragment.
            getFragmentManager().beginTransaction()
                    .add(R.id.container, nFrag)
                    .commit();
        }else {
            //display start Fragment.
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new StartFragment())
                        .commit();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "storing state");
        //To know if we were resolving an error.
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        //saves the selectedBus variable.
        outState.putString(SELECTED_BUS, selectedBus);
        outState.putBoolean("updatebus", updateBus);
        //saves the action string if set.
        outState.putString(ACTION, action);

    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(TAG, "resoling state");
        super.onRestoreInstanceState(savedInstanceState);

        mResolvingError = savedInstanceState.getBoolean(STATE_RESOLVING_ERROR);
        selectedBus = savedInstanceState.getString(SELECTED_BUS);
        updateBus = savedInstanceState.getBoolean("updatebus");
        action = savedInstanceState.getString(ACTION, null);

    }

    private void exitActivity(){
        stopUpdates();
        mResolvingError = false;
        selectedBus = null;
        updateBus = false;
        action = null;
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //used for the google fail call back.
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                startUpdates();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_exit:
                exitActivity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Start of the Google Services Implementation **/

    //check for google services if not you should probably display something.
    public void serviceAvailable(){
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(errorCode != ConnectionResult.SUCCESS) {
            mResolvingError = true;
            showErrorDialog(errorCode);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        if(mClient == null)
            mClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .build();
        //otherwise you already have a client.
    }

    public GoogleApiClient getClient(){
        return mClient;
    }

    public void startUpdates() {
        Log.i(TAG, "Starting updates");
        serviceAvailable(); //check if google services is available.
        buildGoogleApiClient(); //make a client
        // Request a connection to Location Services.
        if(!connect()){
            Log.d(TAG, "startUpdates: issue connecting going to try again");
            disconnect();
            startUpdates();
        }
    }

    public void stopUpdates() {
        Log.i(TAG, "Stopping updates");
        serviceAvailable(); // Check for Google Play services
        if(!mClient.isConnected())
            connect();
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClient, mPendingIntent);
        //remove location updates if map fragment is showing.
        if(mapFrag != null && mapFrag.isVisible())
            mapFrag.stopLocationUpdates(mClient);
        disconnect();
    }

    private boolean connect() {
        if (!mClient.isConnected() && !mClient.isConnecting()) {
            mClient.connect();
            return true;
        } else {
            Log.d(TAG, "GoogleApiClient already connected or is unavailable");
            return false;
        }
    }

    public boolean disconnect(){
        if (mClient.isConnected()) {
            mClient.disconnect();
            return true;
        } else {
            Log.v(TAG, "GoogleApiClient already disconnected or is unavailable");
            return false;
        }
    }

    private PendingIntent getPendingIntent(){
        if(mPendingIntent == null){
            //make a pending intent.
            Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
            mPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mPendingIntent;
    }


    @Override
    public void onConnected(Bundle dataBundle) {
        //request activity recognition updates and get last know location.
        Log.v(TAG, "onConnected called with start");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient,
                DETECTION_INTERVAL_MILLISECONDS, getPendingIntent());
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        Log.d(TAG, "The connection was Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "yah there was a connection failure, sorry");
        if (connectionResult.hasResolution() && !mResolvingError) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                startUpdates();
            }
        } else if(!mResolvingError) {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            mResolvingError = true;
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /** callback method of start fragment **/

    @Override
    public void find(String bus_selection) {
        selectedBus = bus_selection;
        //start map fragment.
        mapFrag = new MapViewFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.container, mapFrag)
                .addToBackStack(null)
                .commit();
    }

    protected String getSelectedBus(){
        return selectedBus;
    }

    /**  callback methods for Notification Fragments **/
    //TODO: impliment these.
    @Override
    public void updateBus(){
        Log.i(TAG, "Updating the location of the bus");
        //update bus location.
        updateBus = true;
    }
    @Override
    public void waitForBus(){
        Log.i(TAG, "waiting for the bus some more");
        finish();
    }
    @Override
    public void exit(){
        Log.i(TAG, "made it to exit function");
        exitActivity();
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity)getActivity()).onDialogDismissed();
        }
    }
}
