/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.client.fat;

import java.util.Locale;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxrs20.client.fat.test.BasicClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.ClientContextInjectionTest;
import com.ibm.ws.jaxrs20.client.fat.test.ComplexClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.DynamicOutboundSSLTest;
import com.ibm.ws.jaxrs20.client.fat.test.HandleResponsesTest;
import com.ibm.ws.jaxrs20.client.fat.test.HostnameVerificationTest;
import com.ibm.ws.jaxrs20.client.fat.test.IBMJson4JProvidersTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientAsyncInvokerTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientAsyncInvokerTestWithConcurrency;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientInvocationTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientSyncInvokerTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20WithClientFeatureEnabledTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClient100ContinueTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientCallbackTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientLtpaTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLDefaultTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLFiltersTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLProxyAuthTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLTestNoLibertySSLCfg;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLTestNoLibertySSLFeature;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientStandaloneTest;
import com.ibm.ws.jaxrs20.client.fat.test.JacksonProvidersTest;
import com.ibm.ws.jaxrs20.client.fat.test.JsonPProvidersTest;
import com.ibm.ws.jaxrs20.client.fat.test.MatchingSSLCiphersTest;
import com.ibm.ws.jaxrs20.client.fat.test.MisMatchingSSLCiphersTest;
import com.ibm.ws.jaxrs20.client.fat.test.PathParamTest;
import com.ibm.ws.jaxrs20.client.fat.test.ProxyClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.SimpleSSLMultipleServersDefaultLibertySSLConfigTest;
import com.ibm.ws.jaxrs20.client.fat.test.SimpleSSLMultipleServersTest;
import com.ibm.ws.jaxrs20.client.fat.test.SimpleSSLTest;
import com.ibm.ws.jaxrs20.client.fat.test.ThirdpartyJerseyClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.TimeoutClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.XmlBindingTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicClientTest.class,
                ClientContextInjectionTest.class,
                ComplexClientTest.class,
                DynamicOutboundSSLTest.class,
                HandleResponsesTest.class,
                HostnameVerificationTest.class,
                IBMJson4JProvidersTest.class,
                JacksonProvidersTest.class,
                JAXRS20ClientAsyncInvokerTest.class,
                JAXRS20ClientAsyncInvokerTestWithConcurrency.class,
                JAXRS20ClientInvocationTest.class,
                JAXRS20ClientSyncInvokerTest.class,
                JAXRS20WithClientFeatureEnabledTest.class,
                JAXRSClient100ContinueTest.class,
                JAXRSClientCallbackTest.class,
                JAXRSClientLtpaTest.class,
                JAXRSClientSSLProxyAuthTest.class,
                JAXRSClientSSLDefaultTest.class,
                JAXRSClientSSLFiltersTest.class,
                JAXRSClientSSLTest.class,
                JAXRSClientSSLTestNoLibertySSLCfg.class,
                JAXRSClientSSLTestNoLibertySSLFeature.class,
                JAXRSClientStandaloneTest.class,
                JsonPProvidersTest.class,
                MatchingSSLCiphersTest.class,
                MisMatchingSSLCiphersTest.class,
                PathParamTest.class,
                ProxyClientTest.class,
                SimpleSSLMultipleServersDefaultLibertySSLConfigTest.class,
                SimpleSSLMultipleServersTest.class,
                SimpleSSLTest.class,
                ThirdpartyJerseyClientTest.class,
                TimeoutClientTest.class,
                XmlBindingTest.class
})
public class FATSuite {
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    private static final boolean isAIX = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("aix");
    private static final boolean isISeries = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("os/400");

    // To avoid going over 3 hour test limit on slow hardware, run only the first and last versions
    // on slow hardware.
    @ClassRule
    public static RepeatTests r;
    static {
        if (!(isWindows || isAIX || isISeries) || FATRunner.FAT_TEST_LOCALRUN) {
            r = RepeatTests.withoutModificationInFullMode()
                            .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1").fullFATOnly())
                            .andWith(new JakartaEE9Action().alwaysAddFeature("jsonb-2.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(new JakartaEE10Action().alwaysAddFeature("jsonb-3.0").alwaysAddFeature("servlet-6.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES().alwaysAddFeature("jsonb-3.0").alwaysAddFeature("servlet-6.1"));

        } else {
            r = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                            .andWith(new JakartaEE10Action().alwaysAddFeature("jsonb-3.0").alwaysAddFeature("servlet-6.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                            .andWith(FeatureReplacementAction.EE11_FEATURES().alwaysAddFeature("jsonb-3.0").alwaysAddFeature("servlet-6.1"));

        }
    }


}
