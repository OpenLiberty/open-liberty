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

import com.ibm.websphere.security.wim.scim20.model.extensions.EnterpriseUser;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class EnterpriseUserImplTest {

    @Test
    public void serialize() throws Exception {
        EnterpriseManagerImpl manager = new EnterpriseManagerImpl();
        manager.setDisplayName("manager1");
        manager.setRef("../Users/uid=manager1,o=ibm,c=us");
        manager.setValue("uid=manager1,o=ibm,c=us");

        EnterpriseUserImpl enterpriseUser = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"employeeNumber\":\"employeeNumber\",");
        expected.append("\"costCenter\":\"costCenter\",");
        expected.append("\"organization\":\"organization\",");
        expected.append("\"division\":\"division\",");
        expected.append("\"department\":\"department\",");
        expected.append("\"manager\":{");
        expected.append("\"value\":\"value\",");
        expected.append("\"$ref\":\"$ref\",");
        expected.append("\"displayName\":\"displayName\"");
        expected.append("}");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(enterpriseUser);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        EnterpriseUser deserialized = SCIMUtil.deserialize(serialized, EnterpriseUser.class);
        assertEquals(enterpriseUser, deserialized);
    }

    public static EnterpriseUserImpl getTestInstance() {
        EnterpriseUserImpl enterpriseUser = new EnterpriseUserImpl();
        enterpriseUser.setCostCenter("costCenter");
        enterpriseUser.setDepartment("department");
        enterpriseUser.setDivision("division");
        enterpriseUser.setEmployeeNumber("employeeNumber");
        enterpriseUser.setManager(EnterpriseManagerImplTest.getTestInstance());
        enterpriseUser.setOrganization("organization");
        return enterpriseUser;
    }
}
