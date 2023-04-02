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
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthCookieAttributesInboundPropNoneNoOPToken2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthCookieAttributesInboundPropNoneWithOPToken2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthCookieAttributesInboundPropRequired2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthCookieAttributesInboundPropSupported2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthExistingAttributes2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthHeaderName2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthInboundPropagation2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthIssuerIdentifier2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthMapToUserRegistryNoRegIntrospect2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthMapToUserRegistryWithRegIntrospect2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthMapToUserRegistryWithRegLDAP2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthMapToUserRegistryWithRegMismatch2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OAuth.OAuthValidationMethod2ServerTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@SuppressWarnings("restriction")
@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

        // specify OAuth tests
        OAuthExistingAttributes2ServerTests.class,
        OAuthInboundPropagation2ServerTests.class,
        OAuthValidationMethod2ServerTests.class,
        OAuthMapToUserRegistryNoRegIntrospect2ServerTests.class,
        OAuthMapToUserRegistryWithRegIntrospect2ServerTests.class,
        OAuthMapToUserRegistryWithRegMismatch2ServerTests.class,
        OAuthMapToUserRegistryWithRegLDAP2ServerTests.class,
        OAuthCookieAttributesInboundPropNoneNoOPToken2ServerTests.class,
        OAuthCookieAttributesInboundPropNoneWithOPToken2ServerTests.class,
        OAuthCookieAttributesInboundPropSupported2ServerTests.class,
        OAuthCookieAttributesInboundPropRequired2ServerTests.class,
        OAuthIssuerIdentifier2ServerTests.class,
        OAuthHeaderName2ServerTests.class,

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
