/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.el.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;

/**
 * Bean used to test the JSF 2.2 Jira http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1092
 * 
 * This Jira makes sure that the exception thrown due to ValueChangeEvent doesn't get wrapped into AbortProcessingException in
 * JSF implementation. Instead it gets propagated as is to the web page, as response. If user invokes xtml corresponds to this bean,
 * then user should be seeing NullPointerException on the response page.
 * 
 */
@ManagedBean(name = "elException")
@SessionScoped
public class ELExceptionBean {

    private String stringVal;

    public void throwException(ValueChangeEvent vce) throws AbortProcessingException {
        throw new NullPointerException("Exception intentionally thrown from the test case");
    }

    public String getStringVal() {
        return stringVal;
    }

    public void setProperty(String stringVal) {
        this.stringVal = stringVal;
    }

}
