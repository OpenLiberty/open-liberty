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
public class RequestMetadataTest {

    @Test
    public void testNoArgConstructor() {
        RequestMetadata metadata = new RequestMetadata();
        long id = Long.valueOf(metadata.getRequestId());
        metadata = new RequestMetadata();
        assertEquals(Long.valueOf(id + 1), Long.valueOf(metadata.getRequestId()));
    }

    @Test
    public void testMapConstructor() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("clientInfo", "someInfo");
        RequestMetadata requestMetadata = new RequestMetadata(metadata);
        assertEquals("someInfo", requestMetadata.getRequestMetadata().get("clientInfo"));
    }

}
