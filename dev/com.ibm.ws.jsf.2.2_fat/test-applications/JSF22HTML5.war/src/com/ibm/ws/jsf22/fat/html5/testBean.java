/*
 * Copyright (c) 2015, 2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.html5;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIViewParameter;

/**
 *
 */
@ManagedBean(name = "testBean")
@ApplicationScoped
public class testBean {

    public String submittedValue;

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
        }
        else
            submittedValue = "getSubmittedValue PASS";

    }

}