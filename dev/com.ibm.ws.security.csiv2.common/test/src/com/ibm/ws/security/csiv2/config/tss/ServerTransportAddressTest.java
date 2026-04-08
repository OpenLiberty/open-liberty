/*
 * Copyright 2017, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.config.tss;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CSIIOP.TransportAddress;

/**
 *
 */
public class ServerTransportAddressTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {}

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.config.tss.ServerTransportAddress#hashCode()}.
     */
    @Test
    public void testHashCode() {
        Map<ServerTransportAddress, Object> addressTransportMechMap = new HashMap<ServerTransportAddress, Object>();
        ServerTransportAddress address1 = new ServerTransportAddress(new TransportAddress("host", (short) 123));
        ServerTransportAddress address2 = new ServerTransportAddress(new TransportAddress("host", (short) 123));

        Object object1 = new Object();
        Object object2 = new Object();
        addressTransportMechMap.put(address1, object1);
        addressTransportMechMap.put(address2, object2);

        assertEquals("The hash codes must be the same.", address1.hashCode(), address2.hashCode());
        assertEquals("There must only be one address in the map.", 1, addressTransportMechMap.size());
    }

}
