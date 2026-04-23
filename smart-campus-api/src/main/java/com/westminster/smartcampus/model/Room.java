package com.westminster.smartcampus.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Physical space on campus. A room owns a collection of sensor IDs
 * (kept as IDs rather than full Sensor objects to avoid cycles when
 * the JSON is serialised).
 */
public class Room {

    private String id;
    private String name;
    private int capacity;
    private List<String> sensorIds = new ArrayList<>();

    public Room() {
        // no-arg constructor is required for Jackson deserialisation
    }

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public List<String> getSensorIds() { return sensorIds; }
    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = (sensorIds == null) ? new ArrayList<>() : sensorIds;
    }
}
