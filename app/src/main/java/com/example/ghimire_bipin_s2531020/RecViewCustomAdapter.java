package com.example.ghimire_bipin_s2531020;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecViewCustomAdapter extends RecyclerView.Adapter<RecViewCustomAdapter.ViewHolder> {

    public interface OnSituationClick {
        void onClick(TrafficSituation s);
    }

    private final List<TrafficSituation> mDataSet;
    private final OnSituationClick listener;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView, typeText, durationText;
        private final ImageView typeIcon;
        private final View durationStripe;

        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.textView);
            typeText = v.findViewById(R.id.typeText);
            durationText = v.findViewById(R.id.durationText);
            typeIcon = v.findViewById(R.id.typeIcon);
            durationStripe = v.findViewById(R.id.durationStripe);
        }
    }

    public RecViewCustomAdapter(List<TrafficSituation> dataSet, OnSituationClick listener) {
        this.mDataSet = dataSet;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.text_row_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int position) {
        TrafficSituation s = mDataSet.get(position);
        Context c = h.itemView.getContext();

        h.typeText.setText(prettyType(s.getType()));
        h.textView.setText(s.getDescription());
        h.typeIcon.setImageResource(iconFor(s.getType()));
        h.durationStripe.setBackgroundColor(colourFor(c, s));
        h.durationText.setText(durationLabel(s));
        h.itemView.setOnClickListener(v -> listener.onClick(s));
    }

    @Override
    public int getItemCount() { return mDataSet.size(); }

    // "MaintenanceWorks" -> "Maintenance Works"
    private String prettyType(String raw) {
        if (raw == null) return "Unknown";
        return raw.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    private int iconFor(String type) {
        String t = type == null ? "" : type.toLowerCase();

        if (t.contains("maintenance") || t.contains("construction") || t.contains("roadorcarriageway")) return R.drawable.ic_roadworks;
        if (t.contains("accident")) return R.drawable.ic_accident;
        if (t.contains("obstruction") || t.contains("abnormaltraffic")) return R.drawable.ic_obstruction;
        if (t.contains("environment") || t.contains("weather")) return R.drawable.ic_weather;
        return R.drawable.ic_warning;
    }

    private int colourFor(Context c, TrafficSituation s) {
        long hours = s.getDurationHours();
        int id;
        if (hours < 0) id = R.color.durationUnknown;
        else if (hours <= 24) id = R.color.durationShort;
        else if (hours <= 24 * 7) id = R.color.durationMedium;
        else id = R.color.durationLong;
        return ContextCompat.getColor(c, id);
    }

    private String durationLabel(TrafficSituation s) {
        long hours = s.getDurationHours();
        String duration;
        if (hours < 0) duration = "Duration unknown";
        else if (hours <= 24) duration = "Duration: " + hours + " h (short)";
        else if (hours <= 24 * 7) duration = "Duration: " + (hours / 24) + " days (medium)";
        else duration = "Duration: " + (hours / 24) + " days (long)";

        if (s.getDelayType() != null) {
            duration += "  ·  Delays: " + prettyType(s.getDelayType()).toLowerCase();
        }
        return duration;
    }
}