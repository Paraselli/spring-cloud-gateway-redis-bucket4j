# Spring Cloud Gateway + Redis + Bucket4j — Distributed Rate Limiting

Production-oriented demo architecture:

Client
  -> Spring Cloud Gateway
  -> Bucket4j distributed rate limiter
  -> Redis
  -> Customer Service

## What is improved

- Bucket4j state is stored in Redis through Bucket4j's JCache integration.
- Redis is the shared state store, so multiple Gateway instances share the same rate-limit bucket.
- Bucket4j's distributed proxy manager performs bucket updates atomically using Redis-side operations.
- Per-client-IP keys are used.
- HTTP 429 and Retry-After are returned when the limit is exceeded.
- Gateway and Customer Service are independently deployable.
- Redis is externalized from the Gateway process.

## Important production note

This demo is production-oriented, but production readiness also depends on infrastructure and operational controls:
Redis HA/Sentinel/Cluster, TLS, authentication, network policy, monitoring, alerting, capacity planning, trusted proxy configuration, and load testing.

For Kubernetes, do not trust arbitrary client-supplied X-Forwarded-For headers. Configure the ingress/load balancer and Gateway forwarded-header handling so the client identity is derived from a trusted proxy chain.

## Rate limit

Default:
- Capacity: 5 requests
- Refill: 5 tokens every 1 minute
- Key: client IP

Change these values in `RateLimitConfig`.

## Run

Start Redis:

```bash
docker compose up -d redis
```

Start customer service:

```bash
cd customer-service
mvn spring-boot:run
```

Start gateway:

```bash
cd api-gateway
mvn spring-boot:run
```

Test:

```bash
curl -i http://localhost:8080/api/customers/1
```

After five successful requests in the configured interval, the next request receives HTTP 429.

## Multi-instance test

Start two Gateway instances on different ports:

```bash
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar --server.port=8080
java -jar api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar --server.port=8082
```

Send requests alternately to ports 8080 and 8082. The rate-limit bucket is shared through Redis rather than maintained separately in each JVM.

## Build

```bash
mvn -pl customer-service clean package
mvn -pl api-gateway clean package
```
