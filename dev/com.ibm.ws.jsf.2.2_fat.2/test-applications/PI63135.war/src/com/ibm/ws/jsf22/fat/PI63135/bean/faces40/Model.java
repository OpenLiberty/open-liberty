/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.PI63135.bean.faces40;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

@Named
@SessionScoped
public class Model implements Serializable {
    private static final long serialVersionUID = 1L;

    private String message = null;
    private String status = null;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String action() {
        if (message == null) {
            status = "PI63135: Message is NULL!";
        } else if (message.isEmpty()) {
            status = "PI63135: Message is an empty string!";
        } else {
            status = "PI63135: Message is --> " + message;
        }

        return "messageStatus?faces-redirect=true";
    }

    public String getStatus() {
        return status;
    }
}
