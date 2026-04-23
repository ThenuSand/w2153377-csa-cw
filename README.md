# Smart Campus — Sensor & Room Management API

**Module:** 5COSC022W Client-Server Architectures (2025/26)  
**Student:** A.K.M.M Thenu Sandul — w2153377  
**GitHub:** [Repository Link](https://github.com/ThenuSand/w2153377-Thenu-CSA)

A RESTful API for the University of Westminster "Smart Campus" initiative, built using **JAX-RS (Jersey 2)** running on an embedded **Grizzly** HTTP container. The service manages **Rooms**, the **Sensors** deployed within them, and a **historical log of Readings** those sensors emit. All data is kept **in memory** using `HashMap` / `ArrayList`-style structures inside a thread-safe singleton `DataStore`; no database technology and no Spring Boot is used, as required by the coursework brief.

Every error is returned as a structured JSON body using a single shape (no raw stack traces are ever leaked):

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
- Any Java IDE (optional — the project runs from the command line)

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

Start the embedded Grizzly server on port 8080 (default):

```
mvn exec:java
```

To run on a different port, pass the `port` system property:

```
mvn exec:java -Dport=9090
```

### 5. Stop the Server

Press `Ctrl+C` in the terminal where the server is running.

### 6. Access the API

Once the log line `Smart Campus API started at http://localhost:8080/api/v1` appears, open in your browser or Postman:

```
http://localhost:8080/api/v1
```

---

## API Design Overview

The API mirrors the physical campus structure: **rooms** are the top-level resource, **sensors** are deployed inside rooms, and each sensor accumulates **readings** over time. A Discovery endpoint at the API root provides metadata and navigational (HATEOAS) links so clients always know where to start.

### Resource Hierarchy

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1` | Discovery — metadata, version, HATEOAS links to every collection |
| GET, POST | `/api/v1/rooms` | List all rooms / create a new room |
| GET, DELETE | `/api/v1/rooms/{roomId}` | Get room detail / delete (blocked with 409 if sensors are linked) |
| GET, POST | `/api/v1/sensors` | List sensors (optional `?type=` filter) / register a new sensor (422 if `roomId` does not exist) |
| GET, POST | `/api/v1/sensors/{sensorId}/readings` | Reading history / append a new reading (403 if sensor is in MAINTENANCE) |

**Models:** `Room`, `Sensor`, `SensorReading`, `ErrorResponse`.

---

## Sample `curl` Commands

All of the commands below assume the server is running on `localhost:8080`. At least five successful interactions with different parts of the API are included, together with the three custom error scenarios from Part 5.

```bash
# 1. Discovery endpoint (HATEOAS links)
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

# 6. Push a new reading for a sensor (updates the parent sensor's currentValue)
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"value":22.4,"timestamp":0}' \
  "http://localhost:8080/api/v1/sensors/TEMP-777/readings"

# 7. Fetch the reading history for a sensor
curl -s "http://localhost:8080/api/v1/sensors/TEMP-777/readings" | jq

# --- Error paths required by Part 5 of the brief ---

# 8. DELETE a room that still has sensors → HTTP 409 Conflict
curl -i -X DELETE "http://localhost:8080/api/v1/rooms/LIB-301"

# 9. POST a sensor against a non-existent roomId → HTTP 422 Unprocessable Entity
curl -i -X POST -H "Content-Type: application/json" \
  -d '{"id":"OCC-999","type":"Occupancy","status":"ACTIVE","roomId":"GHOST-ROOM"}' \
  "http://localhost:8080/api/v1/sensors"

# 10. POST a reading to a sensor whose status is MAINTENANCE → HTTP 403 Forbidden
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
    │   └── DataStore.java                             # Singleton in-memory store (ConcurrentHashMap)
    ├── resource/
    │   ├── DiscoveryResource.java                     # GET  /api/v1
    │   ├── RoomResource.java                          # /api/v1/rooms
    │   ├── SensorResource.java                        # /api/v1/sensors (+ sub-resource locator)
    │   └── SensorReadingResource.java                 # nested /sensors/{id}/readings
    ├── exception/
    │   ├── RoomNotEmptyException.java                 # → 409 Conflict
    │   ├── LinkedResourceNotFoundException.java       # → 422 Unprocessable Entity
    │   └── SensorUnavailableException.java            # → 403 Forbidden
    ├── mapper/
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   ├── NotFoundExceptionMapper.java
    │   └── GenericExceptionMapper.java                # catch-all ExceptionMapper<Throwable> → 500
    └── filter/
        └── LoggingFilter.java                         # ContainerRequestFilter + ContainerResponseFilter
```

---

# Written Report (Answers to the Brief)

The report below follows the exact structure of the coursework specification. Each section states the task implemented and, where the spec includes a question, answers it in full. Section marks follow the rubric (Parts 1–4 = 10 each for their two sub-tasks, Part 5 = 30 split across five tasks).

## Part 1: Service Architecture & Setup (10 Marks)

### 1.1 Project & Application Configuration (5 Marks)

**Implementation.** The project is a Maven module (`smart-campus-api/pom.xml`) that integrates **Jersey 2** as the JAX-RS implementation and uses an **embedded Grizzly HTTP server** as the lightweight container. The application's entry point is `SmartCampusApplication` in the `config` package, which extends `javax.ws.rs.core.Application` and is annotated with `@ApplicationPath("/api/v1")` to establish the API's versioned entry point. `App.java` boots the Grizzly server on port 8080 (configurable via `-Dport=`) and registers the application class.

**Question:** *In your report, explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.*

By default, Jersey (the reference JAX-RS implementation) treats resource classes as **per-request**. Every time the server receives an HTTP request that targets a resource method, a brand-new instance of the enclosing resource class is created, field-injected, the method is invoked, and the instance is then eligible for garbage collection. This is very different from a typical singleton service model, where one instance is shared across every request.

There is a big upside to per-request lifecycle: instance fields are safe from concurrent access, because only one thread ever sees a given instance. There is also a big downside: the resource instance is not a good place to store state that should survive across requests. If I kept a `Map<String, Room>` as a field in `RoomResource`, it would be wiped the moment the request completed and the next request's fresh instance would see an empty map.

Because of this, in the Smart Campus API the resource classes are kept stateless. All persistent in-memory state lives in a single `DataStore` enum, which is effectively a thread-safe singleton. The maps inside it are `ConcurrentHashMap` so multiple request threads can read and mutate them at the same time without a race condition, and the per-sensor reading list is wrapped with `Collections.synchronizedList` so iteration-plus-append cannot interleave. Where a larger compound update needs to be atomic (for example "check-then-put") I use `computeIfAbsent` or an explicit lock, but for the operations in this coursework the `ConcurrentHashMap` guarantees are sufficient. If I had instead marked the resource with `@Singleton`, I would have had to hand-synchronise every shared field inside the resource itself, which is far easier to get wrong.

### 1.2 The "Discovery" Endpoint (5 Marks)

**Implementation.** `DiscoveryResource` handles `GET /api/v1` and returns a JSON object containing API metadata — version, administrative contact, and a resource map of URIs for every top-level collection (`/api/v1/rooms`, `/api/v1/sensors`). The URIs are built dynamically from `@Context UriInfo` so they always match the server's current host and port.

**Question:** *Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?*

HATEOAS (Hypermedia as the Engine of Application State) is the Richardson-Maturity-Level-3 part of REST: responses not only carry data but also carry links that tell the client what it can do next. My discovery endpoint is a simple example. It returns a JSON object whose `resources` field lists URIs for every top-level collection, so a client who has never seen the API can start from `/api/v1` and navigate the whole surface.

This matters for several reasons. First, URIs become an implementation detail of the server rather than a contract baked into every client; if we decide to move `/api/v1/rooms` to `/api/v1/buildings/rooms` we change the discovery document and the clients that follow links still work. Second, clients do not need to hard-code knowledge of which actions are legal in the current state of a resource: a room that is empty could include a `delete` link in its representation while a room that still has sensors would omit it, and the client does not need to duplicate that business rule. Third, onboarding is faster because a developer exploring the API in a browser can click their way around without flipping back to a PDF. Static docs drift away from the code over time; hypermedia links are generated from the code itself, so they cannot drift.

---

## Part 2: Room Management (20 Marks)

### 2.1 Room Resource Implementation (10 Marks)

**Implementation.** `RoomResource` is mounted on `/api/v1/rooms` and exposes `GET /` to list every room, `POST /` to create a new room (returns `201 Created` with a `Location` header pointing at the new resource), and `GET /{roomId}` to fetch a single room's details. Each room is serialised with its `id`, `name`, `capacity`, and the list of `sensorIds` currently deployed inside it.

**Question:** *When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.*

Returning only IDs in `GET /rooms` would make the response very small. On a 5 000-room campus the difference between a JSON array of 5 000 strings and a JSON array of 5 000 full room objects (each with its name, capacity, and sensor-id list) is significant, and over a mobile or rural connection that bandwidth cost is real.

However there is a catch. If the client only receives IDs it usually needs the details afterwards, so it loops and issues a follow-up `GET /rooms/{id}` for each one. That is the classic N+1 request problem: 5 000 HTTP round trips at perhaps 50 ms each is minutes of latency, and on the server side it is 5 000 times the per-request overhead (JAX-RS instance creation, filter chain, serialisation). The aggregate cost dwarfs the bandwidth saving.

The Smart Campus API therefore returns the full room objects on the collection endpoint. For truly large collections the right answer is neither "only IDs" nor "full objects"; it is pagination combined with a deliberately slim summary representation (only the fields needed for list views) and a separate detail endpoint for the heavy fields. That gives the client one round trip to render a page, small payloads, and a clear upgrade path if a field ever needs to hold a large blob.

### 2.2 Room Deletion & Safety Logic (10 Marks)

**Implementation.** `DELETE /api/v1/rooms/{roomId}` removes a room and returns `204 No Content`. Before removing, the resource consults `DataStore` for any sensors whose `roomId` matches; if any are found the delete is refused and a `RoomNotEmptyException` is thrown, which the mapper (see 5.1) converts into an HTTP `409 Conflict` with a structured JSON error body naming the offending sensors. This prevents sensor records becoming orphaned references to a non-existent room.

**Question:** *Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.*

Idempotency, in HTTP terms, means that sending the same request N times leaves the server in the same state as sending it once. My DELETE is idempotent in the state sense: the first call removes the room and returns 204 No Content, the second call sees no such room and returns 404 Not Found. The state of the server is identical after call 1 and call 2; in neither case does the room exist afterwards.

The HTTP status code differing between calls is not a violation of idempotency, because idempotency is a property of server state, not of response codes. Some APIs prefer to always return 204 on DELETE for missing resources to make the client logic simpler; I prefer the explicit 404 because it tells the caller "the thing you asked me to delete was not here", which is useful when debugging a script that might have a stale cached ID. In either case repeatedly pressing the delete button can never leave the system in a worse place than pressing it once.

There is one edge case that is not idempotent by design: if the room is not empty the request is rejected with 409 Conflict and the resource is left untouched. That is still safe because the server state does not change at all, but it is worth stating clearly: my DELETE is side-effect-free on failure and idempotent on success.

---

## Part 3: Sensor Operations & Linking (20 Marks)

### 3.1 Sensor Resource & Integrity (10 Marks)

**Implementation.** `SensorResource` is mounted on `/api/v1/sensors` and supports `GET /` (list), `POST /` (register a new sensor), and `GET /{sensorId}` (detail). The POST method is annotated with `@Consumes(MediaType.APPLICATION_JSON)` so only JSON bodies are accepted. Before the new sensor is stored, the resource asks `DataStore` whether the declared `roomId` exists; if not, a `LinkedResourceNotFoundException` is thrown and the mapper (see 5.2) converts it to `422 Unprocessable Entity`. On success the new sensor's id is also appended to the parent room's `sensorIds` list.

**Question:** *We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?*

`@Consumes(MediaType.APPLICATION_JSON)` is a declarative contract: this method will only accept request bodies whose `Content-Type` header advertises JSON. If a client sends `text/plain` or `application/xml`, the JAX-RS runtime matches the request against the resource methods and finds no method on that path that can consume the supplied media type. It responds with **HTTP 415 Unsupported Media Type** before my code is called at all.

This is a really useful property. The resource method never has to check "is this JSON?" nor catch a malformed-XML parse exception; the framework filters garbage out at the edge. If I wanted to accept XML as well, I would simply add `application/xml` to the `@Consumes` list and wire up a message body reader. If I wanted to accept anything and negotiate, I would use `*/*`, but in practice a clear, narrow contract makes integration problems obvious early.

### 3.2 Filtered Retrieval & Search (10 Marks)

**Implementation.** `GET /api/v1/sensors` accepts an optional `@QueryParam("type")` argument. When the parameter is absent (null) the endpoint returns the full collection; when present it returns only the sensors whose `type` matches (case-insensitive), for example `GET /api/v1/sensors?type=CO2`.

**Question:** *You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?*

Using `@QueryParam("type")` keeps the URI hierarchy clean. `/api/v1/sensors` is the collection of sensors; `/api/v1/sensors?type=CO2` is still conceptually the same collection, just filtered. The URL says "here is the sensors collection, here is a filter clause on top of it".

If I had instead used the path style `/api/v1/sensors/type/CO2`, I would imply that `type/CO2` is a sub-resource of the sensors collection, which is not what it is. It is not a resource at all, it is a query. Path parameters also do not compose. How would I express "CO2 sensors in the ACTIVE state"? The query style just adds another parameter: `?type=CO2&status=ACTIVE`. The path style forces invented syntaxes like `/sensors/type/CO2/status/ACTIVE` and that falls apart the first time a filter value is optional.

Query parameters are also the standard caching model; proxies and browsers key cached GETs off the full URL, including the query string, so `?type=CO2` and `?type=Temperature` are different cache keys with no extra effort. Finally, query parameters are trivially optional, and JAX-RS treats an absent `@QueryParam` as `null`, which is exactly the "no filter" case.

---

## Part 4: Deep Nesting with Sub-Resources (20 Marks)

### 4.1 The Sub-Resource Locator Pattern (10 Marks)

**Implementation.** `SensorResource` contains a sub-resource locator method annotated with `@Path("{sensorId}/readings")` but **no HTTP verb**. The locator looks up the parent sensor in `DataStore`, verifies it exists, and returns a freshly constructed `SensorReadingResource` bound to that specific parent sensor. JAX-RS then re-dispatches the incoming request against the returned object, invoking the appropriate reading-level method (see 4.2).

**Question:** *Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?*

A sub-resource locator is a method on a resource that is annotated with `@Path` but not with any HTTP verb. Instead of handling the request itself, it returns another object which JAX-RS then treats as a resource and dispatches into. In this project, `SensorResource.readings(sensorId)` verifies the parent sensor exists and then returns a `SensorReadingResource` bound to that specific parent.

Architecturally this matters because it keeps concerns separate. Without the pattern, every reading endpoint would live inside `SensorResource`, which would end up responsible for both sensor lifecycle and reading history; that class would grow without bound as more nested paths appeared. With the pattern, each class has a single job and can be tested in isolation. The parent validation happens exactly once, at the locator, so the child class does not have to re-check it on every verb.

It also gives the child resource a natural place to hold per-request context. `SensorReadingResource` takes the parent `Sensor` in its constructor, so every method already has the parent at hand without re-fetching it from the data store or passing it as an argument. That pattern is what stops a large nested API becoming a single thousand-line "god controller".

### 4.2 Historical Data Management (10 Marks)

**Implementation.** `SensorReadingResource` is instantiated per request by the locator in 4.1 and is bound to a single parent sensor. It exposes:

- `GET /api/v1/sensors/{sensorId}/readings` — returns the historical list of readings for that sensor, in append order.
- `POST /api/v1/sensors/{sensorId}/readings` — validates the payload, assigns a UUID for the reading id, defaults the `timestamp` to the current epoch millis if the client sent `0`, appends the reading to the sensor's history, and — as required by the spec — **triggers a side-effect update to the parent sensor's `currentValue`** so that `GET /api/v1/sensors/{sensorId}` immediately reflects the latest measurement. If the sensor's status is `MAINTENANCE`, the POST is rejected with a `SensorUnavailableException` that maps to `403 Forbidden` (see 5.3), so stale hardware cannot inject readings.

This ensures the parent sensor's `currentValue` is always consistent with its own reading history.

---

## Part 5: Advanced Error Handling, Exception Mapping & Logging (30 Marks)

No raw Java stack trace is ever leaked to the client. Every non-success response is a structured `ErrorResponse` JSON body of the shape shown at the top of this README, containing `status`, a machine-readable `error` code, a human `message`, and a `timestamp`.

### 5.1 Resource Conflict (409) (5 Marks)

**Implementation.** A custom `RoomNotEmptyException` is thrown by `RoomResource.delete(...)` whenever a caller tries to delete a room whose `sensorIds` list is not empty. The exception carries the offending room id and the count of still-attached sensors. `RoomNotEmptyExceptionMapper` implements `ExceptionMapper<RoomNotEmptyException>` and returns `HTTP 409 Conflict` with an `ErrorResponse` body explaining that the room is still occupied by active hardware, meeting the rubric requirement for a structured JSON error body on 409.

### 5.2 Dependency Validation (422 Unprocessable Entity) (10 Marks)

**Implementation.** A custom `LinkedResourceNotFoundException` is thrown whenever a POST body references a resource that does not exist — specifically, when a sensor is registered against a `roomId` that is not present in `DataStore`. `LinkedResourceNotFoundExceptionMapper` maps this to `HTTP 422 Unprocessable Entity` with an `ErrorResponse` body that names the missing reference type (`"room"`) and its id.

**Question:** *Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?*

404 Not Found means "the URL you requested does not map to any resource on this server". When a client POSTs `{"roomId":"GHOST-ROOM"}` to `/api/v1/sensors`, the URL `/api/v1/sensors` is absolutely valid; the collection exists and the server is willing to accept a POST to it. What is wrong is the content of the body: it references a room that is not known to the system.

404 would mislead any caller writing generic retry logic, because a 404 on a POST normally indicates a mistyped URL that needs fixing before retrying. 422 Unprocessable Entity (from RFC 4918) says "the request was syntactically well-formed and I understood it, but the semantic contents are incorrect", which is precisely the situation here. The client should not retry; the client should fix the referenced id. Returning 422 lets the client distinguish "wrong address" from "wrong payload" without having to parse the error body.

Some APIs prefer 400 Bad Request for the same scenario. 400 is a safe fallback if the toolchain in use does not support 422, but 400 is broader and less informative. My implementation uses 422 and includes the referenced type and id in the error body so the client can react precisely.

### 5.3 State Constraint (403 Forbidden) (5 Marks)

**Implementation.** A custom `SensorUnavailableException` is thrown by `SensorReadingResource.post(...)` when a POST reading is attempted against a sensor whose `status` field is `"MAINTENANCE"` (or otherwise non-`ACTIVE`). The rule models the real-world scenario that a sensor disconnected for maintenance physically cannot accept new readings, so the server refuses to record them. `SensorUnavailableExceptionMapper` converts the exception to `HTTP 403 Forbidden` with an `ErrorResponse` body naming the sensor id and its current status.

### 5.4 The Global Safety Net (500) (5 Marks)

**Implementation.** `GenericExceptionMapper` implements `ExceptionMapper<Throwable>` and acts as the catch-all. Any exception that is not handled by a more specific mapper — `NullPointerException`, `IndexOutOfBoundsException`, a bug in a mapper, a JSON deserialisation failure, etc. — is intercepted here. The full trace is logged on the server via `java.util.logging.Logger` at `SEVERE` so operators can debug it, but the response to the client is a clean `HTTP 500 Internal Server Error` with a generic `ErrorResponse` body that only says an unexpected error occurred.

**Question:** *From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?*

If my API responded to every server error with a raw Java stack trace, it would give a potential attacker a free reconnaissance document. A stack trace typically exposes:

- The **package and class structure** of my application, which reveals framework versions and which libraries are in use (Jersey, Jackson, specific versions often including minor patch levels). An attacker can then look up known CVEs against those exact versions.
- The **full file path** on the server of the compiled class, which sometimes reveals the hosting account name, the operating system, or the directory layout of the deployment.
- **Internal method names and parameter names** that hint at business logic; a trace containing `AuthService.validateToken` tells the attacker that such a service exists.
- **Database or third-party error messages** that may contain table names, column names, SQL fragments, or external hostnames if the error originated from a connector.
- **Thread names and server metadata** that can disclose whether the app is running behind a specific proxy, container or cloud provider.

None of that information is needed by a legitimate API consumer. My `GenericExceptionMapper<Throwable>` therefore logs the trace internally via `java.util.logging.Logger` (where operators can see it) but returns only a generic message and a 500 status to the caller. The balance is: make problems debuggable for the team, make them opaque to the outside world.

### 5.5 API Request & Response Logging Filters (5 Marks)

**Implementation.** `LoggingFilter` is annotated as `@Provider` and implements both `ContainerRequestFilter` and `ContainerResponseFilter`, so it fires once on the way in and once on the way out of every request. Using a `java.util.logging.Logger`, the request filter logs the HTTP method and request URI from the `ContainerRequestContext`, and the response filter logs the final HTTP status code from the `ContainerResponseContext`. The filter adds no business logic — it only observes — and is discovered automatically by Jersey through the `@Provider` annotation, with no per-endpoint wiring required.

**Question:** *Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting `Logger.info()` statements inside every single resource method?*

Logging is a cross-cutting concern: every endpoint needs it, but none of the endpoints should care about it. If I inserted `Logger.info(...)` into every resource method, I would be duplicating the same two or three lines throughout the codebase, and every time I added a new endpoint I would have to remember to copy the pattern. Worse, I would miss logging on the failure paths, because a method that throws never reaches its bottom-of-method log line.

A `ContainerRequestFilter` + `ContainerResponseFilter` pair sits outside the resource method. It fires once per request regardless of which endpoint was hit, regardless of whether that endpoint returned normally or threw. Adding a new resource automatically inherits the logging for free; removing a resource automatically removes the log lines too, because there is no boilerplate to delete.

The same argument extends to other cross-cutting concerns: authentication, rate limiting, request tracing, adding CORS headers, capturing metrics. All of them belong in filters or interceptors. It also means a reviewer reading my `RoomResource` is reading only room logic, not ten lines of log setup, which makes the business rules easier to audit.

---

## Video Demonstration

A short Postman walkthrough (under 10 minutes) has been recorded and submitted via **Blackboard**, covering: POST a room with `201 Created` + `Location` header, DELETE a room showing both the success path and the `409 Conflict` case for a room with sensors, POST a sensor showing both a valid case and the `422` failure for a non-existent `roomId`, the `?type=` query-parameter filter, navigation into `/sensors/{id}/readings` with the `currentValue` side-effect visible on the parent, and a `500` trigger demonstrating that no stack trace is returned in the response.

---

## References

- Coursework specification: *5COSC022W Client-Server Architectures — "Smart Campus" Sensor & Room Management API*, University of Westminster, 2025/26.
- Jakarta RESTful Web Services (JAX-RS) specification.
- Eclipse Jersey user guide.
- RFC 4918 §11.2 — HTTP 422 Unprocessable Entity.
- Compliance note: no Spring Boot, no database technology, and no zipped submission is used; the project is hosted in a public GitHub repository as required by the brief.
