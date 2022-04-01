FROM ubuntu:20.04

LABEL org.opencontainers.image.source = https://github.com/Darkside138/DiscordSoundboard

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=America/Chicago

RUN apt update && apt upgrade -y
RUN apt install wget dos2unix unzip jq curl git -y

#Install Java 17
RUN wget https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz
RUN tar xvf openjdk-17.0.2_linux-x64_bin.tar.gz
RUN mv jdk-17.0.2/ /opt/jdk-17/
ENV JAVA_HOME /opt/jdk-17
ENV PATH $PATH:$JAVA_HOME/bin
RUN echo $JAVA_HOME
RUN java --version

#install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-7.4.2-bin.zip
RUN unzip gradle-7.4.2-bin.zip -d /opt
RUN rm gradle-7.4.2-bin.zip

# Set Gradle in the environment variables
ENV GRADLE_HOME /opt/gradle-7.4.2
ENV PATH $PATH:/opt/gradle-7.4.2/bin

WORKDIR "/tmp"
RUN git clone https://github.com/Darkside138/DiscordSoundboard.git

WORKDIR DiscordSoundboard
RUN gradle bootDistZip assembleBootDist

WORKDIR build/distributions
RUN cp DiscordSoundboard*.zip /etc/DiscordSoundboard.zip

WORKDIR /etc
RUN unzip DiscordSoundboard.zip
RUN rm DiscordSoundboard.zip

WORKDIR /etc/DiscordSoundboard

EXPOSE 8080

COPY docker-entrypoint.sh /
RUN chmod +x /docker-entrypoint.sh
ENTRYPOINT [ "/docker-entrypoint.sh" ]
