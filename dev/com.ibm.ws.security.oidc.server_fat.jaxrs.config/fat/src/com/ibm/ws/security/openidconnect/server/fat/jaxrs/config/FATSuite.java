/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP.NoOPAudiences1ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP.NoOPEncryptionRSServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP.NoOPSignatureRSServerTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

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

        // No OP (No OAuth or OIDC server) tests
        NoOPAudiences1ServerTests.class,
        // for now, test without an OP since our OP can not create JWEs
        // (these tests start a second server named "OP", but it contains no OP function, just jwt builders)
        NoOPSignatureRSServerTests.class,
        NoOPEncryptionRSServerTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonAltRemoteLDAPServerSuite {

    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of test runtime.
     *
     */
    /* always add servlet-5.0 to enable EE9 in the op which had no feature versions to swap out to enable EE9 */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
            .andWith(new JakartaEE9Action().alwaysAddFeature("servlet-5.0").fullFATOnly());

}
