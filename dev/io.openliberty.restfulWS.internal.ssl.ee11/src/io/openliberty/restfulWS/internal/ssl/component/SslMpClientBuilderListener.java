/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.restfulWS.internal.ssl.component;

import static io.openliberty.restfulWS.internal.ssl.component.SslClientBuilderListener.getSSLContext;
import static io.openliberty.restfulWS.internal.ssl.component.SslClientBuilderListener.toRefString;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

import com.ibm.websphere.ssl.SSLException;

import io.openliberty.org.jboss.resteasy.common.client.JAXRSClientConstants;

public class SslMpClientBuilderListener implements RestClientListener {

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) { // for MP Rest Clients
        Object sslRef = builder.getConfiguration().getProperty(JAXRSClientConstants.SSL_REFKEY);
        try {
            getSSLContext(toRefString(sslRef)).ifPresent(builder::sslContext);
        } catch (SSLException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
