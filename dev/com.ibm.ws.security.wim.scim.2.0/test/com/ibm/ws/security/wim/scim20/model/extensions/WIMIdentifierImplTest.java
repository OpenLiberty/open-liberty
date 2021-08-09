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

package com.ibm.ws.security.wim.scim20.model.extensions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.extensions.WIMIdentifier;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class WIMIdentifierImplTest {
    @Test
    public void serialize() throws Exception {

        WIMIdentifierImpl wimIdentifier = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"uniqueId\":\"uniqueId\",");
        expected.append("\"uniqueName\":\"uniqueName\",");
        expected.append("\"externalId\":\"externalId\",");
        expected.append("\"externalName\":\"externalName\",");
        expected.append("\"repositoryId\":\"repositoryId\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(wimIdentifier);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        WIMIdentifier deserialized = SCIMUtil.deserialize(serialized, WIMIdentifier.class);
        assertEquals(wimIdentifier, deserialized);
    }

    public static WIMIdentifierImpl getTestInstance() {
        WIMIdentifierImpl wimUser = new WIMIdentifierImpl();
        wimUser.setExternalId("externalId");
        wimUser.setExternalName("externalName");
        wimUser.setRepositoryId("repositoryId");
        wimUser.setUniqueId("uniqueId");
        wimUser.setUniqueName("uniqueName");
        return wimUser;
    }
}
