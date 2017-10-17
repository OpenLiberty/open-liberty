package com.ibm.ws.cdi12.test.implicitEJB;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

@SessionScoped
public class InjectedBean1 implements Serializable {

    private static final long serialVersionUID = 1L;

    //TODO currently using @Inject doesn't behave the same as using @EJB
    @Inject
    private InjectedEJB ejb;

    public String getData() {
        return ejb.getData();
    }

    public void removeEJB() {
        ejb.removeEJB();
    }

}
