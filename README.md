# DiscordSoundboard

Simple soundboard for discord. You can trigger sounds by commands typed in discord chat or you can choose the sound from the local UI. You will need to create an account that the bot will use to join and play sounds. The bot can only play sounds/respond to commands for servers it has been given access to. Requires java 8 or higher. This bot uses the [DiscordJDA](https://github.com/DV8FromTheWorld/JDA) library.

##Current Release
[discordSoundboard-v1.0.0.jar](https://github.com/Darkside138/DiscordSoundboard/releases/download/1.0.0/discordSoundboard-v1.0.0.jar). Download the jar file and place it in a directory. In that same directory you will need a file called "app.properties" and a directory called "sounds". Put all the clips you want to play in the sounds directory. In the app.properties file you should fill in the login information for your bot (you should create a new discord account for your bot). Once you've created your new bot you must invite it to any server you want to use it on. The property "username_to_join_channel" is your username on discord. When you click a sound file to play in the soundboard the app will look for this username and join the voice channel that user is in. If you don't have this configured properly the bot will not work. Also, the bot can respond to text channel commands. See below for information on those commands.

##app.properties file
The contents of the app.properties file are below with sample values:
```
#Your bots username and password
username=yourbotsemail@email.com
password=password
#The username to look for and join their channel when a sound is played from the UI
username_to_join_channel=yourDiscordUsername
#The title to be used for the app
app_title=Sound Board
#The window width/height in pixels
app_width=400
app_height=400
```

## Usage
Running locally from an IDE: Update the app.properties file with the login information for your "bot". Property "username_to_join_channel" is used by the application when a button in the UI is clicked. Before playing the sound file the bot will look for the username specifiedin all the servers the bot account has access to and join that channel, then play the sound that was clicked. For local usage sounds can be placed in src/main/resources/sounds/.

###Executing the jar file
Once you've compiled the jar file you will need to place the .jar file, the app.properties file, and your sounds/ directory in a folder
then simple execute the .jar file.

###Playing sounds by text commands
Type !list to get a list of sounds file commands the soundboard has available. The name of the commands will be the name of the sound file minus extension. When a user types one of the sound file commands listed the bot will join that users channel and then play the requested sound file. The bot remains in that channel unless the bot is requested to play sound in a different channel.
