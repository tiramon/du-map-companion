# DuMapCompanion

## Requirements

To run the DUMapCompanion you have to use atleast Java 11.

## How to download the current Version

You can always find the newest version at the github release page https://github.com/tiramon/du-map-companion/releases

## Where do i get a Java Version working with this application

Best uninstall any old version before installing the new one.

If you have no Java installed ore only a old version, the current versions can be found at https://www.oracle.com/de/java/technologies/javase-downloads.html

## How to start the companion

If your Java is installed correctly double clicking the JAR should be enough.

To get some log output start the command line in the folder where the jar is lying and type (replace X.X.X with current version)
java -jar du-map-companion-X.X.X.jar

## How to use the application

As long as the application is running it will always look for the newest log file in your DU log folder. You have to do nothing special, just use your Territory Scanners as usual.
The results will be transfered to the website as soon as you click the ingame save button of your scanner result.

## Features

* click on the row of the table to copy the pos link to the clipboard
* use the dropdown to change the sound played when a territory scanner finishes his scan/is resetted by movement

## Change settings

If you want to use another sound for scan finished than the default you can create a file called 'application.properties' in the folder where you run the jar file and add one of the following lines

territoryscan.sound=None

territoryscan.sound=Pling

territoryscan.sound=Alien Voice

## FAQ

### When i start the companion i get an error 'UnsupportedClassVersionError'

This means you are running the application with a version lower than 11. One reason is ... you only have an older version installed. Another reason might be if you already installed Java 11 or higher that the PATH variable is still referencing the Java <11 Folder.
