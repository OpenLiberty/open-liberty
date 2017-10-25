package com.ibm.ws.cdi12.test.multipleNamedEJBs;

import javax.annotation.ManagedBean;

@ManagedBean
public class SimpleManagedBean {

    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
