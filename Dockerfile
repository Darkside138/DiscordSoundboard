FROM ubuntu:latest

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Chicago

RUN apt update && apt upgrade -y
RUN apt install wget openjdk-8-jdk dos2unix unzip jq curl git -y

#install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-6.9.2-bin.zip
    && unzip gradle-6.9.2-bin.zip -d /opt \
    && rm gradle-6.9.2-bin.zip

# Set Gradle in the environment variables
ENV GRADLE_HOME /opt/gradle-6.9.2
ENV PATH $PATH:/opt/gradle-6.9.2/bin

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