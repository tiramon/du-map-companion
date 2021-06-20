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

## How to use the integrated sound framework

The companion app also supports the 'sound framework' which can play sounds based on logfile entries as used by [Arch-Orbital-Hud](https://github.com/Archaegeo/Archaegeo-Orbital-Hud)
Specifaction and other implementations can also be found at
* [https://github.com/ZarTaen/DU_logfile_audioframework](https://github.com/ZarTaen/DU_logfile_audioframework)
* [https://github.com/Dimencia/DU-Audio-Sharp](https://github.com/Dimencia/DU-Audio-Sharp)

## Features

* click on the row of the table to copy the pos link to the clipboard
* play sound when territory scanner finishes or is reseted
* use the dropdown to change the sound played when a territory scanner finishes his scan/is resetted by movement
* integrated sound framework

## Change settings (application.properties)

You can place a file called 'application.properties' in the folder where you run the application, it can be used to change some default settings

### territoryscan.sound

If you want another sound selected by default for scan finished than the default you can create a file called 'application.properties' in the folder where you run the jar file and add one of the following lines

Possible values: None, Pling, Alien Voice

Default value: Pling
 
### skip.to.end

Should only be used if you start the companion before DU or if you have to restart the companion during the game.
It will skip all entries and move directly to the end of the log file. This way all scan and other entries in the log file will be skiped and not send to the server. I use this primarly for debuging because i hacve to restart the companion often and don't want to wait till it reaches the end of the logfile.

Possible values: true, false

Default value : false

### sound.framework.enabled

Here you can change the initial value for the buildin sound framework. If you set this to false it will not interpret any sound entries in the logfile.

Possible values: true, false

Default value: true

### sound.framework.volume

Default base sound for all sounds played by the sound framework.

Possible values: 0-100

Default value: 100

### access

To avoid manually login on each startup you can place your access token here 

Possible values: valid dumap.de discord access token

Default: empty

## FAQ

### When i start the companion i get an error 'UnsupportedClassVersionError'

This means you are running the application with a version lower than 11. One reason is ... you only have an older version installed. Another reason might be if you already installed Java 11 or higher that the PATH variable is still referencing the Java <11 Folder.
