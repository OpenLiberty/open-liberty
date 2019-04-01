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
package com.ibm.ws.jaxws.client;

import javax.xml.ws.WebFault;

@WebFault(name = "AddNumbersException", targetNamespace = "http://provider.jaxws.ws.ibm.com/")
public class AddNumbersException_Exception extends Exception {

    /**
     * Java type that goes as soapenv:Fault detail element.
     *
     */
    private final AddNumbersException faultInfo;

    /**
     *
     * @param message
     * @param faultInfo
     */
    public AddNumbersException_Exception(String message, AddNumbersException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @param message
     * @param faultInfo
     * @param cause
     */
    public AddNumbersException_Exception(String message, AddNumbersException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @return
     *         returns fault bean: com.ibm.ws.jaxws.client.AddNumbersException
     */
    public AddNumbersException getFaultInfo() {
        return faultInfo;
    }

}
