package com.westminster.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root of the API. Hitting GET /api/v1 returns metadata plus a map of
 * the main resource collections - a lightweight form of HATEOAS that
 * lets clients discover the API without reading offline docs.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "Smart Campus Sensor and Room Management API");
        payload.put("version", "1.0.0");
        payload.put("description", "RESTful API for managing rooms, sensors and sensor readings across the campus.");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team", "Smart Campus Backend Team");
        contact.put("email", "smartcampus-admin@westminster.ac.uk");
        payload.put("contact", contact);

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        links.put("sensor_readings", "/api/v1/sensors/{sensorId}/readings");
        payload.put("resources", links);

        return Response.ok(payload).build();
    }
}
