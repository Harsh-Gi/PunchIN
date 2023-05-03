package com.example.thatsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.thatsapp.databinding.ActivityLocationBinding;
import com.example.thatsapp.databinding.ActivityUploadBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.IgnoreExtraProperties;
import com.google.firebase.database.ValueEventListener;

public class LocationActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;

    FusedLocationProviderClient mFusedLocationClient;

    Location currentLocation;

    int PERMISSION_ID = 44;
    ActivityLocationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // method to get the location
        getLastLocation();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.child("RequiredLocation").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.latTextViewE.setText((double) snapshot.child("reqLat").getValue() + "");
                binding.lonTextViewE.setText((double) snapshot.child("reqLon").getValue() + "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding.setLocBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mDatabase = FirebaseDatabase.getInstance().getReference();
//                RequiredLocation requiredLocation = new RequiredLocation(currentLocation, currentLocation.getLatitude(), currentLocation.getLongitude());
                mDatabase.child("RequiredLocation").child("reqLat").setValue(currentLocation.getLatitude());
                mDatabase.child("RequiredLocation").child("reqLon").setValue(currentLocation.getLongitude());
                Toast.makeText(LocationActivity.this, "Location is updated to database succesfully!", Toast.LENGTH_SHORT).show();
            }
        });

        binding.matchLocBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mDatabase = FirebaseDatabase.getInstance().getReference();
//                .child("reqLoc")
                mDatabase.child("RequiredLocation").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float locationResult = -1;

                        Location locationA = new Location("point A");

                        locationA.setLatitude(currentLocation.getLatitude());
                        locationA.setLongitude(currentLocation.getLongitude());

                        Location locationB = new Location("point B");

                        locationB.setLatitude((double) snapshot.child("reqLat").getValue());
                        locationB.setLongitude((double) snapshot.child("reqLon").getValue());

                        locationResult = locationA.distanceTo(locationB);

//                        latitudeTextViewE.setText("abc");
//                        longitTextViewE.setText("xyz");
                        binding.latTextViewE.setText((double) snapshot.child("reqLat").getValue() + "");
                        binding.lonTextViewE.setText((double) snapshot.child("reqLon").getValue() + "");

                        if(locationResult >= 0 && locationResult < 100){
                            binding.locResult.setText("Result: Location is same with distance = " + locationResult);
                        }
                        else{
                            binding.locResult.setText("Result: Location is not same with distance = " + locationResult);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    @IgnoreExtraProperties
    public class RequiredLocation {

        public Location reqLoc;
        public double reqLat;
        public double reqLon;

        public RequiredLocation() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

//        public RequiredLocation(Location reqLoc) {
//            this.reqLoc=reqLoc;
//        }

        public RequiredLocation(Location reqLoc, double reqLat, double reqLon) {
            this.reqLoc=reqLoc;
            this.reqLat=reqLat;
            this.reqLon=reqLon;
        }

    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        currentLocation = task.getResult();
                        if (currentLocation == null) {
                            requestNewLocationData();
                        } else {
                            binding.latTextView.setText(currentLocation.getLatitude() + "");
                            binding.lonTextView.setText(currentLocation.getLongitude() + "");
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            binding.latTextView.setText("Latitude: " + mLastLocation.getLatitude() + "");
            binding.lonTextView.setText("Longitude: " + mLastLocation.getLongitude() + "");
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // If everything is alright then
    @Override
    public void
    onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }
}