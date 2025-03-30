package com.example.csc_492_hw4;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.csc_492_hw4.databinding.ActivityMainBinding;
import com.example.csc_492_hw4.databinding.DialogLoginBinding;
import com.example.csc_492_hw4.databinding.DialogRegisterUserBinding;
import com.example.csc_492_hw4.databinding.DialogTripIdBinding;
import com.example.csc_492_hw4.databinding.FollowMeTitleBinding;
import com.google.android.gms.maps.model.LatLng;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private long startTime;
    private long minSplashTime = 2000;
    private boolean keepOn = true;
    private String TAG = "Main";
    private static final int LOCATION_REQUEST = 111;
    private static final int BACKGROUND_LOCATION_REQUEST = 222;
    private static final int NOTIFICATION_REQUEST = 333;
    private PointReceiver pointReceiver;
    private Intent locationServiceIntent;
    private String tripID = "";
    private String username = "";
    private boolean loggedIn = false;
    private String firstname;
    private String lastname;
    TripExistsAPIVolley tripExistsAPIVolley;


    private UserPasswordObject userPasswordObjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        //EdgeToEdge.enable(this);


        //tripExistsAPIVolley = new TripExistsAPIVolley(this);
        userPasswordObjects = readArrayListFromFile();
        if(userPasswordObjects == null){
            userPasswordObjects = new UserPasswordObject("","");
        }

        SplashScreen.installSplashScreen(this)
                .setKeepOnScreenCondition(
                        new SplashScreen.KeepOnScreenCondition() {
                            @Override
                            public boolean shouldKeepOnScreen() {
                                Log.d(TAG, "shouldKeepOnScreen: " + (System.currentTimeMillis() - startTime));

                                return keepOn || (System.currentTimeMillis() - startTime <= minSplashTime);
                            }
                        }
                );

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


    }

    public void startTripButton(View v){
        login();
    }

    public void login(){

        if (loggedIn){
            boolean hasPerm = checkAppPermission();

            if (hasPerm) {

                //Toast.makeText(this, "Location Permission process success in Login", Toast.LENGTH_SHORT).show();
                TripIDDialog();
                return;

            }
            return;
        }


        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me");
        bindtitle.followMeMessage.setText("Please login to continue");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogLoginBinding bind = DialogLoginBinding.inflate(getLayoutInflater());

        userPasswordObjects = readArrayListFromFile();
        if( userPasswordObjects != null && !userPasswordObjects.getUsername().isEmpty() ){
            bind.username.setText(userPasswordObjects.getUsername());
            bind.checkBox2.setChecked(true);

        }

        if( userPasswordObjects != null && !userPasswordObjects.getPassword().isEmpty()){
            bind.password.setText(userPasswordObjects.getPassword());
            bind.checkBox2.setChecked(true);
        }
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Login", (dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            if(bind.checkBox2.isChecked()){
                UserPasswordObject list = new UserPasswordObject(bind.username.getText().toString(), bind.password.getText().toString());
                writeObjectToFile(list);
            }
            else{
                UserPasswordObject list = new UserPasswordObject("", "");
                writeObjectToFile(list);
            }
            VerifyUserCredentialsAPIVolley credentialsCheckerAPIVolley
                    = new VerifyUserCredentialsAPIVolley(this);
            credentialsCheckerAPIVolley.checkCredentials(bind.username.getText().toString(), bind.password.getText().toString());



        });
        alertDialogBuilder.setNegativeButton("Cancel",(dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            if(bind.checkBox2.isChecked()){
                UserPasswordObject list = new UserPasswordObject(bind.username.getText().toString(), bind.password.getText().toString());
                writeObjectToFile(list);
            }
            else{
                UserPasswordObject list = new UserPasswordObject("", "");
                writeObjectToFile(list);
            }

        });

        alertDialogBuilder.setNeutralButton("Register", (dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            String username = bind.username.getText().toString();
            String password = bind.password.getText().toString();
            //register(username,password);
            registerLogin();

        });
        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void registerLogin(){



        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me");
        bindtitle.followMeMessage.setText("Please register to continue");
        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogRegisterUserBinding bind = DialogRegisterUserBinding.inflate(getLayoutInflater());
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Register", (dialogInterface, which) -> {

            String username = bind.usernameRegister.getText().toString();
            String password = bind.passwordRegister.getText().toString();
            String email = bind.emailRegister.getText().toString();
            String firstname = bind.firstnameRegister.getText().toString();
            String lastname = bind.lastnameRegister.getText().toString();
            register(username,password,firstname,lastname,email);

        });
        alertDialogBuilder.setNegativeButton("Cancel",(dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);

        });


        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void register(String username,String password,String firstName,String lastName, String email){
        boolean userLengthCheck = usernameLengthCheck(username);
        boolean passwordLengthCheck = passwordLengthCheck(password);


        CreateUserAccountAPIVolley createUserAccountAPIVolley
                = new CreateUserAccountAPIVolley(this);
        createUserAccountAPIVolley.createUser(firstName, lastName, email, username, password);





    }


    public void handleCreateUserAccountSuccess(String firstName, String lastName,
                                               String email, String userName) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());

        bindtitle.followMeTitle.setText("Follow Me - Registration Success");
        bindtitle.followMeMessage.setText("\nWelcome " + firstName +" " + lastName+"!\n" +
                "Your username is: " + userName +"\n"
                + "Your email is: " +email +"");
        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        //DialogLoginBinding bind = DialogLoginBinding.inflate(getLayoutInflater());
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, which) -> {
            login();

        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void handleCreateUserAccountFail(Object o){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());

        bindtitle.followMeTitle.setText("Follow Me - Registration Failed");

        String errorMessage = "";
        if (o != null) {
            errorMessage = o.toString();
        }bindtitle.followMeMessage.setText(errorMessage);


        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());

        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, which) -> {
            //login();
            registerLogin();
        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }







    public void writeObjectToFile( UserPasswordObject list) {
        try {
            FileOutputStream fos = openFileOutput("UserPasswordCache", MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(list);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





    private boolean usernameLengthCheck(String username){

        return username.length() >= 8;
    }

    private boolean passwordLengthCheck(String password){

        return password.length() >= 8;
    }



    public void saveCredentials(View v){
        // Is the view now checked?
        boolean checked = ((CheckBox) v).isChecked();

        // Do something based on the checked state
        if (checked) {

            // Checkbox is checked
        } else {
            // Checkbox is unchecked
        }
    }

    public void handleVerifyUserCredentialsSuccess(String userName, String firstName, String lastName) {

        loggedIn = true;
        username = userName;
        firstname = firstName;
        lastname = lastName;
        boolean hasPerm = checkAppPermission();

        if (hasPerm) {

            //Toast.makeText(this, "Location Permission process success", Toast.LENGTH_SHORT).show();
            TripIDDialog();

        }
    }

    public void noNetworkRegister(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());

        bindtitle.followMeTitle.setText("Follow Me - No Network");

        String errorMessage = "No network connection - cannot create user account now.";
        bindtitle.followMeMessage.setText(errorMessage);

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        //DialogLoginBinding bind = DialogLoginBinding.inflate(getLayoutInflater());
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, which) -> {


        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void noNetworkLogin(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());

        bindtitle.followMeTitle.setText("Follow Me - No Network");

        String errorMessage = "No network connection - cannot login now";
        bindtitle.followMeMessage.setText(errorMessage);

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        //DialogLoginBinding bind = DialogLoginBinding.inflate(getLayoutInflater());
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, which) -> {


        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    public void handleVerifyUserCredentialsFail() {
        //binding.progressBar.setVisibility(View.GONE);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());

        bindtitle.followMeTitle.setText("Follow Me - User Credientials Failed");

        String errorMessage = "Invalid username or password";
       bindtitle.followMeMessage.setText(errorMessage);

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        //DialogLoginBinding bind = DialogLoginBinding.inflate(getLayoutInflater());
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        //alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Ok", (dialogInterface, which) -> {
            login();

        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    private UserPasswordObject readArrayListFromFile() {
        UserPasswordObject list = null;
        try {
            FileInputStream fis = openFileInput("UserPasswordCache");
            ObjectInputStream ois = new ObjectInputStream(fis);
            list = (UserPasswordObject) ois.readObject();
            ois.close();
            fis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        keepOn = false;
        return list;
    }


    private void startLocationService() {

        if (checkAppPermission()) {
            // Create a receiver to get the location updates
            //pointReceiver = new PointReceiver(this);

            // Register the receiver
            ContextCompat.registerReceiver(this,
                    pointReceiver,
                    new IntentFilter("com.example.broadcast.MY_BROADCAST"),
                    ContextCompat.RECEIVER_EXPORTED);
        }
        //starting service
        locationServiceIntent = new Intent(this, LocationService.class);

        Log.d(TAG, "startService: START");
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

    public void updateLocation(int counter, LatLng latLng, float bearing) {
        // Update the UI
        Log.d(TAG, "updateLocation: " + latLng + " " + bearing);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST) {
            if (permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    //Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == NOTIFICATION_REQUEST) {
            if (permissions[0].equals(android.Manifest.permission.POST_NOTIFICATIONS)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    //Toast.makeText(this, "Notification Permission not Granted", Toast.LENGTH_SHORT).show();
                }

            }
        }
        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (permissions[0].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    TripIDDialog();
                    //Toast.makeText(this, "Background Location Permission has been Granted", Toast.LENGTH_SHORT).show();

                } else {

                }

            }
        }

    }

    public void TripIDDialog(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            noGPSEnabled();
            return;
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me");
        bindtitle.followMeMessage.setText("Please provide a trip id for this journey.\n" +
                "Share this ID with your friends.");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogTripIdBinding bind = DialogTripIdBinding.inflate(getLayoutInflater());

        if(!tripID.isEmpty()){
            bind.editTextTripId.setText(tripID);
            tripID = "";
        }
        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("OK", (dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);


             if(bind.editTextTripId.getText().toString().isEmpty()){
                //emptyTripID();
                Toast.makeText(this, "Trip ID cannot be empty", Toast.LENGTH_LONG).show();

            }
            else{
                tripID = bind.editTextTripId.getText().toString();

                //tripExistsAPIVolley.tripExists(tripID);
                Intent intent = new Intent(this, TripLeadActivity.class);
                intent.putExtra("TRIPID", tripID);
                intent.putExtra("USERNAME",username);
                intent.putExtra("FIRSTNAME",firstname);
                intent.putExtra("LASTNAME", lastname);

                tripID="";
                startActivity(intent);
            }


        });
        alertDialogBuilder.setNegativeButton("Cancel",(dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);

        });

        alertDialogBuilder.setNeutralButton("Generate", (dialogInterface, which) -> {
            tripID = makeTripId();

            TripIDDialog();


        });
        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }

    private String makeTripId() {
        String ALLOWED_CHARACTERS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        sb.append("-");
        for (int i = 0; i < 5; ++i)
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));


        return sb.toString();
    }

    public void tripExists() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Id Exists");
        builder.setMessage("Trip Id " + tripID + " exists. Please make a different id.");
        builder.setPositiveButton("OK", (dialogInterface, which) -> {
            // Dismiss the dialog
            TripIDDialog();


        });
        builder.create().show();

    }

    public void tripNotExist() {

        Intent intent = new Intent(this, TripLeadActivity.class);
        intent.putExtra("TRIPID", tripID);
        intent.putExtra("USERNAME",username);
        intent.putExtra("FIRSTNAME",firstname);
        intent.putExtra("LASTNAME", lastname);

        //intent.putExtra("PASSWORD",pass)
        startActivity(intent);

    }

    public void tripExistsError(String s, String tripId) {
        //binding.progressBar.setVisibility(View.GONE);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Error");
        builder.setMessage("Trip Id " + tripId + " error: " + s);

        builder.create().show();
    }

    public void emptyTripID(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me - Empty Trip");
        bindtitle.followMeMessage.setText("Please type in a Trip ID instead of leaving it empty.");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogTripIdBinding bind = DialogTripIdBinding.inflate(getLayoutInflater());


        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("OK", (dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);

            TripIDDialog();

        });


        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }
    public void noGPSEnabled(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me - Cannot Follow");
        bindtitle.followMeMessage.setText("GPS is not enabled. Please enable GPS by turning" +
                " on 'Use location' in the Location setting to continue");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogTripIdBinding bind = DialogTripIdBinding.inflate(getLayoutInflater());


        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("Go to Location Settings", (dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);

            Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(callGPSSettingIntent);

        });
        alertDialogBuilder.setNegativeButton("Cancel",(dialogInterface, which) -> {
            // Dismiss the dialog
            //Intent intent = new Intent(this, MainActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);

        });


        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }


    public void followTripButton(View v){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        FollowMeTitleBinding bindtitle = FollowMeTitleBinding.inflate(getLayoutInflater());
        bindtitle.followMeTitle.setText("Follow Me");
        bindtitle.followMeMessage.setText("Enter the Trip ID to follow");

        //alertDialogBuilder.setTitle("No Location Available");
        alertDialogBuilder.setCustomTitle(bindtitle.getRoot());
        //alertDialogBuilder.setTitle("Bus Tracker - CTA");
        DialogTripIdBinding bind = DialogTripIdBinding.inflate(getLayoutInflater());


        //alertDialogBuilder.setMessage("Unable to retrieve location. Please ensure that location services are enabled on your device.");

        alertDialogBuilder.setView(bind.getRoot());
        //alertDialogBuilder.setMessage("Unable to connect Bus Tracker API due to network problem.  Please check your network connect.");
        alertDialogBuilder.setPositiveButton("OK", (dialogInterface, which) -> {


            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                noGPSEnabled();
            }
            else{
                String id = bind.editTextTripId.getText().toString();

                Intent intent = new Intent(this, TripFollowerActivity.class);
                intent.putExtra("ID", id);


                startActivity(intent);
            }



        });
        alertDialogBuilder.setNegativeButton("Cancel",(dialogInterface, which) -> {

        });

        //alertDialogBuilder.setIcon(R.mipmap.ic_launcher_round);

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);

        alertDialog.show();
    }


}

