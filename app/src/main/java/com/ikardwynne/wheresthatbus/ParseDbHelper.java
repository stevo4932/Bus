package com.ikardwynne.wheresthatbus;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ParseDbHelper extends Application{

    private static final String TAG = "ParseDbHelper";
    Context ctx;
    private MainActivity act_main;
    private LatLng lastBusLocation;

    public ParseDbHelper(Context context){
        ctx = context;
        act_main = new MainActivity();
        lastBusLocation = null;
        //Parse.enableLocalDatastore(ctx);
        Parse.initialize(ctx, "hQN6CcxhmWfWDEX8wnoHx8Nx7BXmaI2asE7WMFUg", "wdlYdmu64NeJR4JMiYZePM3114cZvvoRvwTJRK9T");
    }

    public void getBusLocation(final String bus, final GoogleMap map){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Buses");
        query.whereEqualTo("Bus_Name", bus);
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            public void done(ParseObject object, ParseException e) {
                if (object == null) {
                    Log.d(TAG, "The getFirst request failed.");
                } else {
                    //update the map with the buses location.
                    LatLng latlng = new LatLng(object.getDouble("Lat"), object.getDouble("Lon"));
                    Date updatedAt = object.getUpdatedAt();
                    if(!addBusMarker(latlng, updatedAt, bus, map)){
                        Toast.makeText(ctx, "No current information is avalible\n" +
                                            "for the "+bus+" bus", Toast.LENGTH_LONG).show();
                    }else{
                        //for Debugging.
                        Toast.makeText(ctx, "bus marker added", Toast.LENGTH_SHORT).show();
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

    public boolean addBusMarker(LatLng latlng, Date updatedAt, String bus, GoogleMap map){
        long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
        long updateHours = TimeUnit.MILLISECONDS.toHours( updatedAt.getTime());
        //checks to see if the new location is relatively recent.
        boolean isCurrent = (currentHours != Long.MAX_VALUE && currentHours != Long.MIN_VALUE) &&
                (updateHours != Long.MAX_VALUE && updateHours!= Long.MIN_VALUE) &&
                (currentHours == updateHours || currentHours == (updateHours - 1));

        if(isCurrent) {
            if(!latlng.equals(lastBusLocation)) {
                lastBusLocation = latlng;
                map.addMarker(new MarkerOptions()
                        .position(latlng)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                                //TODO: probably need to format this better.
                        .title(bus)
                        .snippet(updatedAt.toString()));
            }
            return true;
        }else{
            return false;
        }
    }

}
