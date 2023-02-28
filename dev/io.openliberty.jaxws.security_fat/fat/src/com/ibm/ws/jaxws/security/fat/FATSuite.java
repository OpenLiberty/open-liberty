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
@SuiteClasses({ BasicAuthWithoutSSLTest.class, //2:01
//                BasicAuthWithSSLTest.class, //2:47
                ClientCertFailOverToBasicAuthTest.class, //3:12
                ClientCertificateTest.class, //12:01
                EJBInJarServiceSecurityTest.class, //2:34
                EJBInWarServiceSecurityTest.class, //3:41
                POJOServiceSecurityTest.class, //2:31
//                SSLConfigurationNoTrustStoreTest.class, //2:49
//                SSLConfigurationTest.class, //4:47
//                SSLConfigurationUnmanagedTest.class, //3:32
//                SSLRefConfigurationTest.class, //3:37
                TransportSecurityUsingDispatchClientCertTest.class, //2:41
                TransportSecurityUsingDispatchTest.class, //2:58
                WsdlLocationHttpsTest.class //2:07
}) // 16:42 simply EmptyAction, 3:37 Lite mode, 51:00 in full mode
public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly()).andWith(FeatureReplacementAction.EE9_FEATURES().removeFeature("jaxwstest-2.2").addFeature("xmlwstest-3.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11)).andWith(FeatureReplacementAction.EE10_FEATURES().removeFeature("jaxwstest-2.2").removeFeature("xmlwstest-3.0").addFeature("xmlwstest-4.0"));

}
