/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.webcontainer60.internal.osgi.request;

import io.openliberty.webcontainer60.servlet.IRequest60;

//import jakarta.servlet.ServletConnection;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.request.IRequest40Impl;
import com.ibm.wsspi.http.HttpInboundConnection;


/**
 *
 */
public class IRequest60Impl extends IRequest40Impl implements IRequest60 {

    private static final TraceComponent tc = Tr.register(IRequest60Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param connection
     */
    public IRequest60Impl(HttpInboundConnection connection) {
        super(connection);
    }

    @Override
    public String getRequestId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getRequestId()", "Test Servlet 6.0 API");
        }

        return "getRequestId() 60";
    }

    @Override
    public String getProtocolRequestId(){
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getProtocolRequestId()", "Test Servlet 6.0 API");
        }

        return "getProtocolRequestId() 60";
    }

    /*
    @Override
    ServletConnection getServletConnection() {


    }
     */
}
