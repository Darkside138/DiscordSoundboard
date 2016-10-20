#!/bin/sh
USER="userToRunSounboardAs"
SERVER_PORT=8080
SERVICE_NAME=Soundboard
PATH_TO_JAR=/home/yourUsername/discordSoundboard
PID_PATH_NAME=/tmp/soundboard-pid
case $1 in
    start)
        echo "Starting $SERVICE_NAME ..."
        if [ ! -f $PID_PATH_NAME ]; then
            cd $PATH_TO_JAR
            sudo -u $USER nohup java -Dserver.port=$SERVER_PORT -jar $PATH_TO_JAR/DiscordSoundboard.jar /tmp 2>> /dev/null >> /dev/null &
                echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is already running ..."
        fi
    ;;
    stop)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stoping ..."
            kill $PID;
            echo "$SERVICE_NAME stopped ..."
            rm $PID_PATH_NAME
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
    restart)
        if [ -f $PID_PATH_NAME ]; then
            PID=$(cat $PID_PATH_NAME);
            echo "$SERVICE_NAME stopping ...";
            kill $PID;
            echo "$SERVICE_NAME stopped ...";
            rm $PID_PATH_NAME
            echo "$SERVICE_NAME starting ..."
            cd $PATH_TO_JAR
            sudo -u $USER nohup java -Dserver.port=$SERVER_PORT -jar $PATH_TO_JAR/DiscordSoundboard.jar /tmp 2>> /dev/null >> /dev/null &
                echo $! > $PID_PATH_NAME
            echo "$SERVICE_NAME started ..."
        else
            echo "$SERVICE_NAME is not running ..."
        fi
    ;;
esac
