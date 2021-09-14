The io.openliberty.java.internal_fat FAT runs a sanity check to make sure we are running it against the expected version of Java.  To do this, the typical Open Liberty FAT test setup is used with a WAR file application for testing.  
The WAR file application is compiled at the Java version being tested using functionality specific to that version of Java.

This FAT is primarily used for testing versions of Java that are "Early Access" which usually means it is prior to full gradle support.  So to build this test, it requires a unique gradle setup and that requires additional build files that would be fairly unwieldy to all store in this FAT project.  
The main test code source is included in this FAT project, in the src-reference directory, but it is helpful to have a completely separate project to be able to cleanly build the WAR file application at the necessary Java level.  

Please note, changes to the WAR file application source code inside this FAT will not be honored.  The WAR file application is built externally and uploaded as a binary.

The project for building the WAR file application for this FAT is found in the open-liberty-misc repository, located at https://github.com/OpenLiberty/open-liberty-misc/tree/main/io.openliberty.java.internal_fat_17
