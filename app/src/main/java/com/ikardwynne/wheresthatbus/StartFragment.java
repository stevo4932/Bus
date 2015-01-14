package com.ikardwynne.wheresthatbus;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;


public class StartFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private String bus_selection = null;
    private static final String TAG = "StartFragment";
    private View root_view;
    private Callbacks activity;

    public StartFragment(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.start_fragment, container, false);
        fillData();

        //Handle the button clicks.
        Button find_button = (Button) root_view.findViewById(R.id.menu_button_find);
        find_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.find(bus_selection);
            }
        });

        return root_view;
    }

    //used to get user's spinner selection.
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        bus_selection = parent.getItemAtPosition(pos).toString();
        Log.v(TAG, bus_selection);
    }
    //sets the bus selection to the default spinner value.
    public void onNothingSelected(AdapterView<?> parent) {
        bus_selection = parent.getItemAtPosition(0).toString();
        Log.v(TAG, bus_selection);
    }

    private void fillData(){
        //copied from google android documentation.
        Spinner spinner = (Spinner) root_view.findViewById(R.id.bus_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.getActivity(), R.array.buses, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }



    public interface Callbacks{
        public void find(String bus_selection);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Callbacks) activity;
    }
}
