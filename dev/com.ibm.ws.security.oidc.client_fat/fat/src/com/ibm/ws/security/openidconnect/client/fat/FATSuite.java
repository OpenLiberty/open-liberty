/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientConsentTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientCookieNameTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientDiscoveryBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientDiscoveryErrorTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientDiscoveryJWTBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientLTPACookieTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientSameSiteTests;
import com.ibm.ws.security.openidconnect.client.fat.IBM.OidcClientWasReqURLTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                OidcClientBasicTests.class,
                // OidcClientLogoutTests.class
                OidcClientCookieNameTests.class,
                OidcClientConsentTests.class,
                OidcClientLTPACookieTests.class,
                OidcClientDiscoveryBasicTests.class,
                OidcClientDiscoveryErrorTests.class,
                OidcClientDiscoveryJWTBasicTests.class,
                // OidcCertificationRPBasicProfileTests.class,
                OidcClientSameSiteTests.class,
                OidcClientWasReqURLTests.class,
//                OidcClientSignatureAlgTests.class,
//                OidcClientEncryptionTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonLocalLDAPServerSuite {
    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of of test runtime.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
                    .andWith(new JakartaEE9Action().fullFATOnly());
}
