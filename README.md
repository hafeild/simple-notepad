# Overview

SimpleNotepad is a super simple notepad. You can only edit one file at a
time. Content is auto saved to disk frequently and any changes to the
underlying file are loaded into the editor just as frequently. SimpleNotepad
is ideal for viewing and editing a notepad across desktops when the
underlying file is located in a auto synced directory, e.g. using a service
like Dropbox, Google Drive, or Box.


# Getting and compiling

In a terminal, clone and build the project:

    git clone https://github.com/hafeild/simple-notepad.git
    cd simple-notepad
    ./build.sh

# Running

From a terminal, do:

    java -jar simple-notepad.jar

Or, double click the jar file in a file browser. To open/save a file, right
click anywhere in the editor that pops up.