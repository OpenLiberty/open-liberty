/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.providers;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.org.jboss.resteasy.common.client.JAXRSClientConstants;
import io.openliberty.restfulWS.client.security.LtpaHandler;
import io.openliberty.restfulWS.client.security.OAuthHandler;
import io.openliberty.restfulWS.client.security.SamlPropagationHandler;

@Priority(Priorities.AUTHENTICATION-1)
public class ClientConfigFilter implements ClientRequestFilter {
    private static final TraceComponent tc = Tr.register(ClientConfigFilter.class);

    @Override
    public void filter(ClientRequestContext crc) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "filter", crc);
        }

        if (isTrue(crc.getProperty(JAXRSClientConstants.LTPA_HANDLER))) {
            LtpaHandler.configClientLtpaHandler(crc);
        }
        if (isTrue(crc.getProperty(JAXRSClientConstants.MPJWT_HANDLER))) {
            OAuthHandler.handleMpJwtToken(crc);
        }
        if (isTrue(crc.getProperty(JAXRSClientConstants.JWT_HANDLER))) {
            OAuthHandler.handleJwtToken(crc);
        }
        if (isTrue(crc.getProperty(JAXRSClientConstants.OAUTH_HANDLER))) {
            OAuthHandler.handleOAuthToken(crc);
        }
        if (isTrue(crc.getProperty(JAXRSClientConstants.SAML_HANDLER))) {
            SamlPropagationHandler.configClientSAMLHandler(crc);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "filter");
        }
    }

    private static boolean isTrue(Object o) {
        return Boolean.TRUE.equals(o) || "true".equals(o);
    }
}
