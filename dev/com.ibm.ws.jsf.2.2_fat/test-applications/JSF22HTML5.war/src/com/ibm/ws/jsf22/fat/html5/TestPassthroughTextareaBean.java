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
