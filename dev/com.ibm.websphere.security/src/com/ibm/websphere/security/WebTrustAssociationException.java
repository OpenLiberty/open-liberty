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
package com.ibm.websphere.security;

/**
 * Thrown to indicate that a error occurred during the processing of
 * isTargetInterceptor of TrustAssociationIntercepter.
 * 
 * @ibm-spi
 */
public class WebTrustAssociationException extends Exception {

    private static final long serialVersionUID = -4068474794305197973L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new WebTrustAssociationException with an empty description string.
     */
    public WebTrustAssociationException() {
        this("No Error Message");
    }

    /**
     * Create a new WebTrustAssociationException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public WebTrustAssociationException(String err) {
        super(err);
    }

}
