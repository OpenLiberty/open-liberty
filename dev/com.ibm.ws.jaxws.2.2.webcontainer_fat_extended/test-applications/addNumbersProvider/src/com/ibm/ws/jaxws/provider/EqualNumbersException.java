/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.provider;

import javax.xml.ws.WebFault;

/*
 * The EqualNumberException tests whether or not @WebFault annotated Exception
 * will properly inherit the message field from its super class, as required by
 * section 3.7 of the JAX-WS 2.2 specification.
 */
@WebFault()
public class EqualNumbersException extends Exception {

    private static final long serialVersionUID = -2844570262814091172L;

    private String info;

    public EqualNumbersException() {

    }

    public EqualNumbersException(String message, String info) {
        super(message);
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

}
