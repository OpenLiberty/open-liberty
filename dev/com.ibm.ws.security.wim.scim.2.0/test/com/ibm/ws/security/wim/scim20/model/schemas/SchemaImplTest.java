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

public class SchemaImplTest {
    @Test
    public void serialize() throws Exception {
        SchemaImpl schema = new SchemaImpl();
        schema.setAttributes(Arrays.asList(new SchemaAttribute[] { SchemaAttributeImplTest.getTestInstance() }));
        schema.setDescription("description");
        schema.setId("id");
        schema.setName("name");

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{\"id\":\"id\",");
        expected.append("\"name\":\"name\",");
        expected.append("\"description\":\"description\",");
        expected.append("\"attributes\":[{");
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
        expected.append("}]}");

        /*
         * Serialize.
         */
        String serializedResponse = SCIMUtil.serialize(schema);
        assertEquals(expected.toString(), serializedResponse);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }
}
