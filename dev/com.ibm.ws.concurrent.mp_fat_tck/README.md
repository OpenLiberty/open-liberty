# How to test TCK changes locally

## 1. Clone and build MP TCK locally

1. In a dir outside of OpenLiberty, clone the MP Concurrency repo.  Run:
    `git clone git@github.com:eclipse/microprofile-concurrency.git`
    
2. Build a snapshot of the TCK locally. Run:
    `mvn clean install`
    
## 2. Run the MP TCK in the OpenLiberty repo as a FAT

1. Run the FAT normally:
    `./gradlew com.ibm.ws.concurrent.mp_fat_tck:buildandrun`
    
## 3. Iterate making changes and running tests (i.e. "Inner dev loop")

1. In the MP Concurrency repo, from the `tck` dir, run:
    `mvn install`
    
2. Run the FAT normally:
    `./gradlew com.ibm.ws.concurrent.mp_fat_tck:buildandrun`