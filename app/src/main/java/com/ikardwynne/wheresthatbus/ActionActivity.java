package com.ikardwynne.wheresthatbus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class ActionActivity extends Activity {
    private static final String TAG = "ActionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String message = null;
        String action = getIntent().getStringExtra("action");
        if(action != null) {

            switch (action){
                case "onfoot":
                    message = " Google seems to think you are walking.\n" +
                            "Are you still waiting for the bus?";
                    break;
                case "vehicle":
                    message = " Google seems to think you are in a vehicle.\n" +
                            "Are you on the bus?";
                    break;
                default:
                    Log.d(TAG, "Error: action is: "+action);
                    setResult(Activity.RESULT_CANCELED, new Intent());
                    finish();
            }

            final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(getResources().getString(R.string.app_name));
            alertDialog.setMessage(message);
            alertDialog.setButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    send("yes");
                }
            });
            alertDialog.setButton2("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    send("no");
                }
            });
            alertDialog.setButton3("Exit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    send("exit");
                }
            });
            alertDialog.show();
        }else{
            Log.v(TAG, "activity string null");
            setResult(Activity.RESULT_CANCELED, new Intent());
            finish();
        }
    }

    private void send(String selection){
        Intent intent = new Intent();
        intent.putExtra("selection", selection);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
