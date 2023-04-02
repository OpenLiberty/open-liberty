/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.el.beans.faces40;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Named;

/**
 * Bean used to test the JSF 2.2 Jira http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1092
 *
 * This Jira makes sure that the exception thrown due to ValueChangeEvent doesn't get wrapped into AbortProcessingException in
 * JSF implementation. Instead it gets propagated as is to the web page, as response. If user invokes xtml corresponds to this bean,
 * then user should be seeing NullPointerException on the response page.
 *
 */
@Named("elException")
@SessionScoped
public class ELExceptionBean implements Serializable {

    private static final long serialVersionUID = 1L;

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
