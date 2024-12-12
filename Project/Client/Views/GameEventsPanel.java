package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Font;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;


import Project.Client.Client;
import Project.Client.Interfaces.IMessageEvents;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.ITimeEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.Constants;
import Project.Common.Phase;
import Project.Common.TimerType;


public class GameEventsPanel extends JPanel implements IPhaseEvent, IReadyEvent, IMessageEvents ,ITimeEvents, ITurnEvent {

   

 
    private JPanel content;
    private boolean debugMode = true; // Set this to false to disable debugging styling
    private JLabel timerText;
    private JButton rockButton, paperButton, scissorsButton,spockButton, lizardButton, skipButton, awayButton;
  

    // GridBagConstraints for the vertical glue
    private GridBagConstraints gbcGlue = new GridBagConstraints();

    public GameEventsPanel() {
        super(new BorderLayout(10, 10));
        content = new JPanel(new GridBagLayout());

        if (debugMode) {
            content.setBorder(BorderFactory.createLineBorder(Color.RED)); // Red border for debugging
            content.setBackground(new Color(240, 240, 240)); // Light grey background for debugging
        }

        // Wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        if (debugMode) {
            scroll.setBorder(BorderFactory.createLineBorder(Color.GREEN)); // Green border for debugging
        } else {
            scroll.setBorder(BorderFactory.createEmptyBorder());
        }

        this.add(scroll, BorderLayout.CENTER);

        // Add vertical glue to push messages to the top
        gbcGlue.gridx = 0;
        gbcGlue.gridy = GridBagConstraints.RELATIVE;
        gbcGlue.weighty = 1.0; // Give vertical glue a weight
        gbcGlue.fill = GridBagConstraints.BOTH;
        content.add(Box.createVerticalGlue(), gbcGlue);

        timerText = new JLabel();
        this.add(timerText, BorderLayout.NORTH);
        timerText.setVisible(false);
        Client.INSTANCE.addCallback(this);
    }

    public void addText(String text) {
        SwingUtilities.invokeLater(() -> {
            JEditorPane textContainer = new JEditorPane("text/plain", text);
            textContainer.setEditable(false);

            if (debugMode) {
                textContainer.setBorder(BorderFactory.createLineBorder(Color.BLUE)); // Blue border for debugging
                textContainer.setBackground(new Color(255, 255, 200)); // Light yellow background for debugging
            } else {
                textContainer.setBorder(BorderFactory.createEmptyBorder());
                textContainer.setBackground(new Color(0, 0, 0, 0));
            }

            // Set the text and then recalculate the preferred size
            textContainer.setText(text);
            Dimension preferredSize = textContainer.getPreferredSize();
            textContainer.setPreferredSize(new Dimension(content.getWidth(), preferredSize.height));

            // GridBagConstraints settings for each message
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; // Column index 0
            gbc.gridy = content.getComponentCount() - 1; // Place before the glue
            gbc.weightx = 1; // Let the component grow horizontally to fill the space
            gbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
            gbc.insets = new Insets(0, 0, 5, 0); // Add spacing between messages for debugging

            // Remove the glue, add the text, then re-add the glue
            content.remove(content.getComponentCount() - 1);
            content.add(textContainer, gbc);
            content.add(Box.createVerticalGlue(), gbcGlue);

            content.revalidate();
            content.repaint();

            // Scroll down on new message
            JScrollPane parentScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, content);
            if (parentScrollPane != null) {
                SwingUtilities.invokeLater(() -> {
                    JScrollBar vertical = parentScrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });
            }
        });
     // cae6
        JPanel buttonPanel = new JPanel();//cae6
        buttonPanel.setLayout(new FlowLayout());

        // Creating Buttons
        rockButton = createChoiceButton("R");
        paperButton = createChoiceButton("P");
        scissorsButton = createChoiceButton("S");
        spockButton = createChoiceButton("Spock");
        lizardButton = createChoiceButton("Lizard");
        skipButton = createChoiceButton("Skip");
        awayButton = createChoiceButton("away");


        // Adding Buttons to Panel
        buttonPanel.add(rockButton);
        buttonPanel.add(paperButton);
        buttonPanel.add(scissorsButton);
        buttonPanel.add(spockButton);
        buttonPanel.add(lizardButton);
        buttonPanel.add(skipButton);
        buttonPanel.add(awayButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }
    private JButton createChoiceButton(String choice) {
        JButton button = new JButton(choice);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(230, 230, 250)); // Lavender 
        button.setFocusPainted(false);
        
        button.addActionListener(e -> {
            try {
                if (Client.INSTANCE != null) {
                    Client.INSTANCE.sendTurnAction(choice); 
                }
                button.setEnabled(false); 
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    
        return button;
    }
    




    @Override
    public void onReceivePhase(Phase phase) {//cae6
        addText(String.format("The current phase is %s", phase));
    } //

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (isQuiet) {
            return;
        }
        String clientName = Client.INSTANCE.getClientNameFromId(clientId);
        addText(String.format("%s[%s] is %s", clientName, clientId, isReady ? "ready" : "not ready"));
    }

    @Override
    public void onMessageReceive(long id, String message) {
        if (id == Constants.GAME_EVENT_CHANNEL) { // using -2 as an internal channel for GameEvents
            addText(message);
        }
    }
    @Override
    public void onTookTurn(long clientId, boolean didtakeTurn, String choice){
        String clientName = Client.INSTANCE.getClientNameFromId(clientId);
        addText(String.format("%s took their turn and chose %s", clientName, choice));
    }
    

@Override //cae6 
public void onTimerUpdate(TimerType timerType, int time) {
    timerText.setText(String.format("%s timer: %s", timerType.name(), time));
    timerText.setVisible(time > 0);
}
}
