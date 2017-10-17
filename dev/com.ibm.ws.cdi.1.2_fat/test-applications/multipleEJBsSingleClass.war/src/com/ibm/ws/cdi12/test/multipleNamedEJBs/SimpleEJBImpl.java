package com.ibm.ws.cdi12.test.multipleNamedEJBs;

import javax.inject.Inject;

public class SimpleEJBImpl implements SimpleEJBLocalInterface1, SimpleEJBLocalInterface2 {

    /**
     * Deliberately store state from both interfaces in the same place
     * to test that they don't interfere with each other
     * when injected using different bean names.
     */
    private SimpleManagedBean bean;

    @Inject
    public SimpleEJBImpl(SimpleManagedBean injected) {
        this.bean = injected;
    }

    public SimpleEJBImpl() {
        throw new RuntimeException("Wrong Constructor called: SimpleEJBImpl()");
    }

    @Override
    public String getData1() {
        return bean.getData();
    }

    @Override
    public void setData1(String data) {
        bean.setData(data);
    }

    @Override
    public String getData2() {
        return bean.getData();
    }

    @Override
    public void setData2(String data) {
        bean.setData(data);
    }
}
