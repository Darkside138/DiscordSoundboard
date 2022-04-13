FROM gradle:7.4.2-jdk17-alpine as BaseBuilder

LABEL org.opencontainers.image.source = https://github.com/Darkside138/DiscordSoundboard

WORKDIR "/tmp"
RUN git clone https://github.com/Darkside138/DiscordSoundboard.git

WORKDIR DiscordSoundboard
RUN gradle assembleBootDist

WORKDIR build/distributions
RUN cp DiscordSoundboard*.zip /etc/DiscordSoundboard.zip

WORKDIR /etc
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip

FROM bellsoft/liberica-openjdk-alpine:17.0.2-9

WORKDIR /etc/DiscordSoundboard

COPY --from=BaseBuilder /etc/DiscordSoundboard .

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT [ "/docker-entrypoint.sh" ]