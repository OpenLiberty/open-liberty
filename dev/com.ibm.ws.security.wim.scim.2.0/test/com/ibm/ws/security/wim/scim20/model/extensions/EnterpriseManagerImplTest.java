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

import com.ibm.websphere.security.wim.scim20.model.extensions.EnterpriseManager;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class EnterpriseManagerImplTest {

    @Test
    public void serialize() throws Exception {
        EnterpriseManagerImpl manager = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"value\",");
        expected.append("\"$ref\":\"$ref\",");
        expected.append("\"displayName\":\"displayName\"");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(manager);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        EnterpriseManager deserialized = SCIMUtil.deserialize(serialized, EnterpriseManager.class);
        assertEquals(manager, deserialized);
    }

    public static EnterpriseManagerImpl getTestInstance() {
        EnterpriseManagerImpl manager = new EnterpriseManagerImpl();
        manager.setDisplayName("displayName");
        manager.setRef("$ref");
        manager.setValue("value");
        return manager;
    }
}
