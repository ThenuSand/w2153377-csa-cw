package com.westminster.smartcampus.resource;

import com.westminster.smartcampus.exception.RoomNotEmptyException;
import com.westminster.smartcampus.model.Room;
import com.westminster.smartcampus.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Room management endpoints. Spec calls this a "SensorRoom Resource" but
 * the path and payload shape is the important contract, not the Java name.
 * The class name RoomResource reads more naturally in logs and IDEs.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final Map<String, Room> rooms = DataStore.INSTANCE.rooms();

    /** GET /api/v1/rooms - list every room. */
    @GET
    public Response listRooms() {
        Collection<Room> all = rooms.values();
        return Response.ok(all).build();
    }

    /** POST /api/v1/rooms - create a new room. */
    @POST
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new com.westminster.smartcampus.model.ErrorResponse(
                            400, "BAD_REQUEST",
                            "Room id is required in the request body."))
                    .build();
        }

        if (rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new com.westminster.smartcampus.model.ErrorResponse(
                            409, "ROOM_ALREADY_EXISTS",
                            "A room with id '" + room.getId() + "' already exists."))
                    .build();
        }

        rooms.put(room.getId(), room);

        // 201 Created with a Location header is the REST-idiomatic response.
        URI location = UriBuilder.fromResource(RoomResource.class)
                .path("{roomId}")
                .build(room.getId());

        return Response.created(location).entity(room).build();
    }

    /** GET /api/v1/rooms/{roomId} - fetch a single room. */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new NotFoundException("Room '" + roomId + "' was not found.");
        }
        return Response.ok(room).build();
    }

    /** DELETE /api/v1/rooms/{roomId} - remove a room if it has no sensors. */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            // Per REST semantics we treat repeated deletes as idempotent:
            // if the target is already gone, we still return 404 for the
            // first caller who missed it. See the report for the reasoning.
            throw new NotFoundException("Room '" + roomId + "' was not found.");
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId, room.getSensorIds().size());
        }

        rooms.remove(roomId);
        return Response.noContent().build();
    }
}
