/*
 * Copyright (c)  2016  IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.PI63135;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
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
        }
        else if (message.isEmpty()) {
            status = "PI63135: Message is an empty string!";
        }
        else {
            status = "PI63135: Message is --> " + message;
        }
        
        return "messageStatus?faces-redirect=true";
    }
    
    public String getStatus() {
        return status;
    }
}
