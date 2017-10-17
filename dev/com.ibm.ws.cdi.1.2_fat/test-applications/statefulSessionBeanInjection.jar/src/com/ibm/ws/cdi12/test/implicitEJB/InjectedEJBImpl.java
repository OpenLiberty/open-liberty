package com.ibm.ws.cdi12.test.implicitEJB;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Stateful
@Dependent
public class InjectedEJBImpl implements InjectedEJB {

    private static final long serialVersionUID = 1L;

    @Inject
    private InjectedBean2 bean;

    public InjectedEJBImpl() {
        System.out.println("xtor");
    }

    @Override
    public String getData() {
        return "STATE" + bean.increment();
    }

    @Override
    @Remove
    public void removeEJB() {
        System.out.println("REMOVE");
    }

}
