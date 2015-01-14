package com.ikardwynne.wheresthatbus;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class NotificationFragment extends Fragment {

    private static final String TAG = "NotificationFragment";
    private static final String ON_FOOT = "onfoot";
    private static final String IN_VEHICLE = "vehicle";

    private static final String ON_FOOT_MESSAGE = " Google thinks you are walking.\n" +
                                                  "Are you still waiting for the bus?";
    private static final String IN_VEHICLE_MESSAGE = " Google thinks you are in a vehicle.\n" +
                                                     "Are you on the bus?";

    private View view;
    private String activity;

    private NotificationCallbacks callbacks;

    public NotificationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getArguments().getString("activity");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_notification, container, false);
        setTextBox();
        setButtons();
        return view;


    }

    private void setTextBox(){
        TextView textbox = (TextView) view.findViewById(R.id.textbox1);
        switch (activity){
            case ON_FOOT:
                textbox.setText(ON_FOOT_MESSAGE);
                break;
            case IN_VEHICLE:
                textbox.setText(IN_VEHICLE_MESSAGE);
                break;
            default:
                Log.d(TAG, "Error: no activity reported");
        }
    }

    private void setButtons(){

        //yes button press
        view.findViewById(R.id.button_yes).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (activity){
                    case ON_FOOT:
                        callbacks.waitForBus();
                        break;
                    case IN_VEHICLE:
                        callbacks.updateBus();
                        break;
                    default:
                        Log.d(TAG, "ERROR: activity no avalible on click");
                }
            }
        });

        //no button press
        view.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (activity){
                    case ON_FOOT:
                        Log.i(TAG, "no button pressed on foot");
                        callbacks.exit();
                        break;
                    case IN_VEHICLE:
                        callbacks.waitForBus();
                        break;
                    default:
                        Log.d(TAG, "ERROR: activity no avalible on click");
                }
            }
        });

        //exit button press
        view.findViewById(R.id.button_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               callbacks.exit();
            }
        });
    }

    public interface NotificationCallbacks{
        public void updateBus();
        public void waitForBus();
        public void exit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = (NotificationCallbacks) activity;
    }


}
