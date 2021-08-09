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

import java.util.Arrays;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.serviceprovider.AuthenticationScheme;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class ServiceProviderConfigImplTest {
    @Test
    public void serialize() throws Exception {
        ServiceProviderConfigImpl spc = new ServiceProviderConfigImpl();
        spc.setAuthenticationSchemes(
                                     Arrays.asList(new AuthenticationScheme[] { AuthenticationSchemeImplTest.getTestInstance() }));
        spc.setBulk(new BulkImpl());
        spc.setChangePassword(new ChangePasswordImpl());
        spc.setDocumentationUri("documentationUri");
        spc.setEtag(new ETagImpl());
        spc.setFilter(new FilterImpl());
        spc.setPatch(new PatchImpl());
        spc.setSort(new SortImpl());

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"documentationUri\":\"documentationUri\",");
        expected.append("\"patch\":{");
        expected.append("\"supported\":false");
        expected.append("},");
        expected.append("\"bulk\":{");
        expected.append("\"supported\":false,");
        expected.append("\"maxOperations\":0,");
        expected.append("\"maxPayloadSize\":0");
        expected.append("},");
        expected.append("\"filter\":{");
        expected.append("\"supported\":true");
        expected.append("},");
        expected.append("\"changePassword\":{");
        expected.append("\"supported\":true");
        expected.append("},");
        expected.append("\"sort\":{");
        expected.append("\"supported\":true");
        expected.append("},");
        expected.append("\"etag\":{");
        expected.append("\"supported\":false");
        expected.append("},");
        expected.append("\"authenticationSchemes\":[{");
        expected.append("\"type\":\"type\",");
        expected.append("\"name\":\"name\",");
        expected.append("\"description\":\"description\",");
        expected.append("\"specUri\":\"specUri\",");
        expected.append("\"documentationUri\":\"documentationUri\"");
        expected.append("}]");
        expected.append("}");

        /*
         * Serialize.
         */
        String serializedResponse = SCIMUtil.serialize(spc);
        assertEquals(expected.toString(), serializedResponse);

        /*
         * No need to test deserialization as this is a response only. The
         * customer would never send an instance into us.
         */
    }
}
