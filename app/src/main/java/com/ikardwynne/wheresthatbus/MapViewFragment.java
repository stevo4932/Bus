package com.ikardwynne.wheresthatbus;


import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


/**
    Fragment class for the google map.
    Displays user's location and bus locations.
 */
public class MapViewFragment extends Fragment implements OnMapReadyCallback, LocationListener {
    private static final String TAG = "MapViewFragment";
    private MainActivity mainActivity;

    private static View view;
    private ParseDbHelper mParseDbHelper;

    //Variables for google map
    private GoogleMap map;
    private GoogleApiClient mClient;

    //Variables for location updates.
    private LocationRequest mLocationRequest;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 15000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private Location location;
    private boolean mLocationUpdateOn;

    public MapViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mainActivity = (MainActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mLocationRequest = createLocationRequest();
        mParseDbHelper = new ParseDbHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // if  view is already set, remove parent if present.
        if(view != null){
            ViewGroup parent = (ViewGroup) view.getParent();
            if(parent != null)
                parent.removeView(view);
        }
        try {
            view = inflater.inflate(R.layout.map_fragment, container, false);
        }catch (InflateException ignored){}
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mClient = mainActivity.getClient();
        if(isValidClient())
            location = LocationServices.FusedLocationApi.getLastLocation(mClient);
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates(mClient);
    }

    @Override
    public void onPause() {
       stopLocationUpdates(mClient);
       super.onPause();
    }

    /** Google Map Section **/

    private boolean isValidClient(){
        return mClient != null && mClient.isConnected();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.clear();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMyLocationEnabled(true);
        map.setIndoorEnabled(false);
        map.moveCamera(updateCamera());
        getBusLocation();
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if(location == null && isValidClient())
                    location = LocationServices.FusedLocationApi.getLastLocation(mClient);
                map.moveCamera(updateCamera());
                return true;
            }
        });
    }

    private CameraUpdate updateCamera(){
        return CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(),location.getLongitude()), 15);
    }

    /** Location Section **/

    protected LocationRequest createLocationRequest() {
        if(mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
        return mLocationRequest;
    }

    protected void startLocationUpdates(GoogleApiClient mClient){
        if(isValidClient() && mLocationRequest != null && !mLocationUpdateOn) {
            Log.i(TAG, "Starting Location Updates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mClient, mLocationRequest, this);
            mLocationUpdateOn = true;
        }else{
            Log.d(TAG, "Issue starting Location Updates");
        }
    }

    protected void stopLocationUpdates(GoogleApiClient mClient) {
        Log.i(TAG, "Stopping Location Update");
        if(isValidClient() && mLocationUpdateOn) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mClient, this);
            mLocationUpdateOn = false;
        }else
            Log.d(TAG, "possible issue stopping location updates");
    }

    @Override
    public void onLocationChanged(Location location) {
        //should now call an update map method in main activity.
        if(this.location != location) {
            this.location = location;
        }
    }

    /** Parse Database Section **/

    protected void setBusLocation(){
        String bus = mainActivity.getSelectedBus();
        if(location != null && bus != null)
            mParseDbHelper.setBusLocation(bus, location);
        //add the new location to the map.
        map.addMarker(new MarkerOptions()
                .position(new LatLng(location.getLatitude(), location.getLongitude()))
                .title(bus));
    }

    protected void getBusLocation(){
        String bus = mainActivity.getSelectedBus();
        if(map != null && bus != null)
            mParseDbHelper.getBusLocation(bus, map);
    }
}
