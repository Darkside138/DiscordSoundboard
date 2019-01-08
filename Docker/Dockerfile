FROM ubuntu:16.04
RUN apt update && apt upgrade -y
RUN apt install wget tar openjdk-8-jdk dos2unix -y
RUN mkdir -p /etc/Discord-Soundbot
WORKDIR "/etc/Discord-Soundbot"
RUN wget https://github.com/Darkside138/DiscordSoundboard/releases/download/v2.2.2-BETA/DiscordSoundboard-2.2.2-beta.tar && tar xvf DiscordSoundboard-2.2.2-beta.tar
RUN rm soundboard.bat DiscordSoundboard-2.2.2-beta.tar
RUN mkdir sounds
RUN chmod +x soundboard.sh
RUN dos2unix soundboard.sh
EXPOSE 8080
COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT [ "/docker-entrypoint.sh" ]
#CMD [ "java -Dserver.port=8080 -jar /etc/Discord-Soundbot/DiscordSoundboard.jar" ]
