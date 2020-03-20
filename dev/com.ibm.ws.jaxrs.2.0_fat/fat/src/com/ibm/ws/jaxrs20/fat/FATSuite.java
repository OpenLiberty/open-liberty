/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.ws.jaxrs20.fat.annotationscan.AnnotationScanTest;
import com.ibm.ws.jaxrs20.fat.beanparam.BeanParamTest;
import com.ibm.ws.jaxrs20.fat.beanvalidation.JAXRSClientServerValidationTest;
import com.ibm.ws.jaxrs20.fat.beanvalidation.JAXRSPerRequestValidationTest;
import com.ibm.ws.jaxrs20.fat.beanvalidation.JAXRSValidationDisabledTest;
import com.ibm.ws.jaxrs20.fat.bookcontinuationstore.JAXRSContinuationsTest;
import com.ibm.ws.jaxrs20.fat.bookstore.JAXRS20ClientServerBookTest;
import com.ibm.ws.jaxrs20.fat.callback.JAXRS20CallBackTest;
import com.ibm.ws.jaxrs20.fat.checkFeature.CheckFeature12Test;
import com.ibm.ws.jaxrs20.fat.class_as_provider_resource.SameClassAsProviderAndResourceTest;
import com.ibm.ws.jaxrs20.fat.client.ClientTest;
import com.ibm.ws.jaxrs20.fat.context.ContextTest;
import com.ibm.ws.jaxrs20.fat.contextresolver.DepartmentTest;
import com.ibm.ws.jaxrs20.fat.exceptionmappers.ExceptionMappersTest;
import com.ibm.ws.jaxrs20.fat.exceptionmappingWithOT.ExceptionMappingWithOTTest;
import com.ibm.ws.jaxrs20.fat.extraproviders.ExtraProvidersTest;
import com.ibm.ws.jaxrs20.fat.getClasses_getSingletons.SameClassInGetClassesAndGetSingletonsTest;
import com.ibm.ws.jaxrs20.fat.helloworld.HelloWorldTest;
import com.ibm.ws.jaxrs20.fat.ibm.json.IBMJSON4JTest;
import com.ibm.ws.jaxrs20.fat.jackson.JacksonPOJOTest;
import com.ibm.ws.jaxrs20.fat.jackson1x.JacksonPOJOwithUserJacksonLib1xTest;
import com.ibm.ws.jaxrs20.fat.jackson2x.JacksonPOJOwithUserJacksonLib2xTest;
import com.ibm.ws.jaxrs20.fat.jacksonJsonIgnore.JacksonJsonIgnoreTest;
import com.ibm.ws.jaxrs20.fat.json.UTF8Test;
import com.ibm.ws.jaxrs20.fat.link.LinkHeaderTest;
import com.ibm.ws.jaxrs20.fat.managedbeans.ManagedBeansTest;
import com.ibm.ws.jaxrs20.fat.paramconverter.ParamConverterTest;
import com.ibm.ws.jaxrs20.fat.params.ParamsTest;
import com.ibm.ws.jaxrs20.fat.providercache.ProviderCacheTest;
import com.ibm.ws.jaxrs20.fat.readerwriterprovider.ReaderWriterProvidersTest;
import com.ibm.ws.jaxrs20.fat.resourcealgorithm.SearchPolicyTest;
import com.ibm.ws.jaxrs20.fat.response.ResponseAPITest;
import com.ibm.ws.jaxrs20.fat.restmetrics.RestMetricsTest;
import com.ibm.ws.jaxrs20.fat.security.annotations.SecurityAnnotationsTest;
import com.ibm.ws.jaxrs20.fat.security.ssl.SecuritySSLTest;
import com.ibm.ws.jaxrs20.fat.securitycontext.SecurityContextTest;
import com.ibm.ws.jaxrs20.fat.service.scope.ServiceScopeTest;
import com.ibm.ws.jaxrs20.fat.servletcoexist.JAXRSServletCoexistTest;
import com.ibm.ws.jaxrs20.fat.standard.StandardProvidersTest;
import com.ibm.ws.jaxrs20.fat.subresource.ExceptionsSubresourcesTest;
import com.ibm.ws.jaxrs20.fat.thirdpartyjersey.JerseyTest;
import com.ibm.ws.jaxrs20.fat.thirdpartyjerseywithinjection.JerseyInjectionTest;
import com.ibm.ws.jaxrs20.fat.uriInfo.UriInfoTest;
import com.ibm.ws.jaxrs20.fat.wadl.WADLTest;
import com.ibm.ws.jaxrs20.fat.webcontainer.JAXRSWebContainerTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                AnnotationScanTest.class,
                BeanParamTest.class,
                CheckFeature12Test.class,
                ClientTest.class,
                ContextTest.class,
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
                ManagedBeansTest.class,
                ParamConverterTest.class,
                ParamsTest.class,
                ProviderCacheTest.class,
                ReaderWriterProvidersTest.class,
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
                //ValidationTest.class, //TODO: fix up and reenable tests - 6325
                WADLTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1"));
}
