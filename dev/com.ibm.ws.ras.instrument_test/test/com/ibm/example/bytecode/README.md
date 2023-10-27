## Source files

The classes in this directory are the source files for the .class files located in the `test data` directory.
If you are adding a new class to this directory you need to manually compile it at the java level it was written to be tested against.

### Generate new class files

```sh
cd com.ibm.ws.ras.instrument_test

# TODO Add JDK 11 to your path

javac -d "test/test data/" test/com/ibm/example/bytecode/HelloWorldJava11.java 