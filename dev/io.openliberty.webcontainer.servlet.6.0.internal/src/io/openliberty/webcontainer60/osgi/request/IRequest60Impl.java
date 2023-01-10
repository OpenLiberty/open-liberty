/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.webcontainer60.osgi.request;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.request.IRequest40Impl;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.ee7.HttpInboundConnectionExtended;

import io.openliberty.websphere.servlet60.IRequest60;

public class IRequest60Impl extends IRequest40Impl implements IRequest60 {
    private static final TraceComponent tc = Tr.register(IRequest60Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    public IRequest60Impl(HttpInboundConnection connection) {
        super(connection);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor , inboundConnection [" + connection + "]");
        }
    }

    @Override
    public int getStreamId() {
        int id = -1;
        HttpInboundConnectionExtended ice;

        if ((ice = getICE()) != null) {
            id = ice.getStreamId();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getH2StreamId , returns stream id [" + id + "]");
        }

        return id;
    }

    @Override
    public int getConnectionId() {
        int id = -1;
        HttpInboundConnectionExtended ice;

        if ((ice = getICE()) != null) {
            id = ice.getConnectionId();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getConnectionId , returns connection id [" + id + "]");
        }

        return id;
    }

    private HttpInboundConnectionExtended getICE() {
        HttpInboundConnection conn = getHttpInboundConnection();
        if (conn instanceof HttpInboundConnectionExtended) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getICE , returns HttpInboundConnectionExtended");
            }
            return ((HttpInboundConnectionExtended) conn);
        } else {
            return null;
        }
    }
}
