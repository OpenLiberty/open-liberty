/*
 * Copyright 2015, 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.security.config.tss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.omg.CSIIOP.SCS_GSSExportedName;
import org.omg.CSIIOP.SCS_GeneralNames;
import org.omg.CSIIOP.ServiceConfiguration;

import com.ibm.ws.transport.iiop.security.util.Util;

/**
 *
 */
public class TSSServiceConfigurationConfigTest {

    @Test
    public void testGeneralNameSCC() throws Exception {
        ServiceConfiguration sc = new ServiceConfiguration();
        sc.syntax = SCS_GeneralNames.value;
        String generalName = "OU=au,CN=GeneralName";
        sc.name = Util.encodeGeneralName(generalName);
        TSSGeneralNameConfig config = (TSSGeneralNameConfig) TSSServiceConfigurationConfig.decodeIOR(sc);
        assertEquals(generalName, config.getName());
        config.toString("  ", new StringBuilder());
    }

    @Test
    public void testGSSExportedNameSCC() throws Exception {
        ServiceConfiguration sc = new ServiceConfiguration();
        sc.syntax = SCS_GSSExportedName.value;
        String oid = "1.1"; //ok syntax but invalid
        String exportedName = "ExportedName";
        sc.name = Util.encodeGSSExportName(oid, exportedName);
        TSSGSSExportedNameConfig config = (TSSGSSExportedNameConfig) TSSServiceConfigurationConfig.decodeIOR(sc);
        assertEquals(oid, config.getOid());
        assertEquals(exportedName, config.getName());
        config.toString("  ", new StringBuilder());
    }

    @Test
    public void testUnknownSCC() throws Exception {
        ServiceConfiguration sc = new ServiceConfiguration();
        sc.syntax = 0x494210ce;
        sc.name = new byte[] { 0x00, 0x00, 0x00, 0x0a, 0x6c, 0x64, 0x61, 0x70, 0x52, 0x65, 0x61, 0x6c, 0x6d, 0x00 };
        TSSUnknownServiceConfigurationConfig config = (TSSUnknownServiceConfigurationConfig) TSSServiceConfigurationConfig.decodeIOR(sc);
        assertEquals(sc.syntax, config.getSyntax());
        assertTrue(Arrays.equals(sc.name, config.getName()));
        config.toString("  ", new StringBuilder());
    }

}
