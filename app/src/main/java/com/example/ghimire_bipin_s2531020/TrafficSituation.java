package com.example.ghimire_bipin_s2531020;
import android.util.Log;

public class TrafficSituation {

    private String Type;
    private String Description;
    private String roadName;
    private String startTime;
    private String endTime;
    private String Impact;
    private String delayType;
    private String direction;
    private boolean urgentRoadworks;
    private String locationText;
    private double startLat = Double.NaN, startLon = Double.NaN;
    private double endLat = Double.NaN, endLon = Double.NaN;

    public TrafficSituation() {
    }

    public String getType() {
        return Type;
    }

    public void setType(String Type) {
        this.Type = Type;
        Log.d("Traffic Situation", "Type set:" + this.Type);
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String Description) {
        this.Description = Description;
        Log.d("Traffic Situation", "Description set:" + this.Description);
    }

    public String getRoadName(){
        return roadName;
    }

    public void setRoadName(String roadName){
        this.roadName = roadName;
    }

    public String getStartTime(){
        return startTime;
    }

    public void setStartTime(String startTime){
        this.startTime = startTime;
    }

    public String getEndTime(){
        return endTime;
    }

    public void setEndTime(String endTime){
        this.endTime = endTime;
    }
    public String getImpact(){
        return Impact;
    }

    public void setImpact(String Impact){
        this.Impact = Impact;
    }

    public String getDelayType(){
        return delayType;
    }

    public void setDelayType(String delayType){
        this.delayType = delayType;
    }


    public String getLocationText() { return locationText; }
    public void setLocationText(String l) { this.locationText = l; }

    public double getStartLat() { return startLat; }
    public void setStartLat(double v) { startLat = v; }
    public double getStartLon() { return startLon; }
    public void setStartLon(double v) { startLon = v; }
    public double getEndLat() { return endLat; }
    public void setEndLat(double v) { endLat = v; }
    public double getEndLon() { return endLon; }
    public void setEndLon(double v) { endLon = v; }

    public boolean hasStart() { return !Double.isNaN(startLat) && !Double.isNaN(startLon); }
    public boolean hasEnd() { return !Double.isNaN(endLat) && !Double.isNaN(endLon); }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isUrgentRoadworks() {
        return urgentRoadworks;
    }

    public void setUrgentRoadworks(boolean urgentRoadworks) {
        this.urgentRoadworks = urgentRoadworks;
    }

    // Hours between start and end, or -1 if either is missing or unreadable
    public long getDurationHours() {
        if (startTime == null || endTime == null) return -1;
        try {
            java.time.OffsetDateTime start = java.time.OffsetDateTime.parse(startTime.trim());
            java.time.OffsetDateTime end = java.time.OffsetDateTime.parse(endTime.trim());
            return java.time.Duration.between(start, end).toHours();
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return "TrafficSituation{" +
                "Type='" + Type + '\'' +
                ", Description='" + Description + '\'' +
                ", roadName='" + roadName + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", Impact='" + Impact + '\'' +
                ", delayType='" + delayType + '\'' +
                '}';
    }


}










