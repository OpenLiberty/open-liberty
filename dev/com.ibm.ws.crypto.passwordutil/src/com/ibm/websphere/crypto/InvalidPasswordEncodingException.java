/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
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
package com.ibm.websphere.crypto;

/**
 * Exception thrown when a provided password for encoding is invalid.
 */
public class InvalidPasswordEncodingException extends Exception {
    private static final long serialVersionUID = 1868689009769673329L;

    /**
     * Constructor.
     */
    public InvalidPasswordEncodingException() {
        super();
    }

}
