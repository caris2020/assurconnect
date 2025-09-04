# Multi-stage build for Spring Boot backend
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/pom.xml backend/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -f backend/pom.xml -q -DskipTests dependency:go-offline
COPY backend backend
RUN --mount=type=cache,target=/root/.m2 mvn -f backend/pom.xml -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /opt/app
COPY --from=build /app/backend/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
# Respect PORT provided by Fly.io and allow custom JAVA_OPTS
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /opt/app/app.jar"]


