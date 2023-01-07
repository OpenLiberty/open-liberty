/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.CookieAttributes2ServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCCookieAttributesInboundPropNoneWithOPToken2ServerTests extends CookieAttributes2ServerTests {

    private static final Class<?> thisClass = OIDCCookieAttributesInboundPropNoneWithOPToken2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        tokenTestingSetup("server_disableLtpaCookie_authnSessionDisabled_none.xml", Constants.NONE, Constants.OIDC_OP, true);
        testSettings.setInboundProp(Constants.NONE);

    }

}
