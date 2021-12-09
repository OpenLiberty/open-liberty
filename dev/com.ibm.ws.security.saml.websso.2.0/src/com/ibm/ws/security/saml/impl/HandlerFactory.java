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
package com.ibm.ws.security.saml.impl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.acs.AcsHandler;
import com.ibm.ws.security.saml.sso20.metadata.MetadataHandler;
import com.ibm.ws.security.saml.sso20.slo.SLOHandler;

public class HandlerFactory {
    public static final TraceComponent tc = Tr.register(HandlerFactory.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    private static MetadataHandler metadataHandler = new MetadataHandler();
    private static AcsHandler acsHandler = new AcsHandler();
    private static SLOHandler sloHandler = new SLOHandler();

    static public SsoHandler getHandlerInstance(SsoRequest samlRequest) {
        SsoHandler result = null;
        Constants.SamlSsoVersion samlVersion = samlRequest.getSamlVersion();
        Constants.EndpointType type = samlRequest.getType();
        if (samlVersion == Constants.SamlSsoVersion.SAMLSSO20) {
            switch (type) {
                case SAMLMETADATA:
                    return metadataHandler;
                case ACS:
                    return acsHandler;
                case SLO:
                    return sloHandler;
                case LOGOUT:
                    return sloHandler;
                default:
                    break;
            }
        } else {
            // reserve for saml11
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handler"
                         + "(" + samlVersion
                         + ")(" + type
                         + "):" + result);
        }
        return result;
    }

    /**
     * @return the metadataHandler
     */
    public static MetadataHandler getMetadataHandler() {
        return metadataHandler;
    }

    /**
     * @param metadataHandler the metadataHandler to set
     */
    public static void setMetadataHandler(MetadataHandler metadataHandler) {
        HandlerFactory.metadataHandler = metadataHandler;
    }

    /**
     * @return the acsHandler
     */
    public static AcsHandler getAcsHandler() {
        return acsHandler;
    }

    /**
     * @param acsHandler the acsHandler to set
     */
    public static void setAcsHandler(AcsHandler acsHandler) {
        HandlerFactory.acsHandler = acsHandler;
    }
}
