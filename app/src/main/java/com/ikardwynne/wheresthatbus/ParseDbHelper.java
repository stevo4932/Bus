package com.ikardwynne.wheresthatbus;

import android.content.Context;
import android.location.Location;

import com.parse.Parse;
public class ParseDbHelper {

    Context ctx;

    public ParseDbHelper(Context context){
        ctx = context;
        Parse.enableLocalDatastore(ctx);
        Parse.initialize(ctx, "hQN6CcxhmWfWDEX8wnoHx8Nx7BXmaI2asE7WMFUg", "wdlYdmu64NeJR4JMiYZePM3114cZvvoRvwTJRK9T");
    }

    public Location getBusLocation(String bus){
        return null;
    }

    public void setBusLocation(String bus, Location location){

    }

}
