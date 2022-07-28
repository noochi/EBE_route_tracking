package com.example.routetrackingapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.example.routetrackingapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private FirebaseDatabase db;
    private List<User> users;
    private DatabaseReference reference;
    private LocationManager locationManager;
    private final int MIN_TIME = 1000; // 1 sec
    private final int MIN_DISTANCE = 1; // 1 meter
    Marker myMarker;
    private Location userLocation;
    private Polyline gpsTrack;
    private FloatingActionButton startTrackingBtn;
    private boolean trackingActivated;
    private FloatingActionButton stopTrackingBtn;
    private TextView timerText;
    private Timer timer;
    private TimerTask timerTask;
    private double time = 0.0;
    private RelativeLayout timerLayout;
    private Button abortBtn;
    private double distanceRun;
    private TextView distance;
    private TextView speed;
    private double currentSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseDatabase.getInstance();
        reference = db.getReference().child("User-101");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        ViewGroup.LayoutParams params = mapFragment.getView().getLayoutParams();
        mapFragment.getMapAsync(this);

        startTrackingBtn = findViewById(R.id.startButton);
        stopTrackingBtn = findViewById(R.id.stopButton);
        abortBtn = findViewById(R.id.abortRunButton);
        timerText = findViewById(R.id.timer);
        timerLayout = findViewById(R.id.timerLayout);
        distance = findViewById(R.id.distance);
        speed = findViewById(R.id.speed);

        timerLayout.setVisibility(View.GONE);

        startTrackingBtn.setOnClickListener(view -> {
            trackingActivated = true;
            startTimer();
            startTrackingBtn.setVisibility(View.GONE);
            stopTrackingBtn.setVisibility(View.VISIBLE);
            abortBtn.setVisibility(View.VISIBLE);
            timerLayout.setVisibility(View.VISIBLE);
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        stopTrackingBtn.setOnClickListener(view -> {
            if(gpsTrack.getPoints().isEmpty()) {
                abortRun(builder, params);
            } else{
                trackingActivated = false;
                timerTask.cancel();
                builder.setCancelable(true);
                builder.setTitle("Neue Route");
                builder.setMessage("Wollen Sie die aufgenommene Route speichern?");
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            switchToSaveForm();
                        });
            }
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                trackingActivated = true;
                startTimer();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        });
        abortBtn.setOnClickListener(view -> {
            abortRun(builder, params);
            builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                trackingActivated = true;
                startTimer();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        });

        timer = new Timer();
    }

    private void startTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        time++;
                        timerText.setText(getTimerText());
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    private String getTimerText() {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);

        return formatTime(seconds, minutes, hours);
    }

    private String formatTime(int seconds, int minutes, int hours) {
        return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    private void abortRun(AlertDialog.Builder builder, ViewGroup.LayoutParams params) {
        trackingActivated = false;
        timerTask.cancel();
        builder.setCancelable(true);
        builder.setTitle("Lauf abbrechen!");
        builder.setMessage("Wollen Sie den Lauf wirklich abbrechen?");
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    if(timerTask != null) {
                        time = 0.0;
                        timerText.setText(formatTime(0,0,0));
                    }
                    distanceRun = 0;
                    distance.setText(formatDistance(0));
                    startTrackingBtn.setVisibility(View.VISIBLE);
                    stopTrackingBtn.setVisibility(View.GONE);
                    timerLayout.setVisibility(View.GONE);
                    abortBtn.setVisibility(View.GONE);
                    float factor = this.getResources().getDisplayMetrics().density;
                    params.height = (int) (400 * factor);
                    List<LatLng> points = new ArrayList<>();
                    gpsTrack.setPoints(points);
                });
    }

    private void switchToSaveForm() {
        Intent switchToSaveFormIntent = new Intent(this, SaveRouteActivity.class);
        Gson gson = new Gson();
        switchToSaveFormIntent.putExtra("route", gson.toJson(gpsTrack.getPoints()));
        switchToSaveFormIntent.putExtra("time", time);
        switchToSaveFormIntent.putExtra("distance",distanceRun);
        startActivity(switchToSaveFormIntent);
    }

    private void readChanges() {
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        MyLocation location = snapshot.getValue(MyLocation.class);

                    } catch (Exception e) {
                        Toast.makeText(MapsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void getLocationUpdates() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
                    userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
                    userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else {
                    Toast.makeText(this, "No Provider enabled", Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationUpdates();
            } else {
                Toast.makeText(this, "Permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        getLocationUpdates();
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.MAGENTA);
        polylineOptions.width(8);
        gpsTrack = mMap.addPolyline(polylineOptions);

        mMap.setMyLocationEnabled(true);
        if (userLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 14f));
        }
        readChanges();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location != null) {
            userLocation = location;
            saveLocation(location);
            if (trackingActivated) {
                updateLocation(location);
            }
        } else {
            Toast.makeText(this, "No Location", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocation(Location location) {
        if(!gpsTrack.getPoints().isEmpty()) {
            LatLng lastPoint = gpsTrack.getPoints().get(gpsTrack.getPoints().size()-1);
            float[] results = new float[1];
            Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, location.getLatitude(), location.getLongitude(), results);
            currentSpeed = location.getSpeed();
            distanceRun += results[0];
            distance.setText(formatDistance(distanceRun));
            speed.setText(formatSpeed(currentSpeed));
        }
        List<LatLng> points = gpsTrack.getPoints();
        points.add(new LatLng(location.getLatitude(), location.getLongitude()));
        gpsTrack.setPoints(points);
    }

    private String formatDistance(double distanceRun) {
        int km = (int) (distanceRun * 0.001);
        int meter = (int) ((distanceRun % 1000)/10);
        return km + "," + String.format("%02d", meter);
    }

    private String formatSpeed(double Speed) {
        double kmph = Speed * 3.6;
        int km = (int) Math.floor(kmph);
        int m = (int) ((kmph - km) * 100);
        return km + "," + String.format("%02d", m);
    }

    private void saveLocation(Location location) {
        reference.setValue(location);
    }

    public void getNearestRoutes(Location location) {

        // Write a message to the database
        DatabaseReference myRef = db.getReference().child("Routes");
        myRef.orderByChild("meanLatLng").equals(location);
    }
}