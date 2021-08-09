/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.util.wsoc;

/**
 * Indicates that an operation performed on a Web Socket instance failed.
 * 
 * @author Tim Burns
 */
public class WsocTestException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Calls <code>super()</code>
     */
    public WsocTestException() {
        super();
    }

    /**
     * Calls <code>super(message)</code>
     * 
     * @param message A message explaining the cause of the problem
     */
    public WsocTestException(String message) {
        super(message);
    }

    /**
     * Calls <code>super(cause)</code>
     * 
     * @param cause explains the cause of the problem
     */
    public WsocTestException(Throwable cause) {
        super(cause);
    }

    /**
     * Calls <code>super(message, cause)</code>
     * 
     * @param message explains the cause of the problem
     * @param cause explains the cause of the problem
     */
    public WsocTestException(String message, Throwable cause) {
        super(message, cause);
    }

}
