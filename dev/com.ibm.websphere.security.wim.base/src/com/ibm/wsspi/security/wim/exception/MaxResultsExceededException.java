/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.exception;

public class MaxResultsExceededException extends WIMApplicationException {

    private static final long serialVersionUID = -246640993723598147L;

    /**
     *
     */
    public MaxResultsExceededException() {
        super();
    }

    /**
     * @param message
     */
    public MaxResultsExceededException(String key, String message) {
        super(key, message);
    }

    /**
     * @param cause
     */
    public MaxResultsExceededException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public MaxResultsExceededException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}
