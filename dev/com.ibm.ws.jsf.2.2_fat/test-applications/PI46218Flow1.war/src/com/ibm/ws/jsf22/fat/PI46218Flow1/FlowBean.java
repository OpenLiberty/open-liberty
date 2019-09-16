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
package com.ibm.ws.jsf22.fat.PI46218Flow1;

import java.io.Serializable;

import javax.faces.flow.FlowScoped;
import javax.inject.Named;

@Named("flowBean")
@FlowScoped("test-flow-1")
public class FlowBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private String _message;

    public String getMessage() {
        return _message;
    }
    
    public void setMessage(String message) {
        _message = message;
    }
}
