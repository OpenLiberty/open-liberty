package com.ibm.ws.fat.jsf.factory;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.context.FlashFactory;

/*
 * This is a custom FlashFactory which is new to JSF 2.2
 * The getFlash method will return a custom Flash implementation.
 */

public class TestFlashFactory extends FlashFactory {

    private final FlashFactory parent;

    public TestFlashFactory(FlashFactory parent) {
        this.parent = parent;
    }

    @Override
    public Flash getFlash(boolean create) {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.log("TestFlashFactory.getFlash");

        return new TestFlashImpl(getWrapped().getFlash(create));
    }

    @Override
    public FlashFactory getWrapped() {
        return parent;
    }
}