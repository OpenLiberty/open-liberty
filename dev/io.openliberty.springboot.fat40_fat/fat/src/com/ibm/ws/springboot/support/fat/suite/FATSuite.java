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

import com.ibm.ws.springboot.support.fat.AopSpringBootAppTests40;
import com.ibm.ws.springboot.support.fat.AopWebAppTests40;
import com.ibm.ws.springboot.support.fat.ApplicationArgsTests40;
import com.ibm.ws.springboot.support.fat.ApplicationStartedEventTests40;
import com.ibm.ws.springboot.support.fat.CDITests40;
import com.ibm.ws.springboot.support.fat.CommonWebFluxTests40;
import com.ibm.ws.springboot.support.fat.CommonWebFluxTests40War;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests40;
import com.ibm.ws.springboot.support.fat.ConcurrencyAppTests40;
import com.ibm.ws.springboot.support.fat.ConcurrencyAppTests40War;
import com.ibm.ws.springboot.support.fat.ConfigActuatorXMLOverrideTests40;
import com.ibm.ws.springboot.support.fat.ConfigDropinRootTests40;
import com.ibm.ws.springboot.support.fat.ConfigServerXMLOverrideTests40;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationClassloaderTests40;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationTagTests40;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationTagWarTests40;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationWithArgsTests40;
import com.ibm.ws.springboot.support.fat.EnableSpringBootTraceTests40;
import com.ibm.ws.springboot.support.fat.ErrorPage40Test;
import com.ibm.ws.springboot.support.fat.ExceptionOccuredAfterAppIsAvailableTest40;
import com.ibm.ws.springboot.support.fat.ExtractedAppTests40;
import com.ibm.ws.springboot.support.fat.GenerateWebServerPluginTests40;
import com.ibm.ws.springboot.support.fat.HTTPMetricsNoContextRootTest;
import com.ibm.ws.springboot.support.fat.HTTPMetricsWithContextRootTest;
import com.ibm.ws.springboot.support.fat.InvalidAppTests;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkAppTests40;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkAppTests40War;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkWeavingAppTests40;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkWeavingAppTests40War;
import com.ibm.ws.springboot.support.fat.JPAHibernateAppTests40;
import com.ibm.ws.springboot.support.fat.JPAHibernateAppTests40War;
import com.ibm.ws.springboot.support.fat.JPALibertyAppTests40War;
import com.ibm.ws.springboot.support.fat.JSPTests40;
import com.ibm.ws.springboot.support.fat.JTAAppTests40;
import com.ibm.ws.springboot.support.fat.JTAAppTests40War;
import com.ibm.ws.springboot.support.fat.JakartaFeatureTests40;
import com.ibm.ws.springboot.support.fat.MBeanAppTests40;
import com.ibm.ws.springboot.support.fat.MBeanAppTests40War;
import com.ibm.ws.springboot.support.fat.MimeMapping40;
import com.ibm.ws.springboot.support.fat.MissingServletTests40;
import com.ibm.ws.springboot.support.fat.MissingSslFeatureTests40;
import com.ibm.ws.springboot.support.fat.MissingWebsocketFeatureTests40;
import com.ibm.ws.springboot.support.fat.MultiContextTests40;
import com.ibm.ws.springboot.support.fat.MultiModuleProjectTests40;
import com.ibm.ws.springboot.support.fat.MultipleApplicationsNotSupported40;
import com.ibm.ws.springboot.support.fat.NeedSpringBootFeatureTests40;
import com.ibm.ws.springboot.support.fat.NoServletRequiredAppTests40;
import com.ibm.ws.springboot.support.fat.NonZipExtensionFilesInBootInfLibTests40;
import com.ibm.ws.springboot.support.fat.PreThinnedSpringBootTests40;
import com.ibm.ws.springboot.support.fat.SSLMutualAuthTests40;
import com.ibm.ws.springboot.support.fat.SSLTests40;
import com.ibm.ws.springboot.support.fat.SpringSecurityTests40;
import com.ibm.ws.springboot.support.fat.TemplateTests40;
import com.ibm.ws.springboot.support.fat.UnsupportedConfigWarningTest40;
import com.ibm.ws.springboot.support.fat.UseDefaultHostTest40;
import com.ibm.ws.springboot.support.fat.ValidationTests40;
import com.ibm.ws.springboot.support.fat.ValidationTests40War;
import com.ibm.ws.springboot.support.fat.WarmStartTests40;
import com.ibm.ws.springboot.support.fat.WebAnnotationTests40;
import com.ibm.ws.springboot.support.fat.WebSocketSpringBootAppTests40;
import com.ibm.ws.springboot.support.fat.WebSocketWebAppTests40;
import com.ibm.ws.springboot.support.fat.utility.SpringBootUtilityThinTest;

@RunWith(Suite.class)
@SuiteClasses({
                CommonWebServerTests40.class,
                ErrorPage40Test.class,
                CDITests40.class,
                ConfigDropinRootTests40.class,
                ConfigServerXMLOverrideTests40.class,
                ConfigSpringBootApplicationClassloaderTests40.class,
                ConfigSpringBootApplicationTagTests40.class,
                ConfigSpringBootApplicationWithArgsTests40.class,
                EnableSpringBootTraceTests40.class,
                ExceptionOccuredAfterAppIsAvailableTest40.class,
                MissingServletTests40.class,
                MissingSslFeatureTests40.class,
                SSLTests40.class,
                SSLMutualAuthTests40.class,
                NeedSpringBootFeatureTests40.class,
                MultipleApplicationsNotSupported40.class,
                NonZipExtensionFilesInBootInfLibTests40.class,
                PreThinnedSpringBootTests40.class,
                UnsupportedConfigWarningTest40.class,
                UseDefaultHostTest40.class,
                WarmStartTests40.class,
                ExtractedAppTests40.class,
                GenerateWebServerPluginTests40.class,
                JakartaFeatureTests40.class,
                MimeMapping40.class,
                ConfigSpringBootApplicationTagWarTests40.class,
                SpringBootUtilityThinTest.class,
                JSPTests40.class,
                MultiModuleProjectTests40.class,
                ConfigActuatorXMLOverrideTests40.class,
                CommonWebFluxTests40.class,
                CommonWebFluxTests40War.class,
                NoServletRequiredAppTests40.class,
                MultiContextTests40.class,
                WebAnnotationTests40.class,
                WebSocketSpringBootAppTests40.class,
                WebSocketWebAppTests40.class,
                SpringSecurityTests40.class,
                InvalidAppTests.class,
                ApplicationArgsTests40.class,
                MissingWebsocketFeatureTests40.class,
                TemplateTests40.class,
                ApplicationStartedEventTests40.class,
                ConcurrencyAppTests40.class,
                ConcurrencyAppTests40War.class,
                MBeanAppTests40.class,
                MBeanAppTests40War.class,
                JTAAppTests40.class,
                JTAAppTests40War.class,
                JPAEclipseLinkAppTests40War.class,
                JPAEclipseLinkWeavingAppTests40War.class,
                JPAHibernateAppTests40War.class,
                JPALibertyAppTests40War.class,
                JPAEclipseLinkAppTests40.class,
                JPAEclipseLinkWeavingAppTests40.class,
                JPAHibernateAppTests40.class,
                //JPALibertyAppTests40.class,
                ValidationTests40.class,
                ValidationTests40War.class,
                HTTPMetricsNoContextRootTest.class,
                HTTPMetricsWithContextRootTest.class,
                AopSpringBootAppTests40.class,
                AopWebAppTests40.class
})

public class FATSuite {
}
