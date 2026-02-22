FROM eclipse-temurin:21-jre

# Install required tools
RUN apt-get update && \
    apt-get install -y unzip curl && \
    curl "https://awscli.amazonaws.com/awscli-exe-linux-$(uname -m).zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf aws awscliv2.zip /var/lib/apt/lists/*

WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring

COPY target/auth-service-*.jar app.jar
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh && \
    mkdir -p /app/logs && \
    chown -R spring:spring /app /entrypoint.sh

USER spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -sf http://localhost:8081/auth/actuator/health || exit 1

ENTRYPOINT ["/entrypoint.sh"]
