package com.example.routetrackingapp;

import java.util.Collection;

public class User {

    private String username;
    private Number id;
    private Collection<Route> createdRoutes;

    public User(String username, Number id) {
        this.username = username;
        this.id = id;
    }
}
