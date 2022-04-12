#!/bin/sh
set -e

mkdir -p /etc/DiscordSoundboard/config
[ -f /etc/DiscordSoundboard/bin/application.properties ] && mv /etc/DiscordSoundboard/bin/application.properties /etc/DiscordSoundboard/config/application.properties

cd /etc/DiscordSoundboard/config

sed -i 's/bot_token=SOME_TOKEN_YOU_GOT_FROM_DISCORD/bot_token='$bottoken'/g' application.properties
sed -i 's/username_to_join_channel=YourUserName/username_to_join_channel='$username'/g' application.properties

cd /etc/DiscordSoundboard/bin

cp /etc/DiscordSoundboard/lib/DiscordSoundboard* /etc/DiscordSoundboard/bin/DiscordSoundboard.jar
exec java -Dserver.port=8080 -jar /etc/DiscordSoundboard/bin/DiscordSoundboard.jar --spring.config.location=classpath:file:///etc/DiscordSoundboard/config/application.properties

exec "$@"
