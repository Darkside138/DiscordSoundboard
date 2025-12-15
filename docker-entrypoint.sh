#!/bin/sh
set -e

#Make config directory if it doesn't already exist
mkdir -p /etc/DiscordSoundboard/config
#If the file is still in /bin move it to ../config so that we can expose that as a persistent directory
# on something like Unraid.
[ -f /etc/DiscordSoundboard/bin/application.properties ] && mv /etc/DiscordSoundboard/bin/application.properties /etc/DiscordSoundboard/config/application.properties
[ -f /etc/DiscordSoundboard/bin/application.yml ] && mv /etc/DiscordSoundboard/bin/application.yml /etc/DiscordSoundboard/config/application.yml

echo "changing to config directory to update application.properties file"
cd /etc/DiscordSoundboard/config

#Overwrite the bottoken and username to join channel config entries from environment variables passed into docker run
sed -i 's/^bot_token=.*/bot_token='bottoken'/' application.properties
sed -i 's/^username_to_join_channel=.*/username_to_join_channel='$username'/' application.properties
sed -i 's/^command_character=.*/command_character='$commandcharacter'/' application.properties
sed -i 's/^admin_user_list=.*/admin_user_list='$adminuserlist'/' application.properties
sed -i 's/^moderator_user_list=.*/moderator_user_list='$moderatoruserlist'/' application.properties
sed -i 's/^dj_user_list=.*/dj_user_list='$djuserlist'/' application.properties
sed -i 's/^spring.security.oauth2.client.registration.discord.client-id=.*/spring.security.oauth2.client.registration.discord.client-id='$discordclientid'/' application.properties
sed -i 's/^spring.security.oauth2.client.registration.discord.client-secret=.*/spring.security.oauth2.client.registration.discord.client-secret='$discordclientsecret'/' application.properties
sed -i 's/^jwt.secret=.*/jwt.secret='$jwtsecret'/' application.properties
sed -i 's~^app.frontend-url=.*~app.frontend-url='$frontendurl'~' application.properties

echo "changing working directory to /etc/DiscordSoundboard/bin"

cd /etc/DiscordSoundboard/bin

echo "copying jar file from lib to bin folder"
#Copy the jar file into the bin dir. This wouldn't be necessary if we setup the class path to look in the lib dir. Should probably update this later
cp /etc/DiscordSoundboard/lib/DiscordSoundboard* /etc/DiscordSoundboard/bin/DiscordSoundboard.jar

#Run the bot. Pass the /config/application.properties as part of the classpath, other wise the bot will not work
#java -Dserver.port=8080 -jar /etc/DiscordSoundboard/bin/DiscordSoundboard.jar --spring.config.location=classpath:file:///etc/DiscordSoundboard/config/application.properties,file:///etc/DiscordSoundboard/config/application.yml

# ==== CONFIG ====
APP_NAME="discordsoundboard"
JAR_PATH="/etc/DiscordSoundboard/bin/DiscordSoundboard.jar"
PROFILE="prod"

# ==== START ====
echo "Starting $APP_NAME with profile: $PROFILE..."
nohup java -jar -Dspring.profiles.active=$PROFILE "$JAR_PATH" --spring.config.location=classpath:file:///etc/DiscordSoundboard/config/application.properties,file:///etc/DiscordSoundboard/config/application.yml

PID=$!
echo "$APP_NAME started with PID $PID"

tail -f /dev/null
