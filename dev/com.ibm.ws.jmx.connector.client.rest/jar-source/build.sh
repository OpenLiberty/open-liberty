#!/bin/bash

# The two classes in this jar implement a different interface for Java 8
# and Java 9, due to the way the interface was added to the JDK.
# As the Java 8 interface doesn't exist in a Java 9+ JDK, it is tricky to
# build these classes in the Liberty build. The solution is to build the
# classes locally, and make them available as an external dependency via
# DHE. This is a helper script to compile the classes and create the jar
# ready for upload.
# 


mkdir classes
$JAVA8_HOME/bin/javac -cp ../src -d classes com/ibm/ws/jmx/Java8HelperImpl.java 
$JAVA9_HOME/bin/javac -target 8 -source 8 -cp ../src -d classes com/ibm/ws/jmx/Java9HelperImpl.java
# The interface class gets compiled, but it shouldn't go into the final jar
rm -r classes/com/ibm/ws/jmx/connector
rm serialization-helper.jar
cd classes
$JAVA9_HOME/bin/jar -cf ../serialization-helper.jar *
cd ..
rm -r classes
