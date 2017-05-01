package net.deschulz.wherearewev0;

import android.*;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity
        extends FragmentActivity
        implements OnMapReadyCallback,LocationListener,
                    GoogleApiClient.ConnectionCallbacks {

    private GoogleMap mMap;
    private static final String TAG = "WhereAreWe";
    private static final int UPDATE_INTERVAL = 15 * 1000;
    private static final int FASTEST_UPDATE_INTERVAL = 2 * 1000;
    private static final int REQUEST_CODE_PERMISSIONS = 101;

    private GoogleApiClient mApiClient;
    /* Metadata about updates we want to receive */
    private LocationRequest mLocationRequest;
    /* Last-known device location */
    private Location mCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Verify play services is active and up to date
        int resultCode = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this);
        switch (resultCode) {
            case ConnectionResult.SUCCESS:
                Log.d(TAG, "Google Play Services is ready to go!");
                break;
            default:
                showPlayServicesError(resultCode);
                return;
        }

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();

        mLocationRequest = LocationRequest.create()
                //Set the required accuracy level
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                //Set the desired (inexact) frequency of location updates
                .setInterval(UPDATE_INTERVAL)
                //Throttle the max rate of update requests
                .setFastestInterval(FASTEST_UPDATE_INTERVAL);

    }

    @Override
    protected void onStart() {
        //mApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        //mApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        //When we move into the foreground, attach to Play Services
        mApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        //Disable updates when we are not in the foreground
        if (mApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mApiClient, this);
        }
        //Detach from Play Services
        mApiClient.disconnect();
    }

    /* update the display and disconnect */
    private void updateDisplay() {
        Log.d(TAG, "updateDisplay()");
        if (mCurrentLocation != null) {

            LatLng weAreHere =
                    new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());

            mMap.addMarker(new MarkerOptions().position(weAreHere).title("You Are Here"));

            mMap.moveCamera(CameraUpdateFactory.newLatLng(weAreHere));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        }
    }
    /*
     * When Play Services is missing or at the wrong version, the client
     * library will assist with a dialog to help the user update.
     */
    private void showPlayServicesError(int errorCode) {
        GoogleApiAvailability.getInstance()
                .showErrorDialogFragment(this, errorCode, 10,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialogInterface) {
                                finish();
                            }
                        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected to Play Services");

        //Get last known location immediately
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_PERMISSIONS);
            return;
        }
        Log.d(TAG, "calling fetchAndListenForLocation");

        fetchAndListenForLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void fetchAndListenForLocation() {
        mCurrentLocation = LocationServices.FusedLocationApi
                .getLastLocation(mApiClient);

        // the first time we start, we'll be looking at the cached position since
        // we haven't looked at the GPS yet.  Maybe this should populate the form
        // with nothing instead of calling updateDisplay()?
        if (mCurrentLocation != null) {
            Log.d(TAG, "This is the previous location -- update to it, or clear?");
            updateDisplay();
        }
        //Register for updates
        LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient,
                mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * LocationListener Callbacks
     */

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Received location update");
        mCurrentLocation = location;
        updateDisplay();
        // just get one update and then disconnect
        //mApiClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                int grantResult = grantResults[i];
                String permission = permissions[i];
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission " + permission
                            + " is required for this application to work.");
                    finish();
                    return;
                }
            }
        }
    }

}
