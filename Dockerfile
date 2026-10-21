# Multi-stage build: compile with the full JDK + Maven, run on a slim JRE image.

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom first and download dependencies, so Docker caches this layer
# and only re-downloads when pom.xml changes (not on every code edit).
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copy the source and build the jar (tests run in CI, not inside the image).
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user for safety.
RUN useradd --system --no-create-home spring
USER spring

# finalName in pom.xml makes the jar name stable: target/app.jar
COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080
# Database settings come from environment variables
# (SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD).
ENTRYPOINT ["java", "-jar", "app.jar"]
