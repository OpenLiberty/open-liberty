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
package com.ibm.ws.transport.iiop.security.config.tss;

import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.IOP.Codec;

import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.server.config.tss.ServerLTPAMechConfig;
import com.ibm.ws.security.csiv2.util.LocationUtils;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public class TSSASMechConfigTest {

    private final String targetName = "testRealm";
    private Authenticator authenticator;
    private TokenManager tokenManager;
    private Codec codec;
    private final Mockery mockery = new JUnit4Mockery();
    private final WsLocationAdmin locationAdmin = mockery.mock(WsLocationAdmin.class);

    @Test
    public void decodeReturnsNullMechConfig() throws Exception {
        AS_ContextSec asContextSec = new AS_ContextSec();
        asContextSec.target_supports = 0;
        TSSNULLASMechConfig nullMechConfigAtClientSide = (TSSNULLASMechConfig) TSSASMechConfig.decodeIOR(asContextSec);
        assertNotNull("There must be a TSSNULLASMechConfig object.", nullMechConfigAtClientSide);
    }

    @Test
    public void decodeReturnsGSSUPMechConfig() throws Exception {
        TSSGSSUPMechConfig gssupConfig = new TSSGSSUPMechConfig();
        gssupConfig.setTargetName(targetName);
        gssupConfig.setRequired(false);
        AS_ContextSec asContextSec = gssupConfig.encodeIOR(codec);
        TSSGSSUPMechConfig gssupConfigAtClientSide = (TSSGSSUPMechConfig) TSSASMechConfig.decodeIOR(asContextSec);
        assertNotNull("There must be a TSSGSSUPMechConfig object.", gssupConfigAtClientSide);
    }

    @Test
    public void decodeReturnsLTPAMechConfig() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(locationAdmin).resolveString(WsLocationConstants.SYMBOL_PROCESS_TYPE);
                will(returnValue(WsLocationConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });
        LocationUtils lu = new LocationUtils(locationAdmin);
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, false);
        AS_ContextSec asContextSec = ltpaConfig.encodeIOR(codec);
        ServerLTPAMechConfig ltpaConfigAtClientSide = (ServerLTPAMechConfig) TSSASMechConfig.decodeIOR(asContextSec);
        assertNotNull("There must be a ServerLTPAMechConfig object.", ltpaConfigAtClientSide);
    }

}
