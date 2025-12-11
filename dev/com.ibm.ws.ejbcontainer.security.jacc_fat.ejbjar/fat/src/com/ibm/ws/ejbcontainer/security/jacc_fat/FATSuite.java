/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.security.jacc_fat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.ejbcontainer.security.jacc_fat.audit.EJBJarX03JACCAuditTest;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                EJBJarX01Test.class,
                EJBJarX02Test.class, //LITE
                EJBJarX03Test.class,
                EJBJarX03JACCAuditTest.class, // added for jacc audit
                EJBJarMixM01Test.class,
                EJBJarMixM02Test.class, //Added one test to run in Lite mode
                EJBJarMixM03Test.class, //Changed to run in FULL mode
                EJBJarMixM04Test.class,
                EJBJarMixM05Test.class, //Lite
                EJBJarMixMC06Test.class, //Changed to run in FULL
                EJBJarMixM07ExtTest.class, //LITE
                EJBJarMixM08ExtTest.class, //CHanged to run in FULL
                EJBJarMixM09ExtTest.class,
                EJBJarMixM10ExtTest.class,
                EJBJarMixMC06InWarEarTest.class,
                EJBJarMixM01InWarEarTest.class, //Changed to run in Full
                EJBJarX02InWarTest.class, //Added one test to run in Lite mode//Changed to run in FULL
                EJBJarMixM02InWarTest.class, //Added one test to run in Lite mode//Changed to run in FULL/

/**
 * The following test will be removed since we do not support Dynamic feature on rolemappings.
 * this is something that is handled by the third party provider.
 */
//RoleMappingReconfig.class
})
public class FATSuite {
    private static final Set<String> EE78_FEATURES;
    private static final String[] EE78_FEATURES_ARRAY = {
                                                          "appSecurity-1.0",
                                                          "usr:jaccTestProvider-1.0"
    };

    private static final Set<String> EE9_FEATURES;
    private static final String[] EE9_FEATURES_ARRAY = {
                                                         "usr:jaccTestProvider-2.0"
    };

    private static final Set<String> EE10_FEATURES;
    private static final String[] EE10_FEATURES_ARRAY = {
                                                          "usr:jaccTestProvider-2.1"
    };

    private static final Set<String> EE11_FEATURES;
    private static final String[] EE11_FEATURES_ARRAY = {
                                                          "usr:jaccTestProvider-3.0"
    };

    static {
        EE78_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE78_FEATURES_ARRAY)));
        EE9_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));
        EE10_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE10_FEATURES_ARRAY)));
        EE11_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE11_FEATURES_ARRAY)));
    }

    /*
     * Run EE9 tests in LITE mode if Java 8, EE10 tests in LITE mode if >= Java 11, EE11 tests in LITE mode if >= Java 17 and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new JakartaEE9Action().removeFeatures(EE78_FEATURES).addFeatures(EE9_FEATURES).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)).andWith(new JakartaEE10Action().removeFeatures(EE78_FEATURES).removeFeatures(EE9_FEATURES).addFeatures(EE10_FEATURES).conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17)).andWith(FeatureReplacementAction.EE11_FEATURES().removeFeatures(EE78_FEATURES).removeFeatures(EE9_FEATURES).removeFeatures(EE10_FEATURES).addFeatures(EE11_FEATURES));
}