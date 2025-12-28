#!/bin/bash
set -e

# Wait for PostgreSQL to be ready
wait_for_postgres() {
    echo "Waiting for PostgreSQL to be ready..."
    until PGPASSWORD=$SPRING_DATASOURCE_PASSWORD psql -h "postgres" -U "$SPRING_DATASOURCE_USERNAME" -d "postgres" -c '\q'; do
        >&2 echo "PostgreSQL is unavailable - sleeping"
        sleep 1
    done
    echo "PostgreSQL is ready!"
}

# Function for proper shutdown
shutdown() {
    echo "Stopping application..."
    kill -TERM "$APP_PID" 2>/dev/null
    wait "$APP_PID" 2>/dev/null
    echo "Shutdown complete."
    exit 0
}

trap shutdown SIGTERM SIGINT

# Start the application
echo "Starting Java application..."
gosu appuser java -jar /app/app.jar --server.port=8080 &
APP_PID=$!

# Wait for the application to start
wait_for_app() {
    echo "Waiting for application to be ready..."
    until curl -f http://localhost:8080/actuator/health; do
        >&2 echo "Application is unavailable - sleeping"
        sleep 1
    done
    echo "Application is ready!"
}

wait_for_app

# Keep the container running
wait "$APP_PID"


