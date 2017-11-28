/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.kernel.server;

import java.util.List;

import javax.management.MBeanException;

/**
 * The ServerEndpointControlMBean provides control over the state of endpoints of the server.
 * Endpoints represent inbound communication to the server, like http and messaging.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 *
 * @ibm-api
 */
public interface ServerEndpointControlMBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     */
    String OBJECT_NAME = "WebSphere:feature=kernel,name=ServerEndpointControl";

    /**
     * Pauses the server endpoint(s) specified by target(s)
     *
     * @param targets Comma separated list of zero or more targets representing the endpoint(s) to be paused
     *            Null or empty string ("") will pause all registered endpoints
     * @throws MBeanException
     */

    public void pause(String targets) throws MBeanException;

    /**
     * Resumes the server endpoint(s) specified by target(s)
     *
     * @param targets Comma separated list of zero or more targets representing the endpoint(s) to be resumed
     *            Null or empty string ("") will resume all registered endpoints
     * @throws MBeanException
     */

    public void resume(String targets) throws MBeanException;

    /**
     * Returns true if the server endpoint(s) specified by target(s) is paused, otherwise false.
     *
     * @param targets String representation of the endpoint to be paused
     * @return The state of the specified target, if targets is null or empty the cumulative state of all registered endpoints is returned
     * @throws MBeanException
     */

    public boolean isPaused(String targets) throws MBeanException;

    /**
     * Returns the name of all endpoints that can be paused/resumed
     *
     * @return List of endpoints and their state
     * @throws MBeanException
     */

    public List<String> listEndpoints() throws MBeanException;

}
