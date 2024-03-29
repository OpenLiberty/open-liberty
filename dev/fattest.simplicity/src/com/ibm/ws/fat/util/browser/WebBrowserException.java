/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.fat.util.browser;

/**
 * Indicates that an operation performed on a WebBrowser instance failed.
 * 
 * @author Tim Burns
 */
public class WebBrowserException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Calls <code>super()</code>
     */
    public WebBrowserException() {
        super();
    }

    /**
     * Calls <code>super(message)</code>
     * 
     * @param message A message explaining the cause of the problem
     */
    public WebBrowserException(String message) {
        super(message);
    }

    /**
     * Calls <code>super(cause)</code>
     * 
     * @param cause explains the cause of the problem
     */
    public WebBrowserException(Throwable cause) {
        super(cause);
    }

    /**
     * Calls <code>super(message, cause)</code>
     * 
     * @param message explains the cause of the problem
     * @param cause explains the cause of the problem
     */
    public WebBrowserException(String message, Throwable cause) {
        super(message, cause);
    }

}
