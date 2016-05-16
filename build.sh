#!/bin/bash

## Build.
mkdir -p bin
javac src/*.java -d bin
jar cfe simple-notepad.jar SimpleNotepad -C bin .

## Let the user know how to use it.
echo -e "You can run the program by doing the following:\n"
echo -e "\tjava -jar simple-notpad.jar\n"
