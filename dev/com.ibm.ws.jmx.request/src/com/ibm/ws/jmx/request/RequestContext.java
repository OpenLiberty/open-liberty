/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.request;

import java.util.Map;

/**
 * RequestContext provides ThreadLocal storage for RequestMetadata. The
 * RequestMetadata should be set to the RequestContext at the beginning of a
 * request and removed at the conclusion of a request following this pattern:
 * 
 * <code>
 * try {
 * RequestMetadata requestMetadata = new RequestMetadata(metadata);
 * RequestContext.setRequestMetadata(requestMetadata);
 * ...
 * } finally {
 * RequestContext.removeRequestMetadata();
 * }
 * 
 * </code>
 */
public class RequestContext {

    private static ThreadLocal<RequestMetadata> threadLocal = new ThreadLocal<RequestMetadata>();
    private static RequestMetadata noMetadata;

    /**
     * Gets the metadata for the current request
     * 
     * @return the request metadata
     */
    public static RequestMetadata getRequestMetadata() {
        RequestMetadata metadata = threadLocal.get();
        if (metadata == null)
            metadata = getNoMetadata();
        return metadata;
    }

    /**
     * Sets the metadata for the current request into ThreadLocal storage.
     * 
     * @param metadata
     */
    public static void setRequestMetadata(RequestMetadata metadata) {
        threadLocal.set(metadata);
    }

    /**
     * Removes the request metadata from ThreadLocal storage. A request thread
     * which sets request metadata must call this method prior to completing
     * the request in order to clear the data from the thread.
     */
    public static void removeRequestMetadata() {
        threadLocal.remove();
    }

    private static synchronized RequestMetadata getNoMetadata() {
        if (noMetadata == null) {
            noMetadata = new RequestMetadata();
            Map<String, Object> metadata = noMetadata.getRequestMetadata();
            metadata.put(RequestMetadata.REQUEST_ID, "no ID");
        }
        return noMetadata;
    }
}
