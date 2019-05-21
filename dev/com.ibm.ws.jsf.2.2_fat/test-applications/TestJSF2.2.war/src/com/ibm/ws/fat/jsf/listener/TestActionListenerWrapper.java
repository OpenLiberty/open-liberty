package com.ibm.ws.fat.jsf.listener;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionListener;
import javax.faces.event.ActionListenerWrapper;

public class TestActionListenerWrapper extends ActionListenerWrapper {

    @Override
    public ActionListener getWrapped() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.log("TestActionListenerWrapper.getWrapped()");
        return new TestActionListener();
    }
}
