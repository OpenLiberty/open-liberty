/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ActionStringTest {

    /**
     * Tests constructor
     * Expected result: make sure that the object is constructed properly.
     */
    @Test
    public void CtorNull() {
        ActionString as = new ActionString();
        assertNotNull(as);
        assertNull(as.getActions());

        String data = "testString";
        as = new ActionString(data);
        assertNotNull(as);
        assertEquals(data, as.getActions());
    }

    /**
     * Tests constructor
     * Expected result: make sure that the object is constructed properly.
     */
    @Test
    public void CtorParam() {
        String data = "testString";
        ActionString as = new ActionString(data);
        assertNotNull(as);
        assertEquals(data, as.getActions());
    }

    /**
     * Tests setActions method
     * Expected result: make sure that the object is set properly.
     */
    @Test
    public void setActionsNormal() {
        String data = "testString";
        ActionString as = new ActionString();
        assertNotNull(as);
        as.setActions(data);
        assertEquals(data, as.getActions());
    }

    /**
     * Tests getReverseActions method
     * Expected result: make sure the output is correct.
     */
    @Test
    public void getReverseActionsNull() {
        ActionString as = new ActionString();
        assertNull(as.getReverseActions());
    }

    /**
     * Tests getReverseActions method
     * Expected result: make sure the output is correct.
     */
    @Test
    public void getReverseActionsValid() {
        String input = "GET,POST";
        String output = "!GET,POST";
        ActionString as = new ActionString(input);
        assertEquals(output, as.getReverseActions());
        as = new ActionString(output);
        assertEquals(input, as.getReverseActions());
    }
}
