#How to build libTest.so

#These instructions were executed ona RedHat release 6 64 bit machine.

#Use an IBM java, for example, WAS was installed, so that could be used
export $PATH=/opt/WAS/java/bin:$PATH

#make a directory to work in, e.g 
mkdir jca_fat_embeddedrar

#create a test program named Test.java with this code
  public class Test {
    public static void main(String[] args) {
      System.loadLibrary("Test");
      System.out.println("loaded Test native lib");
    }
  }

#put Test.java in the directory

javac Test.java

mkdir fat/jca/dll
#put TestWrapper.c in this directory
#put TestWrapper.java in this directory

cd fat/jcc/dll
gcc -I/opt/WAS/java/include -I/opt/WAS/java/include/genunix -fPIC -c TestWrapper.c
gcc -shared -nostdlib -Wl,-Bstatic,-soname,libTestWrapper.so.1 -o libTestWrapper.so.1.0 TestWrapper.o
mv libTestWrapper.so.1.0 libTest.so
ldd libTest.so
#  should show statically linked

javac TestWrapper.java

# Run Test to check the native library can be loaded
cd ../../..
java -Djava.library.path=fat/jca/dll Test
