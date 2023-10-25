package com.ibm.example.bytecode;

import java.util.concurrent.Callable;

public class HelloWorldJava8Lambdas {
  public static void printHi() {
    System.out.println("hi");
  }
  
  public static int addThingsStatic(int a, int b) {
    Callable<String> helloLambda = () -> "Hello";
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
