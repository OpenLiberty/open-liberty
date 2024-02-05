/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;

public class WSATUtil {
    private static final TraceComponent TC = Tr.register(WSATUtil.class);

    public static EndpointReferenceType createEpr(String hostname, String... recoveryIds) {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType uri = new AttributedURIType();
        uri.setValue(hostname);
        epr.setAddress(uri);
        ReferenceParametersType para = new ReferenceParametersType();

        if (recoveryIds.length > 0 && recoveryIds[0] != null) {
            para.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_REC_REF, String.class, recoveryIds[0]));
        }

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
            redirectURL = new URL(recoveryURL.getProtocol(), recoveryURL.getHost(), recoveryURL.getPort(), origURL.getFile());
        } catch (MalformedURLException e) {
            return null;
        }

        String ret = redirectURL.toString();

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "REROUTE ADDRESS: {0}", ret);
        }

        return ret;
    }

    /**
     * @param endpointReference
     * @return
     */
    public static EndpointReferenceType cloneEPR(EndpointReferenceType epr) {
        EndpointReferenceType newEpr = EndpointReferenceUtils.duplicate(epr);
        // duplicate doesn't seem to copy the ReferenceParams?, so add
        // back the originals plus our new participant id.
        ReferenceParametersType refs = new ReferenceParametersType();
        for (Object ref : epr.getReferenceParameters().getAny()) {
            refs.getAny().add(ref);
        }
        newEpr.setReferenceParameters(refs);
        return newEpr;
    }
}