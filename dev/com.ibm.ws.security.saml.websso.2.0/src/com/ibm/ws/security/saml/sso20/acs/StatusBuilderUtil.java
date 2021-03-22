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
package com.ibm.ws.security.saml.sso20.acs;

import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusDetail;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml.saml2.core.impl.StatusDetailBuilder;
import org.opensaml.saml.saml2.core.impl.StatusMessageBuilder;

/**
 *
 */
public class StatusBuilderUtil {

//    Status status;
//    StatusCode statusCode;
//    StatusMessage statusMessage;
//    StatusDetail statusDetail;

    public StatusBuilderUtil() {

    }

    public Status buildStatus() {
        StatusBuilder statusBuilder = new StatusBuilder();
        Status status = statusBuilder.buildObject();

        StatusCodeBuilder statusCodeBuilder = new StatusCodeBuilder();
        StatusCode statusCode = statusCodeBuilder.buildObject();

        StatusDetailBuilder statusDetailBuilder = new StatusDetailBuilder();
        StatusDetail statusDetail = statusDetailBuilder.buildObject();

        StatusMessageBuilder statusMessageBuilder = new StatusMessageBuilder();
        StatusMessage statusMessage = statusMessageBuilder.buildObject();

        status.setStatusCode(statusCode);
        status.setStatusDetail(statusDetail);
        status.setStatusMessage(statusMessage);

        return status;

    }

    /**
     * @param sloResponseStatus
     * @param responderUri
     */
    public void setStatus(Status sloStatus, String statusCode) {
        
        sloStatus.getStatusCode().setValue(statusCode);

    }

}
