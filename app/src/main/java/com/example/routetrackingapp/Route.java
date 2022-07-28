package com.example.routetrackingapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.sql.Time;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Route implements Serializable {

    private String routeName;
    private Number id;
    private User creator;
    private List<LatLng> routeData;
    private double time;
    private double distance;

    public LatLng getMeanLatLng() {
        return meanLatLng;
    }

    public void setMeanLatLng(LatLng meanLatLng) {
        this.meanLatLng = meanLatLng;
    }

    private LatLng meanLatLng;
    private double elevation;

    public Route(String routeName, List<LatLng> routeData, double time, double distance) {
        this.routeName = routeName;
        this.routeData = routeData;
        this.time = time;
        this.distance = distance;
        this.meanLatLng = calculateMeanLatLng(routeData);
    }

    private LatLng calculateMeanLatLng(List<LatLng> routeData) {
        float lat = 0;
        float lng = 0;
        for (LatLng data : routeData) {
            lat += data.latitude;
            lng += data.longitude;
        }
        float meanLat = lat/routeData.size();
        float meanLng = lng/routeData.size();
        LatLng mean = new LatLng(meanLat, meanLng);
        return mean;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public Number getId() {
        return id;
    }

    public void setId(Number id) {
        this.id = id;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public List<LatLng> getRouteData() {
        return routeData;
    }

    public void setRouteData(List<LatLng> routeData) {
        this.routeData = routeData;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("routeName", routeName);
        result.put("routeData", routeData);
        result.put("time", time);
        result.put("distance", distance);
        result.put("meanLatLng", meanLatLng);

        return result;
    }

}
