/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package web.lpsclient;

import javax.xml.ws.WebFault;

@WebFault(name = "Exception", targetNamespace = "http://lpsservice.web/")
public class Exception_Exception
    extends java.lang.Exception
{
	private static final long serialVersionUID = 4180513203254663360L;
	/**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private web.lpsclient.Exception faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public Exception_Exception(String message, web.lpsclient.Exception faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public Exception_Exception(String message, web.lpsclient.Exception faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: web.lpsclient.Exception
     */
    public web.lpsclient.Exception getFaultInfo() {
        return faultInfo;
    }

}
