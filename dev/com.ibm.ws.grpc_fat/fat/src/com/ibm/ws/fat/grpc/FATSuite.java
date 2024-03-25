/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.fat.grpc;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                ServiceSupportTests.class, // always keep this on top
                ClientConfigTests.class,
                ClientHeaderPropagationTests.class,
                HelloWorldCDITests.class,
                HelloWorldTest.class,
                HelloWorldEarTest.class,
                HelloWorldTlsTest.class,
                HelloWorldThirdPartyApiTest.class,
                SecureHelloWorldTest.class,
                GrpcMetricsTest.class,
                ServiceConfigTests.class,
                ServiceInterceptorTests.class,
                StoreServicesRESTClientTests.class,
                StoreServicesSecurityTests.class,
                // leave out for now, to avoid intermittent build breaks StreamingTests.class,
                ClientInterceptorTests.class,
                StoreProducerServletClientTests.class,
                StoreConsumerServletClientTests.class
})

public class FATSuite {

    private static final Class<?> c = FATSuite.class;

    static String[] removedFeatures = { "mpOpenAPI-1.1", "mpMetrics-2.3", "mpJwt-1.1", "mpConfig-1.3", "mpRestClient-1.3", "appSecurity-2.0" };

    static String[] addedFeatures = { "mpOpenAPI-3.0", "mpMetrics-4.0", "mpJwt-2.0", "mpConfig-3.0", "mpRestClient-3.0", "appSecurity-4.0" };

    //updated to use MP61 features
    static String[] ee10AddedFeatures = { "mpOpenAPI-3.1", "mpMetrics-5.1", "mpJwt-2.1", "mpConfig-3.1", "mpRestClient-3.0", "appSecurity-5.0" };

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES()
                                    .removeFeatures(new HashSet<>(Arrays.asList(removedFeatures)))
                                    .addFeatures(new HashSet<>(Arrays.asList(addedFeatures)))
                                    .alwaysAddFeature("servlet-5.0")
                                    .alwaysAddFeature("jsonb-2.0")
                                    .conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES()
                                    .removeFeatures(new HashSet<>(Arrays.asList(removedFeatures)))
                                    .removeFeatures(new HashSet<>(Arrays.asList(addedFeatures)))
                                    .addFeatures(new HashSet<>(Arrays.asList(ee10AddedFeatures)))
                                    .alwaysAddFeature("servlet-6.0")
                                    .alwaysAddFeature("jsonb-3.0"));
}
