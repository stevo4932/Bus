package com.ikardwynne.wheresthatbus;

import android.app.Application;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;

public class ParseDbHelper extends Application{

    private static final String TAG = "ParseDbHelper";
    private MainActivity activity;
    private LatLng lastBusLocation;

    public ParseDbHelper(MainActivity activity){
        this.activity = activity;
        lastBusLocation = null;
        Parse.initialize(this.activity, "hQN6CcxhmWfWDEX8wnoHx8Nx7BXmaI2asE7WMFUg",
                "wdlYdmu64NeJR4JMiYZePM3114cZvvoRvwTJRK9T");
    }

    public void getBusLocation(final String bus, final GoogleMap map){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Buses");
        query.whereEqualTo("Bus_Name", bus);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    Toast.makeText(activity, "No Bus Information", Toast.LENGTH_LONG).show();
                } else {
                    //update the map with the buses location.
                    LatLng latlng = new LatLng(object.getDouble("Lat"), object.getDouble("Lon"));
                    Date updatedAt = object.getUpdatedAt();
                    if(!addBusMarker(latlng, updatedAt, bus, map)){
                        Toast.makeText(activity, "No current information is available\n" +
                                            "for the "+bus+" bus", Toast.LENGTH_LONG).show();
                    }else{
                        //for Debugging.
                        Toast.makeText(activity, "Bus Found", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void setBusLocation(final String bus, final Location location){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Buses");
        query.whereEqualTo("Bus_Name", bus);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    Log.d(TAG, "The getFirst request failed.");
                } else {
                    //update objects location.
                    object.put("Lat", location.getLatitude());
                    object.put("Lon", location.getLongitude());
                    object.saveInBackground();
                }
            }
        });
    }

    private boolean addBusMarker(LatLng latlng, Date updatedAt, String bus, GoogleMap map){
        if(latlng.longitude == 0 && latlng.latitude == 0)
            return false;

        //TODO: Fix the is current variable.
        //long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
        //long updateHours = TimeUnit.MILLISECONDS.toHours( updatedAt.getTime());
        //checks to see if the new location is relatively recent.
        /*boolean isCurrent = (currentHours != Long.MAX_VALUE && currentHours != Long.MIN_VALUE) &&
                (updateHours != Long.MAX_VALUE && updateHours!= Long.MIN_VALUE) &&
                (currentHours == updateHours || currentHours == (updateHours - 1));*/

        if(!latlng.equals(lastBusLocation)) {
            lastBusLocation = latlng;
            map.addMarker(new MarkerOptions()
                    .position(latlng)
                    .title(bus)
                    .snippet(updatedAt.toString()));
            return true;
        }else
            return false;
    }

}
