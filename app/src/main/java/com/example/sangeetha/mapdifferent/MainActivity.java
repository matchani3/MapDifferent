package com.example.sangeetha.mapdifferent;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.multidex.MultiDex;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends Activity {

    Spinner mSprPlaceType;

    String[] mPlaceType=null;
    String[] mPlaceTypeName=null;


    // flag for Internet connection status
    Boolean isInternetPresent = false;

    // Connection detector class
    ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // Google Places
    GooglePlaces googlePlaces;

    // Places List
    PlacesList nearPlaces;

    // GPS Location
    GPSTracker gps;

    // Button
    Button btnShowOnMap;

    // Progress dialog
    ProgressDialog pDialog;

    // Places Listview
    ListView lv;

    // ListItems data
    ArrayList<HashMap<String, String>> placesListItems = new ArrayList<HashMap<String,String>>();


    // KEY Strings
    public static String KEY_REFERENCE = "reference"; // id of the place
    public static String KEY_NAME = "name"; // name of the place
    public static String KEY_VICINITY = "vicinity"; // Place area name
    String term_to_search;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiDex.install(this);
        setContentView(R.layout.activity_main);

        // Array of place types
        mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);

        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);

        Button btnFind;
        // Getting reference to Find Button
        btnFind = ( Button ) findViewById(R.id.btn_find);

        Boolean status = this.isGooglePlayServicesAvailable(this);

        if(status!=true){ // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(0, this, requestCode);
            dialog.show();
            return;

        }
        cd = new ConnectionDetector(getApplicationContext());
            // Check if Internet present
        isInternetPresent = cd.isConnectingToInternet();
        if (!isInternetPresent) {
            // Internet Connection is not present
            alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
                    "Please connect to the Internet", false);
            // stop executing code by return
            return;
        }

        // creating GPS Class object
        gps = new GPSTracker(this);

        // check if GPS location can get
        if (gps.canGetLocation()) {
            Log.d("Your Location", "latitude:" + gps.getLatitude() + ", longitude: " + gps.getLongitude());
        } else {
            // Can't get user's current location
            alert.showAlertDialog(MainActivity.this, "GPS Status",
                    "Couldn't get location information. Please enable GPS",
                    false);
            // stop executing code by return
            return;
        }

        // Getting listview
        lv = (ListView) findViewById(R.id.list);

        btnFind.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                placesListItems.clear();
                int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                String type = mPlaceType[selectedPosition];
                term_to_search = type;
                // calling background Async task to load Google Places
                // After getting places from Google all the data is shown in listview
                new LoadPlaces().execute();
            }
        });

                 /**
                  * ListItem click event
                  * On selecting a listitem SinglePlaceActivity is launched
                  * */
         lv.setOnItemClickListener(new OnItemClickListener() {

             @Override
             public void onItemClick(AdapterView<?> parent, View view,
                                     int position, long id) {
//                 lv.setAdapter(null);
                 // getting values from selected ListItem
                 String reference = ((TextView) view.findViewById(R.id.reference)).getText().toString();

                 // Starting new intent
                 Intent in = new Intent(getApplicationContext(),
                         SinglePlaceActivity.class);

                 // Sending place refrence id to single place activity
                 // place refrence id used to get "Place full details"
                 in.putExtra(KEY_REFERENCE, reference);
                 startActivity(in);
             }
         });
     }

     /**
      * Background Async Task to Load Google places
      * */
     class LoadPlaces extends AsyncTask<String, String, String> {
         /**
          * Before starting background thread Show Progress Dialog
          * */
         @Override
         protected void onPreExecute() {
             super.onPreExecute();
             pDialog = new ProgressDialog(MainActivity.this);
             pDialog.setMessage(Html.fromHtml("<b>Search</b><br/>Loading Places..."));
             pDialog.setIndeterminate(false);
             pDialog.setCancelable(false);
             pDialog.show();
         }

         /**
          * getting Places JSON
          * */
         protected String doInBackground(String... args) {
             // creating Places class object
             googlePlaces = new GooglePlaces();

             try {
                 // Separeate your place types by PIPE symbol "|"
                 // If you want all types places make it as null
                 // Check list of types supported by google
                 //
                 String types = term_to_search; // Listing places only cafes, restaurants

                 // Radius in meters - increase this value if you don't find any places
                 double radius = 5000; // 5000 meters

                 // get nearest places
                 nearPlaces = googlePlaces.search(gps.getLatitude(),
                         gps.getLongitude(), radius, types);

             } catch (Exception e) {
                 e.printStackTrace();
             }
             return null;
         }

         /**
          * After completing background task Dismiss the progress dialog
          * and show the data in UI
          * Always use runOnUiThread(new Runnable()) to update UI from background
          * thread, otherwise you will get error
          * **/
         protected void onPostExecute(String file_url) {
             // dismiss the dialog after getting all products
             pDialog.dismiss();
             // updating UI from Background Thread
             runOnUiThread(new Runnable() {
                 public void run() {
                     /**
                      * Updating parsed Places into LISTVIEW
                      * */
                     // Get json response status
                     String status = nearPlaces.status;

                     // Check for all possible status
                     if (status.equals("OK")) {
                         // Successfully got places details
                         if (nearPlaces.results != null) {
                             // loop through each place
                             for (Place p : nearPlaces.results) {
                                 HashMap<String, String> map = new HashMap<String, String>();

                                 // Place reference won't display in listview - it will be hidden
                                 // Place reference is used to get "place full details"
                                 map.put(KEY_REFERENCE, p.reference);

                                 // Place name
                                 map.put(KEY_NAME, p.name);

                                 // adding HashMap to ArrayList
                                 placesListItems.add(map);
                             }
                             // list adapter
                             ListAdapter adapter = new SimpleAdapter(MainActivity.this, placesListItems,
                                     R.layout.list_item,
                                     new String[]{KEY_REFERENCE, KEY_NAME}, new int[]{
                                     R.id.reference, R.id.name});

                             // Adding data into listview
                             lv.setAdapter(adapter);

                         }
                     } else if (status.equals("ZERO_RESULTS")) {
                         // Zero results found
                         alert.showAlertDialog(MainActivity.this, "Near Places",
                                 "Sorry no places found. Try to change the types of places",
                                 false);
                     } else if (status.equals("UNKNOWN_ERROR")) {
                         alert.showAlertDialog(MainActivity.this, "Places Error",
                                 "Sorry unknown error occured.",
                                 false);
                     } else if (status.equals("OVER_QUERY_LIMIT")) {
                         alert.showAlertDialog(MainActivity.this, "Places Error",
                                 "Sorry query limit to google places is reached",
                                 false);
                     } else if (status.equals("REQUEST_DENIED")) {
                         alert.showAlertDialog(MainActivity.this, "Places Error",
                                 "Sorry error occured. Request is denied",
                                 false);
                     } else if (status.equals("INVALID_REQUEST")) {
                         alert.showAlertDialog(MainActivity.this, "Places Error",
                                 "Sorry error occured. Invalid Request",
                                 false);
                     } else {
                         alert.showAlertDialog(MainActivity.this, "Places Error",
                                 "Sorry error occured.",
                                 false);
                     }
                 }
             });

         }

     }

     @Override
     public boolean onCreateOptionsMenu(Menu menu) {
         MenuInflater menuInflater = getMenuInflater();
         menuInflater.inflate(R.menu.menu_main, menu);
         return true;
     }

     public boolean isGooglePlayServicesAvailable(Context context) {
         GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
         int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
         return resultCode == ConnectionResult.SUCCESS;
     }
}
