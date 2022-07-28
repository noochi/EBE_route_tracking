package com.example.routetrackingapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.routetrackingapp.databinding.ActivityMapsBinding;
import com.example.routetrackingapp.databinding.ActivitySaveRouteBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.w3c.dom.Text;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveRouteActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ActivitySaveRouteBinding binding;
    private FirebaseDatabase db;
    private DatabaseReference reference;
    private String routeName;
    private List<LatLng> routeData;
    private Collection<Route> routes;
    private Polyline gpsTrack;
    private LocationManager locationManager;
    private Location userLocation;
    private Button submitButton;
    private EditText nameEditText;
    private TextView timeView;
    private TextView elevationView;
    private TextView tempoView;
    private TextView kmView;
    private double time;
    private double distance;

    private final int MIN_TIME = 1000; // 1 sec
    private final int MIN_DISTANCE = 1; // 1 meter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        retrieveData();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        binding = ActivitySaveRouteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = FirebaseDatabase.getInstance();
        reference = db.getReference().child("Routes");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(view -> {
            createRoute();
            Toast.makeText(this, "Your Route has been added", Toast.LENGTH_SHORT).show();
        });

        nameEditText = findViewById(R.id.routeName);
        timeView = findViewById(R.id.timeView);
        elevationView = findViewById(R.id.elevationView);
        tempoView = findViewById(R.id.tempoView);
        kmView = findViewById(R.id.kmView);

        timeView.setText(formatTime(time));
        kmView.setText(formatDistance(distance));
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setAllGesturesEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        getLocationUpdates();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.MAGENTA);
        polylineOptions.width(6);
        gpsTrack = mMap.addPolyline(polylineOptions);
        gpsTrack.setPoints(routeData);

        if (userLocation != null) {
            //myMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(userLocation.getLatitude(),userLocation.getLongitude())).title("Marker in location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(userLocation.getLatitude(), userLocation.getLongitude()), 14f));
        }
    }

    private void createRoute() {
        routeName = nameEditText.getText().toString();
        Route newRoute = new Route(routeName, routeData, time, distance);
        String key = reference.push().getKey();
        Map<String, Object> routeValues = newRoute.toMap();

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put(key, routeValues);
        Log.d(TAG, "createRoute: " + childUpdates);
        reference.updateChildren(childUpdates);

        Intent switchToMain = new Intent(this, MapsActivity.class);
        startActivity(switchToMain);
    }

    private void retrieveData() {
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<LatLng>>() {
        }.getType();
        time = getIntent().getDoubleExtra("time", 0.0);
        distance = getIntent().getDoubleExtra("distance", 0.0);
        routeData = gson.fromJson(getIntent().getStringExtra("route"), listType);
    }

    private String formatTime(double time) {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return String.format("%02d", hours) + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
    }

    private String formatDistance(double distanceRun) {
        int km = (int) (distanceRun * 0.001);
        int meter = (int) (distanceRun % 1000);
        return km + "," + String.format("%02d", meter);
    }

    @SuppressLint("MissingPermission")
    private void getLocationUpdates() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            userLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else {
            Toast.makeText(this, "No Provider enabled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }
}