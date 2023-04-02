/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.cdicommon.beans;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 */
@ManagedBean(name = "navigationHandlerBean")
@SessionScoped
public class NavigationHandlerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String data = ":NavigationHandlerBean:";
    private final String button = "Hit me to test a navigation handler!";

    public void setData(String newData) {
        this.data += newData;
    }

    public String getData() {
        return data;
    }

    public String nextPage() {
        return "NavigationHandlerFail";
    }

    public String getButton() {
        return button;
    }

}
