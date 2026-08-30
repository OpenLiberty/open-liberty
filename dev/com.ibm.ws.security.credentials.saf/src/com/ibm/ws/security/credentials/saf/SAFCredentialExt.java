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
package com.ibm.ws.security.credentials.saf;

import javax.security.auth.Subject;

/**
 * Extension of the SAFCredential SPI interface, for internal bundles to use.
 */
public interface SAFCredentialExt extends com.ibm.wsspi.security.credentials.saf.SAFCredential {

    /**
     * Cache the J2C subject associated with this SAFCredential.
     *
     * @param j2cSubject The J2C Subject.
     */
    public void setJ2CSubject(Subject j2cSubject);

    /**
     * Retrieve the cached J2C subject associated with this SAFCredential.
     *
     * @return The J2C Subject.
     */
    public Subject getJ2CSubject();
}
