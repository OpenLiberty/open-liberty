/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;

import org.junit.Test;

public class PolicyContextHandlerImplTest {

    /**
     * Tests constructor
     * Expected result: make sure that the object is constructed properly.
     */
    @Test
    public void Ctor() {
        PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
        assertNotNull(pch);
    }

    /**
     * Tests supports method
     * Expected result: make sure that it returns proper boolean value
     * 
     * @throws PolicyContextException
     */
    @Test
    public void supportsTrue() throws PolicyContextException {

        PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
        String[] keysArray = pch.getKeys();
        for (String key : keysArray) {
            assertTrue(pch.supports(key));
        }
    }

    /**
     * Tests supports method
     * Expected result: make sure that it returns proper boolean value
     * 
     * @throws PolicyContextException
     */
    @Test
    public void supportsFalse() throws PolicyContextException {
        String key = "invalid.value";
        PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
        assertFalse(pch.supports(key));
    }

    /**
     * Tests getContext method
     * Expected result: make sure that it returns proper context
     * 
     * @throws PolicyContextException
     */
    @Test
    public void getContextNull() throws PolicyContextException {
        PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
        assertNull(pch.getContext(null, null));
    }

    /**
     * Tests getContext method
     * Expected result: make sure that it returns proper context
     * 
     * @throws PolicyContextException
     */
    @Test
    public void getContextValid() throws PolicyContextException {
        Map<String, Object> map = new HashMap<String, Object>();
        String key1 = "key1";
        Object object1 = new Object();
        map.put(key1, object1);
        PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
        assertEquals(object1, pch.getContext(key1, map));
        assertNull(pch.getContext("key2", map));
    }

}
