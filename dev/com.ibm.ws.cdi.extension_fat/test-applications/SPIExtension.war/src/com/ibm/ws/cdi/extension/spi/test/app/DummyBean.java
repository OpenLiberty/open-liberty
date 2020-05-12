package com.ibm.ws.cdi.extension.spi.test.app;

import javax.enterprise.context.RequestScoped;

//A dummy bean to make this application CDI enabled.
@RequestScoped
public class DummyBean {

    public boolean iExist() {
        return true;
    }

}
