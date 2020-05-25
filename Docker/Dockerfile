FROM ubuntu:16.04
RUN apt update && apt upgrade -y
RUN apt install wget openjdk-8-jdk dos2unix unzip -y
WORKDIR "/etc"
RUN wget https://github.com/Darkside138/DiscordSoundboard/releases/latest/DiscordSoundboard.zip -O DiscordSoundboard.zip && unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip
EXPOSE 8080
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT [ "/docker-entrypoint.sh" ]
