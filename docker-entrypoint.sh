#!/bin/sh
set -e

#Make config directory if it doesn't already exist
mkdir -p /etc/DiscordSoundboard/config
#If the file is still in /bin move it to ../config so that we can expose that as a persistent directory
# on something like unraid. Only moves the file if it's not already there.
[ -f /etc/DiscordSoundboard/bin/application.properties ] && mv -n /etc/DiscordSoundboard/bin/application.properties /etc/DiscordSoundboard/config/application.properties
[ -f /etc/DiscordSoundboard/bin/application.yml ] && mv -n /etc/DiscordSoundboard/bin/application.yml /etc/DiscordSoundboard/config/application.yml

cd /etc/DiscordSoundboard/config

#Overwrite the bottoken and username to join channel config entries from environment variables passed into docker run
sed -i 's/bot_token=SOME_TOKEN_YOU_GOT_FROM_DISCORD/bot_token='$bottoken'/g' application.properties
sed -i 's/username_to_join_channel=YourUserName/username_to_join_channel='$username'/g' application.properties
sed -i 's/command_character=?/command_character='$commandcharacter'/g' application.properties

cd /etc/DiscordSoundboard/bin

#Copy the jar file into the bin dir. This wouldn't be necessary if we setup the class path to look in the lib dir. Should probably update this later
cp /etc/DiscordSoundboard/lib/DiscordSoundboard* /etc/DiscordSoundboard/bin/DiscordSoundboard.jar

#Run the bot. Pass the /config/application.properties as part of the classpath, other wise the bot will not work
java -Dserver.port=8080 -jar /etc/DiscordSoundboard/bin/DiscordSoundboard.jar --spring.config.location=classpath:file:///etc/DiscordSoundboard/config/application.properties,file:///etc/DiscordSoundboard/config/application.yml

exec "$@"
