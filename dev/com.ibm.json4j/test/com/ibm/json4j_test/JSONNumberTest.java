/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial tests.
 *******************************************************************************/
package com.ibm.json4j_test;

import com.ibm.json.java.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static junit.framework.Assert.assertEquals;

public class JSONNumberTest {

    /**
     * Test that a whole number larger than a 64bit whole number can be parsed.
     * @throws IOException If there is an error parsing the JSON
     */
    @Test
    public void testLargeWholeNumber() throws IOException {
        String inputJson = "{\"number\":18446744073709551615}";

        JSONObject obj = JSONObject.parse(inputJson);

        assertEquals(new BigInteger("18446744073709551615"), obj.get("number"));
    }

    /**
     * Test that a whole number that fits in a 64bit whole number can be parsed.
     * @throws IOException If there is an error parsing the JSON
     */
    @Test
    public void testWholeNumber() throws IOException {
        String inputJson = "{\"number\":42}";

        JSONObject obj = JSONObject.parse(inputJson);

        assertEquals(42, ((Number)obj.get("number")).intValue());
    }

    /**
     * Test that a decimal number can be parsed.
     * @throws IOException If there is an error parsing the JSON
     */
    @Test
    public void testDecimalNumber() throws IOException {
        String inputJson = "{\"number\":42.0}";

        JSONObject obj = JSONObject.parse(inputJson);

        assertEquals(42.0, ((Number)obj.get("number")).doubleValue());
    }

    /**
     * Test that a decimal number larger than a Java double can be parsed
     * @throws IOException If there is an error parsing the JSON
     */
    @Test
    public void testLargeDecimalNumber() throws IOException {
        String inputJson = "{\"number\":2.0e500}";

        JSONObject obj = JSONObject.parse(inputJson);

        assertEquals(new BigDecimal("2.0e500"), obj.get("number"));
    }

    /**
     * Test that a negative number can be parsed
     * @throws IOException If there is an error parsing the JSON
     */
    @Test
    public void testNegativeNumber() throws IOException {
        String inputJson = "{\"number\":-42}";

        JSONObject obj = JSONObject.parse(inputJson);

        assertEquals(-42, ((Number)obj.get("number")).intValue());
    }
}