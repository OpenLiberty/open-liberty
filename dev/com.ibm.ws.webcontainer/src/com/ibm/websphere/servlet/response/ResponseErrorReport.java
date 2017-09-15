/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.response;


/**
 * @ibm-api
 * 
 */
public class ResponseErrorReport extends com.ibm.websphere.servlet.error.ServletErrorReport {
    /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3978709506010395441L;

	public ResponseErrorReport() {
        super();
    }
    /**
     * Constructs a new ResponseErrorReport with the specified message.
     *
     * @param message Message of exception
     */

    public ResponseErrorReport(String message) {
        super(message);
    }

    /**
     * Constructs a new ResponseErrorReport with the specified message
     * and root cause.
     *
     * @param message Message of exception
     * @param rootCause Exception that caused this exception to be raised
     */

    public ResponseErrorReport(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    /**
     * Constructs a new WebAppErrorReport with the specified message
     * and root cause.
     *
     * @param rootCause Exception that caused this exception to be raised
     */

    public ResponseErrorReport(Throwable rootCause) {
        super(rootCause);
    }

    /**
     * Set the error code of the response.
     */
    public void setErrorCode(int sc) {
        super.setErrorCode(sc);
    }

    /**
     * Set the name of the target Servlet.
     */
    public void setTargetServletName(String servletName) {
        super.setTargetServletName(servletName);
    }
}
