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
package test.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.ibm.ws.ui.persistence.SelfValidatingPOJO;

/**
 * Common root class for any JSON-able POJO unit test.
 */
public abstract class JSONablePOJOTestCase {
    protected Object jsonablePojo;
    protected String sourceJson;

    /**
     * Tests that the Object Under Test is JSONable using Jackson.
     * This simply drives a Jackon marshall / unmarshall to check
     * that the flow back and forth works. It does not test the
     * correctness of the resulting JSON.
     */
    @Test
    public void isJSONable() throws Exception {
        assertNotNull("FAIL: test development error.\nPlease set the 'jsonablePojo' field to an object instance.\nThis is normally done in setUp.",
                      jsonablePojo);

        ObjectMapper mapper = new ObjectMapper();

        // Step 1: Marshall (aka serialize) the Java object to JSON format using Jackson's ObjectMapper
        String json = mapper.writeValueAsString(jsonablePojo);
        System.out.println("Jackson JSON represetation: " + json);
        System.out.println(mapper.defaultPrettyPrintingWriter().writeValueAsString(jsonablePojo));

        // Step 2: Unmarshall (aka deserialize) the JSON into a Java Object
        Object unmarshalledPojo = mapper.readValue(json, jsonablePojo.getClass());
        System.out.println("Deserialized to object: " + unmarshalledPojo);
        System.out.println(mapper.defaultPrettyPrintingWriter().writeValueAsString(unmarshalledPojo));

        assertEquals("FAIL: The unmarshedlled POJO from the JSON does not equal the original POJO",
                     jsonablePojo, unmarshalledPojo);
    }

    /**
     * Extension point for additional checks. Must be implemented by caller
     * even if it does no work. It is recommended that unmarshalledPojo.validateSelf
     * is minimally invoked, because it has a high probability of uncovering NPEs.
     * 
     * @param unmarshalledPojo
     * @throws Exception
     */
    abstract protected void extraPojoMatchesSourceJSONChecks(SelfValidatingPOJO unmarshalledPojo) throws Exception;

    /**
     * Tests that the Object Under Test is JSONable using Jackson.
     */
    @Test
    public void pojoMatchesSourceJSON() throws Exception {
        assertNotNull("FAIL: test development error.\nPlease set the 'jsonablePojo' field to an object instance.\nThis is normally done in setUp.",
                      jsonablePojo);
        assertNotNull("FAIL: test development error.\nPlease set the 'expectedJson' field to the expected JSON String for 'jsonablePojo'.\nThis is normally done in setUp.",
                      sourceJson);

        ObjectMapper mapper = new ObjectMapper();

        String json = mapper.writeValueAsString(jsonablePojo);
        System.out.println("Jackson JSON represetation: " + json);

        Object unmarshalledPojo = mapper.readValue(sourceJson, jsonablePojo.getClass());
        extraPojoMatchesSourceJSONChecks((SelfValidatingPOJO) unmarshalledPojo);
        System.out.println("Deserialized to object: " + unmarshalledPojo);
        System.out.println(mapper.defaultPrettyPrintingWriter().writeValueAsString(unmarshalledPojo));

        assertEquals("FAIL: The unmarshedlled POJO from the JSON does not equal the original POJO",
                     jsonablePojo, unmarshalledPojo);
    }

    /**
     * Reminder for test case writers to implement at least some form of incomplete
     * JSON representation test. Because these are POJOs, the JSON could be incomplete
     * and result in an 'invalid' POJO. The definition of a valid POJO is up to the
     * POJO itself.
     * <p>
     * It is recommended that tests will drive o.equals(), o.hashCode() and o.validateSelf().
     * Note that we do not need to test incorrect JSON. Jackson handles that. Jackson does not
     * understand what an incomplete Object means however, which is why we have these tests.
     */
    @Test
    public void incompleteJson() throws Exception {
        fail("FAIL: test development error.\nPlease override this method.\nImplement at least one test method to validate that the POJO can detect if it is incomplete.");
    }
}
