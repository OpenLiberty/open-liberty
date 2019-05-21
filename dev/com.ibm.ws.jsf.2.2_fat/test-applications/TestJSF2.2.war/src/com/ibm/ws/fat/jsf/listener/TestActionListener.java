package com.ibm.ws.fat.jsf.listener;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

public class TestActionListener implements ActionListener {

    @Override
    public void processAction(ActionEvent event) throws AbortProcessingException {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.log("TestActionListener.processAction()");
    }
}