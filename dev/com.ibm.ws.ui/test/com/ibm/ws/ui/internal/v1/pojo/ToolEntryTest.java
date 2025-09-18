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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.ui.internal.v1.ITool;
import com.ibm.ws.ui.internal.v1.utils.LogRule;
import com.ibm.ws.ui.internal.validation.InvalidToolException;
import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

import test.common.JSONablePOJOTestCase;
import test.common.SharedOutputManager;

/**
 *
 */
public class ToolEntryTest extends JSONablePOJOTestCase {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = new LogRule(outputMgr);

    private final String TOOL_ID = "tool-1.0";
    private final String TOOL_TYPE = ITool.TYPE_FEATURE_TOOL;

    @Before
    public void setUp() {
        jsonablePojo = new ToolEntry(TOOL_ID, TOOL_TYPE);
        sourceJson = "{\"id\":\"tool-1.0\",\"type\":\"featureTool\"}";
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#ToolEntry(java.lang.String, java.lang.String)}.
     */
    @Test
    public void getters() {
        final ITool tool = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertEquals("FAIL: ToolEntry did not return the expected ID",
                     TOOL_ID, tool.getId());
        assertEquals("FAIL: ToolEntry did not return the expected type",
                     TOOL_TYPE, tool.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Override
    @Test
    public void incompleteJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id, type"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"type\":\"featureTool\"}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"id\":null,\"type\":\"featureTool\"}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyId() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"id\":\"\",\"type\":\"featureTool\"}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("id"));
        }
        assertFalse("FAIL: An incomplete Toolbox was considered to equal a valid Toolbox",
                    incompleteObj.equals(jsonablePojo));
        assertEquals("FAIL: An incomplete Toolbox should have a zero hashcode",
                     0, incompleteObj.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_missingType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"id\":\"tool-1.0\"}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_nullType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"id\":\"tool-1.0\",\"type\":null}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_emptyType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"id\":\"tool-1.0\",\"type\":\"\"}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("type"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void incompleteJson_incorrectType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        ToolEntry incompleteObj = mapper.readValue("{\"id\":\"tool-1.0\",\"type\":\"wrongType\"}", ToolEntry.class);
        try {
            incompleteObj.validateSelf();
            fail("FAIL: InvalidToolException should have been thrown");
        } catch (InvalidToolException e) {
            assertNotNull("FAIL: Should have caught an InvalidToolboxException", e);
            assertTrue("FAIL: InvalidToolException message did not contain the missing fields. Message: " + e.getMessage(),
                       e.getMessage().contains("wrongType"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void validateSelf_featureTool() throws Exception {
        final ITool tool = new ToolEntry(TOOL_ID, ITool.TYPE_FEATURE_TOOL);
        tool.validateSelf();
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#validateSelf()}.
     */
    @Test
    public void validateSelf_bookmark() throws Exception {
        final ITool tool = new ToolEntry(TOOL_ID, ITool.TYPE_BOOKMARK);
        tool.validateSelf();
    }

    /** {@inheritDoc} */
    @Override
    protected void extraPojoMatchesSourceJSONChecks(SelfValidatingPOJO unmarshalledPojo) throws Exception {
        unmarshalledPojo.validateSelf();
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameInstance() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertEquals("FAIL: The same ToolEntry instance did not compare as equals=true",
                     t1, t1);
        assertEquals("FAIL: The same ToolEntry instance did not compare hashCode as equals=true",
                     t1.hashCode(), t1.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameValues() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        final ITool t2 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertEquals("FAIL: Two equal ToolEntry with only required fields did not compare as equals",
                     t1, t2);
        assertEquals("FAIL: Two equal ToolEntry with only required fields did not compare hashCode as equals",
                     t1.hashCode(), t2.hashCode());
    }

    /**
     * Test equivalence across .equals() and .hashCode().
     */
    @Test
    public void equalsSameValuesEmpty() {
        final ITool t1 = new ToolEntry();
        final ITool t2 = new ToolEntry();
        assertEquals("FAIL: Two equal ToolEntry with only required fields did not compare as equals",
                     t1, t2);
        assertEquals("FAIL: Two equal ToolEntry with only required fields did not compare hashCode as equals",
                     t1.hashCode(), t2.hashCode());
    }

    @Test
    public void equalsNullThisId() {
        final ITool t1 = new ToolEntry(null, TOOL_TYPE);
        final ITool t2 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertFalse("FAIL: Two ToolEntry considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatId() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        final ITool t2 = new ToolEntry(null, TOOL_TYPE);
        assertFalse("FAIL: Two ToolEntry considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchId() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        final ITool t2 = new ToolEntry("tool-2.0", TOOL_TYPE);
        assertFalse("FAIL: Two ToolEntry considered equal when they have different 'id' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThisType() {
        final ITool t1 = new ToolEntry(TOOL_ID, null);
        final ITool t2 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertFalse("FAIL: Two ToolEntry considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNullThatType() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        final ITool t2 = new ToolEntry(TOOL_ID, null);
        assertFalse("FAIL: Two ToolEntry considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsMismatchType() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        final ITool t2 = new ToolEntry(TOOL_ID, ITool.TYPE_BOOKMARK);
        assertFalse("FAIL: Two ToolEntry considered equal when they have different 'type' values",
                    t1.equals(t2));
    }

    @Test
    public void equalsNotAToolEntry() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertFalse("FAIL: A non-ToolEntry object was considered to equal a ToolEntry",
                    t1.equals(new Object()));
    }

    @Test
    public void equalsNull() {
        final ITool t1 = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertFalse("FAIL: Null was conisdered to equal a Tool",
                    t1.equals(null));
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#hashCode()}.
     */
    @Test
    public void hashCode_nullId() {
        final ITool tool = new ToolEntry(null, TOOL_TYPE);
        assertEquals("FAIL: when the ID is null (which is not a valid case) hashcode should be 0",
                     0, tool.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#hashCode()}.
     */
    @Test
    public void hashCode_hasId() {
        final ITool tool = new ToolEntry(TOOL_ID, TOOL_TYPE);
        assertFalse("FAIL: when the ID is set hashcode should be non-zero",
                    0 == tool.hashCode());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#toString()}.
     */
    @Test
    public void toString_emptyObject() {
        final ITool tool = new ToolEntry();
        assertEquals("FAIL: the empty tool object did not return the expected toString",
                     "ToolEntry {\"id\":null,\"type\":null}", tool.toString());
    }

    /**
     * Test method for {@link com.ibm.ws.ui.internal.v1.pojo.ToolEntry#toString()}.
     */
    @Test
    public void toString_validObject() {
        assertEquals("FAIL: the valid tool object did not return the expected toString",
                     "ToolEntry " + sourceJson, jsonablePojo.toString());
    }
}
