package com.westminster.smartcampus.store;

import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Process-wide in-memory store. A single enum constant gives us a
 * thread-safe singleton without the usual double-checked-locking boilerplate.
 *
 * ConcurrentHashMap is used so that multiple JAX-RS resource instances
 * (Jersey creates a new resource per request by default) can mutate
 * the collections concurrently without extra synchronisation.
 */
public enum DataStore {

    INSTANCE;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    // Readings keyed by sensorId -> list of readings for that sensor
    private final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    DataStore() {
        seed();
    }

    /** A small amount of fixture data so the API is not empty on first boot. */
    private void seed() {
        Room library = new Room("LIB-301", "Library Quiet Study", 40);
        Room lab = new Room("LAB-210", "Computer Science Lab", 30);
        rooms.put(library.getId(), library);
        rooms.put(lab.getId(), lab);

        Sensor tempOne = new Sensor("TEMP-001", "Temperature", "ACTIVE", 21.4, "LIB-301");
        Sensor co2One = new Sensor("CO2-001", "CO2", "ACTIVE", 412.0, "LIB-301");
        Sensor occLab = new Sensor("OCC-010", "Occupancy", "MAINTENANCE", 0.0, "LAB-210");

        sensors.put(tempOne.getId(), tempOne);
        sensors.put(co2One.getId(), co2One);
        sensors.put(occLab.getId(), occLab);

        library.getSensorIds().add(tempOne.getId());
        library.getSensorIds().add(co2One.getId());
        lab.getSensorIds().add(occLab.getId());
    }

    // ----- Room helpers -----
    public Map<String, Room> rooms() { return rooms; }

    // ----- Sensor helpers -----
    public Map<String, Sensor> sensors() { return sensors; }

    // ----- Reading helpers -----
    /**
     * Returns the mutable list of readings for a given sensor, creating
     * it on first access. We wrap it in a synchronised list so
     * iteration and append from different threads cannot interleave.
     */
    public List<SensorReading> readingsFor(String sensorId) {
        return readings.computeIfAbsent(sensorId,
                k -> java.util.Collections.synchronizedList(new ArrayList<>()));
    }

    /**
     * Test-only hook: wipe everything. Not exposed via the API.
     */
    public void clearAll() {
        rooms.clear();
        sensors.clear();
        readings.clear();
    }
}
