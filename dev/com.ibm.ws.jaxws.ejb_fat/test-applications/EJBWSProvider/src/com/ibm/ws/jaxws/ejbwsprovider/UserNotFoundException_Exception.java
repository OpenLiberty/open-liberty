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
package com.ibm.ws.jaxws.ejbwsprovider;

import javax.xml.ws.WebFault;

@WebFault(name = "UserNotFoundException", targetNamespace = "http://ejbbasic.jaxws.ws.ibm.com/")
public class UserNotFoundException_Exception extends Exception {

    /**
     * Java type that goes as soapenv:Fault detail element.
     *
     */
    private final UserNotFoundException faultInfo;

    /**
     *
     * @param message
     * @param faultInfo
     */
    public UserNotFoundException_Exception(String message, UserNotFoundException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @param message
     * @param faultInfo
     * @param cause
     */
    public UserNotFoundException_Exception(String message, UserNotFoundException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     *
     * @return
     *         returns fault bean: com.ibm.ws.jaxws.ejbwsprovider.UserNotFoundException
     */
    public UserNotFoundException getFaultInfo() {
        return faultInfo;
    }

}
