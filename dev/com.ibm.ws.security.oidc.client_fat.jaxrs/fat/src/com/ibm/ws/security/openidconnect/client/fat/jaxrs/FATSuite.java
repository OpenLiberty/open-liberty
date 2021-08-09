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
package com.ibm.ws.security.openidconnect.client.fat.jaxrs;

import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OIDCTokenMappingResolverGenericTest;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientAPITests;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientDiscoveryBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientReAuthnTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                OidcJaxRSClientBasicTests.class,
                OidcJaxRSClientAPITests.class,
                OIDCTokenMappingResolverGenericTest.class,
                OidcJaxRSClientReAuthnTests.class,
                OidcJaxRSClientDiscoveryBasicTests.class,
//                OidcJaxRSClientRequestFilterTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    public static String repeatFlag = null;

    private static final Set<String> REMOVE = new HashSet<String>();
    private static final Set<String> INSERT = new HashSet<String>();

    static {
        /*
         * List of testing features that need to be removed and replaced.
         */
        REMOVE.add("oauth20TokenMapping-1.0");

        INSERT.add("oauth20TokenMapping-2.0");
    }

    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of test runtime.
     *
     */
    /* always add servlet-5.0 to enable EE9 in the op which had no feature versions to swap out to enable EE9 */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
                    .andWith(new JakartaEE9Action().removeFeatures(REMOVE).addFeatures(INSERT).alwaysAddFeature("servlet-5.0").fullFATOnly());

}
