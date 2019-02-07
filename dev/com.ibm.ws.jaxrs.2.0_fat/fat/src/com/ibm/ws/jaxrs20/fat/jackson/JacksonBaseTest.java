/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat.jackson;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.ws.jaxrs20.fat.AbstractTest;

public class JacksonBaseTest extends AbstractTest {

    protected static String target;

    protected static final Map<String, String> params = new HashMap<String, String>();

    @Test
    public void testGETPerson() throws Exception {
        this.runTestOnServer(target, "testGETPerson", params, "OK");
    }

    @Test
    public void testPOSTPerson() throws Exception {
        this.runTestOnServer(target, "testPOSTPerson", params, "OK");
    }

    @Test
    public void testGETCollection() throws Exception {
        this.runTestOnServer(target, "testGETCollection", params, "OK");
    }

    @Test
    public void testPOSTCollection() throws Exception {
        this.runTestOnServer(target, "testPOSTCollection", params, "OK");
    }

    @Test
    public void testGETCollectionWithObject() throws Exception {
        this.runTestOnServer(target, "testGETCollectionWithObject", params, "OK");
    }

    @Test
    public void testPOSTCollectionWithObject() throws Exception {
        this.runTestOnServer(target, "testPOSTCollectionWithObject", params, "OK");
    }

    @Test
    public void testGETCollectionWithCollection() throws Exception {
        this.runTestOnServer(target, "testGETCollectionWithCollection", params, "OK");
    }

    @Test
    public void testPOSTCollectionWithCollection() throws Exception {
        this.runTestOnServer(target, "testPOSTCollectionWithCollection", params, "OK");
    }

    @Test
    public void testGETCollectionWithArray() throws Exception {
        this.runTestOnServer(target, "testGETCollectionWithArray", params, "OK");
    }

    @Test
    public void testPOSTCollectionWithArray() throws Exception {
        this.runTestOnServer(target, "testPOSTCollectionWithArray", params, "OK");
    }

    /**
     * Tests that a HashMap can be serialized and deserialized via Jackson.
     *
     * @throws Exception
     */
    @Test
    public void testHashMap() throws Exception {
        this.runTestOnServer(target, "testHashMap", params, "OK");
    }

    /**
     * Tests that a List can be serialized and deserialized via Jackson.
     *
     * @throws Exception
     */
    @Test
    public void testList() throws Exception {
        this.runTestOnServer(target, "testList", params, "OK");
    }

    @Test
    public void testPOJO() throws Exception {
        this.runTestOnServer(target, "testPOJO", params, "OK");
    }
}
