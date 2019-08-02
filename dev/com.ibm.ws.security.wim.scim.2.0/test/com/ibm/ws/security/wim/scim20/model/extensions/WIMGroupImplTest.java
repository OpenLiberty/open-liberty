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

import com.ibm.websphere.security.wim.scim20.model.extensions.WIMGroup;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class WIMGroupImplTest {

    @Test
    public void serialize() throws Exception {
        WIMGroupImpl wimGroup = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"identifier\":{");
        expected.append("\"uniqueId\":\"uniqueId\",");
        expected.append("\"uniqueName\":\"uniqueName\",");
        expected.append("\"externalId\":\"externalId\",");
        expected.append("\"externalName\":\"externalName\",");
        expected.append("\"repositoryId\":\"repositoryId\"");
        expected.append("},");
        expected.append("\"cn\":\"cn\",");
        expected.append("\"myProperty1\":\"myValue1\",");
        expected.append("\"myProperty2\":\"myValue2\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(wimGroup);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        WIMGroup deserialized = SCIMUtil.deserialize(serialized, WIMGroup.class);
        assertEquals(wimGroup, deserialized);
    }

    public static WIMGroupImpl getTestInstance() {
        WIMGroupImpl wimGroup = new WIMGroupImpl();
        wimGroup.setIdentifier(WIMIdentifierImplTest.getTestInstance());
        wimGroup.setCn("cn");
        wimGroup.setExtendedProperty("myProperty1", "myValue1");
        wimGroup.setExtendedProperty("myProperty2", "myValue2");
        return wimGroup;
    }
}
