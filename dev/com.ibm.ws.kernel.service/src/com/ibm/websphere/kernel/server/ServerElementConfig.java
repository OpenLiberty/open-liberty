/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.websphere.kernel.server;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Service interface for accessing server element configuration.
 * This service provides read-only access to configuration values that are defined
 * as attributes on the <server> element in server.xml.
 *
 * Values are set by the configuration parser when server.xml is loaded.
 */
@ProviderType
public interface ServerElementConfig {

    /**
     * Get the server description.
     * The description attribute provides a human-readable description of the server.
     *
     * @return the server description, or null if not set
     */
    String getDescription();

    /**
     * Get the current quiesce timeout value in milliseconds.
     * The quiesce timeout controls how long the server will wait for
     * listeners and threads to complete their work during shutdown.
     *
     * @return the quiesce timeout in milliseconds
     */
    long getQuiesceTimeoutMillis();
    
    /**
     * Check if the quiesce timeout was explicitly configured in server.xml.
     * This is useful for determining whether the timeout value should override
     * other related timeout configurations.
     *
     * @return true if the user specified a quiesceTimeout attribute on the <server> element,
     *         false if using the default value
     */
    boolean isQuiesceTimeoutExplicitlyConfigured();
}
