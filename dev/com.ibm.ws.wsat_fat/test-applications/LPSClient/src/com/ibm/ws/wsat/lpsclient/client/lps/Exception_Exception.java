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
package com.ibm.ws.wsat.lpsclient.client.lps;

import javax.xml.ws.WebFault;

@WebFault(name = "Exception", targetNamespace = "http://server.lpsserver.wsat.ws.ibm.com/")
public class Exception_Exception
    extends java.lang.Exception
{
	private static final long serialVersionUID = 4180513203254663360L;
	/**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private com.ibm.ws.wsat.lpsclient.client.lps.Exception faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public Exception_Exception(String message, com.ibm.ws.wsat.lpsclient.client.lps.Exception faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public Exception_Exception(String message, com.ibm.ws.wsat.lpsclient.client.lps.Exception faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: com.ibm.ws.wsat.lpsclient.client.lps.Exception
     */
    public com.ibm.ws.wsat.lpsclient.client.lps.Exception getFaultInfo() {
        return faultInfo;
    }

}
