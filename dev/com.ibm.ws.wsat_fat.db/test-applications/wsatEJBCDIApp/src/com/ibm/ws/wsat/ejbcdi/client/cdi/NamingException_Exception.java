/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.ejbcdi.client.cdi;

import javax.xml.ws.WebFault;

@WebFault(name = "NamingException", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/")
public class NamingException_Exception
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private NamingException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public NamingException_Exception(String message, NamingException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public NamingException_Exception(String message, NamingException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: com.ibm.ws.wsat.ejbcdi.client.cdi.NamingException
     */
    public NamingException getFaultInfo() {
        return faultInfo;
    }

}
