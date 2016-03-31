package com.example.sangeetha.mapdifferent;

/**
 * Created by sangeetha on 3/30/16.
 */
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.app.Dialog;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class SinglePlaceActivity extends FragmentActivity implements LocationListener  {

    GoogleMap mGoogleMap;
    double mLatitude=0;
    double mLongitude=0;

    // flag for Internet connection status
    Boolean isInternetPresent = false;

    // Connection detector class
    ConnectionDetector cd;

    // Alert Dialog Manager
    AlertDialogManager alert = new AlertDialogManager();

    // Google Places
    GooglePlaces googlePlaces;

    // Place Details
    PlaceDetails placeDetails;

    // Progress dialog
    ProgressDialog pDialog;

    // KEY Strings
    public static String KEY_REFERENCE = "reference"; // id of the place

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.single_place);
        // Getting reference to the SupportMapFragment

        SupportMapFragment fragment = ( SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Getting Google Map
        mGoogleMap = fragment.getMap();

        // Enabling MyLocation in Google Map
        mGoogleMap.setMyLocationEnabled(true);

        // Getting LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location From GPS
        Location location = locationManager.getLastKnownLocation(provider);
        System.out.print("location is " + location);

        if(location!=null){
            onLocationChanged(location);
        }

        locationManager.requestLocationUpdates(provider, 20000, 0, this);

        Intent i = getIntent();

        // Place referece id
        String reference = i.getStringExtra(KEY_REFERENCE);

        // Calling a Async Background thread
        new LoadSinglePlaceDetails().execute(reference);
    }



    /**
     * Background Async Task to Load Google places
     * */
    class LoadSinglePlaceDetails extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(SinglePlaceActivity.this);
            pDialog.setMessage("Loading profile ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Profile JSON
         * */
        protected String doInBackground(String... args) {
            String reference = args[0];

            // creating Places class object
            googlePlaces = new GooglePlaces();

            // Check if used is connected to Internet
            try {
                placeDetails = googlePlaces.getPlaceDetails(reference);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
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
                    if (placeDetails != null) {
                        String status = placeDetails.status;

                        // check place deatils status
                        // Check for all possible status
                        if (status.equals("OK")) {
                            if (placeDetails.result != null) {
                                String name = placeDetails.result.name;
                                String address = placeDetails.result.formatted_address;
                                String phone = placeDetails.result.formatted_phone_number;
                                String latitude = Double.toString(placeDetails.result.geometry.location.lat);
                                String longitude = Double.toString(placeDetails.result.geometry.location.lng);

                                Log.d("Place ", name + address + phone + latitude + longitude);

                                // Displaying all the details in the view
                                // single_place.xml
                                TextView lbl_name = (TextView) findViewById(R.id.name);
                                TextView lbl_address = (TextView) findViewById(R.id.address);
                                TextView lbl_phone = (TextView) findViewById(R.id.phone);
                                TextView lbl_location = (TextView) findViewById(R.id.location);

                                // Check for null data from google
                                // Sometimes place details might missing
                                name = name == null ? "Not present" : name; // if name is null display as "Not present"
                                address = address == null ? "Not present" : address;
                                phone = phone == null ? "Not present" : phone;
                                latitude = latitude == null ? "Not present" : latitude;
                                longitude = longitude == null ? "Not present" : longitude;

                                lbl_name.setText(name);
                                lbl_address.setText(address);
                                lbl_phone.setText(Html.fromHtml("<b>Phone:</b> " + phone));
//                                lbl_location.setText(Html.fromHtml("<b>Latitude:</b> " + latitude + ", <b>Longitude:</b> " + longitude));

                                // Clears all the existing markers
                                mGoogleMap.clear();


                                // Creating a marker
                                MarkerOptions markerOptions = new MarkerOptions();

                                // Getting a place from the places list
//                                    HashMap<String, String> hmPlace = list.get(i);

                                // Getting latitude of the place
                                double lat = Double.parseDouble(latitude);
                                Log.d("Latitude is ", latitude);

                                // Getting longitude of the place
                                double lng = Double.parseDouble(longitude);
                                Log.d("Longtitude is ", longitude);

                                // Getting name
//                                    String name = hmPlace.get("place_name");

                                // Getting vicinity
//                                    String vicinity = hmPlace.get("vicinity");

                                LatLng latLng = new LatLng(lat, lng);

                                // Setting the position for the marker
                                markerOptions.position(latLng);

                                // Setting the title for the marker.
                                //This will be displayed on taping the marker
                                markerOptions.title(name);

                                // Placing a marker on the touched position
                                mGoogleMap.addMarker(markerOptions);

                            }
                        } else if (status.equals("ZERO_RESULTS")) {
                            alert.showAlertDialog(SinglePlaceActivity.this, "Near Places",
                                    "Sorry no place found.",
                                    false);
                        } else if (status.equals("UNKNOWN_ERROR")) {
                            alert.showAlertDialog(SinglePlaceActivity.this, "Places Error",
                                    "Sorry unknown error occured.",
                                    false);
                        } else if (status.equals("OVER_QUERY_LIMIT")) {
                            alert.showAlertDialog(SinglePlaceActivity.this, "Places Error",
                                    "Sorry query limit to google places is reached",
                                    false);
                        } else if (status.equals("REQUEST_DENIED")) {
                            alert.showAlertDialog(SinglePlaceActivity.this, "Places Error",
                                    "Sorry error occured. Request is denied",
                                    false);
                        } else if (status.equals("INVALID_REQUEST")) {
                            alert.showAlertDialog(SinglePlaceActivity.this, "Places Error",
                                    "Sorry error occured. Invalid Request",
                                    false);
                        } else {
                            alert.showAlertDialog(SinglePlaceActivity.this, "Places Error",
                                    "Sorry error occured.",
                                    false);
                        }
                    } else {
                        alert.showAlertDialog(SinglePlaceActivity.this, "Places Error",
                                "Sorry error occured.",
                                false);
                    }


                }
            });

        }



    }
    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        LatLng latLng = new LatLng(mLatitude, mLongitude);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }
    //
    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }
    //
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

}
