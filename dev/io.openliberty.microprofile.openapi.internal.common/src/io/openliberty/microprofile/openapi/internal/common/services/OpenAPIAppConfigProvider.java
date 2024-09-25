/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.internal.common.services;

import java.util.Optional;

public interface OpenAPIAppConfigProvider {

    /**
     * Retrieve a configuration string representing the included modules.
     * Valid entries are "all", "first", or a comma separated list of applicationName and/or
     * applicationName/moduleName.
     *
     * Null indicates that this was not set in server.xml (distinct from empty string, which will be processed as an illegal argument).
     *
     * @return The string from server.xml that configures included modules.
     */
    Optional<String> getIncludedModules();

    /**
     * Retrieve a configuration string representing the excluded modules.
     * Valid entries are a comma separated list of applicationName and/or
     * applicationName/moduleName.
     *
     * Null indicates that this was not set in server.xml (distinct from empty string, which will be processed as an illegal argument).
     *
     * @return The string from server.xml that configures excluded modules.
     */
    Optional<String> getExcludedModules();

    public static interface OpenAPIAppConfigListener extends Comparable<OpenAPIAppConfigListener> {
        public void processConfigUpdate();

        public int getConfigListenerPriority();
    }

    /**
     * @param listener
     */
    void registerAppConfigListener(OpenAPIAppConfigListener listener);

    /**
     * @param listener
     */
    void unregisterAppConfigListener(OpenAPIAppConfigListener listener);
}
