package com.ikardwynne.wheresthatbus;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {

    private static final String TAG = "ActivityRecognitionIntentService";
    private static final int MIN_CONFIDENCE_LEVEL = 75;
    private static boolean isShowing = false;
    private static final int UPDATE_ID = 5;

    public static final String FOOT = "onfoot";
    public static final String VEHICLE = "vehicle";
    private static final int NOTIFCATION_CODE = 1333;

    public ActivityRecognitionIntentService() {
        // Set the label for the service's background thread
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            // Get the update
            ActivityRecognitionResult result =
                    ActivityRecognitionResult.extractResult(intent);
            // Get the most probable activity
            DetectedActivity mostProbableActivity =
                    result.getMostProbableActivity();

            /*Get the probability that this activity is the
            the user's actual activity*/
            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();

            //REMOVE: for testing purposes only
            //String activityName = getNameFromType(activityType);
            //sendNotification(activityName+" Confidence Level: " + confidence);

            if((activityType == DetectedActivity.IN_VEHICLE) && confidence >= MIN_CONFIDENCE_LEVEL)
                ///sendIntent(VEHICLE);
                sendNotification(VEHICLE);

            else if(activityType == DetectedActivity.ON_FOOT && confidence >= MIN_CONFIDENCE_LEVEL)
                //sendIntent(FOOT);
                sendNotification(FOOT);


        } else {
            /*This implementation ignores intents that don't contain
              an activity update. */
            Log.d(TAG, "Error: No activity update");
        }

    }

    //REMOVE: for testing purposes only.
    private String getNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }

    private void sendIntent(String activity){
        isShowing = true;
        Log.v("intent", "sending intent");
        Intent activityintent = new Intent("user_activity");
        activityintent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        activityintent.putExtra("Activity", activity);
        LocalBroadcastManager.getInstance(this).sendBroadcast(activityintent);
    }

    private void sendNotification(String activity){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.common_ic_googleplayservices)
                        .setContentTitle("Where's That Bus");

        switch (activity){
            case FOOT:
                mBuilder.setContentText("Are you still waiting for the bus?");
                break;
            case VEHICLE:
                mBuilder.setContentText("Are you on the bus?");
                break;
            default:
                mBuilder.setContentText("Activity: "+activity);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("activity", activity);

        PendingIntent pendIntent = PendingIntent.getActivity(this, NOTIFCATION_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pendIntent);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // UPDATE_ID allows you to update the notification later on.
        mNotificationManager.notify(UPDATE_ID, mBuilder.build());
    }
}
