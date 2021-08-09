/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.commandscript.beans;

import java.io.Serializable;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

@Named
@RequestScoped
public class CommandScriptBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private String output;
    private String inputValue;

    public CommandScriptBean() {}

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void submitForm() {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("submitForm called"));
    }

    public void testCommandScript(String output) {
        setOutput(output);
    }

    public void testCommandScriptParam() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        String param1 = params.get("param1");
        String param2 = params.get("param2");
        setOutput(param1 + " " + param2);
    }

    public void performAction(ActionEvent event) throws AbortProcessingException {
        System.out.println("performAction called");
    }

}
