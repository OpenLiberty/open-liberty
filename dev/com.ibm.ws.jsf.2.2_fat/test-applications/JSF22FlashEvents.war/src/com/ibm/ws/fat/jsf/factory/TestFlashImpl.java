package com.ibm.ws.fat.jsf.factory;

import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.FlashWrapper;

/*
 * This is a custom Flash implementation which was created by the TestFlashFactory
 */

public class TestFlashImpl extends FlashWrapper implements FacesWrapper<Flash> {

    private final Flash parent;

    public TestFlashImpl(Flash parent) {
        this.parent = parent;
        getEC().log("TestFlashImpl constructor");

    }

    @Override
    public Flash getWrapped() {
        return parent;
    }

    private ExternalContext getEC() {
        return FacesContext.getCurrentInstance().getExternalContext();
    }
}