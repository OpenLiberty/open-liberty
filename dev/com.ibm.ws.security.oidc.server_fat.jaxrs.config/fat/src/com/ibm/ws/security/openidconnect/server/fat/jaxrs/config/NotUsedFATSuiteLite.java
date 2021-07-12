/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.utils.ldaputils.CommonAltRemoteLDAPServerSuite;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropNoneNoOPToken2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropNoneWithOPToken2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropRequired2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCCookieAttributesInboundPropSupported2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCExistingAttributes2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCInboundPropagation2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryNoRegIntrospect2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryNoRegUserinfo2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryWithRegIntrospect2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryWithRegLDAP2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCMapToUserRegistryWithRegMismatch2ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC.OIDCValidationMethod2ServerTests;

@RunWith(Suite.class)
@SuiteClasses({

        // specify OIDC only tests
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
        OIDCCookieAttributesInboundPropRequired2ServerTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class NotUsedFATSuiteLite extends CommonAltRemoteLDAPServerSuite {

}
