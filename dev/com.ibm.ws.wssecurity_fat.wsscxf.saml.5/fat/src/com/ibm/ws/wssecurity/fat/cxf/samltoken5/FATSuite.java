/*******************************************************************************
 * Copyright (c) 2021,2024 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.samltoken5;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.actions.LargeProjectRepeatActions;
import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.wssecurity.fat.cxf.samltoken5.LiteAlwaysRunTest.AlwaysRunAndPassTest;
import com.ibm.ws.wssecurity.fat.cxf.samltoken5.OneServerTests.CxfSAMLAsymSignEnc1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken5.OneServerTests.CxfSAMLSymSignEnc1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken5.TwoServerTests.CxfSAMLAsymSignEnc2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken5.TwoServerTests.CxfSAMLSymSignEnc2ServerTests;
import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({

        //Lite
        AlwaysRunAndPassTest.class,

        //Full for EE7 old/new format ehcache; 
        //as well as for EE7 wsseccbh-1.0/wsseccbh-2.0 on some tests
        //Full mode also runs Lite tests
        CxfSAMLSymSignEnc2ServerTests.class,
        CxfSAMLAsymSignEnc2ServerTests.class,
        CxfSAMLSymSignEnc1ServerTests.class,
        CxfSAMLAsymSignEnc1ServerTests.class

})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonLocalLDAPServerSuite {
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
    public static RepeatTests repeat = LargeProjectRepeatActions.createEE9OrEE10SamlRepeats();

}
