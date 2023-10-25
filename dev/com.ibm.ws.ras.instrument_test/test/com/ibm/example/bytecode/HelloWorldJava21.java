package com.ibm.example.bytecode;

public class HelloWorldJava21 {
  public static void printHi() {
    System.out.println("hi");
  }
  
  public static int addThingsStatic(int a, int b) {
    int c = a + b;
    return c;
  }
  
  public int addThings(int a, int b) {
    int c = a + b;
    return c;
  }
  
  public Object instancer(Class blah) throws IllegalAccessException, InstantiationException {
    return blah.newInstance();
  }
}
