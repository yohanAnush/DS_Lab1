package chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    JList activeUsersList = new JList();
    JCheckBox broadcastCheckBox = new JCheckBox("Broadcast to Everyone");

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        
        textField.setEditable(false);
        messageArea.setEditable(false);
        activeUsersList.setBounds(0, 100, 400, 40);
        activeUsersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        frame.getContentPane().add(activeUsersList, BorderLayout.WEST);
        frame.getContentPane().add(textField, BorderLayout.NORTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.getContentPane().add(broadcastCheckBox, BorderLayout.SOUTH);
        frame.pack();

        
        // Add Listeners
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
                // Check if any user in the userlist is selected.
                // If so, format the message in a way all the selected users are
                // there in the beginning of the message, seperated by >>
                // Example: A>>B>>Hello indicates a client is sending the message "Hello"
                // and he/she has selected A, and B from the user list.
                // All the above conditions must be checked only if "Broadcast to everyone" is not checked,
                // as it overrides any notation, or selection.
                if (broadcastCheckBox.isSelected()) {
                    out.println(textField.getText().replaceAll(">>", ""));  // to ignore the notation of sending a message to a specific person(s).
                }
                else if (!activeUsersList.isSelectionEmpty()) {
                    String users = String.join(">>", activeUsersList.getSelectedValuesList());
                    out.println(users + ">>" + textField.getText());
                }
                else {
                    out.println(textField.getText());
                }
                
                textField.setText("");
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        String name = JOptionPane.showInputDialog(
                                            frame,
                                            "Choose a screen name:",
                                            "Screen name selection",
                                            JOptionPane.PLAIN_MESSAGE);
        
        frame.setTitle(frame.getTitle() + " (" + name + ")");   // To make is easy to findout to whom the client window belongs.

        return name;
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("ACTIVEUSERS")) {
                String userListWithToken = line.substring(11);  // to cut the "ACTIVEUSERS" part.
                StringTokenizer userList = new StringTokenizer(userListWithToken, ":");
                DefaultListModel<String> dlm = new DefaultListModel();
                
                while (userList.hasMoreTokens()) {
                    dlm.addElement(userList.nextToken());
                }

                activeUsersList.setModel(dlm);
            }
            
            
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}