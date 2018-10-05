/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 *
 */
public class HttpEndpointImplTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Test
    public void testResolveHostName() throws Exception {
        HttpEndpointImpl endpoint = new HttpEndpointImpl();
        Field name_field = HttpEndpointImpl.class.getDeclaredField("name");
        name_field.setAccessible(true);
        name_field.set(endpoint, "testEndpoint");

        // the defaultHost == localhost (which is the default), so it should try to resolve '*' to something else
        String hostName = endpoint.resolveHostName("*", "localhost");
        assertFalse("Should resolve * to something other than localhost", HttpServiceConstants.LOCALHOST.equals(hostName));

        // the defaultHost is something specific, so it should use that instead (should use that exact value!)
        hostName = endpoint.resolveHostName("*", "127.0.0.1");
        assertEquals("Should resolve * to localhost: defaultHostName is set to 127.0.0.1, so it should use 127.0.0.1",
                     "127.0.0.1", hostName);

        // specify a specific host value that is a bunch of garbage.
        hostName = endpoint.resolveHostName("unresolvable.nonsense.no.way", "127.0.0.1");
        assertNull("Should return null, due to the specific host value containing unresolvable value", hostName);

        // specify a default host value that is a bunch of garbage.
        hostName = endpoint.resolveHostName("*", "unresolvable.nonsense.no.way");
        assertEquals("Should resolve localhost, due to unresolvable defaultHostName value", "localhost", hostName);
        assertTrue("Should see a warning message about unresolvable default host", outputMgr.checkForStandardOut("CWWKT0023W"));

        // The default is empty, so is not used. The answer should not be localhost
        hostName = endpoint.resolveHostName("*", "");
        assertFalse("Should resolve * to something other than localhost, defaultHostName is also empty", HttpServiceConstants.LOCALHOST.equals(hostName));

    }

}
