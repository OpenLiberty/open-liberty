Instructions on running tests using a different java level are found at:

https://github.com/OpenLiberty/open-liberty/wiki/FAT-tests#running-fat-tests-locally-with-different-java-levels

Search for "Running FAT tests locally with different java levels".

The current instructions are copied, below.

The current default java is at least java 11.  However, running the server at java8 is still allowed.

When testing the feature start buckets, the server must be run using the current default java *AND* using java 8.

---

Running FAT tests locally with different java levels

Normally everything in OpenLiberty runs/builds with JDK 8. If you want
to build and run a FAT on a different java level locally, do the
following:

export JAVA_HOME=/path/to/desired/jdk_home
export PATH=$JAVA_HOME/bin:$PATH
./gradlew --stop
./gradlew :com.ibm.ws.whatever_fat:buildandrun

NOTE: Stopping the gradle daemon (./gradlew --stop) is important,
because it will force a new gradle daemon to be created next time you
run a gradle command which will contain the PATH update.

Alternatively, if you only want the Liberty server to run with the
controlled JDK (i.e. keep the build and the client side junit process
on JDK 8), then instead do: (change the path to CD-Open wlp if running
tests from there)

mkdir /path/to/open-liberty/dev/build.image/wlp/etc/
echo "JAVA_HOME=/path/to/desired/jdk_home" > /path/to/open-liberty/dev/build.image/wlp/etc/server.env
