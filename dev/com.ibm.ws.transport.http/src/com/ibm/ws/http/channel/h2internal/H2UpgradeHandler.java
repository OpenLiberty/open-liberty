/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.transport.access.TransportConnectionAccess;
import com.ibm.ws.transport.access.TransportConnectionUpgrade;

/**
 * HTTP/2 specific upgrade handler
 */
public class H2UpgradeHandler implements TransportConnectionUpgrade {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(H2UpgradeHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /**
     * HttpUpgradeHandler method
     */
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "H2UpgradeHandler destroy entry");
        }
    }

    @Override
    /**
     * TransportConnectionUpgrade method
     */
    public void init(TransportConnectionAccess x) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "H2UpgradeHandler init entry");
        }
    }

}
