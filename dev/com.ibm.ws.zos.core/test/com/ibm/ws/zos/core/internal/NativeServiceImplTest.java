/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JUnit tests for {@code NativeServiceImpl}.
 */
public class NativeServiceImplTest {

    @Test
    public void testNativeServiceImpl() {
        NativeServiceImpl testImpl = new NativeServiceImpl("SERVICE1", "SAFGRP1", false, false);
        assertEquals("SERVICE1", testImpl.getServiceName());
        assertEquals("SAFGRP1", testImpl.getAuthorizationGroup());
        assertFalse(testImpl.isPermitted());
        assertTrue(testImpl.toString().contains("serviceName=SERVICE1"));
        assertTrue(testImpl.toString().contains("authorizationGroup=SAFGRP1"));
        assertTrue(testImpl.toString().contains("permitted=false"));

        testImpl = new NativeServiceImpl("SERVICE2", "SAFGRP2", true, false);
        assertEquals("SERVICE2", testImpl.getServiceName());
        assertEquals("SAFGRP2", testImpl.getAuthorizationGroup());
        assertTrue(testImpl.isPermitted());
        assertTrue(testImpl.toString().contains("serviceName=SERVICE2"));
        assertTrue(testImpl.toString().contains("authorizationGroup=SAFGRP2"));
        assertTrue(testImpl.toString().contains("permitted=true"));
    }

}
