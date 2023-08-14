/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.fat;

import java.util.Locale;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
                AnnotationScanTest.class,
                BeanParamTest.class,
                CheckFeature12Test.class,
                ClientTest.class,
                ContextTest.class,
                CustomSecurityContextTest.class,
                DepartmentTest.class,
                ExceptionMappersTest.class,
                ExceptionMappingWithOTTest.class,
                ExceptionsSubresourcesTest.class,
                ExtraProvidersTest.class,
                HelloWorldTest.class,
                IBMJSON4JTest.class,
                JacksonJsonIgnoreTest.class,
                JacksonPOJOTest.class,
                JacksonPOJOwithUserJacksonLib1xTest.class,
                JacksonPOJOwithUserJacksonLib2xTest.class,
                JAXRS20CallBackTest.class,
                JAXRS20ClientServerBookTest.class,
                JAXRSClientServerValidationTest.class,
                JAXRSContinuationsTest.class,
                JAXRSPerRequestValidationTest.class,
                JAXRSServletCoexistTest.class,
                JAXRSValidationDisabledTest.class,
                JAXRSWebContainerTest.class,
                JerseyTest.class,
                JerseyInjectionTest.class,
                LinkHeaderTest.class,
                MediaTypeTest.class,
                MultipartTest.class,
                ManagedBeansTest.class,
                OptionsTest.class,
                ParamConverterTest.class,
                ParamsTest.class,
                PrototypeTest.class,
                ProviderCacheTest.class,
                ReaderWriterProvidersTest.class,
                ResourceInfoTest.class,
                ResponseAPITest.class,
                RestMetricsTest.class,
                SameClassAsProviderAndResourceTest.class,
                SameClassInGetClassesAndGetSingletonsTest.class,
                SearchPolicyTest.class,
                SecurityAnnotationsTest.class,
                SecurityContextTest.class,
                SecuritySSLTest.class,
                ServiceScopeTest.class,
                SimpleJsonTest.class,
                StandardProvidersTest.class,
                UriInfoTest.class,
                UTF8Test.class,
                ValidationTest.class,
                WADLTest.class
})

public class FATSuite {
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");

    // To avoid going over 3 hour test limit on slow hardware, run only the first and last versions
    // on slow hardware.
    @ClassRule
    public static RepeatTests r;
    static {
        if (!(isWindows) || FATRunner.FAT_TEST_LOCALRUN) {
            r = RepeatTests.withoutModificationInFullMode()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1").fullFATOnly())
                    .andWith(new JakartaEE9Action().alwaysAddFeature("jsonb-2.0").removeFeature("mpMetrics-2.3").addFeature("mpMetrics-4.0")
                        .removeFeature("microProfile-1.3").addFeature("microProfile-5.0").conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(new JakartaEE10Action().alwaysAddFeature("jsonb-3.0").removeFeature("jsonb-2.0").removeFeature("mpMetrics-2.3").removeFeature("mpMetrics-4.0").removeFeature("microProfile-1.3").addFeature("mpMetrics-5.0")
                        .removeFeature("microProfile-5.0").addFeature("microProfile-6.0"));
        } else {
            r = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                             .andWith(new JakartaEE10Action().alwaysAddFeature("jsonb-3.0").removeFeature("mpMetrics-2.3").removeFeature("microProfile-1.3").addFeature("mpMetrics-5.0")
                                      .addFeature("microProfile-6.0"));
        }
    }
}
