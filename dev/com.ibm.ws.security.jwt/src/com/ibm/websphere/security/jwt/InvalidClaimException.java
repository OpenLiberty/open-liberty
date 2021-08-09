/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.jwt;

public class InvalidClaimException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = -3465772535531570128L;

    public InvalidClaimException(String message) {
        super(message);
    }

    public InvalidClaimException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
