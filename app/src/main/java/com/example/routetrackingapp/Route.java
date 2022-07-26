package com.example.routetrackingapp;

public class Route {

    private String routename;
    private Number id;
    private User creator;

    public Route(String routename, Number id, User creator) {
        this.routename = routename;
        this.id = id;
        this.creator = creator;
    }
}
