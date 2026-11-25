package com.example.ghimire_bipin_s2531020;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
import android.location.Location;


public class SituationViewModel extends ViewModel {
    private final MutableLiveData<List<TrafficSituation>> situations =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<TrafficSituation> selected = new MutableLiveData<>();

    private final List<TrafficSituation> unplanned = new ArrayList<>();
    private final List<TrafficSituation> roadworks = new ArrayList<>();

    public LiveData<List<TrafficSituation>> getSituations() { return situations; }
    public LiveData<TrafficSituation> getSelected() { return selected; }

    private String roadQuery = "";
    private double maxMiles = -1;   // -1 means no distance filter
    // Fixed current location
    private static final double MY_LAT = 55.8642, MY_LON = -4.2518;
    private final MutableLiveData<List<TrafficSituation>> filtered = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<TrafficSituation>> getFiltered() { return filtered; }
    private final MutableLiveData<String> lastUpdated = new MutableLiveData<>("Not loaded yet");
    public LiveData<String> getLastUpdated() { return lastUpdated; }

    public synchronized boolean hasData() {
        return !unplanned.isEmpty() || !roadworks.isEmpty();
    }

    public synchronized void setRoadQuery(String q) {
        roadQuery = q == null ? "" : q.trim();
        publish();
    }

    public synchronized void setMaxDistanceMiles(double miles) {
        maxMiles = miles;
        publish();
    }

    // Distance to the nearer of the situations two points, in miles
    private double distanceMiles(TrafficSituation s) {
        double best = Double.MAX_VALUE;
        float[] r = new float[1];
        if (s.hasStart()) {
            Location.distanceBetween(MY_LAT, MY_LON, s.getStartLat(), s.getStartLon(), r);
            best = Math.min(best, r[0]);
        }
        if (s.hasEnd()) {
            Location.distanceBetween(MY_LAT, MY_LON, s.getEndLat(), s.getEndLon(), r);
            best = Math.min(best, r[0]);
        }
        return best / 1609.344;   // metres to miles
    }

    public synchronized void postUnplanned(List<TrafficSituation> list) {
        unplanned.clear();
        unplanned.addAll(list);
        publish();
    }

    public synchronized void postRoadworks(List<TrafficSituation> list) {
        roadworks.clear();
        roadworks.addAll(list);
        publish();
    }

    private void publish() {
        List<TrafficSituation> all = new ArrayList<>(unplanned);
        all.addAll(roadworks);
        situations.postValue(all);

        String q = roadQuery.toLowerCase();
        List<TrafficSituation> out = new ArrayList<>();
        for (TrafficSituation s: all) {
            boolean roadOk = q.isEmpty() || (s.getRoadName() != null && s.getRoadName().toLowerCase().contains(q));
            boolean distanceOk = maxMiles < 0 || distanceMiles(s) <= maxMiles;
            if (roadOk && distanceOk) out.add(s);
        }
        filtered.postValue(out);
    }

    public void setStatus(String s) { lastUpdated.postValue(s); }

    public void select(TrafficSituation s) { selected.setValue(s); }
}