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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 *
 */
public class RequestContextTest {

    private long requests = Long.valueOf(new RequestMetadata().getRequestId());

    @Test
    public void testNoId() {
        requests += 1;
        assertEquals("no ID", RequestContext.getRequestMetadata().getRequestId());

    }

    @Test
    public void testMetadata() {
        try {
            requests += 1;
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("foo", "bar");
            RequestMetadata requestMetadata = new RequestMetadata(metadata);
            RequestContext.setRequestMetadata(requestMetadata);
            assertEquals(String.valueOf(requests), RequestContext.getRequestMetadata().getRequestId());
            assertEquals("bar", RequestContext.getRequestMetadata().getRequestMetadata().get("foo"));
        } finally {
            RequestContext.removeRequestMetadata();
        }
    }
}
