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

@WebFault(name = "LocalName", targetNamespace = "http://provider.jaxws.ws.ibm.com/")
public class AddNegativesException extends Exception {

    /**
     * Java type that goes as soapenv:Fault detail element.
     *
     */
    private final LocalName faultInfo;

    /**
     *
     * @param message
     * @param faultInfo
     */
    public AddNegativesException(String message, LocalName faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @param message
     * @param faultInfo
     * @param cause
     */
    public AddNegativesException(String message, LocalName faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @return
     *         returns fault bean: com.ibm.ws.jaxws.client.LocalName
     */
    public LocalName getFaultInfo() {
        return faultInfo;
    }

}
