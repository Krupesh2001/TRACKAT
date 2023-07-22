package com.bacar.trackat_android;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeofencingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private List<LatLng> geofencePoints = new ArrayList<>();
    private Polygon geofencePolygon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofencing);

        // Find the MapView by its ID
        mapView = findViewById(R.id.geofence_map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the current activity and go back to the MainActivity
                finish();
            }
        });

        Button createGeofenceButton = findViewById(R.id.create_geofence_button);
        createGeofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (geofencePoints.size() >= 3) {
                    // Store the geofence in the database
                    saveGeofenceToDatabase();
                    Toast.makeText(GeofencingActivity.this, "Geofence created and saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GeofencingActivity.this, "Please draw a complete geofence first!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button getAddressButton = findViewById(R.id.submit_button);
        getAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the home address entered by the user
                EditText addressEditText = findViewById(R.id.address_edit_text);
                String addressString = addressEditText.getText().toString();

                // Get the LatLng from the address using Geocoder
                LatLng addressLatLng = getLatLngFromAddress(addressString);
                if (addressLatLng != null) {
                    // Clear existing geofence points and add the address LatLng as the starting point
                    geofencePoints.clear();
                    geofencePoints.add(addressLatLng);

                    // Update the map with the new starting point
                    if (geofencePolygon != null) {
                        geofencePolygon.remove();
                    }
                    drawGeofencePolygon();

                    // Move the camera to the address location
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(addressLatLng, 15));
                } else {
                    Toast.makeText(GeofencingActivity.this, "Address not found, please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Set a listener to handle map clicks for drawing the geofence
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Add the clicked point to the geofence points list
                geofencePoints.add(latLng);

                // Redraw the geofence polygon
                if (geofencePolygon != null) {
                    geofencePolygon.remove();
                }
                drawGeofencePolygon();
            }
        });
    }

    private void drawGeofencePolygon() {
        if (geofencePoints.size() >= 3) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(geofencePoints)
                    .strokeWidth(2)
                    .strokeColor(getResources().getColor(R.color.polygonStrokeColor))
                    .fillColor(getResources().getColor(R.color.polygonFillColor));
            geofencePolygon = googleMap.addPolygon(polygonOptions);
        }
    }

    private void saveGeofenceToDatabase() {
        // Convert geofencePoints to a JSON string and save it to the database
        // For simplicity, we will just store the JSON string representation in the database
        String geofenceJson = getGeofenceJsonString();

    }

    private String getGeofenceJsonString() {
        // Convert geofencePoints to a JSON array of LatLng objects
        StringBuilder jsonString = new StringBuilder("[");
        for (LatLng point : geofencePoints) {
            jsonString.append("{\"latitude\": ").append(point.latitude).append(", \"longitude\": ").append(point.longitude).append("},");
        }
        jsonString.deleteCharAt(jsonString.length() - 1); // Remove the last comma
        jsonString.append("]");

        return jsonString.toString();
    }

    private LatLng getLatLngFromAddress(String addressString) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(addressString, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}

