/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.cdi;

/**
 *
 */
public class ClaimTypeException extends Exception {

    /**
     * @param message
     */
    public ClaimTypeException(String message) {
        super(message);
    }

    /**  */
    private static final long serialVersionUID = -2995319466244177782L;

}
