/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.oidc;

import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonRemoteLDAPServerSuite;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCInvokeNonexistentPathTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCPublicClientAuthCodeTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCertRequiredTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCertTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCodeCustomStoreBellTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCodeCustomStoreHashTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCodeCustomStoreXORTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCodeDerbyHashTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCodeDerbyXORTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthCodeTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientAuthTaiTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientCredentialTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC.OIDCWebClientImplicitTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCCookieNameTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCGrantTypesCustomStoreBellTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCGrantTypesCustomStoreTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCGrantTypesDerbyTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCGrantTypesTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCPKCETest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCPromptLoginTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCResourceTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCScopesClientCredentialTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCScopesImplicitTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCScopesPasswordTest;
import com.ibm.ws.security.openidconnect.server.fat.OIDC.OIDCScopesTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                AlwaysPassesTest.class,
// Basic OIDC functionality tests
                OIDCWebClientAuthCodeTest.class,
                OIDCWebClientAuthTaiTest.class,
                OIDCWebClientAuthCertTest.class,
                OIDCWebClientAuthCertRequiredTest.class,
                OIDCWebClientAuthCodeDerbyXORTest.class,
                OIDCWebClientAuthCodeDerbyHashTest.class,
                OIDCWebClientAuthCodeCustomStoreXORTest.class,
                OIDCWebClientAuthCodeCustomStoreHashTest.class,
                OIDCWebClientAuthCodeCustomStoreBellTest.class,
                OIDCPublicClientAuthCodeTest.class,
                OIDCWebClientImplicitTest.class,
                OIDCWebClientCredentialTest.class,

                // Specific OIDC tests
                OIDCPromptLoginTest.class,
                OIDCScopesImplicitTest.class,
                OIDCScopesTest.class,
                OIDCScopesClientCredentialTest.class,
                OIDCScopesPasswordTest.class,
                OIDCGrantTypesTest.class,
                OIDCGrantTypesCustomStoreTest.class,
                OIDCGrantTypesCustomStoreBellTest.class,
                OIDCGrantTypesDerbyTest.class,
                OIDCCookieNameTest.class,
                OIDCResourceTest.class,
                OIDCInvokeNonexistentPathTest.class,
                OIDCPKCETest.class

})
public class FATSuite extends CommonRemoteLDAPServerSuite {

    private static final Set<String> REMOVE = new HashSet<String>();
    private static final Set<String> INSERT = new HashSet<String>();

    static {
        /*
         * List of testing features that need to be removed and replaced.
         */
        REMOVE.add("oidcTai-1.0");
        REMOVE.add("sampleTai-1.0");

        INSERT.add("oidcTai-2.0");
        INSERT.add("sampleTai-2.0");
    }

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
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10Repeats(null, null, REMOVE, INSERT);

}
