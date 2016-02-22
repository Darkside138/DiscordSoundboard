# DiscordSoundboard

Simple soundboard for discord. You will need to create an account that the bot will use to join and play sounds. Requires java 8 or higher.
This bot uses the DiscordJDA library.

# Usage
Running locally from an IDE: Update the app.properties file with the login information for your "bot". Property "username_to_join_channel"
is used by the application when a button in the UI is clicked. Before playing the sound file the bot will look for the username specified 
in all the servers the bot account has access to and join that channel, then play the sound that was clicked. For local usage sounds can
be placed in src/main/resources/sounds/.

Executing the jar file:
Once you've compiled the jar file you will need to place the .jar file, the app.properties file, and your sounds/ directory in a folder
then simple execute the .jar file.
