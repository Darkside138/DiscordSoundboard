# ---- Build Stage ----
FROM gradle:7.4.2-jdk17-alpine AS builder

LABEL org.opencontainers.image.source = https://github.com/Darkside138/DiscordSoundboard

RUN apk update && \
    apk add nodejs npm

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

WORKDIR /etc/DiscordSoundboard

COPY --from=builder /etc/DiscordSoundboard .

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]