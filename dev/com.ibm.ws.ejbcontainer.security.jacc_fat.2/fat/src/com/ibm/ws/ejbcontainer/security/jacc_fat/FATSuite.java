/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                DynamicJACCFeatureTest.class,
                EJBJarMixExtInWarEarMergeConflictXMLBindingsTest.class,
                EJBJarMixExtMergeConflictXMLBindingsTest.class,
                EJBJarMixM07ExtInWarEarTest.class,
                EJBJarMixM08ExtInWarTest.class,
                EJBJarMixMC06InWarTest.class,
                EJBJarX01InWarEarTest.class,
                PureAnnA01InWarEarTest.class,
                PureAnnA01InWarTest.class,
                PureAnnA05InWarEarTest.class, //Never ran in the native ejb
                PureAnnA07InWarInheritanceTest.class,
                PureAnnAppBndXMLBindingsInWarEarTest.class,
                PureAnnAppBndXMLBindingsTest.class, //Changed to run in Full mode.
                PureAnnMergeConflictXMLBindingsInWarEarTest.class, //Modified not to run the PureAnnTestBase
                PureAnnMergeConflictXMLBindingsTest.class, //Modified not to run the PureAnnTestBase and also removed from the lite bucket
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

    static {
        EE78_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE78_FEATURES_ARRAY)));
        EE9_FEATURES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(EE9_FEATURES_ARRAY)));
    }

    /*
     * Run EE9 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(new JakartaEE9Action().removeFeatures(EE78_FEATURES).addFeatures(EE9_FEATURES));
}