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

package com.ibm.ws.security.wim.scim20.model.users;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.websphere.security.wim.scim20.model.users.X509Certificate;
import com.ibm.ws.security.wim.scim20.SCIMUtil;

public class X509CertificateImplTest {
    @Test
    public void serialize() throws Exception {

        X509CertificateImpl certificate = getTestInstance();

        /*
         * The expected serialized JSON string.
         */
        StringBuffer expected = new StringBuffer();
        expected.append("{");
        expected.append("\"value\":\"AAECAw==\",");
        expected.append("\"display\":\"display\",");
        expected.append("\"type\":\"type\",");
        expected.append("\"primary\":false");
        expected.append("}");

        /*
         * Serialize.
         */
        String serialized = SCIMUtil.serialize(certificate);
        assertEquals(expected.toString(), serialized);

        /*
         * Deserialize.
         */
        X509Certificate deserialized = SCIMUtil.deserialize(serialized, X509Certificate.class);
        assertEquals(certificate, deserialized);
    }

    public static X509CertificateImpl getTestInstance() {

        X509CertificateImpl certificate = new X509CertificateImpl();
        certificate.setDisplay("display");
        certificate.setPrimary(false);
        certificate.setType("type");
        certificate.setValue(new byte[] { 0x00, 0x01, 0x02, 0x03 });
        return certificate;
    }
}
