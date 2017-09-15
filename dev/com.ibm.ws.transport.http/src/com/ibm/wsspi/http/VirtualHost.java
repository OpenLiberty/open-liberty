/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import java.util.Collection;
import java.util.List;

/**
 * Representation of a VirtualHost
 */
public interface VirtualHost {

    /**
     * The name of the virtual host. The default virtual host name is "default_host".
     * 
     * @return Virtual host name
     */
    String getName();

    /**
     * Retrieve MIME type for extension
     * 
     * @param extension
     * @return mime type or null
     */
    String getMimeType(String extension);

    /**
     * Add the container as a handler for the specified context root.
     * 
     * @param contextRoot
     * @param container
     */
    void addContextRoot(String contextRoot, HttpContainer container);

    /**
     * Remove the container as a handler for the specified context root.
     * 
     * @param contextRoot
     * @param container
     */
    void removeContextRoot(String contextRoot, HttpContainer container);

    /**
     * @return the list of host:port aliases assigned to this virtual host.
     */
    List<String> getAliases();

    /**
     * @param hostAlias
     * @return secure https port associated with the given alias (via endpoint configuration),
     *         or -1 if unconfigured.
     */
    int getSecureHttpPort(String hostAlias);

    /**
     * @param hostAlias
     * @return secure http port associated with the given alias (via endpoint configuration),
     *         or -1 if unconfigured.
     */
    int getHttpPort(String hostAlias);

    /**
     * @param hostAlias
     * @return configured hostname associated with the given alias (via endpoint configuration).
     */
    String getHostName(String hostAlias);

    /**
     * @param contextRoot
     * @param securedPreferred indicates if the caller prefers to receive a secured URL
     * @return corresponding URL string by combining the given contextRoot and endpoint configuration for this
     *         VirtualHost.
     */
    String getUrlString(String contextRoot, boolean securedPreferred);

    /**
     * @return the Collection of allowedFromEndpoints assigned to this virtual host.
     */
    Collection<String> getAllowedFromEndpoints();
}
