FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM amazoncorretto:21-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ARG OTEL_AGENT_VERSION=v2.33.0
ADD https://github.com/aws-observability/aws-otel-java-instrumentation/releases/download/${OTEL_AGENT_VERSION}/aws-opentelemetry-agent.jar /opt/aws-opentelemetry-agent.jar
RUN chmod 644 /opt/aws-opentelemetry-agent.jar

USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/health/liveness || exit 1
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
