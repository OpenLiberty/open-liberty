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
package com.ibm.ws.jmx.connector.server.rest.helpers;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 * Test for {@link com.ibm.ws.jmx.connector.server.rest.helpers.POJOHelper }
 */
public class POJOHelperTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private POJOHelper pojoHelper;

    @Before
    public void setUp() {
        pojoHelper = new POJOHelper();
    }

    @After
    public void tearDown() {
        pojoHelper = null;
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.server.rest.helpers.POJOHelper#getPOJOObject()}.
     */
    @Test
    public void testGetPOJOObject() throws Exception {
        final String json = "{\"pojoExample\":{\"value\":\"valueStructure\",\"type\":\"typeStructure\"";
        String creds = pojoHelper.getPOJOObject();
        assertTrue("POJO does not contain JSON objects", creds.contains(json));
    }

}
