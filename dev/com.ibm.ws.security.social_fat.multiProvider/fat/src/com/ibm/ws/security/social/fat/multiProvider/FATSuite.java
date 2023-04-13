/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.multiProvider;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
        AlwaysPassesTest.class,
        CustomSelectionPageTests.class,
        MultiProvider_usingProviderConfig.class,
        //  Doesn't make sense      MultiProvider_usingProviderConfig_withDuplicates.class,
        MultiProvider_usingSocialConfig.class,
        MultiProvider_usingSocialConfig_withDuplicates.class,
        MultiProvider_usingSocialConfig_withFilters.class,
        MultiProvider_usingSocialConfig_withDisplayNames.class,
        MultiProvider_usingSocialConfig_withOidcFeatures.class,
        MultiProvider_usingSocialConfig_withQueryParams.class,
        MultiProvider_LocalAuthentication.class,
})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*
     * Run EE10 tests in LITE mode and run all tests in FULL mode.
     */
    @ClassRule
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10Repeats();

}
