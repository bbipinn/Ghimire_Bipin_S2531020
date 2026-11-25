package com.example.ghimire_bipin_s2531020;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

// Shows full details of the selected situation plus a small map of just that situation
public class SituationDetailFragment extends Fragment {

    private GoogleMap miniMap;
    private View mapContainer;
    private SituationViewModel vm;

    public SituationDetailFragment() { super(R.layout.fragment_detail); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        TextView type = view.findViewById(R.id.detailType);
        TextView desc = view.findViewById(R.id.detailDesc);
        TextView road = view.findViewById(R.id.detailRoad);
        TextView times = view.findViewById(R.id.detailTimes);
        TextView loc = view.findViewById(R.id.detailLocation);
        mapContainer = view.findViewById(R.id.detailMapContainer);

        vm = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);

        // Create a small static map inside this fragment
        SupportMapFragment mapFrag = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.detailMapContainer);
        if (mapFrag == null) {
            mapFrag = SupportMapFragment.newInstance(new GoogleMapOptions().liteMode(true));
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.detailMapContainer, mapFrag)
                    .commit();
        }
        mapFrag.getMapAsync(m -> {
            miniMap = m;
            miniMap.getUiSettings().setMapToolbarEnabled(false);
            miniMap.setOnMapClickListener(latLng -> { });   // stop taps opening the Google Maps app
            showOnMap(vm.getSelected().getValue());
        });

        vm.getSelected().observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;
            type.setText(s.getType() == null ? "Unknown"
                    : s.getType().replaceAll("([a-z])([A-Z])", "$1 $2"));
            desc.setText(s.getDescription());
            road.setText("Road: " + s.getRoadName());
            times.setText("Start: " + s.getStartTime() + "\nEnd: " + s.getEndTime()
                    + "\nExpected delays: " + s.getDelayType());
            String where = "From: " + s.getStartLat() + ", " + s.getStartLon();
            if (s.hasEnd()) where += "\nTo: " + s.getEndLat() + ", " + s.getEndLon();
            if (s.getLocationText() != null) where += "\n" + s.getLocationText();
            loc.setText(where);

            showOnMap(s);
        });
    }

    // Draws just the selected situation on the mini map
    private void showOnMap(TrafficSituation s) {
        if (s == null) return;
        LatLng start = s.hasStart() ? new LatLng(s.getStartLat(), s.getStartLon()) : null;
        LatLng end = s.hasEnd() ? new LatLng(s.getEndLat(), s.getEndLon()) : null;

        if (start == null && end == null) {          // nothing to show
            mapContainer.setVisibility(View.GONE);
            return;
        }
        mapContainer.setVisibility(View.VISIBLE);
        if (miniMap == null) return;

        miniMap.clear();
        if (start != null) miniMap.addMarker(new MarkerOptions().position(start).title("From"));
        if (end != null) miniMap.addMarker(new MarkerOptions().position(end).title("To"));

        if (start != null && end != null) {
            miniMap.addPolyline(new PolylineOptions().add(start, end).width(10f).color(0xFFC62828));
            LatLngBounds bounds = new LatLngBounds.Builder().include(start).include(end).build();
            try {
                miniMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80));
            } catch (IllegalStateException e) {      // map not sized yet
                miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.getCenter(), 13));
            }
        }
        else {
            miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start != null ? start : end, 14));
        }
    }
}