/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.claimPropagation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.openidconnect.server.fat.claimPropagation.OIDC.OIDCBasicClaimPropagationTestss;

@RunWith(Suite.class)
@SuiteClasses({

        // specify OAuth tests

        // specify OIDC tests
        OIDCBasicClaimPropagationTestss.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

    /*
     * Run EE9 tests in only FULL mode and run EE7/EE8 tests only in LITE mode.
     *
     * This was done to increase coverage of EE9 while not adding a large amount of test runtime.
     *
     */
    /* always add servlet-5.0 to enable EE9 in the op which had no feature versions to swap out to enable EE9 */
    //    @ClassRule
    //    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().liteFATOnly())
    //            .andWith(new JakartaEE9Action().alwaysAddFeature("servlet-5.0").fullFATOnly());

}
