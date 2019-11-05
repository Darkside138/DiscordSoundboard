# DiscordSoundboard

Simple soundboard for discord. You can trigger sounds by commands typed in discord chat or you can 
choose the sound from the web UI. You will need to create a bot account that the bot will use to join 
and play sounds. The bot can only play sounds/respond to commands for servers it has been given access to. 
Requires java 8 or higher. This bot uses the [DiscordJDA](https://github.com/DV8FromTheWorld/JDA) library.

##Beta Release
Updated to the new JDA 3.X library and also updated to a new music player. This should resolve a lot of the issues people were having with sound files (like mono not working). Aso, added the ablity to play youtube URLs.

[3.0.3.zip](https://github.com/Darkside138/DiscordSoundboard/releases/download/3.0.3/DiscordSoundboard-3.0.3.zip)

## Must have Java 8+ 64bit version installed in order for the soundboard to work properly

## Current Release
Latest Release (Use version above). 
Download the zip|tar file and extract it's contents in a directory. If you have an existing install do not overwrite 
the "application.properties" file. In that same directory you will need a directory called "sounds" (This directory is 
configurable in the application.properties). Put all the clips you 
want to play in the sounds directory. In the app.properties file you should fill in the login information for 
your bot (you should create a new discord BOT account for your soundboard). Once you've created your new bot you must invite 
it to any server you want to use it on. The property "username_to_join_channel" is your username on discord. 
When you click a sound file to play in the soundboard the app will look for this username and join the voice 
channel that user is in. If you don't have this configured properly the bot will not work. Also, the bot can 
respond to text channel commands. See below for information on those commands. Once this is complete execute 
the .jar file or the .bat file. You should see a bunch of logging and eventually something like 
"Started MainController in 6.383 seconds (JVM running for 6.939)". Now you should be able to access the UI by 
opening a browser and navigating to "http://localhost:8080".

## Installation and Setup Video
Smugaloof has made a great setup video you can watch [here](https://www.youtube.com/watch?v=DQSXP9AgYvw). 
This will walk you through the process of getting your bot up and running.

## Donations
If you'd like to buy me a beer for my efforts, it's always appreciated. You can do so [here](https://www.paypal.me/DFurrer)

## Planned Future Features
Creating a bot instance for each unique channel/server combination for busy servers. Currently there is one bot thread for each server.

## app.properties file
The contents of the app.properties file are below with sample values:
```
##Soundboard Specific configuration##

#Your bots token. If you don't have a token for your bot visit this link to get one. https://discordapp.com/developers/applications/me
#For more information on how to create an application and bot account visit this link https://discordapp.com/developers/docs/topics/oauth2
bot_token=SOME_TOKEN_YOU_GOT_FROM_DISCORD

#The username to look for and join their channel when a sound is played from the UI
username_to_join_channel=yourDiscordUsername

#The title to be used for the app
app_title=Sound Board

#If the bot should respond to chat commands (true|false)
respond_to_chat_commands=true
command_character=?
message_size_limit=2000

#Specify the directory where your sound files are located. If left empty it will look for a
#directory called "sounds/" in same directory the app was executed from.
#If you specify a directory that does not exist yet the application will attempt to create it.
#Example: C:/Users/someUser/Music
sounds_directory=

#List of users to respond to chat commands from. The list should be comma separated. If the list is empty the bot will
#repsond to all users.
allowedUsers=

#List of banned users. Also, comma separated. If a user is listed here they will no be able to issues commands to the
#bot through chat.
bannedUsers=SomeGuy,SomeotherGuy,ThirdGuy123

#Set the activity string for the bot. If left empty the message will default
activityString=
```

## Usage
Running locally from an IDE: Update the app.properties file with the login information for your "bot". Property 
"username_to_join_channel" is used by the application when a button in the UI is clicked. Before playing the 
sound file the bot will look for the username specified in all the servers the bot account has access to and 
join that channel, then play the sound that was clicked. For local usage sounds can be placed in src/main/resources/sounds/.

### Executing the app
Unzip the application. Update the application.properties with your bot token and any other preferences you would like. There are .bat and sh files in the /bin directory. Execute the proper one and the app should startup.

### Available commands
?help

?info

?list

?volume

?stop

?random

?remove fileName (must have admin to remove files)

Commands can be typed in any text channel that the bot has access to or you can send direct messages to the bot.
Responses will be sent to the requesting user via direct message also.

PM the bot a wav or mp3 file < 1MB and it will add it to the soundboard.

If the there is a file with the same name as a user added to the soundboard that sound will play every time a user
enters a channel.

### Playing sounds by text commands
Type ?list to get a list of sounds file commands the soundboard has available. The name of the commands will 
be the name of the sound file minus extension. When a user types one of the sound file commands listed the bot 
will join that users channel and then play the requested sound file. The bot remains in that channel unless the 
bot is requested to play sound in a different channel.
