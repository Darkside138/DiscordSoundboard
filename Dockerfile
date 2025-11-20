#FROM gradle:9.2.1-jdk25-alpine as BaseBuilder
#
#LABEL org.opencontainers.image.source = https://github.com/Darkside138/DiscordSoundboard
#
#WORKDIR "/tmp"
#RUN git clone https://github.com/Darkside138/DiscordSoundboard.git#jda611
#
#WORKDIR DiscordSoundboard
#RUN gradle assembleBootDist
#
#WORKDIR build/distributions
#RUN cp DiscordSoundboard*.zip /etc/DiscordSoundboard.zip
#
#WORKDIR /etc
#RUN unzip DiscordSoundboard.zip
#RUN rm DiscordSoundboard.zip
#
#FROM bellsoft/liberica-openjdk-alpine:17.0.2-9
#
#WORKDIR /etc/DiscordSoundboard
#
#COPY --from=BaseBuilder /etc/DiscordSoundboard .
#
#EXPOSE 8080
#
#COPY docker-entrypoint.sh /
#RUN chmod +x /docker-entrypoint.sh
#
#ENTRYPOINT [ "/docker-entrypoint.sh" ]

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

RUN git clone https://github.com/Darkside138/DiscordSoundboard.git#jda611

WORKDIR DiscordSoundboard

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the project including dependencies
RUN ./gradlew assembleBootDist --no-daemon --offline

WORKDIR build/distributions
RUN cp DiscordSoundboard*.zip /etc/DiscordSoundboard.zip

WORKDIR /etc
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip

FROM eclipse-temurin:21-jdk AS base

WORKDIR /etc/DiscordSoundboard

COPY --from=builder /etc/DiscordSoundboard .

# Extract JAR files
RUN ls -la *.jar.gz && gunzip *.jar.gz

## Create a non-root user and switch to it for security
#RUN useradd -m appuser && chown -R appuser:appuser /app
#USER appuser

# Expose the application port
EXPOSE 8080

# Include an entrypoint script for startup
COPY docker-entrypoint.sh .

# Make the entrypoint executable
RUN chmod +x docker-entrypoint.sh

# Define the entrypoint to run the application
ENTRYPOINT ["./docker-entrypoint.sh"]