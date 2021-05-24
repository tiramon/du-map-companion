#DuMapCompanion

## Requirements

To run the DUMapCompanion you have to use atleast Java 11.

## How to start the companion

If your Java is installed correctly double clicking the JAR should be enough.

To get some log output start the command line in the folder where the jar is lying and type (replace X.X.X with current version)
java -jar du-map-companion-X.X.X.jar

## FAQ

### When i start the compantion i get an error 'UnsupportedClassVersionError'

This means you are running the application with a version lower than 11. One reason is ... you only have an older version installed. Another reason might be if you already installed Java 11 or higher that the PATH variable is still referencing the Java <11 Folder.