package net.dirtydeeds.discordsoundboard;

import net.dirtydeeds.discordsoundboard.beans.SoundFile;
import net.dirtydeeds.discordsoundboard.service.SoundPlayerImpl;
import net.dv8tion.jda.audio.player.Player;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author dfurrer.
 *
 * This class handles the UI and initializing the bot
 */
@SpringBootApplication
public class MainController {

    SoundPlayerImpl soundPlayer = null;
    
    Player player;

    private JFrame mainFrame;
    private JPanel soundButtonPanel;
    private JPanel volumePanel;
    private JLabel spacer;
    private Properties appProperties;
    private int initialVolume = 75;
    
    public MainController() {
    }

    @RequestMapping("/*")

    public String index(org.springframework.ui.Model model) {

        return "index";

    }

    @Inject
    public MainController(final SoundPlayerImpl soundPlayer) {
        this.soundPlayer = soundPlayer;
        //Load properties
        appProperties = soundPlayer.getProperties();

        //Prepare the main window UI
//        prepareGUI();

        //Init the discord bot
//        initializeDiscordBot();

        //Create the sound player
//        soundPlayer = new SoundPlayer(player, bot);
        Map<String,SoundFile> soundFiles = soundPlayer.getAvailableSoundFiles();
//        showSoundboard(soundFiles);

        //Setup the ChatListener if the user has configured it to be active
        if (Boolean.valueOf(appProperties.getProperty("respond_to_chat_commands"))) {
            ChatSoundBoardListener chatListener = new ChatSoundBoardListener(soundPlayer);
            this.soundPlayer.setBotListener(chatListener);
        }
//        setSoundPlayerVolume(initialVolume);
    }

    public static void main(String[] args) {
        SpringApplication.run(MainController.class, args);
//        new MainController();
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
                soundPlayer.shutdown();
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
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, initialVolume);
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
    private void showSoundboard(Map<String,SoundFile> soundFiles){
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
            setSoundPlayerVolume(sliderEventSource.getValue());
        }
    }

    private void setSoundPlayerVolume(int volume) {
        float volumeFloat = (float) volume / 100;
//        soundPlayer.setVolume(volumeFloat);
    }
    
}
