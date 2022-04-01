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
package io.openliberty.webcontainer60.internal.osgi.response;

import io.openliberty.webcontainer60.servlet.IResponse60;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.osgi.response.IResponse40Impl;
import com.ibm.wsspi.http.HttpInboundConnection;

/**
 *
 */

public class IResponse60Impl extends IResponse40Impl implements IResponse60 {
    private static final TraceComponent tc = Tr.register(IResponse60Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param req
     * @param inConnection
     */
    public IResponse60Impl(IRequest req, HttpInboundConnection inConnection) {
        super(req, inConnection);
    }

    @Override
    public void getDummyResponse60() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getDummyResponse60()", "Test Servlet 6.0 API");
        }
    }
    
}
