# Build stage — compiles the Spring Boot fat jar with Maven on JDK 17.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Runtime stage — slim JRE image, runs as a non-root user.
FROM eclipse-temurin:17-jre
RUN useradd --system --create-home appuser
USER appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Hosts like Render/Railway inject PORT; default to 8080 locally.
# Datasource, JWT keys and cookie flags are supplied via environment:
#   SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD
#   APP_JWT_PRIVATE_KEY_LOCATION / APP_JWT_PUBLIC_KEY_LOCATION (file:/... paths)
#   APP_COOKIE_SECURE=true
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
