/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonAltRemoteLDAPServerSuite;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropNoneNoOPToken2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropNoneWithOPToken2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropRequired2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropSupported2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCExistingAttributes2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCHeaderName2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCInboundPropagation2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCIssuerIdentifier2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCJWKEndpointUrl2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryNoRegIntrospect2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryNoRegUserinfo2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryWithRegIntrospect2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryWithRegLDAP2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryWithRegMismatch2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCSignatureAttributes2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCValidationMethod2ServerTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@SuppressWarnings("restriction")
@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

        // specify OIDC tests
        OIDCExistingAttributes2ServerTests.class,
        OIDCInboundPropagation2ServerTests.class,
        OIDCValidationMethod2ServerTests.class,
        OIDCMapToUserRegistryNoRegIntrospect2ServerTests.class,
        OIDCMapToUserRegistryWithRegIntrospect2ServerTests.class,
        OIDCMapToUserRegistryNoRegUserinfo2ServerTests.class,
        OIDCMapToUserRegistryWithRegMismatch2ServerTests.class,
        OIDCMapToUserRegistryWithRegLDAP2ServerTests.class,
        OIDCCookieAttributesInboundPropNoneNoOPToken2ServerTests.class,
        OIDCCookieAttributesInboundPropNoneWithOPToken2ServerTests.class,
        OIDCCookieAttributesInboundPropSupported2ServerTests.class,
        OIDCCookieAttributesInboundPropRequired2ServerTests.class,
        OIDCSignatureAttributes2ServerTests.class,
        OIDCIssuerIdentifier2ServerTests.class,
        OIDCHeaderName2ServerTests.class,
        OIDCJWKEndpointUrl2ServerTests.class,

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonAltRemoteLDAPServerSuite {

    /*
     * On Windows, always run the default/empty/EE7/EE8 tests.
     * On other Platforms:
     * - if Java 8, run default/empty/EE7/EE8 tests.
     * - All other Java versions
     * -- If LITE mode, run EE9
     * -- If FULL mode, run EE10
     *
     */
    @ClassRule
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10Repeats("servlet-5.0", "servlet-6.0");

}
