#!/bin/bash


## Build.
echo -n "Building..."
mkdir -p bin
javac src/*.java -d bin
status=$?
if [ $status -eq 0 ]; then
    jar cfe simple-notepad.jar SimpleNotepad -C bin .
    status=$?
fi

if [ $status -eq 0 ]; then
    chmod +x simple-notepad.jar
    echo -e "done!\n"

    ## Let the user know how to use it.
    echo -e "You can run the program either of the following methods:\n"
    echo -e "1.\t./simple-notepad"
    echo -e "2.\tjava -jar simple-notepad.jar"
    echo -e "3.\tdouble click simple-notepad.jar in a file browser\n"
else
    echo -e "\nBuild failed.\n"
fi