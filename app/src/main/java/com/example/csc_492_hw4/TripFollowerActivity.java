package com.example.csc_492_hw4;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.csc_492_hw4.databinding.ActivityTripFollowerBinding;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TripFollowerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityTripFollowerBinding binding;
    private String TripID;
    private GoogleMap mMap;
    private final float zoomDefault = 15.0f;
    private static final int LOCATION_REQUEST = 111;
    private Polyline llHistoryPolyline;
    private ArrayList<LatLngTime> latlonHistory =new ArrayList<>();
    private Marker carMarker;
    private Handler handler = new Handler();
    private TripFollowerActivity main = this;
    private LatLng currentLocation = new LatLng(0,0);
    private double distance = 0;
    private long totalTime = 0;
    private long startTime = 0;
    GetLastLocationAPIVolley getLastLocationAPIVolley;
    private boolean autoZoom = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        binding = ActivityTripFollowerBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        getLastLocationAPIVolley = new GetLastLocationAPIVolley(this);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });

        Intent intent = getIntent();
        TripID = intent.getStringExtra("ID");

        binding.followTripId.setText("Trip ID: " + TripID);
        binding.progressBar.setVisibility(View.VISIBLE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.follow_trip_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {


        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (checkPermission()) {
            GetTripPointsAPIVolley getPreviousTripPointsAPIVolley = new GetTripPointsAPIVolley(this);
            getPreviousTripPointsAPIVolley.getPoints(TripID);
        }
    }

    public static String formatDate(Date date) {


        SimpleDateFormat desiredFormat = new SimpleDateFormat("EEE MMM dd, hh:mm a", Locale.US);
        desiredFormat.setTimeZone(TimeZone.getDefault());

        return desiredFormat.format(date);
    }

    public void acceptInitialPathPoints(ArrayList<LatLngTime> points) {
//        binding.progressBar.setVisibility(View.GONE);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Initial Path Points");
        //StringBuilder sb = new StringBuilder();
        latlonHistory.addAll(points);
        LatLngTime last = latlonHistory.get(points.size()-1);
        boolean point1 = last.getLatLng().latitude == 0;
        boolean point2 = last.getLatLng().longitude == 0;
//        if(point1 && point2){
//            latlonHistory.remove(latlonHistory.size()-1);
//        }

        LatLng firstPoint = points.get(0).getLatLng();
        startTime = points.get(0).getDateTime().getTime();
        mMap.addMarker(new MarkerOptions().alpha(0.5f).position(firstPoint).title("My"));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlonHistory.get(latlonHistory.size()-1).getLatLng(), zoomDefault));
        binding.followDate.setText("Trip Start: " + formatDate(latlonHistory.get(0).getDateTime()));
        PolylineOptions polylineOptions = new PolylineOptions();
        DecimalFormat df = new DecimalFormat("#.0");
        //String formattedValue = df.format(value);
        for (int i = 1; i < latlonHistory.size();i++) {

            LatLngTime point = latlonHistory.get(i);
            if(point.getLatLng().latitude == 0.0 && point.getLatLng().longitude == 0.0){
                continue;
            }
            LatLng latLng = point.getLatLng();
            polylineOptions.add(latLng);
            distance += calculateDistance(point.getLatLng(),points.get(i-1).getLatLng());
            //totalTime += point.getDateTime().getTime();

        }

        totalTime = points.get(points.size()-1).getDateTime().getTime();
        long elapsedTime = totalTime - startTime;

        int seconds = (int) (elapsedTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        binding.followTime.setText("Elapsed Time: "+String.format("%02dh %02dm %02ds", hours, minutes, seconds));


        String formattedValue = df.format(distance);
        binding.followDistance.setText("Distance: "+ formattedValue + " km");

        llHistoryPolyline = mMap.addPolyline(polylineOptions);
        llHistoryPolyline.setEndCap(new RoundCap());
        llHistoryPolyline.setWidth(12);
        llHistoryPolyline.setColor(Color.BLUE);
        binding.progressBar.setVisibility(View.GONE);

        GetLastLocationAPIVolley getLastLocationAPIVolley = new GetLastLocationAPIVolley(this);
        getLastLocationAPIVolley.getLastLocation(TripID);
        //float r = getRadius();
//        if (r > 0) {
//            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
//            Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
//            BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);
//
//            MarkerOptions options = new MarkerOptions();
//            options.position(latLng);
//            options.icon(iconBitmap);
//            options.rotation(location.getBearing());
//
//            if (carMarker != null) {
//                carMarker.remove();
//            }
//
//            carMarker = mMap.addMarker(options);
//        }

    }

    public void tripNotFound(String tripId) {

        binding.progressBar.setVisibility(View.GONE);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());

        bindtitle.followMeTitle.setText("Trip Not Found");

        String errorMessage = "The Trip id " + tripId + " was not found.";
        bindtitle.followMeMessage.setText(errorMessage);

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        //DialogLoginBinding bind = DialogLoginBinding.inflate(getLayoutInflater());
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, which) -> {

//            Intent intent = new Intent(this, MainActivity.class);
//
//
//            startActivity(intent);
            finish();

        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void getTripPointsError(String s, String tripId) {
//        binding.progressBar.setVisibility(View.GONE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Get Trip Point Failed");
        builder.setMessage("Trip Id " + tripId + " error: " + s);
        builder.setPositiveButton("OK", (dialogInterface, which) -> {
            finish();
        });
        builder.create().show();
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

    public void MapMarker(){
        float r = getRadius();

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
        Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
        BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);

        MarkerOptions options = new MarkerOptions();
        options.position(latlonHistory.get(latlonHistory.size()-1).getLatLng());
        Location location = new Location("provider");
        location.setLatitude(latlonHistory.get(latlonHistory.size()-2).getLatLng().latitude);
        location.setLongitude(latlonHistory.get(latlonHistory.size()-2).getLatLng().longitude);

//            // Create a Location object and set its latitude and longitude
//            Location location = new Location("provider");
//            location.setLatitude(41.8781);
//            location.setLongitude(-87.6298);

        // Assuming you have another location to calculate the bearing
        Location destination = new Location("provider");
        destination.setLatitude(latlonHistory.get(latlonHistory.size()-1).getLatLng().latitude);
        destination.setLongitude(latlonHistory.get(latlonHistory.size()-1).getLatLng().longitude);

        // Calculate the bearing
        float bearing = location.bearingTo(destination);

        options.position(latlonHistory.get(latlonHistory.size()-2).getLatLng());
        options.icon(iconBitmap);
        options.rotation(bearing);



        if (carMarker != null) {
            carMarker.remove();
        }

        carMarker = mMap.addMarker(options);

        //mMap.animateCamera(CameraUpdateFactory.newLatLng(latlonHistory.get(latlonHistory.size()-1).getLatLng()));

        if(autoZoom){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlonHistory.get(latlonHistory.size()-2).getLatLng(), zoomDefault));
        }

    }

    public void handleLastLocationSuccess(LatLngTime llt) {
//        //binding.progressBar.setVisibility(View.GONE);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (llt == null) {
            binding.followTripTitle.setText("Trip Ended");
            builder.setTitle("Trip Ended");
            builder.setMessage("Trip Id " + TripID + " has ended.");
            builder.setPositiveButton("OK", null);
            builder.create().show();
            MapMarker();
            return;
        }

        double lat = llt.getLatLng().latitude;
        double lon = llt.getLatLng().longitude;
        boolean point1 = lat == currentLocation.latitude;
        boolean point2 = lon == currentLocation.longitude;

        if(!point1 || !point2){
            currentLocation = new LatLng(lat,lon);
        }
        else{
            //GetLastLocationAPIVolley getLastLocationAPIVolley = new GetLastLocationAPIVolley(main);
            getLastLocationAPIVolley.getLastLocation(TripID);
            return;
        }

        latlonHistory.add(llt);
        startObjectAnimators();
        PolylineOptions polylineOptions = new PolylineOptions();
        for (LatLngTime ll : latlonHistory) {
            polylineOptions.add(ll.getLatLng());
        }
        distance += calculateDistance(llt.getLatLng(),latlonHistory.get(latlonHistory.size() -2).getLatLng());

        DecimalFormat df = new DecimalFormat("#.0");
        String formattedValue = df.format(distance);
        String formattedDistance = String.format("%.1f", distance);
        binding.followDistance.setText("Distance: " + formattedDistance + " km");


        totalTime = llt.getDateTime().getTime();
        long elapsedTime = totalTime - startTime;

        int seconds = (int) (elapsedTime / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        binding.followTime.setText("Elapsed Time: "+String.format("%02dh %02dm %02ds", hours, minutes, seconds));

            //currentLocation = llt.getLatLng();

            if (llHistoryPolyline != null) {
                llHistoryPolyline.remove(); // Remove old polyline
            }

            llHistoryPolyline = mMap.addPolyline(polylineOptions);
            llHistoryPolyline.setEndCap(new RoundCap());
            llHistoryPolyline.setWidth(12);
            llHistoryPolyline.setColor(Color.BLUE);

            float r = getRadius();

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
            Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
            BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);

                MarkerOptions options = new MarkerOptions();
                options.position(llt.getLatLng());
                Location location = new Location("provider");
                location.setLatitude(latlonHistory.get(latlonHistory.size()-2).getLatLng().latitude);
                location.setLongitude(latlonHistory.get(latlonHistory.size()-2).getLatLng().longitude);

//            // Create a Location object and set its latitude and longitude
//            Location location = new Location("provider");
//            location.setLatitude(41.8781);
//            location.setLongitude(-87.6298);

            // Assuming you have another location to calculate the bearing
            Location destination = new Location("provider");
            destination.setLatitude(llt.getLatLng().latitude);
            destination.setLongitude(llt.getLatLng().longitude);

            // Calculate the bearing
            float bearing = location.bearingTo(destination);

            options.position(llt.getLatLng());
            options.icon(iconBitmap);
            options.rotation(bearing);



                if (carMarker != null) {
                    carMarker.remove();
                }

                carMarker = mMap.addMarker(options);

            //mMap.animateCamera(CameraUpdateFactory.newLatLng(llt.getLatLng()));

        if(autoZoom){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(llt.getLatLng(), zoomDefault));
        }

            //handler.postDelayed(updateTimerThread, 0);
            //GetLastLocationAPIVolley getLastLocationAPIVolley = new GetLastLocationAPIVolley(main);
            getLastLocationAPIVolley.getLastLocation(TripID);







    }



    public void handleLastLocationFail(String message, String tripId) {
//        binding.progressBar.setVisibility(View.GONE);
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Last Location Error");
//        builder.setMessage("Trip Id " + tripId + " error: " + message);
//        builder.setPositiveButton("OK", null);
//        builder.create().show();
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            long elapsedTime = System.currentTimeMillis() - startTime;

            int seconds = (int) (elapsedTime / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;

            seconds = seconds % 60;
            minutes = minutes % 60;

            binding.followTime.setText("Elapsed Time: "+String.format("%02d:%02d:%02d", hours, minutes, seconds));

            GetLastLocationAPIVolley getLastLocationAPIVolley = new GetLastLocationAPIVolley(main);
            getLastLocationAPIVolley.getLastLocation(TripID);

            handler.postDelayed(this, 1000);
        }
    };

    public void zoomButton (View v){
        //Toast.makeText(this, "Testing", Toast.LENGTH_SHORT).show();
        if(autoZoom){
            autoZoom = false;
            binding.target.setAlpha(0.5f);
        }
        else{
            autoZoom = true;
            binding.target.setAlpha(1f);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlonHistory.get(latlonHistory.size()-1).getLatLng(), zoomDefault));
        }
    }

    private void startObjectAnimators() {

        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(binding.followTripPulse, "alpha", 1.0f, 0.25f, 1.0f);
        objectAnimator.setDuration(500); // Total duration for one pulse cycle (fade out and fade in)
        objectAnimator.start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        GetTripPointsAPIVolley getPreviousTripPointsAPIVolley = new GetTripPointsAPIVolley(this);
                        getPreviousTripPointsAPIVolley.getPoints(TripID);
                    } else {
                        Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    public void NoNetworkEarlyPoints() {

        binding.progressBar.setVisibility(View.GONE);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me - No Network");
        bindtitle.followMeMessage.setText("No network connection - cannot access trip data now.\n"
                +"Cannot follow the trip now.");

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

    public void noNetworkLastLocation(){
        binding.noNetworkTextFollow.setVisibility(View.VISIBLE);
        binding.noNetworkTextFollow.setText("NO NETWORK CONNECTION \n Not all data may be received");
        getLastLocationAPIVolley.getLastLocation(TripID);
    }
}