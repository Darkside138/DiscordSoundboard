#This Dockerfile is for general use (Unraid dockerfiles are in the root of the project
FROM ubuntu:22.04

#Install required packages
RUN apt update && \
    apt upgrade -y && \
    apt install wget openjdk-17-jdk dos2unix unzip jq curl -y \

#Set working dir
WORKDIR "/etc"

#Download and extract DiscordSoundboard
RUN RELEASE_URL=https://api.github.com/repos/Darkside138/DiscordSoundboard/releases/tags/ && \
    VERSION=jda611 && \
    URL=$(curl -sL $RELEASE_URL$VERSION | jq -r '.assets[].browser_download_url') && \
    wget -q $URL -O DiscordSoundboard.zip && \
    unzip DiscordSoundboard.zip && \
    rm DiscordSoundboard*.zip

#Expose the webserver port
EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh

#Set the entry point
ENTRYPOINT [ "/docker-entrypoint.sh" ]
