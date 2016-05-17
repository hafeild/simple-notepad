import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.Preferences;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SimpleNotepad is a super simple notepad. You can only edit one file at a
 * time. Content is auto saved to disk frequently and any changes to the
 * underlying file are loaded into the editor just as frequently. SimpleNotepad
 * is ideal for viewing and editing a notepad across desktops when the
 * underlying file is located in a auto synced directory, e.g. using a service
 * like Dropbox, Google Drive, or Box.
 * 
 * Created by hfeild on 5/14/16.
 */
public class SimpleNotepad implements ActionListener {
    private final static int POLLING_FREQUENCY = 500; // Half a second.
    private final static String PREFERENCE_FILE_NAME = ".simple-notepad.rc";
    private JEditorPane editorPane;
    private JPanel guiPanel;
    private JScrollPane scrollPane;
    private Preferences prefs;
    private String filename;
    private File file;
    private AtomicBoolean contentIsDirty;
    private long lastReadOrWrite;
    private JPopupMenu popup;
    private JMenuItem saveAsMenuItem, openFileMenuItem;
    private PollingThread pollingThread;
    private enum DialogType {SAVE, OPEN};


    /**
     * This listens for right-clicks and displays the menu as a popup. This
     * was taken from the Oracle PopupListener tutorial.
     */
    class PopupListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(),
                        e.getX(), e.getY());
            }
        }
    }

    /**
     * This listens for key presses. After a key is finished being pressed,
     * the editor content is marked as dirty, indicating to the polling thread
     * that it should be written to disk.
     */
    class TypingListener implements KeyListener {
        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            contentIsDirty.set(true);
            System.err.println("Key released; marking dirty!");
        }
    }

    /**
     * A thread that polls two things at POLLING_FREQUENCY intervals:
     *
     * 1. checks if the editor content is dirty; if so, writes it to file.
     * 2. checks if the file on disk has changed; if so, loads it into the
     *    editor.
     */
    class PollingThread extends Thread {
        public void run() {
            while(true){
                // Write the editor content to file if it is dirty.
                if(contentIsDirty.compareAndSet(true, false)) {
                    System.err.println("Content is dirty: writing file.");
                    writeToFile();

                // Reload the file contents if it changed from underneath us.
                } else if(file != null && 
                        file.lastModified() > lastReadOrWrite){
                    System.err.println("File has changed; updating.");
                    loadFromFile();
                }

                try{
                    Thread.sleep(POLLING_FREQUENCY);
                } catch(Exception e){
                    System.err.println("Issue sleeping: "+ e.getMessage());
                }
            }
        }
    }

    /**
     * Sets everything in motion.
     */
    public SimpleNotepad(){
        pollingThread = new PollingThread();
        contentIsDirty = new AtomicBoolean();
        file = null;
        filename = "";
        loadPrefs();

        createUIComponents();

        // Check if the filename is missing -- launch the file chooser.
        if(filename != "") {
            file = new File(filename);
            loadFromFile();
            System.err.println("Stored filename: "+ filename);
        }

        pollingThread.start();
    }

    /**
     * Creates the entire GUI and attaches listeners.
     */
    private void createUIComponents() {
        guiPanel = new JPanel(new GridLayout(1,1));
        guiPanel.setPreferredSize(new Dimension(400, 600));

        editorPane = new JEditorPane(){
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        editorPane.setPreferredSize(new Dimension(-1, -1));

        scrollPane = new JScrollPane(editorPane);
        scrollPane.setHorizontalScrollBarPolicy(
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        guiPanel.add(scrollPane);

        KeyListener typingListener = new TypingListener();
        editorPane.addKeyListener(typingListener);

        addPopupMenu();
    }

    /**
     * Assembles the popup menu and attaches listeners.
     */
    public void addPopupMenu(){
        System.err.println("Attaching popup menu...");

        popup = new JPopupMenu();
        saveAsMenuItem = new JMenuItem("Save as");
        saveAsMenuItem.addActionListener(this);
        popup.add(saveAsMenuItem);
        openFileMenuItem = new JMenuItem("Open file");
        openFileMenuItem.addActionListener(this);
        popup.add(openFileMenuItem);

        MouseListener popupListener = new PopupListener();
        editorPane.addMouseListener(popupListener);
    }

    /**
     * Creates a file pointing at the preference file (stored in the user's 
     * home directory). The actual file may or may not exist.
     *
     * @return The preference file.
     */
    public static File getUserPrefFile() {
        return new File(System.getProperty("user.home") + 
            File.separator + PREFERENCE_FILE_NAME);
    }

    /**
     * Loads the preferences from the preference file. Currently supported
     * preferences:
     *
     *  filename: The name of the last file opened. This is not opened, but
     *            the filename and file instance members are updated.
     */
    public void loadPrefs(){
        File userPrefFile = getUserPrefFile();
        if(!userPrefFile.exists()) return;

        try {
            String curLine;
            BufferedReader reader = new BufferedReader(
                new FileReader(userPrefFile));
            while((curLine = reader.readLine()) != null){
                if(curLine.startsWith("filename: ")){
                    filename = curLine.substring("filename: ".length()).trim();
                }
            }
            reader.close();
        } catch(Exception e) {
            System.err.println("Couldn't read preference file ("+ 
                userPrefFile.getAbsoluteFile() +")\n"+ e.getMessage());
        }
    }

    /**
     * Saves preferences to the preference file. See loadPrefs for a list of
     * preferences that are supported.
     */
    public void savePrefs(){
        File userPrefFile = getUserPrefFile();

        try {
            BufferedWriter writer = new BufferedWriter(
                new FileWriter(userPrefFile));
            writer.write("filename: "+ filename);
            writer.close();
        } catch(Exception e) {
            System.err.println("Couldn't write preference file ("+ 
                userPrefFile.getAbsoluteFile() +")\n"+ e.getMessage());
        }
    }

    /**
     * Alias for showOpenSaveDialog(DialogType.OPEN).
     */
    public void showOpenFileDialog(){
        showOpenSaveDialog(DialogType.OPEN);
    }

    /**
     * Alias for showOpenSaveDialog(DialogType.SAVE).
     */
    public void showSaveFileDialog(){
        showOpenSaveDialog(DialogType.SAVE);
    }

    /**
     * Opens a file chooser to save or open a file. If successful, this updates
     * the file and filename instance members, and causes the preference file to
     * be updated.
     *
     * @param type Whether the file is to be opened or saved.
     */
    public void showOpenSaveDialog(DialogType type){
        JFileChooser chooser = new JFileChooser();
        int returnVal;

        if(type == DialogType.OPEN)
            returnVal = chooser.showOpenDialog(null);
        else
            returnVal = chooser.showSaveDialog(null);

        if(returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            filename = file.getAbsolutePath();
            savePrefs();
        }
    }

    /**
     * Writes the editor content to the current file, if one has been selected.
     */
    public void writeToFile(){
        if(file == null) return;

        System.err.println("Writing to file "+ filename);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(editorPane.getText());
            writer.close();
            lastReadOrWrite = file.lastModified();
        } catch(FileNotFoundException e) {
            System.err.println("File not found! ["+ filename +"]\n"+ 
                e.getMessage());
        } catch(IOException e) {
            System.err.println("IOException writing to file "+ filename +
                "\n"+ e.getMessage());
        }
    }

    /**
     * Loads the content of the current file (if one has been selected) to the
     * editor.
     */
    public void loadFromFile(){
        if(file == null) return;

        System.err.println("Loading file "+ filename);

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuffer buffer = new StringBuffer();
            String curLine;
            while((curLine = reader.readLine()) != null)
                buffer.append(curLine +"\n");
            reader.close();
            lastReadOrWrite = file.lastModified();
            editorPane.setText(buffer.toString());
        } catch(FileNotFoundException e) {
            System.err.println("File not found! ["+ filename +"]\n"+ 
                e.getMessage());
        } catch(IOException e) {
            System.err.println("IOException reading file "+ filename +"\n"+ 
                e.getMessage());
        }
    }

    /**
     * Listens for a menu item to be selected.
     *
     * @param actionEvent The event that triggered the listener.
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if(actionEvent.getSource() == saveAsMenuItem){
            showSaveFileDialog();
            writeToFile();
        } else if(actionEvent.getSource() == openFileMenuItem){
            showOpenFileDialog();
            loadFromFile();
        }
    }

    /**
     * Starts up the app.
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame("Simple Notepad");
        frame.setContentPane(new SimpleNotepad().guiPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
