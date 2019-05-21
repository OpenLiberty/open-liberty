/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.fat.jsf.html5;

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