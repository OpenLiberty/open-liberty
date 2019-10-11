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

package com.ibm.ws.security.wim.scim20.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.groups.Group;
import com.ibm.ws.security.wim.scim20.SCIMUtil;
import com.ibm.ws.security.wim.scim20.model.groups.GroupImpl;

public class SCIMListResponseImplTest {
    @Test
    public void serialize() throws Exception {
        Group group1 = new GroupImpl();
        group1.setDisplayName("group1");
        Group group2 = new GroupImpl();
        group2.setDisplayName("group2");

        ListResponseImpl<Group> listResponse = new ListResponseImpl<Group>();
        listResponse.setItemsPerPage(10);
        listResponse.setStartIndex(0);
        listResponse.setTotalResults(2);
        listResponse.setResources(Arrays.asList(new Group[] { group1, group2 }));

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:ListResponse\"],");
        expected.append("\"totalResults\":2,");
        expected.append("\"startIndex\":0,");
        expected.append("\"itemsPerPage\":10,");
        expected.append("\"Resources\":[{");
        expected.append("\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:Group\"],\"displayName\":\"group1\""); // TODO Need clarification in spec about when to return "schemas" in result
        expected.append("},{");
        expected.append("\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:Group\"],\"displayName\":\"group2\""); // TODO Need clarification in spec about when to return "schemas" in result
        expected.append("}]");
        expected.append("}");

        /*
         * Serialize.
         */
        String serializedResponse = SCIMUtil.serialize(listResponse);
        assertEquals(expected.toString(), serializedResponse);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }
}
