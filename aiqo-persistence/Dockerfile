# Use Eclipse Temurin JDK as base image
FROM eclipse-temurin:25-jdk-jammy

# Set working directory
WORKDIR /app

USER root

RUN apt-get update && apt-get install -y gosu

# Consentire al gruppo sudo di eseguire comandi senza password
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

# Copy the built jar from target directory
COPY target/aiqo-persistence-*.jar /app/app.jar

# Expose ports
EXPOSE 8080 8080

# Create a non-root user and switch to it
RUN adduser --system --group appuser

# Entrypoint script to start PostgreSQL and the application
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]