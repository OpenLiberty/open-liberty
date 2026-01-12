/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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
package com.ibm.ws.springboot.support.fat.suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.springboot.support.fat.AopSpringBootAppTests20;
import com.ibm.ws.springboot.support.fat.AopWebAppTests20;
import com.ibm.ws.springboot.support.fat.ApplicationArgsTests;
import com.ibm.ws.springboot.support.fat.ApplicationStartedEventTests;
import com.ibm.ws.springboot.support.fat.CDITests;
import com.ibm.ws.springboot.support.fat.CommonWebFluxTests20;
import com.ibm.ws.springboot.support.fat.CommonWebFluxTests20Servlet40;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests15;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests15Servlet40;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests20;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests20Servlet40;
import com.ibm.ws.springboot.support.fat.ConcurrencyAppTests20;
import com.ibm.ws.springboot.support.fat.ConcurrencyAppTests20War;
import com.ibm.ws.springboot.support.fat.ConfigActuatorXMLOverrideTests20;
import com.ibm.ws.springboot.support.fat.ConfigDropinRootTests20;
import com.ibm.ws.springboot.support.fat.ConfigServerXMLOverrideTests20;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationClassloaderTests20;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationTagTests20;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationWithArgsTests20;
import com.ibm.ws.springboot.support.fat.EnableSpringBootTraceTests;
import com.ibm.ws.springboot.support.fat.ErrorPage15Test;
import com.ibm.ws.springboot.support.fat.ErrorPage20Test;
import com.ibm.ws.springboot.support.fat.ExceptionOccuredAfterAppIsAvailableTest20;
import com.ibm.ws.springboot.support.fat.ExtractedAppTests20;
import com.ibm.ws.springboot.support.fat.GenerateWebServerPluginTest;
import com.ibm.ws.springboot.support.fat.HTTPMetricsNoContextRootTest;
import com.ibm.ws.springboot.support.fat.HTTPMetricsWithContextRootTest;
import com.ibm.ws.springboot.support.fat.InvalidAppTests;
import com.ibm.ws.springboot.support.fat.JNDINoEEContextAppTests;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkAppTests20War;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkWeavingAppTests20War;
import com.ibm.ws.springboot.support.fat.JPAHibernateAppTests20War;
import com.ibm.ws.springboot.support.fat.JPALibertyAppTests20War;
import com.ibm.ws.springboot.support.fat.JSPTests20;
import com.ibm.ws.springboot.support.fat.JTAAppTests20;
import com.ibm.ws.springboot.support.fat.JTAAppTests20War;
import com.ibm.ws.springboot.support.fat.JavaeeFeatureTests15;
import com.ibm.ws.springboot.support.fat.JavaeeFeatureTests20;
import com.ibm.ws.springboot.support.fat.MBeanAppTests20;
import com.ibm.ws.springboot.support.fat.MBeanAppTests20War;
import com.ibm.ws.springboot.support.fat.MimeMapping15;
import com.ibm.ws.springboot.support.fat.MimeMapping20;
import com.ibm.ws.springboot.support.fat.MissingServletTests15;
import com.ibm.ws.springboot.support.fat.MissingServletTests20;
import com.ibm.ws.springboot.support.fat.MissingSslFeatureTests;
import com.ibm.ws.springboot.support.fat.MissingWebsocketFeatureTests20;
import com.ibm.ws.springboot.support.fat.MultContextTests20;
import com.ibm.ws.springboot.support.fat.MultiModuleProjectTests20;
import com.ibm.ws.springboot.support.fat.MultipleApplicationsNotSupported20;
import com.ibm.ws.springboot.support.fat.NeedSpringBoot15FeatureTests;
import com.ibm.ws.springboot.support.fat.NeedSpringBoot20FeatureTests;
import com.ibm.ws.springboot.support.fat.NoServletRequiredAppTests20;
import com.ibm.ws.springboot.support.fat.NonZipExtensionFilesInBootInfLibTests20;
import com.ibm.ws.springboot.support.fat.PreThinnedSpringBootTests20;
import com.ibm.ws.springboot.support.fat.ProgrammaticTransAppTests20;
import com.ibm.ws.springboot.support.fat.ProgrammaticTransAppTests20War;
import com.ibm.ws.springboot.support.fat.SSLMutualAuthTests15;
import com.ibm.ws.springboot.support.fat.SSLMutualAuthTests20;
import com.ibm.ws.springboot.support.fat.SSLTests15;
import com.ibm.ws.springboot.support.fat.SSLTests20;
import com.ibm.ws.springboot.support.fat.SpringSecurityTests20;
import com.ibm.ws.springboot.support.fat.TemplateTests;
import com.ibm.ws.springboot.support.fat.UnsupportedConfigWarningTest15;
import com.ibm.ws.springboot.support.fat.UnsupportedConfigWarningTest20;
import com.ibm.ws.springboot.support.fat.UseDefaultHostTest15;
import com.ibm.ws.springboot.support.fat.UseDefaultHostTest20;
import com.ibm.ws.springboot.support.fat.ValidationTests20;
import com.ibm.ws.springboot.support.fat.ValidationTests20War;
import com.ibm.ws.springboot.support.fat.WarmStartTests20;
import com.ibm.ws.springboot.support.fat.WebAnnotationTests20;
import com.ibm.ws.springboot.support.fat.WebSocketTests20;
import com.ibm.ws.springboot.support.fat.utility.SpringBootUtilityThinTest;

@RunWith(Suite.class)
@SuiteClasses({
                CommonWebServerTests15.class,
                CommonWebServerTests15Servlet40.class,
                CommonWebServerTests20.class,
                CommonWebServerTests20Servlet40.class,
                CommonWebFluxTests20.class,
                CommonWebFluxTests20Servlet40.class,
                UnsupportedConfigWarningTest15.class,
                UnsupportedConfigWarningTest20.class,
                ConfigDropinRootTests20.class,
                ConfigSpringBootApplicationTagTests20.class,
                ConfigSpringBootApplicationClassloaderTests20.class,
                ConfigSpringBootApplicationWithArgsTests20.class,
                ConfigServerXMLOverrideTests20.class,
                ConfigActuatorXMLOverrideTests20.class,
                ApplicationArgsTests.class,
                CDITests.class,
                GenerateWebServerPluginTest.class,
                UseDefaultHostTest15.class,
                UseDefaultHostTest20.class,
                PreThinnedSpringBootTests20.class,
                WarmStartTests20.class,
                SSLTests15.class,
                SSLTests20.class,
                SSLMutualAuthTests15.class,
                SSLMutualAuthTests20.class,
                SpringSecurityTests20.class,
                JSPTests20.class,
                MissingServletTests15.class,
                MissingServletTests20.class,
                MissingSslFeatureTests.class,
                MissingWebsocketFeatureTests20.class,
                MultContextTests20.class,
                MultipleApplicationsNotSupported20.class,
                NeedSpringBoot15FeatureTests.class,
                NeedSpringBoot20FeatureTests.class,
                InvalidAppTests.class,
                NoServletRequiredAppTests20.class,
                SpringBootUtilityThinTest.class,
                WebAnnotationTests20.class,
                ExtractedAppTests20.class,
                WebSocketTests20.class,
                MimeMapping15.class,
                MimeMapping20.class,
                ErrorPage15Test.class,
                ErrorPage20Test.class,
                EnableSpringBootTraceTests.class,
                ExceptionOccuredAfterAppIsAvailableTest20.class,
                JavaeeFeatureTests15.class,
                JavaeeFeatureTests20.class,
                TemplateTests.class,
                NonZipExtensionFilesInBootInfLibTests20.class,
                MultiModuleProjectTests20.class,
                ApplicationStartedEventTests.class,
                JNDINoEEContextAppTests.class,
                JTAAppTests20.class,
                JTAAppTests20War.class,
                ProgrammaticTransAppTests20.class,
                ProgrammaticTransAppTests20War.class,
                JPAHibernateAppTests20War.class,
                JPALibertyAppTests20War.class,
                JPAEclipseLinkAppTests20War.class,
                JPAEclipseLinkWeavingAppTests20War.class,
                ConcurrencyAppTests20.class,
                ConcurrencyAppTests20War.class,
                ValidationTests20.class,
                ValidationTests20War.class,
                AopWebAppTests20.class,
                AopSpringBootAppTests20.class,
                MBeanAppTests20.class,
                MBeanAppTests20War.class,
                HTTPMetricsNoContextRootTest.class,
                HTTPMetricsWithContextRootTest.class
})

public class FATSuite {
    // EMPTY
}
