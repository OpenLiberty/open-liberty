package com.ibm.ws.cdi12.test.implicitEJB;

import java.io.Serializable;

public interface InjectedEJB extends Serializable {

    public String getData();

    public void removeEJB();

}
