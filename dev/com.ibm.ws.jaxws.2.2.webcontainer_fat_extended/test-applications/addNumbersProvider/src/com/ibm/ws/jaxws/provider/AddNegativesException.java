/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.provider;

import javax.xml.ws.WebFault;

@WebFault(name = "LocalName")
public class AddNegativesException extends Exception {

    private static final long serialVersionUID = 1L;
    private String message = null;

    public AddNegativesException() {

    }

    public AddNegativesException(String message) {
        this.message = message;
    }

    public String getInfo() {
        return message;
    }
}
