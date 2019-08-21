/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.scim20.model.schemas;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.schemas.SchemaAttribute;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class SchemaAttributeImplTest {
    @Test
    public void serialize() throws Exception {
        SchemaAttributeImpl attribute = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"name\":\"name\",");
        expected.append("\"type\":\"type\",");
        expected.append("\"subAttributes\":[{\"name\":\"subAttribute1\"},{\"name\":\"subAttribute2\"}],");
        expected.append("\"multiValued\":false,");
        expected.append("\"description\":\"description\",");
        expected.append("\"required\":false,");
        expected.append("\"canonicalValues\":[\"canonicalValue1\",\"canonicalValue2\"],");
        expected.append("\"caseExact\":false,");
        expected.append("\"mutability\":\"mutability\",");
        expected.append("\"returned\":\"returned\",");
        expected.append("\"uniqueness\":\"uniqueness\",");
        expected.append("\"referenceTypes\":[\"referenceType1\",\"referenceType2\"]");
        expected.append("}");

        /*
         * Serialize.
         */
        String serializedResponse = SCIMUtil.serialize(attribute);
        assertEquals(expected.toString(), serializedResponse);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }

    public static SchemaAttributeImpl getTestInstance() {
        SchemaAttributeImpl subAttribute1 = new SchemaAttributeImpl();
        subAttribute1.setName("subAttribute1");
        SchemaAttributeImpl subAttribute2 = new SchemaAttributeImpl();
        subAttribute2.setName("subAttribute2");

        SchemaAttributeImpl attribute = new SchemaAttributeImpl();
        attribute.setCanonicalValues(Arrays.asList(new String[] { "canonicalValue1", "canonicalValue2" }));
        attribute.setCaseExact(false);
        attribute.setDescription("description");
        attribute.setMultiValued(false);
        attribute.setMutability("mutability");
        attribute.setName("name");
        attribute.setReferenceTypes(Arrays.asList(new String[] { "referenceType1", "referenceType2" }));
        attribute.setRequired(false);
        attribute.setReturned("returned");
        attribute.setSubAttributes(Arrays.asList(new SchemaAttribute[] { subAttribute1, subAttribute2 }));
        attribute.setType("type");
        attribute.setUniqueness("uniqueness");
        return attribute;
    }
}
