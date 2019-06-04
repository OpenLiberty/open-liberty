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
package com.ibm.ws.jsf22.fat.cdicommon.beans;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 *
 */
@ManagedBean(name = "actionListenerBean")
@SessionScoped
public class ActionListenerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private String data = ":ActionListenerBean:";
    private final String button = "Hit me to test an action listener!";

    public void setData(String newData) {
        this.data += newData;
    }

    public String getData() {
        return data;
    }

    public String nextPage() {
        return "ActionListenerEnd";
    }

    public String getButton() {
        return button;
    }

}
