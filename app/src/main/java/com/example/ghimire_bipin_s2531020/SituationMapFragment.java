package com.example.ghimire_bipin_s2531020;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

// Shows the (filtered) situations on a Google Map
public class SituationMapFragment extends SupportMapFragment implements OnMapReadyCallback {

    private SituationViewModel vm;
    private GoogleMap map;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);
        getMapAsync(this);   // onMapReady is called when the map has loaded
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);   // no buttons that open the Google Maps app
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.8, -4.2), 6)); // Scotland

        // Same filtered list as the RecyclerView, so road/distance search applies here too
        vm.getFiltered().observe(getViewLifecycleOwner(), this::drawSituations);

        // Tap a marker's info bubble to open the detail view
        map.setOnInfoWindowClickListener(marker -> {
            TrafficSituation s = (TrafficSituation) marker.getTag();
            if (s == null) return;
            vm.select(s);
            if (requireActivity().findViewById(R.id.detailContainer) == null) {   // portrait
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.listContainer, new SituationDetailFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void drawSituations(List<TrafficSituation> list) {
        if (map == null) return;
        map.clear();
        for (TrafficSituation s : list) {
            LatLng start = s.hasStart() ? new LatLng(s.getStartLat(), s.getStartLon()) : null;
            LatLng end = s.hasEnd() ? new LatLng(s.getEndLat(), s.getEndLon()) : null;
            LatLng pin = start != null ? start : end;
            if (pin == null) continue;   // no coordinates, cant be placed

            Marker m = map.addMarker(new MarkerOptions()
                    .position(pin)
                    .title(prettyType(s.getType()) + (s.getRoadName() != null ? " – " + s.getRoadName() : ""))
                    .snippet(s.getDescription())
                    .icon(BitmapDescriptorFactory.defaultMarker(hueFor(s))));
            if (m != null) m.setTag(s);

            // Two points is a stretch of road, so draw a line between them
            if (start != null && end != null) {
                map.addPolyline(new PolylineOptions().add(start, end).width(10f).color(0xFFC62828));
            }
        }
    }

    // Same duration thresholds as the list stripe
    private float hueFor(TrafficSituation s) {
        long h = s.getDurationHours();
        if (h < 0) return BitmapDescriptorFactory.HUE_AZURE;
        if (h <= 24) return BitmapDescriptorFactory.HUE_GREEN;
        if (h <= 24 * 7) return BitmapDescriptorFactory.HUE_YELLOW;
        return BitmapDescriptorFactory.HUE_RED;
    }

    private String prettyType(String raw) {
        return raw == null ? "Unknown" : raw.replaceAll("([a-z])([A-Z])", "$1 $2");
    }
}