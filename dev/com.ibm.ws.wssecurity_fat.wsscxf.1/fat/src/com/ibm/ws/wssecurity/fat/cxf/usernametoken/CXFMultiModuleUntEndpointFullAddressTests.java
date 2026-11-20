/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.wssecurity.fat.cxf.usernametoken;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Multi-module UsernameToken tests with the policy attachments that use a full URL
 * {@code <wsa:EndpointReference><wsa:Address>http://localhost:8010/<module>/SOAPServiceN} in AppliesTo
 * (publish/files/policy-attachments/endpointfulladress).
 */
@SkipForRepeat({ RepeatWithEE7cbh20.ID })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CXFMultiModuleUntEndpointFullAddressTests extends CXFMultiModuleUntTests {

    @BeforeClass
    public static void setUp() throws Exception {
        FATRunner.setTestNameModifier(n -> "EPFA_" + n);
        deployAndStart("endpointfulladress");
    }

    @AfterClass
    public static void resetName() {
        FATRunner.setTestNameModifier(null);
    }
}
