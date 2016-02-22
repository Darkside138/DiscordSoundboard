# DiscordSoundboard

Simple soundboard for discord. You can trigger sounds by commands type in discord chat or you can choose the sound from the local UI. You will need to create an account that the bot will use to join and play sounds. The bot can only play sounds/respond to commands for servers it has been given access to. Requires java 8 or higher. This bot uses the [DiscordJDA](https://github.com/DV8FromTheWorld/JDA) library.

## Usage
Running locally from an IDE: Update the app.properties file with the login information for your "bot". Property "username_to_join_channel" is used by the application when a button in the UI is clicked. Before playing the sound file the bot will look for the username specifiedin all the servers the bot account has access to and join that channel, then play the sound that was clicked. For local usage sounds can be placed in src/main/resources/sounds/.

###Executing the jar file
Once you've compiled the jar file you will need to place the .jar file, the app.properties file, and your sounds/ directory in a folder
then simple execute the .jar file.

###Playing sounds by text commands
Type !list to get a list of sounds file commands the soundboard has available. The name of the commands will be the name of the sound file minus extension. When a user types one of the sound file commands listed the bot will join that users channel and then play the requested sound file. The bot remains in that channel unless the bot is requested to play sound in a different channel.
