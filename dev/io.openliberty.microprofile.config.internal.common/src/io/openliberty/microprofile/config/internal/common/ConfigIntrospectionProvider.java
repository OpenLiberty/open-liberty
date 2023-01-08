/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.config.internal.common;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;

/**
 * Provides introspection data for the MicroProfile Config introspector.
 * <p>
 * A service of this type must be registered to allow the MP Config introspector to work.
 */
public interface ConfigIntrospectionProvider {

    /**
     * Get all the Config objects registered for each application.
     *
     * @return a map from application name to the set of configs registered for that application
     */
    public Map<String, Set<Config>> getConfigsByApplication();

}
