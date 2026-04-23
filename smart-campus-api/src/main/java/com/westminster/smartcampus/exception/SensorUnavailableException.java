package com.westminster.smartcampus.exception;

/**
 * Thrown when a reading is POSTed to a sensor whose current state does
 * not allow new data (e.g. MAINTENANCE or OFFLINE). Mapped to HTTP 403.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently '" + status
                + "' and cannot accept new readings.");
        this.sensorId = sensorId;
        this.status = status;
    }

    public String getSensorId() { return sensorId; }
    public String getStatus() { return status; }
}
