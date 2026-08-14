# Emergency Equipment Lending Platform

A robust microservices-based platform designed to facilitate the lending and management of emergency equipment. Built with Spring Boot, Spring Cloud, and orchestrated with Docker.

## Architecture

This platform consists of the following microservices:
- **API Gateway:** Central entry point routing requests to backend services.
- **Service Registry (Eureka):** Dynamic service discovery for load balancing and resilience.
- **Config Server:** Centralized configuration management across all environments.
- **Loan Service:** Manages loan requests, approvals, and returns.
- **Inventory Service:** Tracks available emergency equipment and handles stock allocation.

## Features
- **Microservices Architecture:** Independently deployable and scalable services.
- **CI/CD Pipeline:** Fully automated GitHub Actions workflow for building, testing, checking code quality (Checkstyle/PMD/JaCoCo), and publishing Docker images.
- **Automated Testing:** Integration tests using H2 in-memory databases with minimum coverage enforcement.

## Local Setup (Docker Compose)

The easiest way to run the entire platform locally is using Docker Compose:

1. Create a `.env` file at the root (use `.env.example` as a template).
2. Run the following command:
   ```bash
   docker compose up -d
   ```
3. Access the API Gateway at `http://localhost:8080`.

## Local Development (Maven)

To run the services individually for development:

1. Ensure the Config Server and Eureka Server are running first.
2. Navigate to each service directory:
   ```bash
   cd services/loan-service
   mvn spring-boot:run
   ```

## CI/CD 

This project utilizes GitHub Actions for continuous integration. The pipeline automatically:
- Checks out the code
- Sets up Java 25 
- Runs Maven builds and integration tests
- Enforces Checkstyle and PMD static analysis rules
- Generates JaCoCo test coverage reports
- Builds and pushes Docker images to GitHub Container Registry (GHCR) upon merging to `main`.
