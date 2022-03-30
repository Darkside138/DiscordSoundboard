FROM ubuntu:latest

RUN apt update && apt upgrade -y
RUN apt install wget openjdk-8-jdk dos2unix unzip jq curl git -y

WORKDIR "/tmp"
RUN git clone https://github.com/Darkside138/DiscordSoundboard.git

WORKDIR DiscordSoundboard
RUN gradle assembleBootDist

WORKDIR build/distributions
RUN mv DiscordSoundboard*.zip /etc/DiscordSoundboard.zip

WORKDIR /etc/DiscordSoundboard
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT [ "/docker-entrypoint.sh" ]