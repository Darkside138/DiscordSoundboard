DiscordSoundbot
======================

Docker container for DiscordSoundboard
 

Usage:
------

Make a sound directory ex.: mkdir sounds

docker run -d -p 8080:8080 \\  
-e bottoken="Insert bot token here" \\  
-e username="Insert Discord username" \\  
-e chatcommands="true" \\  
-e dm="true" \\  
-e commandcharacter="?" \\  
-e sizelimit="2000" \\  
-e leavesuffix="_leave" \\  
-v "full path to sounds directory":/etc/DiscordSoundboard/bin/sounds \\  
--name DiscordSoundbot \\  
obenned/discordsoundbot
 

<http://dockerserverip:8080>

 

Environment Variables:
----------------------

 

### Required:

 

-   bottoken: Token for your bot, get it from here:
    <https://discordapp.com/developers/applications/me>

-   username: Your discord username

-   chatcommands: Enable chat commands - can be set to true/false

-   dm: Enable direct messages to the bot - can be set to true/false

-   commandcharacter: Set the prefix character for bot commands

-   sizelimit: Set the max characters limit for bot messages - **MAX is 2000**

-   leavesuffix: Set the leave suffix

 

### Optional:

 

-   allowedUserIds: List of users ids allowed to send bot commands, separated by
    comma, if left empty then all users can send commands

-   bannedUserIds: List of user ids banned from using bot commands
