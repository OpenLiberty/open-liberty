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
     * Pauses all registered server endpoints
     *
     * @throws MBeanException if not all registered endpoints could be paused
     */
    public void pause() throws MBeanException;

    /**
     * Pauses the specified server endpoints
     *
     * @param targets Comma separated list of one or more names of endpoints.
     * @throws MBeanException If each of the specified targets couldn't be paused, or if the list of target names is empty or are not all valid
     */
    public void pause(String targets) throws MBeanException;

    /**
     * Resumes all registered server endpoints
     *
     * @throws MBeanException if not all registered endpoints could be resumed
     */
    public void resume() throws MBeanException;

    /**
     * Resumes the specified server endpoints
     *
     * @param targets Comma separated list of one or more names of endpoints.
     * @throws MBeanException If each of the specified targets couldn't be resumed, or if the list of target names is empty or are not all valid
     */
    public void resume(String targets) throws MBeanException;

    /**
     * Query the state of all registered server endpoints.
     *
     * @return True if the cumulative state of all registered endpoints is paused, otherwise false.
     */
    public boolean isPaused();

    /**
     * Query the state of the specified server endpoints.
     *
     * @param targets Comma separated list of one or more names of endpoints.
     * @return If a single target is specified, returns the state of the specified target, if multiple targets are specified, returns true only
     *         all specified targets are paused
     * @throws MBeanException If each of the specified targets couldn't be queried, or if the list of target names is empty or are not all valid
     */
    public boolean isPaused(String targets) throws MBeanException;

    /**
     * Returns the name of all endpoints that can be paused/resumed
     *
     * @return List of endpoints and their state
     */
    public List<String> listEndpoints();

}
