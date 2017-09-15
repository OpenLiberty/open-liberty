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
package com.ibm.ws.jca.internal;

import java.io.Serializable;
import java.util.Properties;

/**
 * Objects of this class are serialized and used to store the information required to recreate an activation
 * spec during recovery
 */
public class ActivationConfig implements Serializable {

    private static final long serialVersionUID = -3812882135080246095L;

    private Properties activationConfigProps = null;

    private final String destinationRef;

    private String authenticationAlias = null;

    private final String applicationName;

    /**
     * @param activationConfigProps
     * @param destinationRef
     * @param authenticationAlias
     */
    public ActivationConfig(Properties activationConfigProps, String destinationRef, String authenticationAlias, String appName) {
        this.activationConfigProps = activationConfigProps;
        this.destinationRef = destinationRef;
        this.authenticationAlias = authenticationAlias;
        this.applicationName = appName;
    }

    /**
     * @return the activationConfigProps
     */
    public Properties getActivationConfigProps() {
        return activationConfigProps;
    }

    /**
     * @return id of a destination
     */
    public String getDestinationRef() {
        return destinationRef;
    }

    /**
     * @return the authenticationAlias
     */
    public String getAuthenticationAlias() {
        return authenticationAlias;
    }

    /**
     * @return the applicationName
     */
    public String getApplicationName() {
        return applicationName;
    }

}
