import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.prefs.Preferences;
import java.util.Date;

/**
 * Created by hfeild on 5/14/16.
 */
public class Editor implements ActionListener {
    private JEditorPane editorPane;
    private JPanel guiPanel;
    private JScrollPane scrollPane;
    private Preferences prefs;
    private String filename;
    private File file;
    private long lastWrite;
    private JPopupMenu popup;
    private JMenuItem saveAsMenuItem, openFileMenuItem;

    private enum DialogType {SAVE, OPEN};


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


    public Editor(){
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

    }

    private void createUIComponents() {
        guiPanel = new JPanel(new GridLayout(1,1));
        guiPanel.setPreferredSize(new Dimension(400, 600));

//        scrollPane.setPreferredSize(new Dimension(-1, -1));

        editorPane = new JEditorPane();
        editorPane.setPreferredSize(new Dimension(-1, -1));

        scrollPane = new JScrollPane(editorPane);

        guiPanel.add(scrollPane);

        addPopupMenu();
    }

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

    public static File getUserPrefFile() {
        return new File(System.getProperty("user.home") + File.separator + ".simple-editor");
    }

    public void loadPrefs(){
        File userPrefFile = getUserPrefFile();
        if(!userPrefFile.exists()) return;

        try {
            String curLine;
            BufferedReader reader = new BufferedReader(new FileReader(userPrefFile));
            while((curLine = reader.readLine()) != null){
                if(curLine.startsWith("filename: ")){
                    filename = curLine.substring("filename: ".length()).trim();
                }
            }
            reader.close();
        } catch(Exception e) {
            System.err.println("Couldn't read preference file ("+ userPrefFile.getAbsoluteFile() +")\n"+
                    e.getMessage());
        }
    }

    public void savePrefs(){
        File userPrefFile = getUserPrefFile();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(userPrefFile));
            writer.write("filename: "+ filename);
            writer.close();
        } catch(Exception e) {
            System.err.println("Couldn't write preference file ("+ userPrefFile.getAbsoluteFile() +")\n"+
                    e.getMessage());
        }
    }

    public void showOpenFileDialog(){
        showOpenSaveDialog(DialogType.OPEN);
    }

    public void showSaveFileDialog(){
        showOpenSaveDialog(DialogType.SAVE);
    }

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

    public void writeToFile(){
        if(file == null) return;

        System.err.println("Writing to file "+ filename);

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(editorPane.getText());
            writer.close();
            lastWrite = file.lastModified();
        } catch(FileNotFoundException e) {
            System.err.println("File not found! ["+ filename +"]\n"+ e.getMessage());
        } catch(IOException e) {
            System.err.println("IOException writing to file "+ filename +"\n"+ e.getMessage());
        }
    }

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
            lastWrite = file.lastModified();
            editorPane.setText(buffer.toString());
        } catch(FileNotFoundException e) {
            System.err.println("File not found! ["+ filename +"]\n"+ e.getMessage());
        } catch(IOException e) {
            System.err.println("IOException reading file "+ filename +"\n"+ e.getMessage());
        }
    }

    public boolean fileIsNewer(){
        return false;
    }

    public boolean bufferNeedsWriting(){
        return false;
    }

    public void sync(){

    }

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


    public static void main(String[] args) {
        JFrame frame = new JFrame("Editor");
        frame.setContentPane(new Editor().guiPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
