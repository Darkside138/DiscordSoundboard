#!/bin/bash
set -e
cd /etc/DiscordSoundboard/bin

sed -i 's/bot_token=SOME_TOKEN_YOU_GOT_FROM_DISCORD/bot_token='$bottoken'/g' app.properties
sed -i 's/username_to_join_channel=yourDiscordUsername/username_to_join_channel='$username'/g' app.properties
sed -i 's/respond_to_chat_commands=true/respond_to_chat_commands='$chatcommands'/g' app.properties
sed -i 's/respond_to_dm=true/respond_to_dm='$dm'/g' app.properties
sed -i 's/command_character=?/command_character='$commandcharacter'/g' app.properties
sed -i 's/message_size_limit=2000/message_size_limit='$sizelimit'/g' app.properties
sed -i 's/leave_suffix=_leave/leave_suffix='$leavesuffix'/g' app.properties
sed -i 's/player=JDAPlayer/player='$player'/g' app.properties
sed -i 's/allowedUserIds=/allowedUserIds='$allowedusers'/g' app.properties
sed -i 's/bannedUserIds=/bannedUserIds='$bannedusers'/g' app.properties
cp /etc/DiscordSoundboard/lib/DiscordSoundboard* /etc/DiscordSoundboard/bin/DiscordSoundboard.jar
exec java -Dserver.port=8080 -jar /etc/DiscordSoundboard/lib/DiscordSoundboard.jar

exec "$@"
