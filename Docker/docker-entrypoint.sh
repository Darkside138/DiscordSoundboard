#!/bin/sh
set -e
cd /etc/DiscordSoundboard/bin

# Only apply a change if the corresponding environment variable is set
[ -n "$bottoken" ] && sed -i "s/^bot_token=.*/bot_token=$bottoken/" application.properties
[ -n "$username" ] && sed -i "s/^username_to_join_channel=.*/username_to_join_channel=$username/" application.properties
[ -n "$chatcommands" ] && sed -i "s/respond_to_chat_commands=true/respond_to_chat_commands=$chatcommands/g" application.properties
[ -n "$dm" ] && sed -i "s/respond_to_dm=true/respond_to_dm=$dm/g" application.properties
[ -n "$commandcharacter" ] && sed -i "s/^command_character=.*/command_character=$commandcharacter/" application.properties
[ -n "$leavesuffix" ] && sed -i "s/leave_suffix=_leave/leave_suffix=$leavesuffix/g" application.properties
[ -n "$allowedusers" ] && sed -i "s/allowedUserIds=/allowedUserIds=$allowedusers/g" application.properties
[ -n "$bannedusers" ] && sed -i "s/bannedUserIds=/bannedUserIds=$bannedusers/g" application.properties
[ -n "$adminuserlist" ] && sed -i "s/^admin_user_list=.*/admin_user_list=$adminuserlist/" application.properties
[ -n "$moderatoruserlist" ] && sed -i "s/^moderator_user_list=.*/moderator_user_list=$moderatoruserlist/" application.properties
[ -n "$djuserlist" ] && sed -i "s/^dj_user_list=.*/dj_user_list=$djuserlist/" application.properties
[ -n "$discordclientid" ] && sed -i "s/^spring.security.oauth2.client.registration.discord.client-id=.*/spring.security.oauth2.client.registration.discord.client-id=$discordclientid/" application.properties
[ -n "$discordclientsecret" ] && sed -i "s/^spring.security.oauth2.client.registration.discord.client-secret=.*/spring.security.oauth2.client.registration.discord.client-secret=$discordclientsecret/" application.properties
[ -n "$jwtsecret" ] && sed -i "s/^jwt.secret=.*/jwt.secret=$jwtsecret/" application.properties
[ -n "$leaveOnEmptyChannel" ] && sed -i "s/^leaveOnEmptyChannel=.*/leaveOnEmptyChannel=$leaveOnEmptyChannel/" application.properties
[ -n "$frontendurl" ] && sed -i "s~^app.frontend-url=.*~app.frontend-url=$frontendurl~" application.properties

exec java -Dserver.port=8080 -jar /etc/DiscordSoundboard/bin/DiscordSoundboard.jar

exec "$@"
