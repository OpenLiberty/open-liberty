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
package com.ibm.ws.webcontainer40.osgi.request;

import java.util.HashMap;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer31.osgi.request.IRequest31Impl;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpRequest;

/**
 *
 */
public class IRequest40Impl extends IRequest31Impl implements IRequest40 {

    private static final TraceComponent tc = Tr.register(IRequest40Impl.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    /**
     * @param connection
     */
    public IRequest40Impl(HttpInboundConnection connection) {
        super(connection);
    }

    /**
     *
     * provide access to HttpRequest object
     *
     */
    @Override
    public HttpRequest getHttpRequest() {
        return this.request;
    }

    @Override
    public HashMap<String, String> getTrailers() {

        HashMap<String, String> trailers = new HashMap<String, String>();

        List<String> trailerNames = request.getTrailerNames();

        if (trailerNames != null && !trailerNames.isEmpty()) {

            for (String trailerName : trailerNames) {
                trailers.put(trailerName, request.getTrailer(trailerName));

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getTrailers()", "Set trailer : " + trailerName + " + " + trailers.get(trailerName));
                }

            }
        }

        return trailers;
    }

}
