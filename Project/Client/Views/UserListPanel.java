package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
//import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import Project.Client.Client;
import Project.Client.ClientPlayer;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.ITurnEvent;
import Project.Client.Interfaces.IEliminatedEvent;
import Project.Common.LoggerUtil;

/**
 * UserListPanel represents a UI component that displays a list of users.
 */
public class UserListPanel extends JPanel implements IReadyEvent, IPointsEvent, ITurnEvent, IEliminatedEvent {
    private JPanel userListArea;
    private GridBagConstraints lastConstraints; // Keep track of the last constraints for the glue
    private HashMap<Long, UserListItem> userItemsMap; // Maintain a map of client IDs to UserListItems

    /**
     * Constructor to create the UserListPanel UI.
     */
    public UserListPanel() {//cae6, 12/11/24
        super(new BorderLayout(10, 10));
        userItemsMap = new HashMap<>(); // Initialize the map

        JPanel content = new JPanel(new GridBagLayout());
        userListArea = content;

        // Wraps a viewport to provide scroll capabilities
        JScrollPane scroll = new JScrollPane(userListArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0)); // Remove border

        this.add(scroll, BorderLayout.CENTER);

        userListArea.addContainerListener(new ContainerListener() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        userListArea.revalidate();
                        userListArea.repaint();
                    });
                }
            }

            @Override
            public void componentRemoved(ContainerEvent e) {
                if (userListArea.isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        userListArea.revalidate();
                        userListArea.repaint();
                    });
                }
            }
        });

        // Add vertical glue to push items to the top
        //cae6, 12/11/24
        lastConstraints = new GridBagConstraints();
        lastConstraints.gridx = 0;
        lastConstraints.gridy = GridBagConstraints.RELATIVE;
        lastConstraints.weighty = 1.0;
        lastConstraints.fill = GridBagConstraints.VERTICAL;
        userListArea.add(Box.createVerticalGlue(), lastConstraints);

        // Listen for resize events to adjust user list items accordingly
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // SwingUtilities.invokeLater(() -> adjustUserListItemsWidth());
            }
        });
        // register to receive events
        Client.INSTANCE.addCallback(this);
    }

    /**
     * Adds a user to the list.
     *
     * @param clientId   The ID of the client.
     * @param clientName The name of the client.
     */
    protected void addUserListItem(long clientId, String clientName) {//cae6, 12/11/24
        SwingUtilities.invokeLater(() -> {
            if (userItemsMap.containsKey(clientId)) {
                LoggerUtil.INSTANCE.warning("User already in the list: " + clientName);
                return; // User already in the list
            }

            LoggerUtil.INSTANCE.info("Adding user to list: " + clientName);

            UserListItem userItem = new UserListItem(clientId, clientName, userListArea);
            // GridBagConstraints settings for each user
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; // Column index 0
            gbc.gridy = userListArea.getComponentCount() - 1; // Place before the glue
            gbc.weightx = 1; // Let the component grow horizontally to fill the space
            gbc.anchor = GridBagConstraints.NORTH; // Anchor to the top
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 0); // Add spacing between users

            // Remove the last glue component if it exists
            if (lastConstraints != null) {
                int index = userListArea.getComponentCount() - 1;
                if (index > -1) {
                    userListArea.remove(index);
                }
            }
            // Add user item
            userListArea.add(userItem, gbc);

            // Add vertical glue to push items to the top
            userListArea.add(Box.createVerticalGlue(), lastConstraints);

            userItemsMap.put(clientId, userItem); 
          // Add to the map
        

            userListArea.revalidate();
            userListArea.repaint();
      
        });
    }




// Call this method after any points update

@Override
public void onPointsUpdate(long clientId, int points) {
    SwingUtilities.invokeLater(() -> {
        // If clientId is valid, update their points
        if (userItemsMap.containsKey(clientId)) {
            UserListItem userItem = userItemsMap.get(clientId);
            userItem.setPoints(points); // Update the points of the specific user
        } else if (clientId <= ClientPlayer.DEFAULT_CLIENT_ID) {
            // If clientId is a special identifier (e.g., default player), hide all points
            userItemsMap.values().forEach(u -> u.setPoints(-1));
        }

        // After updating points, sort the list of users based on points (descending) and name
        List<UserListItem> items = new ArrayList<>(userItemsMap.values());
        Collections.sort(items, (a, b) -> {
            int pointCompare = Integer.compare(b.getPoints(), a.getPoints()); // Sort by points (descending)
            if (pointCompare != 0) return pointCompare;
            return a.getClientName().compareToIgnoreCase(b.getClientName()); // Sort by name (alphabetically)
        });

        // Reorder the users in the panel according to the sorted order
        userListArea.removeAll(); // Clear the current list
        for (int i = 0; i < items.size(); i++) {
            UserListItem item = items.get(i);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 0);
            userListArea.add(item, gbc);
        }

        // Add the vertical glue back to keep the layout correct
        userListArea.add(Box.createVerticalGlue(), lastConstraints);

        // Revalidate and repaint the user list area to apply changes
        userListArea.revalidate();
        userListArea.repaint();
    });
}


    /**
     * Adjusts the width of all user list items.
     */
   /*  private void adjustUserListItemsWidth() {
        SwingUtilities.invokeLater(() -> {
            for (UserListItem item : userItemsMap.values()) {
                item.setPreferredSize(
                        new Dimension(userListArea.getWidth() - 20, item.getPreferredSize().height));
            }
            userListArea.revalidate();
            userListArea.repaint();
        });
    }

    /**
     * Removes a user from the list.
     *
     * @param clientId The ID of the client to be removed.
     */
    protected void removeUserListItem(long clientId) {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Removing user list item for id " + clientId);
            UserListItem item = userItemsMap.remove(clientId); // Remove from the map
            if (item != null) {
                userListArea.remove(item);
                userListArea.revalidate();
                userListArea.repaint();
            }
        });
    }


    /**
     * 
     * Clears the user list.
     */
    protected void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Clearing user list");
            userItemsMap.clear(); // Clear the map
            userListArea.removeAll();
            userListArea.revalidate();
            userListArea.repaint();
        });
    }

    @Override
    public void onTookTurn(long clientId, boolean didTakeTurn, String choice) {
        SwingUtilities.invokeLater(() -> {
            // Check if the user exists in the map
            if (userItemsMap.containsKey(clientId)) {
                UserListItem userItem = userItemsMap.get(clientId);
    
                // Set the turn status for the user
                userItem.setTurn(didTakeTurn);
               
    
                // Optionally, update the user's turn indicator color based on choice
                if (didTakeTurn) {
                    userItem.setTurn(true, Color.GREEN); 
                } else {
                    userItem.setTurn(false, Color.YELLOW); 
                }
            }
        });
    }
    

@Override
    public void markEliminated(long clientId, String clientName) {
        SwingUtilities.invokeLater(() -> {
            if (userItemsMap.containsKey(clientId)) {
                UserListItem userItem = userItemsMap.get(clientId);
                userItem.setEliminated(true);
                userItem.setBackground(Color.RED);
            }
            userListArea.repaint();
              
        });
        
    }





 

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (clientId == ClientPlayer.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.values().forEach(u -> u.setTurn(false));
                // reset all
            });
        } else if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                userItemsMap.get(clientId).setTurn(isReady, Color.GRAY);
                
            });
        }
    }
     
        





}