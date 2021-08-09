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
 * validateEstablishedTrust of TrustAssociationIntercepter.
 * 
 * @ibm-spi
 */
public class WebTrustAssociationFailedException extends WebTrustAssociationException {

    private static final long serialVersionUID = -2991475097046505440L; //@vj1: Take versioning into account if incompatible changes are made to this class

    /**
     * Create a new WebTrustAssociationFailedException with an empty description string.
     */
    public WebTrustAssociationFailedException() {
        this("No message");
    }

    /**
     * Create a new WebTrustAssociationFailedException with the associated string description.
     * 
     * @param message the String describing the exception.
     */
    public WebTrustAssociationFailedException(String msg) {
        super(msg);
    }
}
