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

package com.ibm.ws.security.openidconnect.server.fat.oauth;

import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonRemoteLDAPServerSuite;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthInvokeNonexistentPathTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthPublicClientAuthCodeTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientAuthCertRequiredTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientAuthCertTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientAuthCodeDerbyHashTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientAuthCodeDerbyXORTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientAuthCodeTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientAuthTaiTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientCredentialTest;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.OAuth.OAuthWebClientImplicitTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthCookieNameTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthGrantTypesTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthPKCETest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthPromptLoginTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthScopesClientCredentialTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthScopesImplicitTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthScopesPasswordTest;
import com.ibm.ws.security.openidconnect.server.fat.OAuth.OAuthScopesTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

                AlwaysPassesTest.class,
                // Basic OAuth functionality tests
                OAuthWebClientAuthCodeTest.class,
                OAuthWebClientAuthTaiTest.class,
                OAuthWebClientAuthCertTest.class,
                OAuthWebClientAuthCertRequiredTest.class,
                OAuthWebClientAuthCodeDerbyXORTest.class,
                OAuthWebClientAuthCodeDerbyHashTest.class,
//                OAuthWebClientAuthCodeCustomStoreXORTest.class,
//                OAuthWebClientAuthCodeCustomStoreHashTest.class,
//                OAuthWebClientAuthCodeCustomStoreBellTest.class,
                OAuthPublicClientAuthCodeTest.class,
                OAuthWebClientImplicitTest.class,
                OAuthWebClientCredentialTest.class,

                // Specific OAuth tests
                OAuthPromptLoginTest.class,
                OAuthScopesImplicitTest.class,
                OAuthScopesTest.class,
                OAuthScopesClientCredentialTest.class,
                OAuthScopesPasswordTest.class,
                OAuthGrantTypesTest.class,
                OAuthCookieNameTest.class,
                OAuthInvokeNonexistentPathTest.class,
                OAuthPKCETest.class

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
