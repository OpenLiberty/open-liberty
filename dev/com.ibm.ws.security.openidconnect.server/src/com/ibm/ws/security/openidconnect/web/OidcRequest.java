/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.web;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.security.oauth20.web.OAuth20Request;

public class OidcRequest extends OAuth20Request {

    OidcRequest(String providerName, EndpointType type, HttpServletRequest request) {
        super(providerName, type, request);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OidcRequest [provider:").append(providerName).append(" type:")
                        .append(type).append(" request:").append(request).append("]");
        return sb.toString();
    }
}
