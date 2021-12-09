/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
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
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1"))
                    .andWith(new JakartaEE9Action().alwaysAddFeature("jsonb-2.0").removeFeature("mpMetrics-2.3"));
}
