package com.ibm.ws.cdi.extension.apps.xtorException;

import javax.enterprise.context.RequestScoped;

//A dummy bean to make this application CDI enabled.
@RequestScoped
public class DummyBean {

    public boolean iExist() {
        return true;
    }

}
