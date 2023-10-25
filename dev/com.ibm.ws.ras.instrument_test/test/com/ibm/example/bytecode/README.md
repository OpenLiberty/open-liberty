## Source files

The classes compiled in this package are not meant to be used for testing.
Instead, they are placeholder .java files that we need to manually compile at different java levels.
The compiled classes will then be put into the `test data` directory.

### Generate new class files

```sh
cd com.ibm.ws.ras.instrument_test

# TODO Add JDK 11 to your path

javac -d "test/test data/" test/com/ibm/example/bytecode/HelloWorldJava11.java 