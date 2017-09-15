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

import java.util.Map;

import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * <p>
 * The <code>WSMappingCallbackHandler</code> returns either (i) a Hashmap callback and
 * a reference of a <code>ManagedConnectionFactory</code>.
 * </p>
 * 
 * @author IBM Corporation
 * @version WebSphere V 6.0
 * @since WebSphere V 6.0
 * 
 * @ibm-spi
 */
public class WSMappingCallbackHandler implements CallbackHandler {

    private final Map properties;
    private final ManagedConnectionFactory managedConnectionFactory;

    /**
     * <p>
     * Construct a <code>WSMappingCallbackHandler</code> object with a resource reference binding properties
     * and the corresponding authentication data and the target <code>ManagedConnectionFactory</code>.
     * </p>
     * 
     * @param properties
     * @param managedConnectionFactory
     */
    public WSMappingCallbackHandler(Map properties, ManagedConnectionFactory managedConnectionFactory) {
        this.properties = properties;
        this.managedConnectionFactory = managedConnectionFactory;
    }

    /**
     * <p>
     * Return a properties object and a reference of the target
     * <code>ManagedConnectionFactory</code> via <code>Callback[]</code>.
     */
    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSManagedConnectionFactoryCallback) {
                ((WSManagedConnectionFactoryCallback) callback).setManagedConnectionFactory(managedConnectionFactory);
            } else if (callback instanceof WSMappingPropertiesCallback) {
                ((WSMappingPropertiesCallback) callback).setProperties(properties);
            } else {
                // TODO: Issue a warning with translated message
                // Use translated message for the the UnsupportedCallbackException
                throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
            }
        }
    }

}
