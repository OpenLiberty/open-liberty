/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.osgi.response;

import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet40.IResponse40;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.response.IResponse31Impl;
import com.ibm.wsspi.http.HttpInboundConnection;

/**
 *
 */

public class IResponse40Impl extends IResponse31Impl implements IResponse40 {
    private static final TraceComponent tc = Tr.register(IResponse40Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param req
     * @param inConnection
     */
    public IResponse40Impl(IRequest req, HttpInboundConnection inConnection) {
        super(req, inConnection);
        // TODO Auto-generated constructor stub
    }

    /** {@inheritDoc} */
    @Override
    public void setTrailers(Map<String, String> trailers) {
        Iterator<String> trailerIterator = trailers.keySet().iterator();

        while (trailerIterator.hasNext()) {
            String trailerName = trailerIterator.next();
            this.response.setTrailer(trailerName, trailers.get(trailerName));

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setTrailers(Map<String, String>)", "Set trailer : " + trailerName + " + " + trailers.get(trailerName));
            }

        }
        this.response.writeTrailers();

    }

}
