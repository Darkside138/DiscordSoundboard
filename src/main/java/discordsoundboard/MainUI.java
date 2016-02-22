package main.java.discordsoundboard;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;
import net.dv8tion.jda.audio.player.Player;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import javax.security.auth.login.LoginException;
import javax.swing.*;

public class MainUI {
    
    SoundPlayer soundPlayer;
    Player player;

    private JFrame mainFrame;
    private JPanel controlPanel;
    private Properties appProperties;

    public MainUI(){
        loadProperties();
        
        prepareGUI();

        mainFrame.setTitle(appProperties.getProperty("app_title"));
    }

    private void loadProperties() {
        appProperties = new Properties();
        InputStream stream = null;
        try {
            stream = new FileInputStream(System.getProperty("user.dir") + "/app.properties");
            appProperties.load(stream);
            stream.close();
            return;
        } catch (FileNotFoundException e) {
            System.out.println("Could not find app.properties file.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (stream == null) {
            System.out.println("Loading app.properties file from resources folder");
            try {
                stream = this.getClass().getResourceAsStream("app.properties");
                appProperties.load(stream);
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        MainUI mainUI = new MainUI();

        mainUI.initializeDiscordBot();
        
        mainUI.showSoundboard();
    }

    private void prepareGUI(){
        int width = 400;
        int height = 500;
        if (appProperties.getProperty("app_width") != null) {
            width = Integer.parseInt(appProperties.getProperty("app_width"));
        }
        if (appProperties.getProperty("app_height") != null) {
            height = Integer.parseInt(appProperties.getProperty("app_height"));
        }
        mainFrame = new JFrame("App Title");
        mainFrame.setSize(width,height);
        
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                System.exit(0);
                player.stop();
                player = null;
            }
        });
        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());

        mainFrame.add(controlPanel);
        mainFrame.setVisible(true);
    }

    private void showSoundboard(){
        Map<String,File> soundFiles = soundPlayer.getAvailableSoundFiles();
        for(Map.Entry entry : soundFiles.entrySet()) {
            String mapKey = entry.getKey().toString();
            JButton soundButton1 = new JButton(mapKey);
            soundButton1.setActionCommand(mapKey);
            soundButton1.addActionListener(new ButtonClickListener());
            controlPanel.add(soundButton1);
            mainFrame.setVisible(true);
        }
    }

    private class ButtonClickListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            soundPlayer.playFileForUser(command, appProperties.getProperty("username_to_join_channel"));
        }
    }

    private void initializeDiscordBot() {
        try {
            soundPlayer = new SoundPlayer(player);
            ChatSoundBoardListener chatListener = new ChatSoundBoardListener(soundPlayer);
            JDA bot = new JDABuilder()
                    .setEmail(appProperties.getProperty("username"))
                    .setPassword(appProperties.getProperty("password"))
                    .addListener(chatListener)
                    .buildBlocking();
            
            soundPlayer.setBot(bot);
        }
        catch (IllegalArgumentException e) {
            System.out.println("The config was not populated. Please enter an email and password.");
        }
        catch (LoginException e) {
            System.out.println("The provided email / password combination was incorrect. Please provide valid details.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
