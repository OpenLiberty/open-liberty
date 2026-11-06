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
package com.ibm.ws.jmx.connector.client.rest.internal;

import java.util.concurrent.Callable;

/**
 * Per-thread request identity for a {@link RESTMBeanServerConnection}.
 *
 * <p>Holds the Authorization header to use for requests made on this thread inside
 * {@code RequestCredentialsSupport.runWithBearerToken}. It is deliberately a plain {@link ThreadLocal} and not an
 * {@link InheritableThreadLocal}, so threads created inside an action (such as the notification polling thread)
 * never inherit the identity. The identity records the connection it belongs to, so a second connection used inside
 * the action ignores it and the token cannot leak to a different endpoint.
 *
 * <p>Entry points that must always use the connect-time identity (notification area and registration calls,
 * connection recovery and close) call {@link #unbind()} first and {@link #restore(RequestIdentity)} in a
 * {@code finally} block.
 */
final class RequestIdentity {

    private static final ThreadLocal<RequestIdentity> CURRENT = new ThreadLocal<RequestIdentity>();

    private final Object owner;
    private final String authHeader;

    private RequestIdentity(Object owner, String authHeader) {
        this.owner = owner;
        this.authHeader = authHeader;
    }

    /**
     * Validates a bearer token and returns the Authorization header value for it.
     *
     * @throws IllegalArgumentException if the token is null, empty or contains whitespace (which also rejects
     *                                      CR/LF, so a token can never inject another header)
     */
    static String bearerHeader(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("A bearer token must not be null or empty.");
        }
        for (int i = 0; i < token.length(); i++) {
            if (Character.isWhitespace(token.charAt(i)) || Character.isISOControl(token.charAt(i))) {
                throw new IllegalArgumentException("A bearer token must not contain whitespace or control characters.");
            }
        }
        return "Bearer " + token;
    }

    /**
     * Returns the header to send for a request made by {@code owner} on the current thread.
     */
    static String resolve(Object owner, String connectHeader) {
        RequestIdentity id = CURRENT.get();
        return (id != null && id.owner == owner) ? id.authHeader : connectHeader;
    }

    /**
     * Runs the action with {@code authHeader} bound to the current thread for {@code owner}. The previous binding
     * is restored afterwards, so calls can be nested.
     */
    static <T> T runAs(Object owner, String authHeader, Callable<T> action) throws Exception {
        RequestIdentity previous = CURRENT.get();
        CURRENT.set(new RequestIdentity(owner, authHeader));
        try {
            return action.call();
        } finally {
            restore(previous);
        }
    }

    /**
     * Removes the binding from the current thread and returns it so it can be passed to {@link #restore}.
     */
    static RequestIdentity unbind() {
        RequestIdentity previous = CURRENT.get();
        CURRENT.remove();
        return previous;
    }

    /**
     * Puts back a binding returned by {@link #unbind()} (or removes the binding if it was {@code null}).
     */
    static void restore(RequestIdentity previous) {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}