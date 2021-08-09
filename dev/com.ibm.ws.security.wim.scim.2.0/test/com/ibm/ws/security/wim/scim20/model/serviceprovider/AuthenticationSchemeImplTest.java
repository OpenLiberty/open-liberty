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

package com.ibm.ws.security.wim.scim20.model.serviceprovider;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class AuthenticationSchemeImplTest {
    @Test
    public void serialize() throws Exception {
        AuthenticationSchemeImpl scheme = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"type\":\"type\",");
        expected.append("\"name\":\"name\",");
        expected.append("\"description\":\"description\",");
        expected.append("\"specUri\":\"specUri\",");
        expected.append("\"documentationUri\":\"documentationUri\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serializedResponse = SCIMUtil.serialize(scheme);
        assertEquals(expected.toString(), serializedResponse);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }

    public static AuthenticationSchemeImpl getTestInstance() {
        AuthenticationSchemeImpl scheme = new AuthenticationSchemeImpl();
        scheme.setDescription("description");
        scheme.setDocumentationUri("documentationUri");
        scheme.setName("name");
        scheme.setSpecUri("specUri");
        scheme.setType("type");
        return scheme;
    }
}
