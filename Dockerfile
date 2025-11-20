# ---- Build Stage ----
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy repo from GitHub actions checkout
COPY . .

# Ensure gradlew is executable
RUN chmod +x ./gradlew

# Build your bootDist
RUN ./gradlew assembleBootDist --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jdk

WORKDIR /opt/DiscordSoundboard

# Copy distribution built in the builder stage
COPY --from=builder /app/build/distributions/DiscordSoundboard* ./

RUN apt-get update && apt-get install -y unzip

# Extract the zip
RUN unzip DiscordSoundboard*.zip && \
    rm DiscordSoundboard*.zip

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]