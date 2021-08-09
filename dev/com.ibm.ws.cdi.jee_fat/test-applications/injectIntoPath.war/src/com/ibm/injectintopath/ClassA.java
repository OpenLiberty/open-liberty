package com.ibm.injectintopath;

import javax.enterprise.context.Dependent;

@Dependent
public class ClassA {

    public ClassA() {}

    public String message() {
        return "Hello World!";
    }
}
