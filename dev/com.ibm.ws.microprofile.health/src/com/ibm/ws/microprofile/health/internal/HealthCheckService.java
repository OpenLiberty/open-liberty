/*******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.health.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Microprofile Health Check Service
 */
public interface HealthCheckService {

    /**
     * Performs the health check for a given http request, ie. http://hostname:port/health
     * and returns a health response in httpResponse
     *
     * @param request
     * @param httpResponse
     */
    void performHealthCheck(HttpServletRequest request, HttpServletResponse httpResponse);

    /**
     * Removes references to an application module. Currently this operation removes entries from the
     * BeanManager cache that is maintained to avoid looking it up all the time.
     *
     * @param appName
     * @param moduleName
     */
    void removeModuleReferences(String appName, String moduleName);
}