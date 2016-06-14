import java.io.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.embed.swing.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.FileChooser;

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
public class SimpleNotepad extends Application {
    private final static int POLLING_FREQUENCY = 500; // Half a second.
    private final static String PREFERENCE_FILE_NAME = ".simple-notepad.rc";
    private String filename;
    private File file;
    private AtomicBoolean contentIsDirty;
    private long lastReadOrWrite;
    private PollingThread pollingThread;
    private enum DialogType {SAVE, OPEN};

    private Stage mainStage;
    private TextArea editorArea;
    private ContextMenu popupMenu;
    private MenuItem saveAsMenuItemX, openFileMenuItemX;

    /**
     * Listens for a menu item to be selected.
     *
     * @param actionEvent The event that triggered the listener.
     */
    class MenuClickListener implements EventHandler<ActionEvent>{
        public void handle(ActionEvent e){
            if(e.getSource() == saveAsMenuItemX){
                showSaveFileDialog();
                writeToFile();
            } else if(e.getSource() == openFileMenuItemX){
                showOpenFileDialog();
                loadFromFile();
            }
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
        private boolean stop;
        PollingThread(){
            stop = false;
        }
        public void run() {
            while(!stop){
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

        public void finish(){
            stop = true;
        }

    }

    /**
     * Sets everything in motion.
     *
     * @param stage The primary stage the GUI components will be added to.
     */
    @Override
    public void start(Stage stage){
        Platform.setImplicitExit(true);
        mainStage = stage;

        pollingThread = new PollingThread();
        contentIsDirty = new AtomicBoolean();
        file = null;
        filename = "";
        loadPrefs();

        createUIComponents(stage);

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
     *
     * @param stage The primary stage the GUI components will be added to.
     */
    private void createUIComponents(Stage stage) {
        SwingNode swingNode = new SwingNode();
        StackPane pane = new StackPane();

        editorArea = new javafx.scene.control.TextArea();
        editorArea.setWrapText(true);
        editorArea.textProperty().addListener(
            (observable, oldValue, newValue)-> {
                if(!newValue.equals(oldValue)){
                    contentIsDirty.set(true);
                    System.err.println("Content changed; marking dirty!");
                }
            });

        addPopupMenu();

        pane.getChildren().add(editorArea);
        stage.setScene(new Scene(pane, 400, 600));
        stage.show();
    }

    /**
     * Assembles the popup menu and attaches listeners.
     */
    public void addPopupMenu(){
        System.err.println("Attaching popup menu...");

        MenuClickListener menuClickListener = new MenuClickListener();

        popupMenu = new ContextMenu();
        saveAsMenuItemX = new MenuItem("Save as");
        saveAsMenuItemX.setOnAction(menuClickListener);
        openFileMenuItemX = new MenuItem("Open file");
        openFileMenuItemX.setOnAction(menuClickListener);
        popupMenu.getItems().addAll(saveAsMenuItemX, openFileMenuItemX);
        editorArea.setContextMenu(popupMenu);
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
        FileChooser chooser = new FileChooser();
        File chosenFile;

        if(type == DialogType.OPEN){
            chooser.setTitle("Open existing file");
            chosenFile = chooser.showOpenDialog(mainStage);
        } else {
            chooser.setTitle("Choose where to save file");
            chosenFile = chooser.showSaveDialog(mainStage);
        }

        if(chosenFile != null) {
            file = chosenFile;
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
            writer.write(editorArea.getText());
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
            editorArea.setText(buffer.toString());
        } catch(FileNotFoundException e) {
            System.err.println("File not found! ["+ filename +"]\n"+
                e.getMessage());
        } catch(IOException e) {
            System.err.println("IOException reading file "+ filename +"\n"+
                e.getMessage());
        }
    }

    /**
     * Shuts down the polling thread and exits the program.
     */
    @Override
    public void stop(){
        pollingThread.finish();
        System.exit(0);
    }

    /**
     * Starts up the app.
     */
    public static void main(String[] args) {
        launch(args);

        // JFrame frame = new JFrame("Simple Notepad");
        // frame.setContentPane(new SimpleNotepad().guiPanel);
        // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // frame.pack();
        // frame.setVisible(true);
    }
}
