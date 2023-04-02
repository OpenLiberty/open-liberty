/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.acme.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.TestContainerSuite;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({ AcmeClientTest.class, 
	AcmeSimpleTest.class,
	AcmeURISimpleTest.class,
	AcmeCaRestHandlerTest.class,
	AcmeSwapDirectoriesTest.class,
	AcmeValidityAndRenewTest.class,
	AcmeDisableTriggerSimpleTest.class,
	AcmeConfigVariationsTest.class,
	AcmeURIConfigVariationsTest.class,
	AcmeRevocationTest.class
})
public class FATSuite extends TestContainerSuite {
    /*
     * Repeat with EE9. Since most, if not all, servers don't have an EE feature enabled, we
     * will add servlet-5.0 for the EE9 repeats to test that the acmeCA-2.0 feature supports
     * EE9 since there are no EE features for the JakartaEE9Action to replace in the server 
     * XMLs.
     */
    @ClassRule
	public static RepeatTests repeat = RepeatTests.with(new EmptyAction())
			.andWith(new JakartaEE9Action().alwaysAddFeature("servlet-5.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
			.andWith(new JakartaEE10Action().alwaysAddFeature("servlet-6.0"));
    
}
