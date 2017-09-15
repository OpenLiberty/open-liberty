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

import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * Retrieves the application and modules names during application deployments
 */
public interface AppTracker {

    /** {@inheritDoc} */
    void onStartup(Set<Class<?>> arg0, ServletContext ctx) throws ServletException;

    /**
     * Gets a set of the names of the applications deployed
     *
     * @return
     */
    Set<String> getAppNames();

    /**
     * Gets a set of module names for a given application
     *
     * @param appName
     * @return
     */
    Set<String> getModuleNames(String appName);

}