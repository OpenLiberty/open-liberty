package com.ibm.ws.cdi12.test.implicitEJB;

import java.io.Serializable;

public class InjectedBean2 implements Serializable {

    private static final long serialVersionUID = 1L;

    private int data = 0;

    public int increment() {
        return ++data;
    }
}
