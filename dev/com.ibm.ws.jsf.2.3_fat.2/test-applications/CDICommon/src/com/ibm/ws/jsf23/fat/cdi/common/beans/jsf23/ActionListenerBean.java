/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.cdi.common.beans.jsf23;

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
