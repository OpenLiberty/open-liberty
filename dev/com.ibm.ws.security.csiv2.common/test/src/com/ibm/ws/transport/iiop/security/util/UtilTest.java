/*
 * Copyright 2014, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.transport.iiop.security.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.omg.IOP.Codec;

import com.ibm.ws.security.csiv2.config.LTPAMech;
import com.ibm.ws.security.csiv2.test.tools.TestCodec;

public class UtilTest {

    private final String targetName = "testRealm";

    @Test
    public void oidFromExportedName() {
        byte[] encodedExportedName = Util.encodeGSSExportName(LTPAMech.LTPA_OID, targetName);
        GSSExportedName name = Util.decodeGSSExportedName(encodedExportedName);
        assertEquals("The oid must be found in the exported name.", LTPAMech.LTPA_OID.substring(4), name.getOid());
    }

    @Test
    public void encodeLTPAToken() throws Exception {
        Codec codec = new TestCodec();
        byte[] ltpaTokenBytes = "Some token bytes".getBytes();

        byte[] encodedLTPAToken = Util.encodeLTPAToken(codec, ltpaTokenBytes);
        byte[] decodedLTPAToken = Util.decodeLTPAToken(codec, encodedLTPAToken);

        assertEquals("The decoded bytes must be the same as the original bytes.", Util.byteToString(ltpaTokenBytes), Util.byteToString(decodedLTPAToken));
    }

}
