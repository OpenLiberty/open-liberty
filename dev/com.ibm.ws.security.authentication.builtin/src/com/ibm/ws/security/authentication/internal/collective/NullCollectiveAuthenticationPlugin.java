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
package com.ibm.ws.security.authentication.internal.collective;

import java.security.cert.X509Certificate;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.collective.CollectiveAuthenticationPlugin;

/**
 * Default CollectiveAuthenticationPlugin.
 * <p>
 * Nothing is recognized, nothing is authenticated.
 * <p>
 * This is the default implementation, anything else should take priority.
 */
@Component(service = CollectiveAuthenticationPlugin.class,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "name=NullCollectiveAuthenticationPlugin", "service.ranking:Integer=-1" })
public class NullCollectiveAuthenticationPlugin implements CollectiveAuthenticationPlugin {

    @Activate
    protected void activate() {}

    @Deactivate
    protected void deactivate() {}

    /**
     * {@inheritDoc} <p>
     * Always returns false
     */
    @Override
    public boolean isCollectiveCertificateChain(X509Certificate[] certChain) {
        return false;
    }

    /**
     * {@inheritDoc} <p>
     * Always throws an AuthenticationException
     */
    @Override
    public void authenticateCertificateChain(X509Certificate[] certChain, boolean collectiveCert) throws AuthenticationException {
        throw new AuthenticationException("NullCollectiveAuthenticationPlugin will not authenticate any certificate chain. Authentication is always rejected.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.security.authentication.collective.CollectiveAuthenticationPlugin#isCollectiveCACertificate(java.security.cert.X509Certificate[])
     */
    @Override
    public boolean isCollectiveCACertificate(X509Certificate[] certChain) {
        return false;
    }

}
