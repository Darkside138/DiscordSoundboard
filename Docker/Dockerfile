FROM ubuntu:16.04
RUN apt update && apt upgrade -y
RUN apt install wget openjdk-8-jdk dos2unix unzip jq curl -y
WORKDIR "/etc"
RUN wget $(curl -sL https://api.github.com/repos/Darkside138/DiscordSoundboard/releases/latest | jq -r '.assets[].browser_download_url')
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip
EXPOSE 8080
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT [ "/docker-entrypoint.sh" ]
