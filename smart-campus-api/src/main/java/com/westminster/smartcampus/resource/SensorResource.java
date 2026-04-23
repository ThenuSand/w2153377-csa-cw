package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.model.ErrorResponse;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.model.Sensor;
import com.westminster.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sensor endpoints plus a sub-resource locator that hands off the
 * "/{sensorId}/readings" path to a dedicated SensorReadingResource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final Map<String, Sensor> sensors = DataStore.INSTANCE.sensors();
    private final Map<String, Room> rooms = DataStore.INSTANCE.rooms();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * The @QueryParam is optional. When null, all sensors are returned.
     */
    @GET
    public Response listSensors(@QueryParam("type") String type) {
        List<Sensor> result;
        if (type == null || type.isBlank()) {
            result = sensors.values().stream().collect(Collectors.toList());
        } else {
            result = sensors.values().stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }
        return Response.ok(result).build();
    }

    /** GET /api/v1/sensors/{sensorId} - fetch a single sensor. */
    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = sensors.get(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' was not found.");
        }
        return Response.ok(sensor).build();
    }

    /**
     * POST /api/v1/sensors
     * Creates a sensor. The target roomId must already exist, otherwise
     * we raise LinkedResourceNotFoundException which maps to HTTP 422.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(400, "BAD_REQUEST",
                            "Sensor id is required in the request body."))
                    .build();
        }
        if (sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse(409, "SENSOR_ALREADY_EXISTS",
                            "A sensor with id '" + sensor.getId() + "' already exists."))
                    .build();
        }

        String roomId = sensor.getRoomId();
        if (roomId == null || roomId.isBlank() || !rooms.containsKey(roomId)) {
            throw new LinkedResourceNotFoundException("Room", String.valueOf(roomId));
        }

        // Default status so callers do not have to specify it explicitly.
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        sensors.put(sensor.getId(), sensor);

        // Keep the parent room's sensorIds list in sync.
        Room owningRoom = rooms.get(roomId);
        if (!owningRoom.getSensorIds().contains(sensor.getId())) {
            owningRoom.getSensorIds().add(sensor.getId());
        }

        URI location = UriBuilder.fromResource(SensorResource.class)
                .path("{sensorId}")
                .build(sensor.getId());

        return Response.created(location).entity(sensor).build();
    }

    /**
     * Sub-resource locator. JAX-RS sees this method because of the @Path
     * but no HTTP verb annotation, and it will route any sub-path under
     * "/{sensorId}/readings" into the returned SensorReadingResource.
     *
     * Validating that the parent sensor exists here means child endpoints
     * can assume the id is real and keep their own logic focused on
     * readings.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource readings(@PathParam("sensorId") String sensorId) {
        Sensor parent = sensors.get(sensorId);
        if (parent == null) {
            throw new NotFoundException("Sensor '" + sensorId + "' was not found.");
        }
        return new SensorReadingResource(parent);
    }
}
