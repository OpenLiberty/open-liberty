/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.html5.faces40;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIViewParameter;
import javax.inject.Named;

/**
 *
 */
@Named("testBean")
@ApplicationScoped
public class testBean {

    private String submittedValue;

    /**
     * @return the submittedValue
     */
    public String getSubmittedValue() {
        return submittedValue;
    }

    /**
     * @param submittedValue the submittedValue to set
     */
    public void setSubmittedValue(String submittedValue) {
        this.submittedValue = submittedValue;
    }

    public void testEditableValueHoldergetSubmittedValue() {
        EditableValueHolder holder = new UIViewParameter();
        holder.setSubmittedValue(Boolean.TRUE);

        if (!((Boolean) holder.getSubmittedValue())) {
            System.out.println("getSubmittedValue FAIL");
            submittedValue = "getSubmittedValue FAIL";
        } else
            submittedValue = "getSubmittedValue PASS";

    }

}
