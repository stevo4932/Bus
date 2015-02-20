package com.ikardwynne.wheresthatbus;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {

    private static final String TAG = "ActivityRecognitionIntentService";
    private static final int MIN_CONFIDENCE_LEVEL = 70;
    private static final int UPDATE_ID = 5;
    private int lastActivity = -1;

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
            String activityName = getNameFromType(activityType);

            Log.d(TAG, "activity: "+activityName);
            if(lastActivity != activityType && confidence >= MIN_CONFIDENCE_LEVEL){
                if(activityType == DetectedActivity.IN_VEHICLE) {
                    sendNotification(VEHICLE);
                    lastActivity = activityType;
                }else if(activityType == DetectedActivity.ON_FOOT) {
                    sendNotification(FOOT);
                    lastActivity = activityType;
                }
            }
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
        mBuilder.setAutoCancel(true);
        mBuilder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);


        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // UPDATE_ID allows you to update the notification later on.
        mNotificationManager.notify(UPDATE_ID, mBuilder.build());
    }
}
