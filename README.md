# DiscordSoundboard
[![Downloads](https://img.shields.io/github/downloads/Darkside138/DiscordSoundboard/total.svg)](https://github.com/Darkside138/DiscordSoundboard/releases/latest)
[![Stars](https://img.shields.io/github/stars/Darkside138/DiscordSoundboard.svg)](https://github.com/Darkside138/DiscordSoundboard/stargazers)
[![Release](https://img.shields.io/github/release/Darkside138/DiscordSoundboard.svg)](https://github.com/Darkside138/DiscordSoundboard/releases/latest)
[![License](https://img.shields.io/github/license/Darkside138/DiscordSoundboard.svg)](https://github.com/Darkside138/DiscordSoundboard/blob/master/LICENSE)
![Java CI with Gradle](https://github.com/Darkside138/DiscordSoundboard/workflows/Java%20CI%20with%20Gradle/badge.svg)
[![CodeFactor](https://www.codefactor.io/repository/github/darkside138/discordsoundboard/badge/master)](https://www.codefactor.io/repository/github/darkside138/discordsoundboard/overview/master)

Simple soundboard for discord. You can trigger sounds by commands typed in discord chat, or you can 
choose the sound from the web UI. You will need to create a bot account that the bot will use to join 
and play sounds. The bot can only play sounds/respond to commands for servers it has been given access to. 
Requires java 8 or higher. This bot uses the [DiscordJDA](https://github.com/DV8FromTheWorld/JDA) library.

## 4.* Releases
**If you're updating from any version prior to 4.0 you will need to delete your old DB file.** The release of 4.* updates
the JDA library to 6.1.1 and some updates to the legacy UI. In 4.1.* and above, I added a new UI available at {BASE_URL}/indexV2.html.
The new UI allows OAuth through Discord. The UI should work fine without configuring OAuth but will give extra functionality
if you do configure it for users you grant roles to (Admin, Moderator, or DJ). To configure, you'll need to go to the [Discord
developer portal](https://discord.com/developers/applications/), click your application, select OAuth2. From this screen
you'll need your Client ID and Client Secret, add a Redirect (https://your_soundboard_url/login/oauth2/code/discord). Please
use https if you don't, it will not be secure. Take the Client Id and Client Secret and find these entries in your
application.properties file and replace the values with your own:

spring.security.oauth2.client.registration.discord.client-id=replace_with_your_discord_client_id
spring.security.oauth2.client.registration.discord.client-secret=replace_with_your_discord_secret

Then you just need to generate a JWT secret. Go to this URL https://jwtgenerator.com/tools/jwt-generator and click
"Generate New Secret". Take that value and replace the placeholder, again in application.properties, with the secret you just generated:

jwt.secret=your-secret-key-here-change-this-in-production

Then find this entry in application.properties:

adminUserList=somediscordid

Replace the value with a comma-separated list of discord user ids that you want to have an admin role. 

For reference, here is a list of roles and the permissions given to those roles:

      admin:
        - upload
        - delete-sounds
        - edit-sounds
        - manage-users
        - play-sounds
        - download-sounds
      dj:
        - upload
        - edit-sounds
        - play-sounds
        - download-sounds
      moderator:
        - delete-sounds
        - edit-sounds
        - play-sounds
        - download-sounds
      user:
        - play-sounds
        - download-sounds

      # Default permissions for unauthenticated users
      default-permissions:
        - play-sounds
        - download-sounds

## 3.0 Release
Updated to the new JDA 3.X library and also updated to a new music player. This should resolve a lot of the issues people were having with sound files (like mono not working). Aso, added the ability to play youtube URLs.

[3.3.6.zip](https://github.com/Darkside138/DiscordSoundboard/releases/latest)

## Must have Java 17+ 64bit version installed in order for the soundboard to work properly

Join the official discord: [https://discord.gg/kZTNtfW](https://discord.gg/kZTNtfW)

## Current Release
Latest Release (Use version above). 
Download the zip|tar file and extract its contents in a directory. If you have an existing install, do not overwrite 
the "application.properties" file. In that same directory you will need a directory called "sounds" (This directory is 
configurable in the application.properties). Put all the clips you 
want to play in the sounds directory. In the app.properties file you should fill in the login information for 
your bot (you should create a new discord BOT account for your soundboard). Once you've created your new bot you must invite 
it to any server you want to use it on. The property "username_to_join_channel" is your username on discord. 
When you click a sound file to play in the soundboard, the app will look for this username and join the voice 
channel that user is in. If you don't have this configured properly the bot will not work. Also, the bot can 
respond to text channel commands. See below for information on those commands. Once this is complete execute 
the .jar file, or the .bat file. You should see a bunch of logging and eventually something like 
"Started MainController in 6.383 seconds (JVM running for 6.939)". Now you should be able to access the UI by 
opening a browser and navigating to "http://localhost:8080".

## How to Get a Bot Token and Invite your Bot
https://github.com/Darkside138/DiscordSoundboard/wiki

## Installation and Setup Video
Smugaloof has made a great setup video you can watch [here](https://www.youtube.com/watch?v=DQSXP9AgYvw). 
This will walk you through the process of getting your bot up and running.

### Docker Setup
Dockerhub image source: https://hub.docker.com/r/davefurrer/discordsoundboard

Using docker compose:
```
---
services:
  discordsoundbot:
    image: davefurrer/discordsoundboard:latest
    container_name: DiscordSoundbot
    environment:
      - bottoken=<Insert bot token here>
      - username=<Insert Discord username here>
      - chatcommands=true
      - dm=true
      - commandcharacter=?
      - sizelimit=2000
      - leavesuffix=_leave
    ports:
      - 8080:8080
    volumes:
      - /path/to/sounds/directory:/etc/DiscordSoundboard/bin/sounds
    restart: unless-stopped
```

## Setting correct permissions on your discord bot
To fix the issue of being stuck on "Connecting to websocket":
Login to your Discord Developer Portal and enable Privileged Intents for your bot. Go to https://discord.com/developers/applications select your bot, click Bot on the left, and then enable both of the sliders under Privileged Gateway Intents.

You will also need to configure "Intents". Follow this [link](https://jda.wiki/using-jda/troubleshooting/#im-getting-closecode4014-disallowed-intents) for instuctions on enabling Intents for your bot. Make sure Presence Intent, Server Members Intent, and Message Content Intent are all enabled.

## Donations
If you'd like to buy me a beer for my efforts, it's always appreciated. You can do so [here](https://www.paypal.me/DFurrer)

## Planned Future Features
Creating a bot instance for each unique channel/server combination for busy servers. Currently, there is one bot thread for each server.

## application.properties file
Backup your application.properties before the update to a new Version.
A new (clean) one is provided in the release file and will overwrite your config.

## Usage
Running locally from an IDE: Update the app.properties file with the login information for your "bot". Property 
"username_to_join_channel" is used by the application when a button in the UI is clicked. Before playing the 
sound file the bot will look for the username specified in all the servers the bot account has access to and 
join that channel, then play the sound that was clicked. For local usage sounds can be placed in src/main/resources/sounds/.

### Executing the app
Unzip the application. Update the application.properties with your bot token and any other preferences you would like. There are .bat and sh files in the /bin directory. Execute the proper one and the app should startup.

### Available commands
* ?help
* ?info
* ?reload
* ?list
* ?volume
* ?stop
* ?random
* ?disconnect
* ?url <urlToSound> (Supports Youtube, Vimeo, and Soundcloud)
* ?remove fileName (must have admin to remove files)
* ?entrance userName soundFile (must have modify server permission, Send with blank soundFile to remove.)
* ?leave userName soundFile (must have modify server permission, Send with blank soundFile to remove.)
* ?userdetails userName

Commands can be typed in any text channel that the bot has access to or you can send direct messages to the bot.
Responses will be sent to the requesting discordUser via direct message also.

PM the bot a wav or mp3 file < 10MB and it will add it to the soundboard.

### Playing sounds by text commands
Type ?list to get a list of sounds file commands the soundboard has available. The name of the commands will 
be the name of the sound file minus extension. When a discordUser types one of the sound file commands listed the bot 
will join that discordUser channel and then play the requested sound file. The bot remains in that channel unless the 
bot is requested to play sound in a different channel.

### Local Development
Included in the project files /.run folder there are IntelliJ run configurations. Execute the bootRun to run the application locally.
For the local run to work you need to, at minimum, put your bot_token into application-local.properties.

To create a distribution zip simply execute the gradle assembleBootDist. Once complete there will be a file in /build/distributions called DiscordSoundboard-version.zip