/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.ssl;

import java.util.Properties;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.JSSEProvider;

/**
 * Marker interface for configured SSL subsystem
 */
public interface SSLSupport {

    JSSEHelper getJSSEHelper();

    /**
     * Obtain the default JSSE provider instance.
     *
     * @return JSSEProvider
     */
    JSSEProvider getJSSEProvider();

    /**
     * Obtain the possible JSSE provider for the given name. This will return null
     * if there was no match found.
     *
     * @param providerName
     * @return JSSEProvider
     */
    JSSEProvider getJSSEProvider(String providerName);

    /**
     * Obtain a Liberty SSLSocketFactory.
     *
     * @return SSLSocketFactory
     */
    SSLSocketFactory getSSLSocketFactory();

    /**
     * Obtain a Liberty SSLSocketFactory for a given SSL configuration.
     *
     * @param sslAlias - name of a SSL configuration
     * @return SSLSocketFactory
     * @throws SSLException
     */
    SSLSocketFactory getSSLSocketFactory(String sslAlias) throws SSLException;

    /**
     * Obtain a Liberty SSLSocketFactory for a given set of SSL properties.
     *
     * @param sslProps - properties to create a SSL Socket factory
     * @return SSLSocketFactory
     * @throws SSLException
     */
    SSLSocketFactory getSSLSocketFactory(Properties sslProps) throws SSLException;

}
