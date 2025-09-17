/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ui.internal.v1.pojo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.persistence.InvalidPOJOException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

import test.common.JSONablePOJOTestCase;
import test.common.SharedOutputManager;

/**
 *
 */
public class MessageTest extends JSONablePOJOTestCase {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    @Before
    public void setUp() {
        jsonablePojo = new Message(400, "myMessage");
        sourceJson = "{\"status\":400,\"message\":\"myMessage\"}";
    }

    /**
     * Ensure that the public constructor and all getter/setter operations
     * work as expected. This is consolidated into one test method as this
     * is a very basic validation.
     */
    @Test
    public void constructorAndSetters() {
        Message msg = new Message(400, "myMessage");

        assertEquals("FAIL: The status was not the value set in the constructor",
                     400, msg.getStatus());
        assertEquals("FAIL: The message was not the value set in the constructor",
                     "myMessage", msg.getMessage());
        assertNull("FAIL: The userMessage should be null by default",
                   msg.getUserMessage());
        assertNull("FAIL: The developerMessage should be null by default",
                   msg.getDeveloperMessage());

        msg.setUserMessage("newUserMessage");
        msg.setDeveloperMessage("newDeveloperMessage");

        assertEquals("FAIL: The userMessage was not the value set",
                     "newUserMessage", msg.getUserMessage());
        assertEquals("FAIL: The developerMessage was not the value set",
                     "newDeveloperMessage", msg.getDeveloperMessage());
    }

    @Test
    public void validateSelf() throws Exception {
        Message msg = new Message(400, "myMessage");
        msg.validateSelf();
    }

    @Test(expected = InvalidPOJOException.class)
    public void validateSelfInvalidStatus() throws Exception {
        Message msg = new Message(-1, "myMessage");
        msg.validateSelf();
    }

    @Test(expected = InvalidPOJOException.class)
    public void validateSelfNullMessage() throws Exception {
        Message msg = new Message(400, null);
        msg.validateSelf();
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameInstance() {
        final Message m1 = new Message(400, "myMessage");
        assertEquals("FAIL: The same Tool instance did not compare as equals=true",
                     m1, m1);
        assertEquals("FAIL: The same Message instance did not compare hashCode as equals=true",
                     m1.hashCode(), m1.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsRequiredTrue() {
        final Message m1 = new Message(400, "myMessage");
        final Message m2 = new Message(400, "myMessage");
        assertEquals("FAIL: Two Tools with only required fields did not compare as equals",
                     m1, m2);
        assertEquals("FAIL: Two equal Tools with only required fields did not compare hashCode as equals",
                     m1.hashCode(), m2.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsAllTrue() {
        final String userMessage = "myUserMessage";
        final String developerMessage = "myDeveloperMessage";

        final Message m1 = new Message(400, "myMessage");
        m1.setUserMessage(userMessage);
        m1.setDeveloperMessage(developerMessage);

        final Message m2 = new Message(400, "myMessage");
        m2.setUserMessage(userMessage);
        m2.setDeveloperMessage(developerMessage);

        assertEquals("FAIL: Two Tools with all fields did not compare as equals",
                     m1, m2);
        assertEquals("FAIL: Two equal Tools with all fields did not compare hashCode as equals",
                     m1.hashCode(), m2.hashCode());
    }

    @Test
    public void equalsMismatchStatus() {
        final Message m1 = new Message(400, "myMessage");
        final Message m2 = new Message(401, "myMessage");
        assertFalse("FAIL: Two Messages considered equal when they have different 'status' values",
                    m1.equals(m2));
    }

    @Test
    public void equalsMismatchMessage() {
        final Message m1 = new Message(400, "myMessage");
        final Message m2 = new Message(400, "myMessag");
        assertFalse("FAIL: Two Messages considered equal when they have different 'message' values",
                    m1.equals(m2));
    }

    @Test
    public void equalsMismatchUserMessage() {
        final Message m1 = new Message(400, "myMessage");
        m1.setUserMessage("myUserMessage");
        final Message m2 = new Message(400, "myMessage");
        m2.setUserMessage("myUserMessag");
        assertFalse("FAIL: Two Messages considered equal when they have different 'userMessage' values",
                    m1.equals(m2));
    }

    @Test
    public void equalsMismatchNullUserMessage() {
        final Message m1 = new Message(400, "myMessage");
        final Message m2 = new Message(400, "myMessage");
        m2.setUserMessage("myUserMessage");
        assertFalse("FAIL: Two Messages considered equal when they have different 'userMessage' values",
                    m1.equals(m2));
    }

    @Test
    public void equalsMismatchDeveloperMessage() {
        final Message m1 = new Message(400, "myMessage");
        m1.setDeveloperMessage("myDeveloperMessage");
        final Message m2 = new Message(400, "myMessage");
        m2.setDeveloperMessage("myDeveloperMessag");
        assertFalse("FAIL: Two Messages considered equal when they have different 'developerMessage' values",
                    m1.equals(m2));
    }

    @Test
    public void equalsMismatchNullDeveloperMessage() {
        final Message m1 = new Message(400, "myMessage");
        final Message m2 = new Message(400, "myMessage");
        m2.setDeveloperMessage("myDeveloperMessag");
        assertFalse("FAIL: Two Messages considered equal when they have different 'developerMessage' values",
                    m1.equals(m2));
    }

    @Test
    public void equalsNotATool() {
        assertFalse("FAIL: A non-Message object was considered to equal a Message",
                    jsonablePojo.equals(new Object()));
    }

    @Test
    public void equalsNull() {
        assertFalse("FAIL: Null was conisdered to equal an Message",
                    jsonablePojo.equals(null));
    }

    /** {@inheritDoc} */
    @Override
    @Test
    public void incompleteJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Message incompleteObj = mapper.readValue("{\"status\": 400}", Message.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidPOJOException e) {
            assertNotNull("FAIL: Should have caught an InvalidPOJOException", e);
        }
        assertFalse("FAIL: An incomplete Message was considered to equal a valid Tool",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Message with no 'id' should have a zero hashcode",
                   400 == incompleteObj.hashCode());
    }

    /** {@inheritDoc} */
    @Test
    public void incompleteJsonNoStatus() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Message incompleteObj = mapper.readValue("{\"message\": \"myMessage\"}", Message.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidPOJOException e) {
            assertNotNull("FAIL: Should have caught an InvalidPOJOException", e);
        }
        assertFalse("FAIL: An incomplete Message was considered to equal a valid Tool",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Message with a 'message' should have a non-zero hashcode",
                   0 != incompleteObj.hashCode());
    }

    /** {@inheritDoc} */
    @Test
    public void incompleteJsonNoRequiredFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        Message incompleteObj = mapper.readValue("{\"userMessage\": \"myMessage\",\"developerMessage\": \"myMessage\"}", Message.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: Validation should have failed");
        } catch (InvalidPOJOException e) {
            assertNotNull("FAIL: Should have caught an InvalidPOJOException", e);
        }
        assertFalse("FAIL: An incomplete Message was considered to equal a valid Tool",
                    incompleteObj.equals(jsonablePojo));
        assertTrue("FAIL: An incomplete Message with no 'id' should have a non-zero hashcode",
                   0 == incompleteObj.hashCode());
    }

    /** Valdiate Self! */
    @Override
    protected void extraPojoMatchesSourceJSONChecks(SelfValidatingPOJO unmarshalledPojo) throws Exception {
        unmarshalledPojo.validateSelf();
    }
}
