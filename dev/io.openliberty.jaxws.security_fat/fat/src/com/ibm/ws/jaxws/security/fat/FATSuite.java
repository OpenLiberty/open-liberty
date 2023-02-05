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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/*
 * TODO: Lite Mode
 */
@RunWith(Suite.class)
@SuiteClasses({ POJOServiceSecurityTest.class, EJBInWarServiceSecurityTest.class, EJBInJarServiceSecurityTest.class,
		TransportSecurityUsingDispatchTest.class, TransportSecurityUsingDispatchClientCertTest.class,
		WsdlLocationHttpsTest.class, BasicAuthWithoutSSLTest.class, SSLConfigurationUnmanagedTest.class,
		BasicAuthWithoutSSLTest.class, SSLConfigurationTest.class, BasicAuthWithSSLTest.class,
		ClientCertificateTest.class, ClientCertFailOverToBasicAuthTest.class, SSLRefConfigurationTest.class,
		SSLConfigurationNoTrustStoreTest.class })
public class FATSuite {

	@ClassRule
	public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly())
			.andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jaxwstest-2.2").addFeature("xmlwstest-3.0")
					.conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
			.andWith(FeatureReplacementAction.EE10_FEATURES().removeFeature("jaxwstest-2.2")
					.removeFeature("xmlwstest-3.0").addFeature("xmlwstest-4.0"));

}
