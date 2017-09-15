/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.security;

import org.apache.cxf.transport.Conduit;

import com.ibm.websphere.ras.ProtectedString;

/**
 * Using the class to configure the security related stuff for web services
 */
public interface JaxWsSecurityConfigurationService {
    /**
     * Configure the Basic Authentication
     * 
     * @param conduit
     * @param userName
     * @param password
     */
    void configBasicAuth(Conduit conduit, String userName, ProtectedString password);

    /**
     * Configure the Client SSL configuration.
     * 
     * @param Conduit
     * @param sslRef
     * @param certAlias
     */
    void configClientSSL(Conduit Conduit, String sslRef, String certAlias);

}
