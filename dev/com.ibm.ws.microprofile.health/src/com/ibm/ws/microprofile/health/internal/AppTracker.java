/*******************************************************************************
 * Copyright (c) 2017, 2020 Contributors to the Eclipse Foundation
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

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.StateChangeException;

/**
 * Retrieves the application and modules names during application deployments
 */
public interface AppTracker {

    /**
     * Gets called when the deployed application is starting.
     *
     * @param appInfo
     */
    void applicationStarting(ApplicationInfo appInfo) throws StateChangeException;

    /**
     * Gets called when the deployed application is started.
     *
     * @param appInfo
     */
    void applicationStarted(ApplicationInfo appInfo) throws StateChangeException;

    /**
     * Gets called when the deployed application is stopping.
     *
     * @param appInfo
     */
    void applicationStopping(ApplicationInfo appInfo);

    /**
     * Gets called when the deployed application is stopped.
     *
     * @param appInfo
     */
    void applicationStopped(ApplicationInfo appInfo);

    /**
     * Gets a set of the names of the applications deployed
     *
     * @return
     */
    Set<String> getAppNames();

    /**
     * Gets a set of the names of all applications
     *
     * @return
     */
    Set<String> getAllAppNames();

    /**
     * Gets a set of module names for a given application
     *
     * @param appName
     * @return
     */
    Set<String> getModuleNames(String appName);

    /**
     * Returns true if the application with the specified name is started, otherwise false.
     *
     * @param appName
     * @return true if the application with the specified name is started, otherwise false.
     */
    boolean isStarted(String appName);

    /**
     * Returns true if the application with the specified name is installed, otherwise false.
     *
     * @param appName
     * @return true if the application with the specified name is installed, otherwise false.
     */
    boolean isInstalled(String appName);

    /**
     * Returns true if the application with the specified name is uninstalled, otherwise false.
     *
     * @param appName
     * @return true if the application with the specified name is uninstalled, otherwise false.
     */
    boolean isUninstalled(String appName);

    /**
     * Sets the HealthCheckService associated with this AppTracker.
     *
     * @param healthService
     */
    void setHealthCheckService(HealthCheckService healthService);

}