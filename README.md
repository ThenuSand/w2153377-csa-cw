# Smart Campus — Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**Student:** A.K.M.M Thenu Sandul — w2153377  
**GitHub:** [Repository Link](https://github.com/ThenuSand/w2153377-Thenu-CSA)

This is the RESTful API I built for the University of Westminster "Smart Campus" coursework. It is built on top of **JAX-RS (Jersey 2)** and runs inside an embedded **Grizzly** HTTP container. The service manages **Rooms**, the **Sensors** that are deployed inside them, and a **history of Readings** that each sensor produces. All data is stored **in memory** using `HashMap` and `ArrayList`-style structures inside a thread-safe singleton `DataStore`. As the brief requires, no database technology and no Spring Boot has been used.

Every error is returned as a structured JSON body that always has the same shape, so clients get a consistent format and no raw Java stack trace ever leaks to the outside world:

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
- A Java IDE (optional — the project also runs from the command line)

### 2. Download the Project

```
git clone https://github.com/ThenuSand/w2153377-Thenu-CSA.git
cd w2153377-Thenu-CSA/smart-campus-api
```

### 3. Build the Project

```
mvn clean package
```

### 4. Run the Project

Start the embedded Grizzly server on the default port 8080:

```
mvn exec:java
```

If port 8080 is already in use, pass a different port as a system property:

```
mvn exec:java -Dport=9090
```

### 5. Stop the Server

Press `Ctrl+C` in the terminal that is running the server.

### 6. Access the API

Once the log line `Smart Campus API started at http://localhost:8080/api/v1` appears, open it in a browser or in Postman:

```
http://localhost:8080/api/v1
```

---

## API Design Overview

The API is modelled around the physical layout of the campus: **rooms** are the top-level resource, **sensors** are deployed inside rooms, and every sensor accumulates a history of **readings** over time. A Discovery endpoint at the API root returns metadata and HATEOAS links, so a client always has a starting point to navigate from.

### Resource Hierarchy

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1` | Discovery endpoint — metadata, version, HATEOAS links |
| GET, POST | `/api/v1/rooms` | List all rooms / create a new room |
| GET, DELETE | `/api/v1/rooms/{roomId}` | Get room detail / delete (blocked with 409 if sensors are still attached) |
| GET, POST | `/api/v1/sensors` | List sensors (optional `?type=` filter) / register a new sensor (422 if `roomId` does not exist) |
| GET, POST | `/api/v1/sensors/{sensorId}/readings` | Reading history / append a new reading (403 if the sensor's status is MAINTENANCE) |

**Models:** `Room`, `Sensor`, `SensorReading`, `ErrorResponse`.

---

## Sample `curl` Commands

All of the commands below assume the server is running on `localhost:8080`. I have included more than the five successful interactions the brief asks for, plus the three custom error paths from Part 5 so the marker can verify them easily.

```bash
# 1. Discovery endpoint (shows HATEOAS links)
curl -s "http://localhost:8080/api/v1" | jq

# 2. List every room
curl -s "http://localhost:8080/api/v1/rooms" | jq

# 3. Create a new room (returns 201 Created with a Location header)
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"id":"MPH-101","name":"Marylebone Physics Hall","capacity":120}' \
  "http://localhost:8080/api/v1/rooms"

# 4. Register a new sensor inside an existing room
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"id":"TEMP-777","type":"Temperature","status":"ACTIVE","currentValue":19.8,"roomId":"MPH-101"}' \
  "http://localhost:8080/api/v1/sensors"

# 5. Filter sensors by type using a query parameter
curl -s "http://localhost:8080/api/v1/sensors?type=Temperature" | jq

# 6. Post a new reading (this also updates the parent sensor's currentValue)
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"value":22.4,"timestamp":0}' \
  "http://localhost:8080/api/v1/sensors/TEMP-777/readings"

# 7. Get the reading history for a sensor
curl -s "http://localhost:8080/api/v1/sensors/TEMP-777/readings" | jq

# --- Error paths required by Part 5 of the brief ---

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

The report below follows the same structure as the coursework specification. For each section I have described what I implemented, and wherever the spec includes a question I have quoted it word-for-word and given my answer underneath. The marks for each section match the rubric (Parts 1–4 are 10 marks each for their two sub-tasks; Part 5 is 30 marks split across five sub-tasks).

## Part 1: Service Architecture & Setup (10 Marks)

### 1.1 Project & Application Configuration (5 Marks)

**Implementation.** The project is a single Maven module (`smart-campus-api/pom.xml`) that integrates **Jersey 2** as the JAX-RS implementation and uses an **embedded Grizzly HTTP server** as the lightweight container. The application entry point is `SmartCampusApplication` in the `config` package, which extends `javax.ws.rs.core.Application` and is annotated with `@ApplicationPath("/api/v1")` to establish the API's versioned base path. `App.java` then boots Grizzly on port 8080 (configurable through `-Dport=...`) and registers the application class.

**Question:** *In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.*

By default, Jersey treats resource classes as **per-request (request-scoped)**. Every time the server receives an HTTP request that targets a resource method, a brand-new instance of that resource class is constructed, fields are injected, the method is invoked, and the instance becomes eligible for garbage collection as soon as the response is sent. It is not a singleton by default, even though some service frameworks behave that way.

There is an upside and a downside to the per-request lifecycle. The upside is that instance fields are naturally safe from concurrent access, because only one thread ever sees a given instance, so race conditions at the field level simply cannot happen. The downside is that the resource instance is a terrible place to keep state that must survive across requests. If I kept a `Map<String, Room>` as a field in `RoomResource`, it would be thrown away the moment the response finished, and the next request would see a fresh, empty map.

Because of this, my resource classes are stateless and all persistent in-memory state lives in a single `DataStore` class implemented as a thread-safe singleton, so every request sees the same object. The maps inside it are `ConcurrentHashMap`, which lets multiple request threads read and mutate them at the same time without locking the whole collection. The per-sensor readings list is wrapped in `Collections.synchronizedList`, so an iteration and an append cannot interleave with each other. Where a compound update needs to be atomic — for example a "check then put" — I use `computeIfAbsent`, which performs the lookup and the insertion as a single operation under the map's internal lock. If I had instead marked the resource itself with `@Singleton` I would have had to hand-synchronise every shared field inside the resource, which is much easier to get wrong than delegating the concurrency control to `ConcurrentHashMap`.

### 1.2 The "Discovery" Endpoint (5 Marks)

**Implementation.** `DiscoveryResource` handles `GET /api/v1` and returns a JSON object containing API metadata: the version number, an administrative contact, and a resource map with the URIs of every top-level collection (`/api/v1/rooms`, `/api/v1/sensors`). The URIs are built dynamically from `@Context UriInfo`, so they always match the host and port the server is actually running on, including when the port is overridden at startup.

**Question:** *Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?*

HATEOAS (Hypermedia as the Engine of Application State) is the top rung of the Richardson maturity model for REST. The core idea is that responses do not only carry data — they also carry links that tell the client what it can do next. My Discovery endpoint is a small example: it returns a JSON object whose `resources` field lists the URIs for every top-level collection, so a client that has never seen the API before can start from `/api/v1` and navigate the whole surface without prior knowledge.

This benefits client developers in several concrete ways. First, the URIs become an internal detail of the server instead of a contract baked into every client. If I later moved `/api/v1/rooms` to `/api/v1/buildings/rooms`, I would only have to update the Discovery document, and any client that follows the links would continue to work. Second, clients do not need to hard-code the business rule for which actions are legal in the current state of a resource: a room that is empty could include a `delete` link in its representation while a room that still has sensors could omit it, and the client would not have to duplicate the rule on its side. Third, the API is self-documenting — a developer can explore it in a browser and discover the surface by clicking instead of flipping between the code and a PDF. Static documentation tends to drift away from the actual behaviour over time, whereas hypermedia links are generated from the code itself, so they cannot drift.

---

## Part 2: Room Management (20 Marks)

### 2.1 Room Resource Implementation (10 Marks)

**Implementation.** `RoomResource` is mounted at `/api/v1/rooms` and exposes `GET /` to list every room, `POST /` to create a new room (it returns `201 Created` with a `Location` header pointing at the new resource), and `GET /{roomId}` to fetch the details of a single room. Each room is serialised with its `id`, `name`, `capacity`, and the list of `sensorIds` currently deployed inside it.

**Question:** *When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.*

Returning only the IDs on `GET /rooms` would make the response very small. On a campus with thousands of rooms, the difference between a JSON array of strings and a JSON array of full room objects (each with its name, capacity, and list of sensor IDs) is significant, and on a mobile or slow connection that bandwidth saving is real.

However, there is a catch. If the client receives only IDs, it usually needs the details straight afterwards, so it has to loop and issue a `GET /rooms/{id}` for every single room. This is the classic N+1 request problem: thousands of HTTP round trips at maybe 50 ms each turns into minutes of latency, and on the server side it multiplies the per-request overhead (JAX-RS instance creation, filter chain, serialisation) by the same factor. In most realistic cases the aggregate cost of all those extra requests dwarfs the bandwidth saved by the smaller payload.

For this reason my API returns the full room objects on the collection endpoint. For very large collections the correct answer is actually neither "only IDs" nor "full objects"; it is pagination combined with a deliberately slim summary representation (only the fields needed to render a list view) and a separate detail endpoint for the heavier fields. That approach gives the client one round trip per page, a small payload, and a clean upgrade path if a field ever needs to hold a large blob.

### 2.2 Room Deletion & Safety Logic (10 Marks)

**Implementation.** `DELETE /api/v1/rooms/{roomId}` removes a room and returns `204 No Content`. Before the removal, the resource asks `DataStore` whether any sensors are still attached to that room; if there are, the delete is refused and a `RoomNotEmptyException` is thrown. The mapper (see 5.1) converts this into a `409 Conflict` response with a JSON error body that names the sensors still attached. This prevents sensor records from becoming orphaned references to a room that no longer exists.

**Question:** *Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.*

Yes, my DELETE is idempotent. Idempotency in HTTP means that sending the same request N times leaves the server in the same state as sending it once, and that is the property my implementation satisfies. The first call removes the room and returns `204 No Content`. The second call finds the room is already gone and returns `404 Not Found`. Crucially, after both calls the state of the server is identical — in neither case does the room exist afterwards — and no additional side effects are produced by the extra calls.

The fact that the second call returns a different status code is not a violation of idempotency, because idempotency is defined in terms of server state, not response codes. Some APIs prefer to always return `204` on DELETE even when the resource is already absent because it simplifies the client logic; I prefer the explicit `404` because it tells the caller "the thing you asked me to delete was not here", which is useful when debugging a script that might be working from a stale cached ID. Either way, repeatedly pressing the delete button cannot leave the system in a worse state than pressing it once.

There is one edge case that is safe but not strictly idempotent in the success sense: if the room is not empty, the request is rejected with `409 Conflict` and the resource is left untouched. This is still safe because the server state does not change at all, but it is worth stating explicitly — my DELETE is side-effect-free on failure and idempotent on success.

---

## Part 3: Sensor Operations & Linking (20 Marks)

### 3.1 Sensor Resource & Integrity (10 Marks)

**Implementation.** `SensorResource` is mounted at `/api/v1/sensors` and supports `GET /` (list), `POST /` (register a new sensor), and `GET /{sensorId}` (detail). The POST method is annotated with `@Consumes(MediaType.APPLICATION_JSON)` so it accepts only JSON bodies. Before the new sensor is stored, the resource asks `DataStore` whether the declared `roomId` exists. If it does not, the resource throws a `LinkedResourceNotFoundException` and the mapper (see 5.2) converts it to `422 Unprocessable Entity`. On a successful registration, the new sensor's id is also appended to the parent room's `sensorIds` list, so the two objects remain consistent with each other.

**Question:** *We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?*

`@Consumes(MediaType.APPLICATION_JSON)` is a declarative contract: this method will only accept request bodies whose `Content-Type` header advertises JSON. When a client sends `text/plain` or `application/xml` instead, the JAX-RS runtime tries to match the request to a resource method, cannot find one on that path whose `@Consumes` list includes the supplied media type, and responds with **HTTP 415 Unsupported Media Type** before my code is called at all.

This is a very useful property in practice. My resource method never has to check "is this JSON?" or catch a malformed-XML parse exception, because the framework filters out the wrong media types at the edge. If I wanted to accept XML in addition to JSON, I could just add `application/xml` to the `@Consumes` list and plug in a message body reader. If I wanted to accept anything and negotiate at runtime, I could use `*/*`, but in practice a narrow contract is better, because integration problems then show up early and in a very obvious way (a clear 415 rather than a confusing 500 later in the pipeline).

### 3.2 Filtered Retrieval & Search (10 Marks)

**Implementation.** `GET /api/v1/sensors` accepts an optional `@QueryParam("type")` argument. When the parameter is absent (null) the endpoint returns the whole collection; when present, it returns only the sensors whose `type` matches the given value (the comparison is case-insensitive), for example `GET /api/v1/sensors?type=CO2`.

**Question:** *You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?*

Using `@QueryParam("type")` keeps the URI hierarchy clean. `/api/v1/sensors` is the collection of sensors, and `/api/v1/sensors?type=CO2` is still the same collection, just filtered. Semantically, the URL says "here is the sensors collection, and here is a filter clause on top of it".

If I had used the path style `/api/v1/sensors/type/CO2` instead, I would be implying that `type/CO2` is a sub-resource of the sensors collection, which it is not — it is a query, not a resource. Path parameters also do not compose well. For example, how would I express "CO2 sensors that are also ACTIVE"? With query parameters I just add another one: `?type=CO2&status=ACTIVE`. With path parameters I would have to invent a syntax like `/sensors/type/CO2/status/ACTIVE`, and that falls apart the first time a filter value is optional.

Query parameters are also the standard HTTP caching model. Proxies and browsers key cached GET responses on the full URL, query string included, so `?type=CO2` and `?type=Temperature` are automatically stored under separate cache keys with no extra effort on my side. Finally, query parameters are optional by default — if the client does not supply `type`, JAX-RS injects `null` into the method parameter, which is exactly the "no filter" case. For all these reasons, query parameters are the natural fit for filtering and searching collections.

---

## Part 4: Deep Nesting with Sub-Resources (20 Marks)

### 4.1 The Sub-Resource Locator Pattern (10 Marks)

**Implementation.** `SensorResource` contains a sub-resource locator method annotated with `@Path("{sensorId}/readings")` but **no HTTP verb** annotation. The locator looks up the parent sensor in `DataStore`, verifies it exists (if not, a `NotFoundException` is thrown and mapped to a `404`), and returns a freshly constructed `SensorReadingResource` bound to that specific parent sensor. JAX-RS then re-dispatches the incoming request against the returned object and calls the appropriate reading-level method, as described in 4.2.

**Question:** *Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?*

A sub-resource locator is a method on a resource that is annotated with `@Path` but does not have an HTTP-verb annotation. Instead of handling the request itself, it returns another object, which JAX-RS then treats as a resource and dispatches into. In my project, `SensorResource.readings(sensorId)` verifies the parent sensor exists and returns a `SensorReadingResource` bound to that parent.

Architecturally this matters because it enforces separation of concerns. Without the pattern, every reading endpoint would live inside `SensorResource`, which would then be responsible for both sensor lifecycle and reading history simultaneously. That class would grow without bound as more nested paths were added. With the pattern each class has a single responsibility and can be tested in isolation. The parent-sensor validation happens exactly once, inside the locator, so the child class does not have to re-check it for every HTTP verb it implements.

The pattern also gives the child resource a natural place to hold per-request context. `SensorReadingResource` takes the parent `Sensor` in its constructor, so every method inside it already has the parent at hand without re-fetching it from the data store or passing it around as a method argument. For a project of this size the gain is modest, but the same pattern scales cleanly: in a larger API with deeply nested paths like `sensors/{id}/readings/{rid}/flags/{fid}`, the alternative is a single thousand-line "god controller" that tries to handle everything, and that is exactly what the sub-resource locator pattern is designed to prevent.

### 4.2 Historical Data Management (10 Marks)

**Implementation.** `SensorReadingResource` is created per request by the locator in 4.1 and is always bound to one specific parent sensor. It exposes:

- `GET /api/v1/sensors/{sensorId}/readings` — returns the sensor's reading history in append order.
- `POST /api/v1/sensors/{sensorId}/readings` — validates the payload, assigns a UUID as the reading's id, defaults the `timestamp` to the current epoch-milliseconds if the client sent `0`, appends the reading to the sensor's history, and **triggers a side-effect update to the parent sensor's `currentValue`** as required by the brief, so that a subsequent `GET /api/v1/sensors/{sensorId}` immediately reflects the latest measurement. If the sensor's status is `MAINTENANCE`, the POST is rejected with a `SensorUnavailableException` which maps to `403 Forbidden` (see 5.3), because a physically disconnected sensor cannot accept readings.

This design keeps the parent sensor's `currentValue` permanently consistent with its own reading history, which is the data-integrity guarantee the brief requires.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging (30 Marks)

The API is "leak-proof": no raw Java stack trace is ever returned to a client. Every non-success response is a structured `ErrorResponse` JSON body of the shape shown at the top of this README, containing `status`, a machine-readable `error` code, a human-readable `message`, and a `timestamp`.

### 5.1 Resource Conflict (409) (5 Marks)

**Implementation.** A custom `RoomNotEmptyException` is thrown by `RoomResource.delete(...)` whenever a caller tries to delete a room whose `sensorIds` list is not empty. The exception carries the offending room id and the number of sensors still attached. `RoomNotEmptyExceptionMapper` implements `ExceptionMapper<RoomNotEmptyException>` and returns `HTTP 409 Conflict` with a full `ErrorResponse` JSON body explaining that the room is still occupied by active hardware.

### 5.2 Dependency Validation (422 Unprocessable Entity) (10 Marks)

**Implementation.** A custom `LinkedResourceNotFoundException` is thrown whenever a POST body references a resource that does not exist — specifically when a sensor is registered against a `roomId` that is not in the `DataStore`. `LinkedResourceNotFoundExceptionMapper` maps this to `HTTP 422 Unprocessable Entity` with an `ErrorResponse` body that names the missing reference type (`"room"`) and the id the client supplied, so the client can correct the payload precisely.

**Question:** *Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?*

`404 Not Found` means "the URL you requested does not map to any resource on this server". When a client POSTs `{"roomId":"GHOST-ROOM"}` to `/api/v1/sensors`, the URL `/api/v1/sensors` is perfectly valid — the collection exists and the server is willing to accept a POST to it. What is wrong is the content of the body: it references a room that the server does not know about.

Returning `404` would mislead any caller with generic retry logic, because a `404` on a POST normally indicates a mistyped URL that needs fixing before retrying. `422 Unprocessable Entity` (from RFC 4918) says "the request was syntactically well-formed and I understood it, but the semantic contents are incorrect", which is exactly the situation here. The client should not retry the same payload; the client should fix the referenced id. Returning `422` lets the client distinguish "wrong address" from "wrong payload" without having to parse the error body.

Some APIs prefer `400 Bad Request` for the same scenario. `400` is a reasonable fallback if the toolchain does not support `422`, but it is broader and less informative. My implementation uses `422` and also includes the referenced type and id in the error body, so the client can react precisely without guessing.

### 5.3 State Constraint (403 Forbidden) (5 Marks)

**Implementation.** A custom `SensorUnavailableException` is thrown by `SensorReadingResource.post(...)` when a POST reading is attempted against a sensor whose `status` is `"MAINTENANCE"` (or otherwise not `ACTIVE`). This models the real-world scenario in the brief — a sensor that is physically disconnected for maintenance cannot actually accept readings, so the server refuses to record them. `SensorUnavailableExceptionMapper` converts the exception into `HTTP 403 Forbidden` with an `ErrorResponse` body naming the sensor id and its current status, so the client understands both what is wrong and why.

### 5.4 The Global Safety Net (500) (5 Marks)

**Implementation.** `GenericExceptionMapper` implements `ExceptionMapper<Throwable>` and acts as the catch-all. Any exception that is not caught by a more specific mapper — a `NullPointerException`, an `IndexOutOfBoundsException`, a JSON deserialisation failure, or a bug in one of my own classes — is handled here. The full stack trace is logged on the server through `java.util.logging.Logger` at `SEVERE` level, so operators can still debug it internally, but the response to the client is only a clean `HTTP 500 Internal Server Error` with a generic `ErrorResponse` body saying an unexpected error occurred.

**Question:** *From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?*

If my API returned a raw Java stack trace on every server error, it would effectively hand any attacker a free reconnaissance document. A stack trace typically exposes:

- The **package and class structure** of my application, which reveals the framework and libraries in use (Jersey, Jackson, often with specific version numbers). An attacker can then look up known CVEs targeting those exact versions.
- The **full file path** on the server of the compiled class, which sometimes reveals the hosting account name, the operating system, or the directory layout of the deployment.
- **Internal method and parameter names** that hint at business logic: a trace containing `AuthService.validateToken` tells the attacker that such a service exists and suggests where to aim further probes.
- **Database or third-party error messages** that may contain table names, column names, SQL fragments, or external hostnames if the error originated from a connector.
- **Thread names and server metadata** that can disclose whether the application is running behind a specific proxy, container, or cloud provider.

None of this information is needed by a legitimate API consumer. My `GenericExceptionMapper<Throwable>` therefore logs the stack trace internally where operators can see it, but returns only a generic message and a `500` status to the caller. The principle is: keep problems debuggable for the team, and opaque to the outside world. This aligns with the OWASP guidance on avoiding sensitive information disclosure through error responses.

### 5.5 API Request & Response Logging Filters (5 Marks)

**Implementation.** `LoggingFilter` is annotated as `@Provider` and implements both `ContainerRequestFilter` and `ContainerResponseFilter`, so it runs exactly once on the way in and once on the way out of every request. Using `java.util.logging.Logger`, the request filter logs the HTTP method and request URI from the `ContainerRequestContext`, and the response filter logs the final HTTP status code from the `ContainerResponseContext`. The filter contains no business logic — it only observes — and it is auto-discovered by Jersey through the `@Provider` annotation, so there is no per-endpoint wiring to maintain.

**Question:** *Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?*

Logging is a cross-cutting concern: every endpoint needs it, but no endpoint should actually care about it. If I put `Logger.info(...)` calls inside every resource method, I would be duplicating the same two or three lines throughout the codebase, and every time I added a new endpoint I would have to remember to copy them across. Even worse, I would miss the failure paths, because a method that throws an exception never reaches its "bottom of method" log line.

A `ContainerRequestFilter` and `ContainerResponseFilter` pair sits outside the resource method. It fires once per request regardless of which endpoint was hit, and regardless of whether that endpoint returned normally or threw an exception. Adding a new resource automatically inherits the logging for free, and removing a resource automatically removes its log lines, because there is no boilerplate anywhere that needs deleting.

The same argument extends to other cross-cutting concerns — authentication, rate limiting, request tracing, CORS headers, metrics collection. All of them belong in filters or interceptors, not inside resource methods. It also means that a reviewer reading `RoomResource` is reading only room logic, not ten lines of log setup, which makes the business rules easier to audit.

---

## Video Demonstration

A Postman walkthrough of less than 10 minutes has been recorded and submitted on **Blackboard**. The video covers: POST-ing a room and showing `201 Created` with a `Location` header, DELETE-ing a room in both the success case and the `409 Conflict` case for a room that still has sensors, POST-ing a sensor in both the valid case and the `422` case for a non-existent `roomId`, the `?type=` query-parameter filter on `GET /sensors`, navigating into `/sensors/{id}/readings` and showing the `currentValue` side-effect on the parent sensor, and finally triggering a `500` to demonstrate that no stack trace is returned in the response body.

---

## References

- Coursework specification: *5COSC022W Client-Server Architectures — "Smart Campus" Sensor & Room Management API*, University of Westminster, 2025/26.
- Jakarta RESTful Web Services (JAX-RS) specification.
- Eclipse Jersey user guide.
- RFC 4918 §11.2 — HTTP 422 Unprocessable Entity.
- OWASP guidance on avoiding sensitive information disclosure through error responses.
- Compliance note: no Spring Boot, no database technology, and no zip-file submission has been used. The project is hosted in a public GitHub repository as required by the brief.
