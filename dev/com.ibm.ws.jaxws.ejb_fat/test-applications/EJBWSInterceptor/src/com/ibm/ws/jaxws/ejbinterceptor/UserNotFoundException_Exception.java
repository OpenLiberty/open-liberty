/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.ejbinterceptor;

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
