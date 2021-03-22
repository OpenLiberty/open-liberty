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
package com.ibm.ws.security.saml.sso20.slo;

import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;

import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;

public class SLOMessageContextUtils {

    public static final String LOGOUT_STATUS_CODE_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    private BasicMessageContext<?, ?> messageContext = null;

    public SLOMessageContextUtils(BasicMessageContext<?, ?> msgCtx) {
        this.messageContext = msgCtx;
    }

    public String getSloStatusCode() {
        if (messageContext == null) {
            return STATUS_UNKNOWN;
        }
        Status responseStatus = messageContext.getSLOResponseStatus();
        if (responseStatus == null) {
            return STATUS_UNKNOWN;
        }
        StatusCode statusCode = responseStatus.getStatusCode();
        if (statusCode == null) {
            return STATUS_UNKNOWN;
        }
        String value = statusCode.getValue();
        if (value == null) {
            return STATUS_UNKNOWN;
        }
        return value;
    }

}
