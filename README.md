# Tchinda Event Radar - Proof of Concept

This is a backend-only scheduled intelligence engine built with Spring Boot, WebFlux, and Redis. It runs entirely async and is designed to find, rank, and alert on specific types of events worldwide via SerpAPI.

## Tech Stack
- **Java 21**
- **Maven**
- **Spring Boot 3.2.x**
- **Spring WebFlux** (for non-blocking `WebClient` API calls)
- **Spring Data Redis** (for idempotency and caching via Lettuce)
- **Spring Mail** (for sending HTML digests)
- **No GUI / REST API**. Fully scheduled batch processing.

## Pre-requisites
1. Java 21+ installed (`java -version`).
2. Maven 3.6+ installed (`mvn -version`).
3. Redis Server running locally on default port `6379`.
   ```bash
   # Using Docker:
   docker run -d --name cache -p 6379:6379 redis
   ```
4. A [SerpAPI](https://serpapi.com/) Key.

## Configuration
Edit `src/main/resources/application.yml` before running:

1. **SerpAPI Key:**
   Update `app.serpapi.api-key` with your valid SerpAPI key.
2. **Email Settings:**
   Update `spring.mail.username` and `spring.mail.password` with your SMTP/App password.
   Update `app.email.recipient` and `app.email.sender`.
3. **App Rules:**
   - Configure `queries` templates with `${event}` tags.
   - Configure `event-keywords`.
   - Configure `location-weights`.

## Running the Application
From the root directory of the project, run:

```bash
mvn clean install
mvn spring-boot:run
```

By default, the `@Scheduled` job will trigger automatically at 8:00 AM daily.
You can change the schedule using the `app.scheduler.cron` property in `application.yml`, for example `0 * * * * *` to run every minute for testing.

## How it Works
1. **Scheduler** activates the Intelligence Engine.
2. **Search Expansion:** Query templates are multiplied by keywords.
3. **Execution:** All queries are executed in parallel using `WebClient` and `CompletableFuture`.
4. **Idempotency Check:** Hits Redis to check if we've seen this URL/Title before using SHA-256 fingerprinting. Unknown events are cached for 120 days.
5. **Ranking:** Known keywords and locations are scored. Very old events are discarded.
6. **Delivery:** The top 20 best-rated events are transformed into a responsive HTML email and sent to the stakeholder.
