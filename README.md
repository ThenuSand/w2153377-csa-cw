# Smart Campus — Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**Student:** A.K.M.M Thenu Sandul — w2153377  
**GitHub:** [Repository Link](https://github.com/ThenuSand/w2153377-Thenu-CSA)

This is a RESTful API i built for the University of Westminster "Smart Campus" coursework. It is built using **JAX-RS (Jersey 2)** and runs on an embedded **Grizzly** HTTP server. The service manage **Rooms**, the **Sensors** that are deployed inside them, and a **history of Readings** that each sensor generates. All the data is kept **in memory** using `HashMap` / `ArrayList` inside a singleton `DataStore` class, no database is used because the spec does not allow it. Spring Boot is also not used.

All errors in the API are returned as JSON with the same shape so the client always gets a consistent format. Stack traces are never leaked to the client:

```json
{
  "status": 409,
  "error": "ROOM_NOT_EMPTY",
  "message": "Room 'LIB-301' still has 2 sensor(s) attached and cannot be deleted.",
  "timestamp": 1714000000000
}
```

---

## Setup & Run Guide

### 1. Requirements

- Java JDK 11 or higher
- Apache Maven 3.8 or higher
- A Java IDE (optional, the project can also be ran from the command line)

### 2. Download Project

```
git clone https://github.com/ThenuSand/w2153377-Thenu-CSA.git
cd w2153377-Thenu-CSA/smart-campus-api
```

### 3. Build the Project

```
mvn clean package
```

### 4. Run the Project

This will start the embedded Grizzly server on port 8080:

```
mvn exec:java
```

You can also run it on a different port by passing a system property:

```
mvn exec:java -Dport=9090
```

### 5. Stop the Server

Just press `Ctrl+C` in the terminal where the server is running.

### 6. Access the API

Once you see the log line `Smart Campus API started at http://localhost:8080/api/v1`, you can open it in a browser or in Postman:

```
http://localhost:8080/api/v1
```

---

## API Design Overview

The API is modeled around the physical layout of the campus. **Rooms** are the top level resource, **sensors** are deployed inside rooms, and every sensor has a history of **readings** over time. The Discovery endpoint at the API root returns metadata and HATEOAS links so the client always has a starting point to navigate from.

### Resource Hierarchy

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1` | Discovery endpoint — metadata, version, HATEOAS links |
| GET, POST | `/api/v1/rooms` | List all rooms / create a new room |
| GET, DELETE | `/api/v1/rooms/{roomId}` | Get room detail / delete (blocked with 409 if sensors are linked) |
| GET, POST | `/api/v1/sensors` | List sensors (optional `?type=` filter) / register a new sensor (422 if `roomId` does not exist) |
| GET, POST | `/api/v1/sensors/{sensorId}/readings` | Reading history / append a new reading (403 if sensor is in MAINTENANCE) |

**Models:** `Room`, `Sensor`, `SensorReading`, `ErrorResponse`.

---

## Sample `curl` Commands

All the commands below assume the server is running on `localhost:8080`. I have included more than 5 successful calls (as the brief requires) and also the 3 custom error paths from Part 5 so the marker can see them working.

```bash
# 1. Discovery endpoint (shows HATEOAS links)
curl -s "http://localhost:8080/api/v1" | jq

# 2. List every room
curl -s "http://localhost:8080/api/v1/rooms" | jq

# 3. Create a new room
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"id":"MPH-101","name":"Marylebone Physics Hall","capacity":120}' \
  "http://localhost:8080/api/v1/rooms"

# 4. Register a new sensor inside an existing room
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"id":"TEMP-777","type":"Temperature","status":"ACTIVE","currentValue":19.8,"roomId":"MPH-101"}' \
  "http://localhost:8080/api/v1/sensors"

# 5. Filter sensors by type using a query parameter
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | jq

# 6. Post a new reading for a sensor (this also updates the parent sensor's currentValue)
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"value":22.4,"timestamp":0}' \
  "http://localhost:8080/api/v1/sensors/TEMP-777/readings"

# 7. Get the reading history for a sensor
curl -s "http://localhost:8080/api/v1/sensors/TEMP-777/readings" | jq

# --- The error paths that Part 5 asks for ---

# 8. Try to DELETE a room that still has sensors -> HTTP 409 Conflict
curl -i -X DELETE "http://localhost:8080/api/v1/rooms/LIB-301"

# 9. Try to POST a sensor with a non-existent roomId -> HTTP 422 Unprocessable Entity
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"id":"OCC-999","type":"Occupancy","status":"ACTIVE","roomId":"GHOST-ROOM"}' \
  "http://localhost:8080/api/v1/sensors"

# 10. Try to POST a reading to a sensor whose status is MAINTENANCE -> HTTP 403 Forbidden
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"value":21.0,"timestamp":0}' \
  "http://localhost:8080/api/v1/sensors/TEMP-OFFLINE/readings"
```

---

## Project Structure

```
smart-campus-api/
├── pom.xml                                            # Maven build, JAX-RS + Grizzly dependencies
└── src/main/java/com/westminster/smartcampus/
    ├── App.java                                       # Boots Grizzly + Jersey
    ├── config/
    │   └── SmartCampusApplication.java                # @ApplicationPath("/api/v1")
    ├── model/
    │   ├── Room.java                                  # id, name, capacity, sensorIds
    │   ├── Sensor.java                                # id, type, status, currentValue, roomId
    │   ├── SensorReading.java                         # id, timestamp, value
    │   └── ErrorResponse.java                         # unified JSON error body
    ├── store/
    │   └── DataStore.java                             # Singleton in-memory store
    ├── resource/
    │   ├── DiscoveryResource.java                     # GET  /api/v1
    │   ├── RoomResource.java                          # /api/v1/rooms
    │   ├── SensorResource.java                        # /api/v1/sensors (+ sub-resource locator)
    │   └── SensorReadingResource.java                 # nested /sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java                 # -> 409 Conflict
    │   ├── LinkedResourceNotFoundException.java       # -> 422 Unprocessable Entity
    │   └── SensorUnavailableException.java            # -> 403 Forbidden
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   ├── NotFoundExceptionMapper.java
    │   └── GenericExceptionMapper.java                # catch-all ExceptionMapper<Throwable> -> 500
    └── filter/
        └── LoggingFilter.java                         # ContainerRequestFilter + ContainerResponseFilter
```

---

# Written Report (Answers to the Brief)

The report below follows the same structure as the coursework specification. Each section explains what I implemented for that task, and where the spec has a question i have included my answer underneath. The marks for each section are also shown based on the rubric.

## Part 1: Service Architecture & Setup (10 Marks)

### 1.1 Project & Application Configuration (5 Marks)

**Implementation.** The project is a Maven project (`smart-campus-api/pom.xml`) that uses **Jersey 2** as the JAX-RS implementation and an **embedded Grizzly HTTP server** as the lightweight container. The entry point of the application is `SmartCampusApplication` inside the `config` package, which extends `javax.ws.rs.core.Application` and is annotated with `@ApplicationPath("/api/v1")` so the API has a versioned entry point. `App.java` then boots the Grizzly server on port 8080 (or a different port if you pass `-Dport=...`) and registers the application class.

**Question:** *In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.*

By default Jersey treats resource classes as **per-request**. This means that every time the server gets an HTTP request that targets a resource method, a completely new instance of the resource class is created, the fields are injected, the method is called, and after the response is sent the instance is garbage collected. It is not a singleton by default, even though some frameworks work that way.

There is a good side and a bad side to this. The good side is that instance fields are safe from concurrent access because only one thread ever sees a particular instance, so you don't need to worry about race conditions at the field level. The bad side is that the resource instance is not a good place to keep state that should survive across requests. If I kept a `Map<String, Room>` as a field on `RoomResource`, it would be thrown away the moment the request finishes, and the next request would create a new resource instance with a empty map.

Because of this, in my Smart Campus API the resource classes are kept stateless. All the in-memory state actually lives in a single `DataStore` class which is implemented as a singleton so that every request sees the same object. The maps inside it are `ConcurrentHashMap`, so multiple request threads can read and write at the same time without a race condition, and the per-sensor reading list is wrapped with `Collections.synchronizedList` so iteration and append can't interleave. For larger compound updates that need to be atomic (for example "check-then-put") I use `computeIfAbsent` so the check and the insertion happen together. If I had instead marked the resource itself with `@Singleton`, I would have had to manually synchronise every shared field inside the resource class, which is a lot easier to get wrong.

### 1.2 The "Discovery" Endpoint (5 Marks)

**Implementation.** `DiscoveryResource` handles `GET /api/v1` and returns a JSON object with the API metadata — the version, an administrative contact, and a resource map with the URIs of every top-level collection (`/api/v1/rooms`, `/api/v1/sensors`). The URIs are built dynamically from `@Context UriInfo`, so they always match whatever host and port the server is running on.

**Question:** *Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?*

HATEOAS (Hypermedia as the Engine of Application State) is the top level in the Richardson maturity model for REST. The idea is that responses don't just carry the data, they also carry links that tell the client what it can do next. My discovery endpoint is a simple example of this. It returns a JSON object whose `resources` field list the URIs for every top-level collection, so a client that has never seen the API before can start from `/api/v1` and discover the rest of it on its own.

This is useful for a few reasons. First, the URIs become an internal detail of the server rather than a contract that every client has to hardcode. If I later decide to move `/api/v1/rooms` to `/api/v1/buildings/rooms`, I only have to update the discovery document, and the clients that follow the links will still work. Second, clients don't need to hardcode knowledge about which actions are allowed in the current state of a resource. A room that is empty could include a `delete` link in its response, while a room that still has sensors could omit that link, and the client does not have to duplicate the business rule on its side. Third, onboarding is faster because a developer can open the API in a browser and click their way around instead of flipping back and forth to a PDF. Static docs also tend to drift away from the actual code over time, but hypermedia links are generated from the code itself so they cannot drift.

---

## Part 2: Room Management (20 Marks)

### 2.1 Room Resource Implementation (10 Marks)

**Implementation.** `RoomResource` is mounted on `/api/v1/rooms` and provides `GET /` to list every room, `POST /` to create a new room (it returns `201 Created` with a `Location` header that points to the new resource), and `GET /{roomId}` to fetch the details of a single room. Each room is returned with its `id`, `name`, `capacity`, and the list of `sensorIds` that are currently deployed inside of it.

**Question:** *When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.*

If `GET /rooms` only returned the IDs, the response would be very small. On a campus with thousands of rooms, the difference between a JSON array of strings and a JSON array of full room objects (with their name, capacity, and list of sensor ids) is significant, and on a mobile or slow connection that bandwidth cost is a real thing.

But there is a catch. If the client only gets IDs, it usually needs the details anyway, so it ends up looping and sending a `GET /rooms/{id}` for every room. This is the classic N+1 request problem — thousands of HTTP round trips at maybe 50 ms each turns into minutes of latency, and on the server side it is also thousands of times the per-request overhead (creating the JAX-RS instance, running the filter chain, serialising the response). The total cost ends up being much worse than the payload savings.

For this reason my API returns the full room objects on the collection endpoint. For really huge collections the right answer is actually neither "only IDs" nor "full objects", it is pagination combined with a slim summary representation (only the fields needed for the list view) and a separate detail endpoint for the heavier fields. That way the client gets one round trip per page, a small payload, and a clear path to upgrade if a field ever needs to hold a big blob.

### 2.2 Room Deletion & Safety Logic (10 Marks)

**Implementation.** `DELETE /api/v1/rooms/{roomId}` removes a room and returns `204 No Content`. Before actually removing it, the resource checks the `DataStore` to see if any sensors are still attached to that room; if there are, the delete is rejected and a `RoomNotEmptyException` is thrown. The mapper (see section 5.1) then turns this into a `409 Conflict` response with a JSON error body that names the sensors that are still attached. This prevents sensor records from becoming orphaned references to a room that does not exist anymore.

**Question:** *Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.*

Idempotency in HTTP means that sending the same request multiple times leaves the server in the same state as sending it only once. My DELETE is idempotent in that state sense: the first call removes the room and returns `204 No Content`, the second call sees that the room is not there anymore and returns `404 Not Found`. The state of the server is identical after both call 1 and call 2 — in either case the room does not exist after.

The fact that the status code is different between the two calls is not a violation of idempotency, because idempotency is a property of the *server state*, not of the response codes. Some APIs actually prefer to always return `204` on DELETE even if the resource is already gone, because it makes the client logic simpler. I prefer the explicit `404` because it tells the caller "the thing you asked me to delete was not there", which is helpful when debugging a script that might have a stale cached id. Either way, repeatedly pressing the delete button cannot leave the system in a worse place than pressing it once.

There is one edge case that is not idempotent by design, and that's when the room is not empty. In that case the request is rejected with `409 Conflict` and the resource is left untouched. But this is still safe because the server state doesn't change at all, so i think it is fair to say my DELETE is side-effect-free on failure and idempotent on success.

---

## Part 3: Sensor Operations & Linking (20 Marks)

### 3.1 Sensor Resource & Integrity (10 Marks)

**Implementation.** `SensorResource` is mounted on `/api/v1/sensors` and supports `GET /` (list), `POST /` (register a new sensor), and `GET /{sensorId}` (detail). The POST method is annotated with `@Consumes(MediaType.APPLICATION_JSON)` so the runtime only accepts JSON bodies on it. Before the new sensor is actually saved, the resource checks `DataStore` to see if the declared `roomId` exists. If it does not, it throws a `LinkedResourceNotFoundException` and the mapper (section 5.2) converts that into `422 Unprocessable Entity`. On a successful registration, the new sensor's id is also appended to the parent room's `sensorIds` list so the two objects stay linked.

**Question:** *We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?*

`@Consumes(MediaType.APPLICATION_JSON)` is basically a declarative contract that says "this method only accepts request bodies whose `Content-Type` header is JSON". If a client sends `text/plain` or `application/xml` instead, the JAX-RS runtime tries to match the request to a resource method, but it can't find one on that path that is able to consume the supplied media type. So it responds with **HTTP 415 Unsupported Media Type** before my code is even called.

This is actually a really useful property to have. The resource method never has to check "is this JSON?" or catch a malformed-XML parse exception, because the framework filters out the wrong types at the edge. If i later wanted to accept XML as well I could just add `application/xml` to the `@Consumes` list and plug in a message body reader. If I wanted to accept anything I could use `*/*` and negotiate, but in practice a clear narrow contract is better because integration problems show up early.

### 3.2 Filtered Retrieval & Search (10 Marks)

**Implementation.** `GET /api/v1/sensors` has an optional `@QueryParam("type")` parameter. If the parameter is absent (`null`) the endpoint just returns the whole collection. If it is present, the endpoint only returns sensors whose `type` matches (case insensitive), for example `GET /api/v1/sensors?type=CO2`.

**Question:** *You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?*

Using `@QueryParam("type")` keeps the URI hierarchy clean. `/api/v1/sensors` is the collection of sensors, and `/api/v1/sensors?type=CO2` is still conceptually the same collection, just filtered. The URL basically says "here is the sensors collection, and here is a filter clause on top of it".

If I had used the path style `/api/v1/sensors/type/CO2` instead, I would be implying that `type/CO2` is a sub-resource of the sensors collection, which it really is not — it is a query, not a resource. Path parameters also do not compose well. For example, how would i express "CO2 sensors that are also in the ACTIVE state"? With query parameters I just add another one: `?type=CO2&status=ACTIVE`. With path parameters I would have to invent some weird syntax like `/sensors/type/CO2/status/ACTIVE`, and that falls apart the first time a filter is optional.

Query parameters are also the standard caching model. Proxies and browsers use the full URL (including the query string) as the cache key, so `?type=CO2` and `?type=Temperature` are automatically cached separately with no extra effort. Finally, query parameters are optional by default — if the client doesn't provide `type`, JAX-RS injects `null` into the method parameter, which is exactly the "no filter" case.

---

## Part 4: Deep Nesting with Sub-Resources (20 Marks)

### 4.1 The Sub-Resource Locator Pattern (10 Marks)

**Implementation.** `SensorResource` has a sub-resource locator method that is annotated with `@Path("{sensorId}/readings")` but **no HTTP verb** annotation. The locator looks up the parent sensor in `DataStore`, checks that it exists, and returns a new `SensorReadingResource` bound to that specific sensor. JAX-RS then continues dispatching the request against the returned object and calls the correct method (GET or POST) on it, see section 4.2.

**Question:** *Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?*

A sub-resource locator is a method on a resource that is annotated with `@Path` but does not have an HTTP verb annotation. Instead of handling the request itself, it returns a different object that JAX-RS then treats as another resource and dispatches into. In my project, `SensorResource.readings(sensorId)` checks that the parent sensor exists and then returns a `SensorReadingResource` that is bound to that parent.

This matters architecturally because it keeps the concerns separated. Without this pattern, every reading endpoint would have to live inside `SensorResource`, which would then be responsible for both sensor lifecycle AND reading history at the same time. That class would just keep growing as more nested paths got added. With the pattern, each class only has one job and can be tested on it's own. The parent sensor validation also happens exactly once, inside the locator, so the child class doesn't have to re-check it for every verb it supports.

It also gives the child resource a natural place to hold per-request context. `SensorReadingResource` takes the parent `Sensor` in its constructor, which means every method already has the parent object at hand without having to re-fetch it from the data store or pass it as an argument every time. This pattern is what stops a large nested API from becoming a single thousand-line "god controller".

### 4.2 Historical Data Management (10 Marks)

**Implementation.** `SensorReadingResource` is created per request by the locator in 4.1 and is always bound to one specific parent sensor. It provides:

- `GET /api/v1/sensors/{sensorId}/readings` — returns the history of readings for that sensor in the order they were appended.
- `POST /api/v1/sensors/{sensorId}/readings` — validates the payload, assigns a UUID for the new reading id, defaults the `timestamp` to the current epoch-ms if the client sent `0`, appends the reading to the history, and as the spec requires it also **triggers a side-effect update to the parent sensor's `currentValue`** so that a later `GET /api/v1/sensors/{sensorId}` shows the latest measurement immediately. If the sensor's status is `MAINTENANCE` the POST is rejected with a `SensorUnavailableException` that maps to `403 Forbidden` (see 5.3), because a disconnected sensor cannot accept readings.

This makes sure that the parent sensor's `currentValue` is always consistent with its own reading history.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging (30 Marks)

No raw Java stack trace is ever leaked to the client. Every non-success response is a structured `ErrorResponse` JSON body with the same shape that is shown at the top of this README, containing `status`, a machine readable `error` code, a human `message`, and a `timestamp`.

### 5.1 Resource Conflict (409) (5 Marks)

**Implementation.** A custom `RoomNotEmptyException` is thrown by `RoomResource.delete(...)` every time a caller tries to delete a room whose `sensorIds` list is not empty. The exception carries the offending room id and the number of sensors still attached. `RoomNotEmptyExceptionMapper` implements `ExceptionMapper<RoomNotEmptyException>` and returns `HTTP 409 Conflict` with an `ErrorResponse` body explaining that the room still has active hardware on it. This satisfies the rubric's requirement for a structured JSON error body on the 409 scenario.

### 5.2 Dependency Validation (422 Unprocessable Entity) (10 Marks)

**Implementation.** A custom `LinkedResourceNotFoundException` is thrown whenever a POST body references a resource that doesn't exist — specifically, when a sensor is registered with a `roomId` that is not in the `DataStore`. `LinkedResourceNotFoundExceptionMapper` maps this exception to `HTTP 422 Unprocessable Entity` with an `ErrorResponse` body that includes the missing reference type (`"room"`) and the id that the client supplied.

**Question:** *Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?*

`404 Not Found` means "the URL you requested doesn't map to any resource on this server". But when a client POSTs `{"roomId":"GHOST-ROOM"}` to `/api/v1/sensors`, the URL `/api/v1/sensors` is actually completely valid — the collection exists and the server is willing to accept a POST to it. What is wrong is the *content* of the body: it references a room that the server doesn't know about.

`404` would mislead any caller that has generic retry logic, because a 404 on a POST normally indicates a mistyped URL that needs to be fixed before retrying. `422 Unprocessable Entity` (from RFC 4918) basically says "the request was syntactically well-formed and I understood it, but the semantic contents are wrong", which is exactly the situation here. The client shouldn't retry the same payload, the client should fix the referenced id. Returning 422 lets the client tell the difference between "wrong address" and "wrong payload" without even having to parse the error body.

Some APIs prefer `400 Bad Request` for this same scenario. `400` is a safe fallback if the toolchain does not support `422`, but it is broader and less informative. My implementation uses `422` and also includes the referenced type and id in the error body so the client can react precisely.

### 5.3 State Constraint (403 Forbidden) (5 Marks)

**Implementation.** A custom `SensorUnavailableException` is thrown by `SensorReadingResource.post(...)` when someone tries to POST a reading against a sensor whose `status` is `"MAINTENANCE"` (or any non-`ACTIVE` status). This models the real-world case where a sensor has been physically disconnected for maintenance and cannot actually take new readings, so the server refuses to record them. `SensorUnavailableExceptionMapper` then converts the exception to `HTTP 403 Forbidden` with an `ErrorResponse` body that includes the sensor id and its current status.

### 5.4 The Global Safety Net (500) (5 Marks)

**Implementation.** `GenericExceptionMapper` implements `ExceptionMapper<Throwable>` and acts as the catch-all. Any exception that is not caught by a more specific mapper — for example a `NullPointerException`, an `IndexOutOfBoundsException`, a bug in another mapper, or a JSON deserialisation failure — is handled here. The full stack trace is still logged on the server through `java.util.logging.Logger` at `SEVERE` level so operators can debug it later, but the response to the client is just a clean `HTTP 500 Internal Server Error` with a generic `ErrorResponse` body saying an unexpected error happened.

**Question:** *From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?*

If my API responded to every server error with a raw Java stack trace, it would basically hand any attacker a free recon document. A stack trace typically exposes:

- The **package and class structure** of the application, which gives away the framework and libraries being used (Jersey, Jackson, often with specific version numbers). An attacker can then look up known CVEs that target those exact versions.
- The **full file path** on the server of the compiled class, which sometimes leaks the hosting account name, the operating system, or the directory layout of the deployment.
- **Internal method and parameter names** that hint at business logic, for example a trace containing `AuthService.validateToken` tells the attacker that such a service exists.
- **Database or third-party error messages** that might include table names, column names, SQL fragments, or external hostnames if the error originated from a database connector.
- **Thread names and server metadata** that can reveal whether the app is running behind a specific proxy, container, or cloud provider.

None of this is useful to a legitimate API consumer. So my `GenericExceptionMapper<Throwable>` logs the stack trace internally using `java.util.logging.Logger` (where the operators can see it) but only returns a generic message and a 500 status to the caller. The balance is: keep problems debuggable for the team, but keep them opaque to the outside world.

### 5.5 API Request & Response Logging Filters (5 Marks)

**Implementation.** `LoggingFilter` is annotated as `@Provider` and implements both `ContainerRequestFilter` and `ContainerResponseFilter`, so it runs once on the way in and once on the way out for every single request. Using a `java.util.logging.Logger`, the request filter logs the HTTP method and request URI from the `ContainerRequestContext`, and the response filter logs the final HTTP status code from the `ContainerResponseContext`. The filter has no business logic in it, it only observes, and it is auto-discovered by Jersey through the `@Provider` annotation so there is no per-endpoint wiring needed.

**Question:** *Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?*

Logging is a cross-cutting concern — every endpoint needs it, but no endpoint should actually care about it. If I put `Logger.info(...)` inside every resource method, I would be copy-pasting the same two or three lines all over the codebase, and every time I added a new endpoint I would have to remember to paste those lines again. Worse, I would miss the failure paths, because if the method throws an exception it never reach it's "bottom of method" log line anyway.

A `ContainerRequestFilter` + `ContainerResponseFilter` pair sits outside of the resource method. It fires once per request no matter which endpoint was hit, and no matter whether that endpoint returned normally or threw an exception. Adding a new resource means it inherits the logging automatically, and removing a resource means the log lines for it go away too, because there is no boilerplate code to delete.

The same argument applies to other cross-cutting concerns as well — authentication, rate limiting, request tracing, CORS headers, metrics collection. All of them belong in filters or interceptors, not inside the resource methods. It also means that a reviewer reading my `RoomResource` is only reading room logic, not ten lines of log setup, which makes the business rules easier to audit.

---

## Video Demonstration

A short Postman walkthrough (less than 10 minutes) has been recorded and submitted on **Blackboard**. It covers: POST-ing a room and showing `201 Created` with the `Location` header, DELETE-ing a room in both the success case and the `409 Conflict` case for a room that still has sensors, POST-ing a sensor in both the valid case and the `422` case for a non-existent `roomId`, the `?type=` query parameter filter, navigating into `/sensors/{id}/readings` and showing the `currentValue` side-effect on the parent sensor, and finally triggering a `500` error to demonstrate that no stack trace is returned in the response.

---

## References

- Coursework specification: *5COSC022W Client-Server Architectures — "Smart Campus" Sensor & Room Management API*, University of Westminster, 2025/26.
- Jakarta RESTful Web Services (JAX-RS) specification.
- Eclipse Jersey user guide.
- RFC 4918 §11.2 — HTTP 422 Unprocessable Entity.
- Compliance note: no Spring Boot, no database technology and no zip file submission is used. The project is hosted in a public GitHub repository as required by the brief.
