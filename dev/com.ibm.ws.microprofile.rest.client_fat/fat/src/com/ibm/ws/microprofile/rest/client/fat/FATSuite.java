/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import java.util.Locale;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AsyncMethodTest.class,
                BasicTest.class,
                BasicCdiTest.class,
                BasicEJBTest.class,
                CdiPropsAndProvidersTest.class,
                CollectionsTest.class,
                HandleResponsesTest.class,
                HeaderPropagationTest.class,
                HeaderPropagation12Test.class,
                HostnameVerifierTest.class,
                JsonbContextTest.class,
                MultiClientCdiTest.class,
                ProduceConsumeTest.class,
                PropsTest.class,
                RESTClientUserFeatureTest.class,
                SseTest.class
})
public class FATSuite {
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    /*
     * If you are adding a new MicroProfile version to this list of repeats, don't forget to update
     * `publish/features/javax/MyRESTClient.mf` and `publish/features/jakarta/MyRESTClient.mf` to
     * tolerate the new versions.
     */
    public static RepeatTests repeatMP13Up(String...servers) {

        // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
        // Windows.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP61, // 3.0+EE10
                                              MicroProfileActions.MP13, //mpRestClient-1.0
                                              MicroProfileActions.MP20, //mpRestClient-1.1
                                              MicroProfileActions.MP22, // 1.2
                                              MicroProfileActions.MP30, // 1.3
                                              MicroProfileActions.MP33, // 1.4
                                              MicroProfileActions.MP40, // 2.0
                                              MicroProfileActions.MP50);// 3.0

        } else {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP13); //mpRestClient-1.0

        }

    }

    public static RepeatTests repeatMP14Up(String...servers) {

        // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
        // Windows.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP61, // 3.0+EE10
                                              MicroProfileActions.MP14, //mpRestClient-1.1
                                              MicroProfileActions.MP20, //mpRestClient-1.1
                                              MicroProfileActions.MP22, // 1.2
                                              MicroProfileActions.MP30, // 1.3
                                              MicroProfileActions.MP33, // 1.4
                                              MicroProfileActions.MP40, // 2.0
                                              MicroProfileActions.MP50);// 3.0

        } else {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP14); //mpRestClient-1.1

        }

    }

    public static RepeatTests repeatMP20Up(String...servers) {

        // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
        // Windows.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP61, // 3.0+EE10
                                              MicroProfileActions.MP20, //mpRestClient-1.1
                                              MicroProfileActions.MP22, // 1.2
                                              MicroProfileActions.MP30, // 1.3
                                              MicroProfileActions.MP33, // 1.4
                                              MicroProfileActions.MP40, // 2.0
                                              MicroProfileActions.MP50);// 3.0

        } else {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP20); //mpRestClient-1.1

        }

    }

    public static RepeatTests repeatMP22Up(String...servers) {

        // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
        // Windows.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP61, // 3.0+EE10
                                              MicroProfileActions.MP22, // 1.2
                                              MicroProfileActions.MP30, // 1.3
                                              MicroProfileActions.MP33, // 1.4
                                              MicroProfileActions.MP40, // 2.0
                                              MicroProfileActions.MP50);// 3.0

        } else {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP22); //mpRestClient-1.2

        }

    }

    public static RepeatTests repeatMP30Up(String...servers) {

        // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
        // Windows.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP61, // 3.0+EE10
                                              MicroProfileActions.MP30, // 1.3
                                              MicroProfileActions.MP33, // 1.4
                                              MicroProfileActions.MP40, // 2.0
                                              MicroProfileActions.MP50);// 3.0

        } else {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP30);// mpRestClient-1.3

        }

    }
    
    public static RepeatTests repeatMP40Up(String...servers) {

        // To avoid bogus timeout build-breaks on slow Windows hardware only run a few versions on
        // Windows.
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP61, // 3.0+EE10
                                              MicroProfileActions.MP30, // 1.3
                                              MicroProfileActions.MP33, // 1.4
                                              MicroProfileActions.MP40, // 2.0
                                              MicroProfileActions.MP50);// 3.0

        } else {
            return MicroProfileActions.repeat(servers,
                                              MicroProfileActions.MP70_EE11, // 4.0_EE11
                                              MicroProfileActions.MP70_EE10, // 4.0_EE10
                                              MicroProfileActions.MP30);// mpRestClient-1.3

        }
    }
}

