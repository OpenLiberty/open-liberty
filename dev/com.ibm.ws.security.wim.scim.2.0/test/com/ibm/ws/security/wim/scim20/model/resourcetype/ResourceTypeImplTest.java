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

package com.ibm.ws.security.wim.scim20.model.resourcetype;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.resourcetype.SchemaExtension;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class ResourceTypeImplTest {

    @Test
    public void serialize() throws Exception {
        SchemaExtensionImpl extension1 = new SchemaExtensionImpl();
        extension1.setRequired(true);
        extension1.setSchema("schema1");
        SchemaExtensionImpl extension2 = new SchemaExtensionImpl();
        extension2.setRequired(false);
        extension2.setSchema("schema2");

        ResourceTypeImpl resourceType = new ResourceTypeImpl();
        resourceType.setDescription("description");
        resourceType.setEndpoint("endpoint");
        resourceType.setId("id");
        resourceType.setName("name");
        resourceType.setSchema("schema");
        resourceType.setSchemaExtensions(Arrays.asList(new SchemaExtension[] { extension1, extension2 }));

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"schemas\":[\"" + ResourceTypeImpl.SCHEMA_URN + "\"],");
        expected.append("\"id\":\"id\",");
        expected.append("\"name\":\"name\",");
        expected.append("\"description\":\"description\",");
        expected.append("\"endpoint\":\"endpoint\",");
        expected.append("\"schema\":\"schema\",");
        expected.append("\"schemaExtensions\":[{");
        expected.append("\"schema\":\"schema1\",");
        expected.append("\"required\":true");
        expected.append("},{");
        expected.append("\"schema\":\"schema2\",");
        expected.append("\"required\":false");
        expected.append("}]");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(resourceType);
        assertEquals(expected.toString(), serialized);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }
}
