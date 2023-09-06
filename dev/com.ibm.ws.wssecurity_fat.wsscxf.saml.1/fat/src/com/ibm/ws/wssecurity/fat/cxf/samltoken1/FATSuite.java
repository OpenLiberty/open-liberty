/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

package com.ibm.ws.wssecurity.fat.cxf.samltoken1;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.fat.common.utils.ldaputils.CommonLocalLDAPServerSuite;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.OneServerTests.CxfSAMLWSSTemplates1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.OneServerTests.CxfSSLSAMLBasic1ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.TwoServerTests.CxfSAMLWSSTemplates2ServerTests;
import com.ibm.ws.wssecurity.fat.cxf.samltoken1.TwoServerTests.CxfSSLSAMLBasic2ServerTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
// import com.ibm.ws.wssecurity.fat.utils.common.RepeatWithEE7cbh20;

@RunWith(Suite.class)
@SuiteClasses({

        //Lite for EE9/EE10  
        //Full mode also run small subset EE7 tests
        CxfSSLSAMLBasic2ServerTests.class,

        //Full mode runs only EE7cbh1 as well as Lite
        CxfSSLSAMLBasic1ServerTests.class,
        CxfSAMLWSSTemplates2ServerTests.class,
        CxfSAMLWSSTemplates1ServerTests.class,

})

/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite extends CommonLocalLDAPServerSuite {

    @ClassRule
    //issue 23060
    //EE7cbh2 rule is not used for this FAT project, to avoid the extended test execution time
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES()).andWith(FeatureReplacementAction.EE10_FEATURES());

}
