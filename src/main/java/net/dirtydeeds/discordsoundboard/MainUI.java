package net.dirtydeeds.discordsoundboard;

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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author dfurrer.
 *
 * This class handles the UI and initializing the bot
 */
public class MainUI {
    
    SoundPlayer soundPlayer;
    Player player;

    private JFrame mainFrame;
    private JPanel soundButtonPanel;
    private JPanel volumePanel;
    private JLabel spacer;
    private Properties appProperties;
    private JDA bot;

    public MainUI(){
        //Load properties
        //Load properties
        loadProperties();

        //Prepare the main window UI
        prepareGUI();

        //Init the discord bot
        initializeDiscordBot();

        //Create the sound player
        soundPlayer = new SoundPlayer(player, bot);
        Map<String,File> soundFiles = soundPlayer.getAvailableSoundFiles();
        showSoundboard(soundFiles);

        //Setup the ChatListener if the user has configured it to be active
        if (Boolean.valueOf(appProperties.getProperty("respond_to_chat_commands"))) {
            ChatSoundBoardListener chatListener = new ChatSoundBoardListener(soundPlayer);
            bot.addEventListener(chatListener);
        }
    }

    public static void main(String[] args){
        new MainUI();
    }

    //Loads in the properties from the app.properties file
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
                stream = this.getClass().getResourceAsStream("/app.properties");
                appProperties.load(stream);
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //Sets up the initial UI
    private void prepareGUI(){
        int width = 400;
        int height = 500;
        if (appProperties.getProperty("app_width") != null) {
            width = Integer.parseInt(appProperties.getProperty("app_width"));
        }
        if (appProperties.getProperty("app_height") != null) {
            height = Integer.parseInt(appProperties.getProperty("app_height"));
        }
        mainFrame = new JFrame(appProperties.getProperty("app_title"));
//        mainFrame.setSize(width,height);
        
        mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent){
                System.exit(0);
                bot.shutdown();
                player.stop();
                player = null;
            }
        });

        soundButtonPanel = new JPanel(new FlowLayout()) {
            @Override
            public Dimension getMaximumSize() {
                return super.getMaximumSize();
            }
        };
        soundButtonPanel.setLayout(new FlowLayout());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new RefreshUIListener());
//        mainPanel.add(refreshButton);

        volumePanel = new JPanel(new FlowLayout());
        JPanel vPan = new JPanel();
        GridLayout grid = new GridLayout();
        grid.setColumns(1);
        grid.setHgap(20);
        grid.setRows(3);
        vPan.setLayout(grid);
        JLabel volumeLabel = new JLabel("Volume:");
        volumePanel.add(vPan);
        vPan.add(spacer = new JLabel(" "),"span, grow");
        vPan.add(volumeLabel);
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        slider.addChangeListener(new SliderListener());
        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        vPan.add(slider);

        mainFrame.add(soundButtonPanel);
        
        mainFrame.setSize(width,height);
        mainFrame.setVisible(true);
    }

    private class RefreshUIListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            soundButtonPanel.removeAll();
            showSoundboard(soundPlayer.getAvailableSoundFiles());
        }
    }

    //This method creates a button for each sound file that is available
    private void showSoundboard(Map<String,File> soundFiles){
        for(Map.Entry entry : soundFiles.entrySet()) {
            String mapKey = entry.getKey().toString();
            JButton soundButton1 = new JButton(mapKey);
            soundButton1.setActionCommand(mapKey);
            soundButton1.addActionListener(new ButtonClickListener());
            soundButtonPanel.add(soundButton1);
        }
        soundButtonPanel.add(spacer = new JLabel(" "),"span, grow");
        soundButtonPanel.add(spacer = new JLabel(" "),"span, grow");
        soundButtonPanel.add(volumePanel);
        mainFrame.setVisible(true);
    }

    //When buttons are clicked join the specified users channel and play the sound
    private class ButtonClickListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            soundPlayer.playFileForUser(command, appProperties.getProperty("username_to_join_channel"));
        }
    }

    class SliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent changeEvent) {
            JSlider sliderEventSource = (JSlider) changeEvent.getSource();
            float volume = (float) sliderEventSource.getValue() / 100;
            soundPlayer.setVolume(volume);
        }
    }

    //Logs the discord bot in and adds the ChatSoundBoardListener if the user configured it to be used
    private void initializeDiscordBot() {
        try {
            bot = new JDABuilder()
                    .setEmail(appProperties.getProperty("username"))
                    .setPassword(appProperties.getProperty("password"))
                    .buildBlocking();
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
