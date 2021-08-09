/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.auth.callback;

import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.callback.Callback;

/**
 * <p>
 * The <code>WSManagedConnectionFactoryCallback</code> allows a reference of the target
 * <code>ManagedConnectionFactory</code> to be collected by
 * <code>WSMapingCallbackHandler</code> and pass it to the
 * <code>WSPrincipalMappingLoginModule</code>.
 * </p>
 * 
 * @author IBM Corporation
 * @version WebSphere V 6.0
 * 
 * @ibm-spi
 */
public class WSManagedConnectionFactoryCallback implements Callback {

    private final String hint;
    private ManagedConnectionFactory managedConnectionFactory;

    /**
     * <p>
     * Construct a <code>WSManagedConnectionFactoryCallback</code> object with a usage hint.
     * </p>
     * 
     * @param hint The usage hint.
     */
    public WSManagedConnectionFactoryCallback(String hint) {
        this.hint = hint;
    }

    /**
     * <p>
     * Set the ManagedConnectionFactory reference.
     * </p>
     * 
     * @param managedConnectionFactory The ManagedConnectionFactory.
     */
    public void setManagedConnectionFactory(ManagedConnectionFactory managedConnectionFactory) {
        this.managedConnectionFactory = managedConnectionFactory;
    }

    /**
     * <p>
     * Return the ManagedConnectionFactory.
     * </p>
     * 
     * @return The ManagedConnectionFactory.
     */
    public ManagedConnectionFactory getManagedConnectionFacotry() {
        return managedConnectionFactory;
    }

}
