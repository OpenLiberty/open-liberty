/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.service;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

public class WSATUtil {

    public static EndpointReferenceType createEpr(String hostname) {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType uri = new AttributedURIType();
        uri.setValue(hostname);
        epr.setAddress(uri);
        ReferenceParametersType para = new ReferenceParametersType();
        epr.setReferenceParameters(para);
        return epr;
    }

    /**
     * @param string
     * @param recoveryID
     * @return
     */
    public static String createRedirectAddr(String origAddr, String newAddr) {
        URL redirectURL;
        try {
            URL origURL = new URL(origAddr);
            URL recoveryURL = new URL(newAddr);
            redirectURL = new URL(origURL.getProtocol(), recoveryURL.getHost(), recoveryURL.getPort(), origURL.getFile());
        } catch (MalformedURLException e) {
            return null;
        }

        return redirectURL.toString();
    }
}