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

public interface OpenAPIAppConfigProvider {

    /**
     * Returns all configuration from the server.xml's mpOpenAPI element
     *
     * @return An object containing mpOpenAPI configuration
     */
    OpenAPIServerXMLConfig getConfiguration();

    public static interface OpenAPIAppConfigListener {
        /**
         * This method will be called whenever the server.xml is updated and that update changes the mpOpenAPI include or exclude statements
         */
        public void processConfigUpdate();

        /**
         * Get the priority of this listener, higher priorities must be run first
         */
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
