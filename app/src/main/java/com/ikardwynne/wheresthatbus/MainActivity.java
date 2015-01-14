package com.ikardwynne.wheresthatbus;


 import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
 import com.google.android.gms.common.api.Status;
 import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.Parse;
import com.parse.ParseObject;


public class MainActivity extends Activity implements OnMapReadyCallback,
                                                      ConnectionCallbacks,
                                                      OnConnectionFailedListener,
                                                      LocationListener,
                                                      StartFragment.Callbacks,
                                                      NotificationFragment.NotificationCallbacks{

    private final static String TAG = "MainActivity";
    //Pending intent used when fetching activity.
    private PendingIntent mPendingIntent;

    //The Google client
    private GoogleApiClient mClient;

    // Flag that indicates if a request is underway.
    private boolean mInProgress;

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


    //Used for User Location.
    private Location location;
    private LocationRequest mLocationRequest;
    private boolean mLocationUpdateOn;
    private boolean mLocationUpdateRequested;
    private static final String LOCATION_UPDATE = "locationStatus";
    private static final String LOCATION_UPDATE_REQUESTED = "locationStatusRequested";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private FragmentManager mFragmentManager;
    private MapFragment mMapFragment;
    private GoogleMap map;
    private Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set variables
        mPendingIntent = null;
        mFragmentManager = getFragmentManager();
        mClient = null;
        mInProgress = false;
        location = null;
        map = null;
        marker = null;
        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        mLocationUpdateOn = savedInstanceState != null
                && savedInstanceState.getBoolean(LOCATION_UPDATE, false);
        mLocationUpdateRequested = savedInstanceState != null
                && savedInstanceState.getBoolean(LOCATION_UPDATE_REQUESTED, false);

        //set up parse database.
        //Parse.enableLocalDatastore(this);
        //Parse.initialize(this, "hQN6CcxhmWfWDEX8wnoHx8Nx7BXmaI2asE7WMFUg", "wdlYdmu64NeJR4JMiYZePM3114cZvvoRvwTJRK9T");

        /*//test parse database
        ParseObject testObject = new ParseObject("TestObject");
        testObject.put("foo", "bar");
        testObject.saveInBackground();*/


        //start google play services.
        startUpdates();

        String action = getIntent().getStringExtra("activity");
        if(action != null) {
            Log.v(TAG, action);

            Bundle b = new Bundle();
            b.putString("activity", action);
            NotificationFragment nFrag = new NotificationFragment();
            nFrag.setArguments(b);

            mFragmentManager.beginTransaction()
                    .add(R.id.container, nFrag)
                    .commit();

        }else {

            //display start Fragment.
            if (savedInstanceState == null) {
                mFragmentManager.beginTransaction()
                        .add(R.id.container, new StartFragment())
                        .commit();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
        outState.putBoolean(LOCATION_UPDATE, mLocationUpdateOn);
        outState.putBoolean(LOCATION_UPDATE_REQUESTED, mLocationUpdateRequested);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mLocationUpdateRequested && !mLocationUpdateOn)
            startLocationUpdates();
    }

    @Override
    protected void onPause() {
        if(mLocationUpdateOn)
            stopLocationUpdates();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_RESOLVE_ERROR) {
            //used on google services and connection failures.
            mResolvingError = false;
            startUpdates();
        }else
            Log.d(TAG, "Error: Bad request code in onActivityResults");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit ) {
            stopUpdates();
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    /*Start of the Google Services Implementation*/

    //check for google services if not you should probably display something.
    public void serviceAvailable(){
        int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(errorCode != ConnectionResult.SUCCESS) {
            mResolvingError = true;
            showErrorDialog(errorCode);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    public void startUpdates() {
        Log.i(TAG, "Starting updates");
        serviceAvailable(); //check if google services is available.
        buildGoogleApiClient(); //make a client
        createLocationRequest();
        // Request a connection to Location Services
        // on failure disconnect service and try again.
        if(!connect()){
            Log.d(TAG, "startUpdates: issue connecting going to try again");
            disconnect();
            startUpdates();
        }
    }

    public void stopUpdates() {
        Log.i(TAG, "Stopping updates");
        serviceAvailable(); // Check for Google Play services
        if(mClient.isConnected()){
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClient, mPendingIntent);
            if(mLocationUpdateOn) {
                stopLocationUpdates();
                mLocationUpdateRequested = false;
            }
            disconnect();
        }else{
            connect();
            stopUpdates();
        }
    }

    private boolean connect() {
        if (!mInProgress && !mClient.isConnected() && !mClient.isConnecting()) {
            mInProgress = true;
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
            mInProgress = false;
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
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, DETECTION_INTERVAL_MILLISECONDS, getPendingIntent());
        getCurrentLocation();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        mInProgress = false;
        Log.v(TAG, "yah there was a connection failure, sorry");
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                startUpdates();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(mFragmentManager, "errordialog");
    }

    public void onDialogDismissed() {
        mResolvingError = false;
        startUpdates();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        //TODO: you should fill this in also.
        Log.d(TAG, "The connection was Suspended");
    }


    /*This Section used for user location*/

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    private void startLocationUpdates(){
        if(mClient != null && mClient.isConnected() && mLocationRequest != null && !mLocationUpdateOn) {
            Log.i(TAG, "Starting Location Updates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, mLocationRequest, this);
            mLocationUpdateOn = true;
        }else{
            Log.d(TAG, "Issue starting Location Updates");
        }
    }

    private void stopLocationUpdates() {
        Log.i(TAG, "Stopping Location Update");
        if(mClient != null && mLocationUpdateOn) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mClient, this);
            mLocationUpdateOn = false;
        }else
            Log.d(TAG, "issue stopping location updates");
    }

    public Location getCurrentLocation() {
        if(location == null)
            location = LocationServices.FusedLocationApi.getLastLocation(mClient);
        return location;

    }

    @Override
    public void onLocationChanged(Location location) {
        if(this.location != location) {
            this.location = location;
            //update markers and camera location.
            if (map != null) {
                if (marker != null)
                    marker.remove();
                marker = setNewMarker();
                map.moveCamera(updateCamera());
            }
            Toast.makeText(this, "Location Update: "+this.location.getLatitude()+", "+this.location.getLongitude(), Toast.LENGTH_SHORT).show();
        }
    }


    /*Start of Google Map Implementation*/

    @Override
    public void find(String bus_selection) {

       if(!mLocationUpdateOn) {
           startLocationUpdates();
           mLocationUpdateRequested = true;
       }
        //take selection, find the info for that bus
        //then launch appropriate fragment.
       //TODO: start asynch task to fetch latest bus information if available.
        startMapFragment();
    }

    private void startMapFragment(){
        mMapFragment = MapFragment.newInstance();
        mFragmentManager.beginTransaction()
            .replace(R.id.container, mMapFragment)
            .addToBackStack(null)
            .commit();
        mMapFragment.getMapAsync(this);
    }

    private Marker setNewMarker(){
        LatLng latLng = new LatLng(getCurrentLocation().getLatitude(),
                                   getCurrentLocation().getLongitude());
        return map.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Your Current Location"));
    }

    private CameraUpdate updateCamera(){
        LatLng latlng = new LatLng(getCurrentLocation().getLatitude(),
                                   getCurrentLocation().getLongitude());
        return CameraUpdateFactory.newLatLngZoom(latlng, 15);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        marker = setNewMarker();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setIndoorEnabled(false);
        map.moveCamera(updateCamera());

    }

    /* callback methods for Notification Fragments*/
    @Override
    public void updateBus(){
        Log.i(TAG, "Updating the location of the bus");
        //update bus location.
    }
    @Override
    public void waitForBus(){
        Log.i(TAG, "waiting for the bus some more");
        finish();
    }
    @Override
    public void exit(){
        Log.i(TAG, "made it to exit function");
        stopUpdates();
        finish();
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
