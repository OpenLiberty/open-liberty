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
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP.NoOPAudiences1ServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP.NoOPEncryptionRSServerTests;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.noOP.NoOPSignatureRSServerTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@SuppressWarnings("restriction")
@RunWith(Suite.class)
@SuiteClasses({

        AlwaysPassesTest.class,

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
