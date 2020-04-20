package com.ibm.ws.cdi.extension.spi.test.app;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class DummyBean {
    
    public boolean iExist() {
        return true;
    }

}
