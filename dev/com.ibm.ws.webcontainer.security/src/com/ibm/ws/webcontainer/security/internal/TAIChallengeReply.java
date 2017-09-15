/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.webcontainer.security.internal;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webcontainer.servlet.IExtendedResponse;

public class TAIChallengeReply extends WebReply {
    private static final TraceComponent tc = Tr.register(TAIChallengeReply.class);

    public TAIChallengeReply(int code) {
        // the response code returned from NegotiateTrustAssociationInterceptor.negotiateAndValidateEstablishedTrust()
        super(code, null);
    }

    /**
     * @see com.ibm.ws.security.web.WebReply#writeResponse(HttpServletResponse)
     */
    @Override
    public void writeResponse(HttpServletResponse rsp) throws IOException {
        if (rsp.isCommitted())
            return;

        if (rsp instanceof IExtendedResponse) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "IExtendedResponse type - status code is " + ((IExtendedResponse) rsp).getStatusCode());
            }
            if (((IExtendedResponse) rsp).getStatusCode() == HttpServletResponse.SC_OK) {
                rsp.setStatus(responseCode);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response code set is " + responseCode);
                }
            }
        } else {
            rsp.setStatus(responseCode);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Response code set is " + responseCode);
            }
        }
    }
}
