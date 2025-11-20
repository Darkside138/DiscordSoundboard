# ---- Build Stage ----
FROM eclipse-temurin:17-jdk AS builder

RUN apt-get update && apt-get install -y unzip

WORKDIR /app

# Copy repo from GitHub actions checkout
COPY . .

# Ensure gradlew is executable
RUN chmod +x ./gradlew

# Build your bootDist
RUN ./gradlew assembleBootDist --no-daemon

WORKDIR build/distributions
RUN cp DiscordSoundboard*.zip /etc/DiscordSoundboard.zip

WORKDIR /etc
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip

# ---- Runtime Stage ----
FROM bellsoft/liberica-openjdk-alpine:17.0.2-9

RUN apt-get update

WORKDIR /etc/DiscordSoundboard

COPY --from=builder /etc/DiscordSoundboard .

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]