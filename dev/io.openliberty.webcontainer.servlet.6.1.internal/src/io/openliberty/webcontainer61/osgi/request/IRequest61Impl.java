/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer61.osgi.request;

import java.util.Arrays;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.SSLContext;

import io.openliberty.webcontainer60.osgi.request.IRequest60Impl;
import io.openliberty.websphere.servlet61.IRequest61;

public class IRequest61Impl extends IRequest60Impl implements IRequest61 {
    private static final TraceComponent tc = Tr.register(IRequest61Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public IRequest61Impl(HttpInboundConnection connection) {
        super(connection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor , inboundConnection [" + connection + "]");
        }
    }

    /**
     * Support request attribute "jakarta.servlet.request.secure_protocol"
     */
    @Override
    public String getSecureProtocol() {
        HttpInboundConnection conn = getHttpInboundConnection();
        String secureProtocol = null;

        SSLContext ssl = conn.getSSLContext();

        if (null != ssl) {
            secureProtocol = ssl.getSession().getProtocol(); 
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getSecureProtocol , [" + secureProtocol + "]");
        }
        return secureProtocol;
    }
}
