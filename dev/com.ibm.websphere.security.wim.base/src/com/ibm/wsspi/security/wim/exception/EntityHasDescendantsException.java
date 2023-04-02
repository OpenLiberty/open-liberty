/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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

public class EntityHasDescendantsException extends WIMApplicationException {

    private static final long serialVersionUID = -4407138116448494730L;

    /**
     *
     */
    public EntityHasDescendantsException() {
        super();
    }

    /**
     * @param message
     */
    public EntityHasDescendantsException(String key, String message) {
        super(key, message);
    }

    /**
     * @param cause
     */
    public EntityHasDescendantsException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public EntityHasDescendantsException(String key, String message, Throwable cause) {
        super(key, message, cause);
    }

}
