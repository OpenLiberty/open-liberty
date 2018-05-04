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
package com.ibm.ws.jsf23.fat.evalscripts.beans;

import java.io.Serializable;
import java.util.Arrays;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

@Named
@RequestScoped
public class EvalScriptsBean implements Serializable {
    private static final long serialVersionUID = 1L;

    private String inputValue;

    public EvalScriptsBean() {}

    public void eval() {
        FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts().add("document.getElementById('outputText1').innerHTML='Test Passed!'");
    }

    public void evalList() {
        java.util.List<String> javascriptList = Arrays.asList("document.getElementById('outputText1').innerHTML='Text Value 1'",
                                                              "document.getElementById('outputText2').innerHTML='Text Value 2'",
                                                              "document.getElementById('outputText3').innerHTML='Text Value 3'");
        FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts().addAll(javascriptList);
    }

    public void evalFunction() {
        FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts().add("testFunction()");
    }

    public void submitForm() {
        FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts().add("document.getElementById('form1:outputText1').innerHTML='Test Passed!'");
    }

    public String getInputValue() {
        return inputValue;
    }

    public void setInputValue(String inputValue) {
        this.inputValue = inputValue;
    }
}
