/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler.helper;

import java.io.IOException;

import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 *
 */
public interface RESTRoutingHelper {

    /**
     * @return
     */
    boolean routingAvailable();

    /**
     * @param request
     * @param response
     */
    void routeRequest(RESTRequest request, RESTResponse response) throws IOException;

    /**
     * @param request
     * @param response
     * @param legacyURI
     */
    void routeRequest(RESTRequest request, RESTResponse response, boolean legacyURI) throws IOException;

    /**
     * @param request
     * @return
     */
    boolean containsLegacyRoutingContext(RESTRequest request);

    /**
     * @param request
     * @return
     */
    boolean containsRoutingContext(RESTRequest request);

}