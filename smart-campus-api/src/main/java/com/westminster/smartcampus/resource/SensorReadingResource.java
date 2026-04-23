package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.SensorUnavailableException;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.model.SensorReading;
import com.westminster.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

/**
 * Nested resource reached through SensorResource.readings(..). Handles
 * the historical reading log for ONE parent sensor. Not annotated with
 * @Path itself - the parent locator establishes the path prefix.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final Sensor parent;

    public SensorReadingResource(Sensor parent) {
        this.parent = parent;
    }

    /** GET /api/v1/sensors/{sensorId}/readings - full history. */
    @GET
    public Response getHistory() {
        List<SensorReading> history = DataStore.INSTANCE.readingsFor(parent.getId());
        // Defensive copy to avoid exposing the internal list.
        return Response.ok(new java.util.ArrayList<>(history)).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings - append a new reading.
     * Also updates the parent sensor's currentValue so GETs on the
     * sensor reflect the latest measurement.
     */
    @POST
    public Response addReading(SensorReading reading) {
        if (reading == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new com.westminster.smartcampus.model.ErrorResponse(
                            400, "BAD_REQUEST",
                            "Reading body is required."))
                    .build();
        }

        // Sensors that are not ACTIVE are not allowed to record readings.
        if (!"ACTIVE".equalsIgnoreCase(parent.getStatus())) {
            throw new SensorUnavailableException(parent.getId(), parent.getStatus());
        }

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0L) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        DataStore.INSTANCE.readingsFor(parent.getId()).add(reading);

        // Side effect required by the spec: parent sensor reflects the
        // latest reading's value.
        parent.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
