# DuMapCompanion

## Table of contents
* [Features](#features)
* [Requirements](#requirements)
* [How to download the current Version](#how-to-download-the-current-version)
* [Where do i get a Java Version working with this application](#where-do-i-get-a-java-version-working-with-this-application)
* [How to start the companion](#how-to-start-the-companion)
* [How to use the application](#how-to-use-the-application)
* [How to use the integrated sound framework](#how-to-use-the-integrated-sound-framework)
* [Change settings (application.properties)](#change-settings-applicationproperties)
    * [territoryscan.sound](#territoryscansound)
    * [skip.to.end](#skiptoend)
    * [sound.framework.enabled](#soundframeworkenabled)
    * [sound.framework.volume](#soundframeworkvolume)
    * [access](#access)
    * [territoryscan.sound](#territoryscansound)
* [FAQ](#faq)
     * [When i double click the jar, the application does not start](#when-i-double-click-the-jar-the-application-does-not-start)
     * [The application uses up to 1GB of memory which is to much for my system](#the-application-uses-up-to-1gb-of-memory-which-is-to-much-for-my-system)
     * [When i start the companion i get an error 'UnsupportedClassVersionError'](#when-i-start-the-companion-i-get-an-error-unsupportedclassversionerror)
     * [When i start the companion i get a 'javax.net.ssl.SSLException: No PSK available. Unable to resume.'](#when-i-start-the-companion-i-get-a-javaxnetsslsslexception-no-psk-available-unable-to-resume)

## Features

* authentication with discord
* click on the row of the table to copy the pos link to the clipboard
* play sound when territory scanner finishes or is reseted
* use the dropdown to change the sound played when a territory scanner finishes his scan/is reseted by movement
* integrated sound framework with volume slider
* always on top activated by checkbox
* Update information about new versions when the application is started (after discord login)

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

If the application uses to much memory on your system you can also start it in the command line with (replace X.X.X with current version)
java -Xmx256m -jar du-map-companion-X.X.X.jar

If you have problems with starting the application you should try the command line variant, because this also outputs some logging information

## How to use the application

As long as the application is running it will always look for the newest log file in your DU log folder. You have to do nothing special, just use your Territory Scanners as usual.
The results will be transfered to the website as soon as you click the ingame save button of your scanner result.

## How to use the integrated sound framework

The companion app also has an integrated sound framework based on the specifications of [https://github.com/ZarTaen/DU_logfile_audioframework](https://github.com/ZarTaen/DU_logfile_audioframework) which can play sounds based on logfile entries, to activate check the checkbox inside the application or set the corresponding properties listed in the application.properties list below

Other implementations can also be found at
* [https://github.com/ZarTaen/DU_logfile_audioframework](https://github.com/ZarTaen/DU_logfile_audioframework)
* [https://github.com/Dimencia/DU-Audio-Sharp](https://github.com/Dimencia/DU-Audio-Sharp)

Used by amongst others
* [Arch-Orbital-Hud](https://github.com/Archaegeo/Archaegeo-Orbital-Hud)

## Change settings (application.properties)

You can place a file called 'application.properties' in the folder where you run the application, it can be used to change some default settings

### territoryscan.sound

If you want another sound selected by default for scan finished than the default you can create a file called 'application.properties' in the folder where you run the jar file and add one of the following lines

__Possible values:__ None, Pling, Alien Voice

__Default value:__ Pling
 
### skip.to.end

Should only be used if you start the companion before DU or if you have to restart the companion during the game.
It will skip all entries and move directly to the end of the log file. This way all scan and other entries in the log file will be skiped and not send to the server. I use this primarly for debuging because i hacve to restart the companion often and don't want to wait till it reaches the end of the logfile.

__Possible values:__ true, false

__Default value:__ false

### sound.framework.enabled

Here you can change the initial value for the buildin sound framework. If you set this to false it will not interpret any sound entries in the logfile.

__Possible values:__ true, false

__Default value:__ false

### sound.framework.volume

Default base sound for all sounds played by the sound framework.

__Possible values:__ 0-100

__Default value:__ 100

### access

To avoid manually login on each startup you can place your access token here 

__Possible values:__ valid dumap.de discord access token

__Default value:__ empty

## FAQ

### When i double click the jar, the application does not start

If you have problems starting the application with double click try the command line variant which outputs some more logging information

### The application uses up to 1GB of memory which is to much for my system

You can run the application with less memory usage via command line, but this can affect the performance of the application. In local tests 256MB was the lowest value which keeps the application still working.

It can be launched in command line by typing 
java -Xmx256m -jar du-map-companion-X.X.X.jar

Default value if not used is -Xmx1024m

### When i start the companion i get an error 'UnsupportedClassVersionError'

This means you are running the application with a version lower than 11. One reason is ... you only have an older version installed. Another reason might be if you already installed Java 11 or higher that the PATH variable is still referencing the Java <11 Folder.


### When i start the companion i get a 'javax.net.ssl.SSLException: No PSK available. Unable to resume.'

This means you are running the application with an old version of Java 11. There is a bug that should be fixed in 11.0.3+. So please just upgrade your Java Version.