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

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 *
 */
@ManagedBean(name = "testPassthroughTextareaBean")
@RequestScoped
public class TestPassthroughTextareaBean implements Serializable {
    /**  */
    private static final long serialVersionUID = 1L;

    private String phone;
    private String complainText;

    /**
     * @return the phone
     */
    public String getPhone() {
        return phone;
    }

    /**
     * @param phone the phone to set
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * @return the complainText
     */
    public String getComplainText() {
        return complainText;
    }

    /**
     * @param complainText the complainText to set
     */
    public void setComplainText(String complainText) {
        this.complainText = complainText;
    }

    public void submit()
    {
        System.out.println("phone: " + phone);
        System.out.println("complainText: " + complainText);
    }
}
