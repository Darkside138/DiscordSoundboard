# ---- Build Stage ----
FROM gradle:8.12-jdk25-alpine AS builder

LABEL org.opencontainers.image.source = https://github.com/Darkside138/DiscordSoundboard

USER root
RUN apt-get update && apt-get install -y nodejs npm

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
FROM eclipse-temurin:25-jdk-alpine

WORKDIR /etc/DiscordSoundboard

COPY --from=builder /etc/DiscordSoundboard .

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]