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
package com.ibm.websphere.jmx.connector.rest;

import java.util.concurrent.Callable;

/**
 * Optional capability of the {@link javax.management.MBeanServerConnection} returned by the REST connector's
 * {@link javax.management.remote.JMXConnector#getMBeanServerConnection()}. It lets several callers share one
 * connection while each request is authenticated with the caller's own bearer token.
 *
 * <pre>
 * MBeanServerConnection mbsc = connector.getMBeanServerConnection();
 * Object v = ((RequestCredentialsSupport) mbsc)
 *         .runWithBearerToken(userJwt, () -&gt; mbsc.getAttribute(name, "Foo"));
 * </pre>
 *
 * <p>Behavior:
 * <ul>
 * <li><b>Threads:</b> the token applies only to requests made on the calling thread while the action runs. Work
 * handed to another thread inside the action uses the connect-time identity.</li>
 * <li><b>Notifications:</b> adding or removing a listener inside the action is allowed. The notification area and
 * the registrations are always created and delivered as the connect-time identity, as are connection recovery,
 * the background polling thread and {@code close()}.</li>
 * <li><b>Failures:</b> a 401 or 403 on a request surfaces as the usual {@code IOException} and the connection stays
 * open.</li>
 * <li><b>Routing:</b> routing context set through the {@code WebSphere:name=RoutingContext} MBean remains
 * connection-wide.</li>
 * <li><b>Certificate connections:</b> not supported. A connection made with
 * {@link ConnectorSettings#CERTIFICATE_AUTHENTICATION} throws {@link IllegalStateException}.</li>
 * </ul>
 *
 * @ibm-api
 */
public interface RequestCredentialsSupport {

    /**
     * Runs the action so that every REST request made by this connection on the calling thread carries
     * {@code Authorization: Bearer <token>}. Calls may be nested; the previous token is restored when the action ends,
     * normally or by exception.
     *
     * @param token  the bearer token (for example a JWT). Must be non-empty and contain no whitespace.
     * @param action the work to run
     * @return the action's result
     * @throws IllegalArgumentException if the token is null, empty or contains whitespace
     * @throws IllegalStateException    if the connection was made with certificate authentication
     * @throws Exception                anything thrown by the action
     */
    <T> T runWithBearerToken(String token, Callable<T> action) throws Exception;

    /**
     * Replaces the connect-time bearer token. It is used for everything that is not made on behalf of a specific
     * caller: the notification polling thread, connection recovery, {@code close()} and requests made outside
     * {@link #runWithBearerToken}. Call this before the current token expires.
     *
     * @param token the new bearer token. Must be non-empty and contain no whitespace.
     * @throws IllegalArgumentException if the token is null, empty or contains whitespace
     * @throws IllegalStateException    if the connection was made with certificate authentication
     */
    void updateConnectionToken(String token);
}