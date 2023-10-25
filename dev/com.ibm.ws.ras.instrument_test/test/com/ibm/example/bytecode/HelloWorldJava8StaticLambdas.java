package com.ibm.example.bytecode;

import java.util.concurrent.Callable;

public class HelloWorldJava8StaticLambdas {
  public static final Callable<String> helloLambda = () -> "Hello";
  
  public static final Converter<String> STRING_CONVERTER;
  
  public static void printHi() {
    System.out.println("hi");
  }
  
  static {
    STRING_CONVERTER = (v -> v);
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
  
  public static interface Converter<T> {
    T convert(String param1String);
  }
}
