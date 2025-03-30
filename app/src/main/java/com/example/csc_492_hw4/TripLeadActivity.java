package com.example.csc_492_hw4;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.csc_492_hw4.databinding.ActivityTripLeadBinding;
import com.example.csc_492_hw4.databinding.DialogTripIdBinding;
import com.example.csc_492_hw4.databinding.FollowMeTitleBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.SphericalUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TripLeadActivity extends AppCompatActivity implements OnMapReadyCallback{

    //private com.example.csc_492_hw4.databinding.ActivityTripLeadBinding binding;
    private ActivityTripLeadBinding binding;
    private String TripID;
    private GoogleMap mMap;
    private static final int LOCATION_REQUEST = 111;
    //private static final int LOCATION_REQUEST = 111;
    private static final int BACKGROUND_LOCATION_REQUEST = 222;
    private static final int NOTIFICATION_REQUEST = 333;
    private PointReceiver pointReceiver;
    private Intent locationServiceIntent;
    private LocationManager locationManager;
    private String TAG = "TripLeadAct";
    private LocationListener locationListener;
    private Polyline llHistoryPolyline;
    private final ArrayList<LatLng> latLonHistory = new ArrayList<>();
    private Marker carMarker;
    public static int screenHeight;
    public static int screenWidth;
    private final float zoomDefault = 15.0f;
    private double distance = 0.0;
    private Handler handler = new Handler();
    private long startTime = 0;
    private String firstName;
    private String lastName;
    private String username;
    private TestTripPointsVolley getTripPointsForLeadVolley;
    private  AddTripPointAPIVolley addTripPointAPIVolley;
    private int checkPoint = -1;
    private boolean pause = false;
    TripExistsAPIVolley tripExistsAPIVolley;
    private String password;
    private boolean soundOnce= false;
    private AnimatorSet animatorSet;
    private ObjectAnimator objectAnimator1;
    private ObjectAnimator objectAnimator2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        binding = ActivityTripLeadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getTripPointsForLeadVolley = new TestTripPointsVolley(this);
        tripExistsAPIVolley = new TripExistsAPIVolley(this);


        addTripPointAPIVolley = new AddTripPointAPIVolley(this);

        Intent intent = getIntent();



        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        TripID = intent.getStringExtra("TRIPID");
        username = intent.getStringExtra("USERNAME");
        firstName = intent.getStringExtra("FIRSTNAME");
        lastName = intent.getStringExtra("LASTNAME");
        password = intent.getStringExtra("PASSWORD");
        binding.startTripId.setText("Trip Id: " + TripID);

        objectAnimator1 =
                ObjectAnimator.ofFloat(binding.gps, "alpha", 1.0f, 0.25f);
        objectAnimator2 =
                ObjectAnimator.ofFloat(binding.gpsText, "alpha", 1.0f, 0.25f);




        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.start_trip_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }






        // Get the current date and time
        Date now = new Date();
        // Define the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d, hh:mm a", Locale.getDefault());

        // Format the current date and time
        String formattedDate = sdf.format(now);
        binding.startDate.setText("Trip Start: " + formattedDate);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
    }

    public void tripExists() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Id Exists");
        builder.setMessage("Trip Id " + TripID + " exists. Please make a different id. Returning to main menu");
        builder.setPositiveButton("OK", (dialogInterface, which) -> {
            // Dismiss the dialog
            //TripIDDialog();

            Intent intent = new Intent(this, MainActivity.class);



            finish();

        });
        builder.create().show();

    }

    public void tripNotExist() {

        if(!soundOnce){

            soundOnce = true;
        }
        SoundPlayer.getInstance().setupSound(this, "engine", R.raw.notif_sound, false);

        doSound1();
        doLocations();
        //setupLocationListener();


    }

    public void tripExistsError(String s, String tripId) {
        //binding.progressBar.setVisibility(View.GONE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Error");
        builder.setMessage("Trip Id " + tripId + " error: " + s);

        builder.setPositiveButton("OK", (dialogInterface, which) -> {
            finish();
        });
        builder.create().show();
    }

    private void startObjectAnimators() {

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(binding.pulse, "alpha", 1.0f, 0.25f, 1.0f);
        objectAnimator.setDuration(500); // Total duration for one pulse cycle (fade out and fade in)
        objectAnimator.start();
    }

    private void startObjectAnimators2() {

        objectAnimator1.setDuration(750);
        objectAnimator1.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator1.setRepeatMode(ObjectAnimator.REVERSE);

        objectAnimator2.setDuration(750);
        objectAnimator2.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator2.setRepeatMode(ObjectAnimator.REVERSE);

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(objectAnimator1, objectAnimator2);
        animatorSet.start();
    }

    public void handleAddTripPointSuccess(String tripId, String latitude,
                                          String longitude, String datetime,
                                          String userName) {
//        //binding.progressBar.setVisibility(View.GONE);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Trip Point Added");
//        builder.setMessage("Trip Id: " + tripId + "\n" +
//                "Latitude: " + latitude + "\n" +
//                "Longitude: " + longitude + "\n" +
//                "Datetime: " + datetime + "\n" +
//                "User Name: " + userName);
//        builder.setPositiveButton("OK", null);
//        builder.create().show();
//        if (points.isEmpty()) {
//            binding.addPointButton.setEnabled(false);
//        }
//        binding.getPrevPointsButton.setEnabled(true);
    }

    public void handleAddTripPointFail(String s) {

    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {


        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);



        if (checkPermission()) {
            // Get the last known location
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                LatLng lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                //mMap.addMarker(new MarkerOptions().position(lastKnownLatLng).title("Last Known Location"));
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, zoomDefault));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, zoomDefault));
            }




            startTime = System.currentTimeMillis();
            tripExistsAPIVolley.tripExists(TripID);

            //doLocations();
            //setupLocationListener();
        }
    }

    public void doSound1() {
        if (notReady())
            return;
        SoundPlayer.getInstance().start("engine");
    }
    @SuppressLint("SetTextI18n")
    public void NoNetworkTripID(){


        addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);

        binding.noNetworkTextLead.setVisibility(View.VISIBLE);
        binding.noNetworkTextLead.setText("NO NETWORK CONNECTION \n Not all data may be received");
        if(locationListener != null){
            locationManager.removeUpdates(locationListener);
            addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);

            locationListener = null;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me - No Network");
        bindtitle.followMeMessage.setText("No network connection - cannot verify Trip id now.\n"
        +"Stopping the Trip now.");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogTripIdBinding bind = DialogTripIdBinding.inflate(getLayoutInflater());


        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("OK", (dialogInterface, which) -> {
            finish();
        });



        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    private void setupLocationListener() {





        locationListener = new MyLocListener(this);

        //minTime	    long: minimum time interval between location updates, in milliseconds
        //minDistance	float: minimum distance between location updates, in meters
        if (checkPermission() && locationManager != null)
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 15, locationListener);

    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, LOCATION_REQUEST);
            return false;
        }
        return true;
    }

    public void updateLocation(LatLng l, float bearing) {


        LatLng latLng = new LatLng(l.latitude, l.longitude);

        latLonHistory.add(latLng); // Add the LL to our location history


        if (llHistoryPolyline != null) {
            llHistoryPolyline.remove(); // Remove old polyline
        }

        if (latLonHistory.size() == 1) { // First update


            addTripPointAPIVolley.sendPoint(TripID, latLng.latitude, latLng.longitude, new Date(),username);
            startObjectAnimators();

            mMap.addMarker(new MarkerOptions().alpha(0.5f).position(latLng).title("My Origin"));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomDefault));
            binding.gps.setVisibility(View.INVISIBLE);
            binding.gpsText.setVisibility(View.INVISIBLE);
            objectAnimator2.cancel();
            objectAnimator1.cancel();
//            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(binding.gps, "alpha", 1.0f, 0.0f);
//            objectAnimator.setDuration(2000);
//            ObjectAnimator objectAnimator2 = ObjectAnimator.ofFloat(binding.gpsText, "alpha", 1.0f, 0.0f);
//            objectAnimator2.setDuration(2000);
            String formattedDistance = String.format("%.1f", distance);
            binding.distance.setText("Distance: " + formattedDistance + " km");
            //binding.elapsedTime.setText("Elapsed Time: 00h:00m:00s");
            //
            // startTime = System.currentTimeMillis();
            //handler.postDelayed(updateTimerThread, 0);
            long elapsedTime = System.currentTimeMillis() - startTime;

            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;

            seconds = seconds % 60;
            minutes = minutes % 60;

            binding.elapsedTime.setText("Elapsed Time: "+String.format("%02dh %02dm %02ds", hours, minutes, seconds));

            return;
        }

        if (latLonHistory.size() > 1) { // Second (or more) update
            startObjectAnimators();
            PolylineOptions polylineOptions = new PolylineOptions();

            if (!pause) {

//                if(checkPoint> -1){
//
//                    for(int i = checkPoint; i < latLonHistory.size() -1; i++){
//                        LatLng point = latLonHistory.get(i);
//                        addTripPointAPIVolley.sendPoint(TripID, point.latitude, point.longitude, new Date(),username);
//                    }
//
//                    checkPoint = -1;
//
//
//                }
                addTripPointAPIVolley.sendPoint(TripID, latLng.latitude, latLng.longitude, new Date(),username);
            }


            for (LatLng ll : latLonHistory) {
                polylineOptions.add(ll);
            }
            llHistoryPolyline = mMap.addPolyline(polylineOptions);
            llHistoryPolyline.setEndCap(new RoundCap());
            llHistoryPolyline.setWidth(12);
            llHistoryPolyline.setColor(Color.BLUE);

            LatLng point1 = latLonHistory.get(latLonHistory.size()-2);
            LatLng point2 = latLonHistory.get(latLonHistory.size()-1);
            distance += calculateDistance(point1,point2);
            DecimalFormat df = new DecimalFormat("#.0");
            String formattedValue = df.format(distance);
            //double distance = 0.2345;
            String formattedDistance = String.format("%.1f", distance);
            binding.distance.setText("Distance: " + formattedDistance + " km");
            long elapsedTime = System.currentTimeMillis() - startTime;

            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;

            seconds = seconds % 60;
            minutes = minutes % 60;

            binding.elapsedTime.setText("Elapsed Time: "+String.format("%02dh %02dm %02ds", hours, minutes, seconds));
            float r = getRadius();
            if (r > 0) {
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
                Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
                BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);

                MarkerOptions options = new MarkerOptions();
                options.position(latLng);
                options.icon(iconBitmap);
                options.rotation(bearing);

                if (carMarker != null) {
                    carMarker.remove();
                }

                carMarker = mMap.addMarker(options);
            }
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        //getTripPointsForLeadVolley.getPoints(TripID);
        binding.gps.setVisibility(View.INVISIBLE);
    }

    private float getRadius() {
        float z = mMap.getCameraPosition().zoom;
        return 15f * z - 130f;
    }

    public static double calculateDistance(LatLng point1, LatLng point2) {
        // Calculate the distance in meters
        double distanceInMeters = SphericalUtil.computeDistanceBetween(point1, point2);

        // Convert the distance to kilometers
        double distanceInKilometers = distanceInMeters / 1000.0;


        double roundedValue = round(distanceInKilometers, 2);

        // Round to two decimal places
        return roundedValue;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null)
            locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission() && locationManager != null && locationListener != null)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, locationListener);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;

            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;

            seconds = seconds % 60;
            minutes = minutes % 60;

            binding.elapsedTime.setText("Elapsed Time: "+String.format("%02d:%02d:%02d", hours, minutes, seconds));

            handler.postDelayed(this, 1000);
        }
    };

    public void shareContent(View v) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Follow Me Trip ID: " + TripID);
        shareIntent.putExtra(Intent.EXTRA_TEXT, firstName + " " + lastName +" has shared a 'Follow" +
                " Me' Trip Id with you.\n\n"
        +"Use Follow Me Trip ID: " + TripID);

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    public void stopLocations(View v) {

//        if(locationListener != null){
//            locationManager.removeUpdates(locationListener);
//            addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);
//
//            locationListener = null;
//        }


        if (pointReceiver != null) {
            unregisterReceiver(pointReceiver);
        }
        stopService(locationServiceIntent);
        addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);
        finish();

    }

    public void pauseButton(View v){
        if(!pause){
            binding.pausePlay.setImageResource(R.drawable.play);
            binding.trackPauseWords.setVisibility(View.VISIBLE);
            checkPoint = latLonHistory.size() -1;
            pause =true;
        }
        else{
            binding.pausePlay.setImageResource(R.drawable.pause);
            binding.trackPauseWords.setVisibility(View.INVISIBLE);
            pause = false;
        }
    }

//    @Override
//    protected void onDestroy() {
//        if(locationListener != null){
//            locationManager.removeUpdates(locationListener);
//            addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);
//
//        }
//        super.onDestroy();
//    }

    private boolean notReady() {
        if (SoundPlayer.loadCount == SoundPlayer.doneCount) {
            return false;
        }
        String msg = String.format(Locale.getDefault(),
                "Sound loading not complete (%d of %d),\n" +
                        "Please try again in a moment",
                SoundPlayer.doneCount, SoundPlayer.loadCount);
        //Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        return true;
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == LOCATION_REQUEST) {
//            for (int i = 0; i < permissions.length; i++) {
//                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
//                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
//                        setupLocationListener();
//                    } else {
//                        Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//        }
//    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST) {
            if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
        if (requestCode == NOTIFICATION_REQUEST) {
            if (permissions[0].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    Toast.makeText(this, "Notification Permission not Granted", Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent(this, MainActivity.class);
//
//
//                    startActivity(intent);
                    finish();
                }

            }
        }
        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (permissions[0].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationService();

                } else {
                    Toast.makeText(this, "Background Location Permission not Granted", Toast.LENGTH_SHORT).show();
                    finish();
                }

            }
        }

    }









    public void doLocations() {
        boolean hasPerm = checkAppPermission();
        if (hasPerm) {

            startLocationService();
        }

    }


    private void startLocationService() {

        if (checkAppPermission()) {
            // Create a receiver to get the location updates
            pointReceiver = new PointReceiver(this);

            // Register the receiver
            ContextCompat.registerReceiver(this,
                    pointReceiver,
                    new IntentFilter("com.example.broadcast.MY_BROADCAST"),
                    ContextCompat.RECEIVER_EXPORTED);
        }
        //starting service
        locationServiceIntent = new Intent(this, LocationService.class);

        Log.d(TAG, "startService: START");
        startObjectAnimators2();
        ContextCompat.startForegroundService(this, locationServiceIntent);
        Log.d(TAG, "startService: END");

    }
    private boolean checkAppPermission() {

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    }, LOCATION_REQUEST);
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.POST_NOTIFICATIONS
                        }, NOTIFICATION_REQUEST);
                return false;
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, BACKGROUND_LOCATION_REQUEST);
            return false;
        }

        return true;
    }

    public void getTripPointsError(String s, String tripId) {

    }

    public void NoNetworkEarlyPoints() {


        addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);

        binding.noNetworkTextLead.setVisibility(View.VISIBLE);
        binding.noNetworkTextLead.setText("NO NETWORK CONNECTION \n Not all data may be received");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me - No Network");
        bindtitle.followMeMessage.setText("No network connection - cannot get earlier data now.\n\n"
                +"Stopping the Trip now.");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogTripIdBinding bind = DialogTripIdBinding.inflate(getLayoutInflater());


        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("OK", (dialogInterface, which) -> {

        });



        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void tripNotFound(String tripId) {
    }

    public void noNetworkSendPoint() {

        //addTripPointAPIVolley.sendPoint(TripID, 0, 0, new Date(),username);

        binding.noNetworkTextLead.setVisibility(View.VISIBLE);
        binding.noNetworkTextLead.setText("NO NETWORK CONNECTION \n Not all data may be sent");


    }
}