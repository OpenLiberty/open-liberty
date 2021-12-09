# How to test TCK changes locally

## 1. Clone and build MP TCK locally

1. In a dir outside of OpenLiberty, clone the MP Context Propagation repo.  Run:
    `git clone git@github.com:eclipse/microprofile-context-propagation.git`
    
2. Build a snapshot of the TCK locally. Run:
    `mvn clean install`

## 2. You might need to edit the following file (see comments within) to point at snapshot that you built,
     /com.ibm.ws.concurrent.mp.1.3_fat_tck/publish/tckRunner/tck/pom.xml
    
## 3. Run the MP TCK in the OpenLiberty repo as a FAT

1. Run the FAT normally:
    `./gradlew com.ibm.ws.concurrent.mp.1.3_fat_tck:buildandrun`
    
## 4. Iterate making changes and running tests (i.e. "Inner dev loop")

1. In the MP Context Propagation repo, from the `tck` dir, run:
    `mvn install`
    
2. Run the FAT normally:
    `./gradlew com.ibm.ws.concurrent.mp.1.3_fat_tck:buildandrun`

# Creating a TCK test bucket for a new version

1. Copy from the previous version tck project

2. Delete the old build and generated folders and use :clean to avoid the following error,
   java.lang.ClassNotFoundException: com.ibm.ws.concurrent.mp.v#_#.fat.tck.FATSuite,com.ibm.ws.concurrent.mp.v#_#.fat.tck.FATSuite

3. Replace old version number with new version number in test bucket files.

4. Update MPContextPropagationTCKLauncher and /publish/tckRunner/tck/pom.xml to run against
   local snapshot of new release that is in development.