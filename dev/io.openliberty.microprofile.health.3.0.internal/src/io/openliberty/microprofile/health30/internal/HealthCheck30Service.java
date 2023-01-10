/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.microprofile.health30.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.microprofile.health.internal.HealthCheckService;

/**
 * Microprofile Health Check Service
 */
public interface HealthCheck30Service extends HealthCheckService {

    /**
     * Performs the health check for a given health check procedure
     * and returns a health response in httpResponse
     *
     * @param request
     * @param httpResponse
     * @param healthCheckProcedure
     */
    void performHealthCheck(HttpServletRequest request, HttpServletResponse httpResponse, String healthCheckProcedure);

}