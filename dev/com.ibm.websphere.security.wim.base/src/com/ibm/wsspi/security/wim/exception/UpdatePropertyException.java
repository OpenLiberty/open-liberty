/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
package com.ibm.wsspi.security.wim.exception;

public class UpdatePropertyException extends WIMApplicationException {

    private static final long serialVersionUID = 6449986048740351789L;

    /**
     *
     */
    public UpdatePropertyException() {
        super();
    }

    /**
     * @param message
     */
    public UpdatePropertyException(String key, String message) {
        super(key, message);
    }

    /**
     * @param cause
     */
    public UpdatePropertyException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public UpdatePropertyException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }
}
