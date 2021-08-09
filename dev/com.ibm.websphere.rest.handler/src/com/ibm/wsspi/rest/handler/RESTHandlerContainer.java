/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.rest.handler;

import java.io.IOException;
import java.util.Iterator;

/**
 * This container keeps track of all registered RESTHandler services and is able to find the best match for an incoming request.
 * 
 * @ibm-spi
 */
public interface RESTHandlerContainer {

    /**
     * The current version of the container.
     */
    public static final int REST_HANDLER_CONTAINER_VERSION = 1;

    /**
     * This parameter represents a comma-separated list of host names within the collective.
     */
    public static final String COLLECTIVE_HOST_NAMES = "collective.hostNames";

    /**
     * 
     * This parameter represents a comma-separated list of the server names to be used in a routing context.
     */
    public static final String COLLECTIVE_SERVER_NAMES = "collective.serverNames";

    /**
     * 
     * This parameter represents a comma-separated list of the server installation directories to be used in a routing context.
     */
    public static final String COLLECTIVE_SERVER_INSTALL_DIRS = "collective.installDirs";

    /**
     * 
     * This parameter represents a comma-separated list of the server user directories to be used in a routing context.
     */
    public static final String COLLECTIVE_SERVER_USER_DIRS = "collective.serverUserDirs";

    /**
     * This method allows servlets or other REST entry points to delegate an incoming call to the RESTHandler framework. This container
     * will match and delegate the incoming request to a registered RESTHandler, or return false if no match was found.
     * 
     * @param request encapsulates the artifacts for the HTTP request
     * @param response encapsulates the artifacts for the HTTP response
     * @return true if the request was matched and delegated to a registered RESTHandler, false if no match was found
     * @throws IOException
     */
    public boolean handleRequest(RESTRequest request, RESTResponse response) throws IOException;

    /**
     * Provides access to an aggregated collection of the registered paths that the current RESTHandler are listening to.
     * 
     * @return an Iteration over the paths that are available to be invoked.
     */
    public Iterator<String> registeredKeys();

}
