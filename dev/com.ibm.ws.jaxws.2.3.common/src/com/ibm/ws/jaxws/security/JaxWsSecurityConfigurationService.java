/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
