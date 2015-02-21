package com.ikardwynne.wheresthatbus;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;


/*
  TODO: Display Dialog instead of the NotificationFragment.
  TODO: Test new changes.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
                                                      OnConnectionFailedListener, StartFragment.Callbacks,
                                                      NotificationFragment.NotificationCallbacks,
                                                      MapViewFragment.MapCallbacks{

    private final static String TAG = "MainActivity";
    //Id for persistent update notifications.
    private static final int UPDATE_ID = 56788;
    //Pending intent used when fetching activity.
    private PendingIntent mPendingIntent;

    //The Google client
    private GoogleApiClient mClient;

    //constants for detection interval.
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int DETECTION_INTERVAL_SECONDS = 10;
    private static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;
    private static final String ACTIVITY_ON = "ActivityOn";

    //persistent boolean for activity updates.
    private boolean activityOn;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    //Used for the bus locations.
    private String selectedBus;
    private static final String BUS = "Bus";

    private MapViewFragment mapFrag;
    private boolean replace;

    //Notification action string.
    private String action;
    private static final String ACTION = "action";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setValues();

        if(action == null)
            action = getIntent().getStringExtra("activity");

        if (action != null) {
            //Show notification fragment.
            Log.i(TAG, action);

            Bundle b = new Bundle();
            b.putString("activity", action);
            NotificationFragment nFrag = new NotificationFragment();
            nFrag.setArguments(b);
            //start the notification fragment.
            getFragmentManager().beginTransaction()
                    .add(R.id.container, nFrag)
                    .commit();
        }else if(selectedBus != null){
            //show map fragment with the selected bus.
            getMapFragment(false, false);
        }else{
            //display start Fragment.
            if (savedInstanceState == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new StartFragment())
                        .commit();
            }
        }
    }

    private void setValues() {
        mPendingIntent = null;
        SharedPreferences shared = getPreferences(MODE_PRIVATE);
        activityOn = shared.getBoolean(ACTIVITY_ON, false);
        selectedBus = shared.getString(BUS, null);
        mResolvingError = shared.getBoolean(STATE_RESOLVING_ERROR, false);
        action = shared.getString(ACTION, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences shared = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putBoolean(ACTIVITY_ON, activityOn);
        editor.putString(BUS, selectedBus);
        editor.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        editor.putString(ACTION, action);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        if(activityOn){
            //Display a alert dialog box asking user if they are going to wait for the bus.
            new AlertDialog.Builder(this)
                    .setTitle("Exit")
                    .setMessage("Are you going to wait for the bus?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {finish();}
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            stopUpdates();
                            finish();
                        }
                    })
                    .show();
        }else
            finish();
    }
    //this function's days are numbered!
    private void exitActivity(){
        stopUpdates();
        mResolvingError = false;
        selectedBus = null;
        action = null;
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //used for the google fail call back.
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK)
                // getMapFragment already called so need to call startUpdates.
                startUpdates(); /** This should be checked **/
        }
    }

    /*@Override
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
    }*/

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

    private PendingIntent getPendingIntent(){
        if(mPendingIntent == null){
            //make a pending intent.
            Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
            mPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mPendingIntent;
    }

    public void startUpdates() {
        Log.i(TAG, "Starting updates");
        serviceAvailable(); //check if google services is available.
        buildGoogleApiClient(); //make a client
        // Request a connection
        if (!mClient.isConnected() && !mClient.isConnecting())
            mClient.connect();
        else
            Log.d(TAG, "Error: Could not connect to google API");
    }

    public void stopUpdates() {
        Log.i(TAG, "Stopping updates");
        if(mClient != null && mClient.isConnected()) {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClient, getPendingIntent()).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        activityOn = false;
                        //Toast.makeText(MainActivity.this, "Activity Recognition Off", Toast.LENGTH_SHORT).show();
                        removeNotification();
                        mClient.disconnect();
                    } else
                        Toast.makeText(MainActivity.this, "Error: Could not turn off Activity Recognition", Toast.LENGTH_SHORT).show();
                }
            });
        }else
            Log.d(TAG, "Updates not stopped");
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        //request activity recognition updates and get last know location.
        Log.i(TAG, "Client Connected!");
        //start map fragment.

        if(replace)
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mapFrag)
                    .commit();
        else
            getFragmentManager().beginTransaction()
                    .add(R.id.container, mapFrag)
                    .commit();

        if(!activityOn) {
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient,
                    DETECTION_INTERVAL_MILLISECONDS, getPendingIntent()).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        activityOn = true;
                        //Toast.makeText(MainActivity.this, "Activity Recognition on", Toast.LENGTH_SHORT).show();
                        makeNotification();
                    } else
                        Toast.makeText(MainActivity.this, "Error: Could not turn on Activity Recognition", Toast.LENGTH_SHORT).show();
                }
            });
        }
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

    /*Used to set the persistent notification that informs the user of the runing
    Activity Recognition. */
    private void makeNotification(){
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 4832, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.common_ic_googleplayservices)
                .setContentTitle("Where's That Bus")
                .setContentText("Activity Recognition Running")
                .setContentIntent(pendingIntent)
                .setOngoing(true);
        // UPDATE_ID allows you to remove the notification later on.
        mNotificationManager.notify(UPDATE_ID, mBuilder.build());
    }

    //removes activity recognitions persistent notification.
    private void removeNotification(){
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(UPDATE_ID);
    }

    //build and start map fragment.
    private void getMapFragment(boolean update, boolean replace){
        this.replace = replace;
        mapFrag = new MapViewFragment();
        Bundle b = new Bundle();
        b.putString("bus", selectedBus);
        b.putBoolean("update", update);
        mapFrag.setArguments(b);
        startUpdates();
    }

    /** callback method of start fragment **/

    @Override
    public void find(String bus_selection) {
        selectedBus = bus_selection;
        //start google play services.
        getMapFragment(false, true);
    }

    /**  callback methods for Notification Fragments **/
    @Override
    public void updateBus(){
        Log.i(TAG, "Updating the location of the bus");
        //update bus location.
        action = null;
        getMapFragment(true, true);
    }
    @Override
    public void waitForBus(){
        Log.i(TAG, "waiting for the bus some more");
        action = null;
        finish();
    }
    @Override
    public void exit(){
        Log.i(TAG, "made it to exit function");
        exitActivity();
    }

    /** callback method for MapViewFragment **/

    @Override
    public void newBus() {
        selectedBus = null;
        stopUpdates();
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new StartFragment())
                .commit();
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
    public void onDialogDismissed() {
        mResolvingError = false;
    }
}
