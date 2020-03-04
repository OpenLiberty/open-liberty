/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http2;

import java.util.HashSet;
import java.util.Set;

/**
 * Keep track of available Http2ConnectionHandlers
 */
public class Http2Consumers {

    private static Set<Http2ConnectionHandler> handlers;

    public static void addHandler(Http2ConnectionHandler h) {
        if (handlers == null) {
            handlers = new HashSet<Http2ConnectionHandler>();
        }
        handlers.add(h);
    }

    public static void removeHandler(Http2ConnectionHandler h) {
        if (handlers != null) {
            handlers.remove(h);
        }
    }

    /**
     * Return the set of Http2ConnectionHandlers that have been registered.
     *
     * @return Set<Http2ConnectionHandler> or null if the set is empty
     */
    public static Set<Http2ConnectionHandler> getHandlers() {
        if (handlers == null || handlers.isEmpty()) {
            return null;
        }
        return handlers;
    }
}
