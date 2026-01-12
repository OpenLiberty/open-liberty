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

import com.ibm.ws.springboot.support.fat.AopSpringBootAppTests30;
import com.ibm.ws.springboot.support.fat.AopWebAppTests30;
import com.ibm.ws.springboot.support.fat.ApplicationArgsTests30;
import com.ibm.ws.springboot.support.fat.ApplicationStartedEventTests30;
import com.ibm.ws.springboot.support.fat.CDITests30;
import com.ibm.ws.springboot.support.fat.CommonWebFluxTests30;
import com.ibm.ws.springboot.support.fat.CommonWebServerTests30;
import com.ibm.ws.springboot.support.fat.ConcurrencyAppTests30;
import com.ibm.ws.springboot.support.fat.ConcurrencyAppTests30War;
import com.ibm.ws.springboot.support.fat.ConfigActuatorXMLOverrideTests30;
import com.ibm.ws.springboot.support.fat.ConfigDropinRootTests30;
import com.ibm.ws.springboot.support.fat.ConfigServerXMLOverrideTests30;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationClassloaderTests30;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationTagTests30;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationTagWarTests30;
import com.ibm.ws.springboot.support.fat.ConfigSpringBootApplicationWithArgsTests30;
import com.ibm.ws.springboot.support.fat.EnableSpringBootTraceTests30;
import com.ibm.ws.springboot.support.fat.ErrorPage30Test;
import com.ibm.ws.springboot.support.fat.ExceptionOccuredAfterAppIsAvailableTest30;
import com.ibm.ws.springboot.support.fat.ExtractedAppTests30;
import com.ibm.ws.springboot.support.fat.GenerateWebServerPluginTests30;
import com.ibm.ws.springboot.support.fat.HTTPMetricsNoContextRootTest;
import com.ibm.ws.springboot.support.fat.HTTPMetricsWithContextRootTest;
import com.ibm.ws.springboot.support.fat.InvalidAppTests;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkAppTests30;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkAppTests30War;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkWeavingAppTests30;
import com.ibm.ws.springboot.support.fat.JPAEclipseLinkWeavingAppTests30War;
import com.ibm.ws.springboot.support.fat.JPAHibernateAppTests30;
import com.ibm.ws.springboot.support.fat.JPAHibernateAppTests30War;
import com.ibm.ws.springboot.support.fat.JPALibertyAppTests30War;
import com.ibm.ws.springboot.support.fat.JSPTests30;
import com.ibm.ws.springboot.support.fat.JTAAppTests30;
import com.ibm.ws.springboot.support.fat.JTAAppTests30War;
import com.ibm.ws.springboot.support.fat.JakartaFeatureTests30;
import com.ibm.ws.springboot.support.fat.MBeanAppTests30;
import com.ibm.ws.springboot.support.fat.MBeanAppTests30War;
import com.ibm.ws.springboot.support.fat.MimeMapping30;
import com.ibm.ws.springboot.support.fat.MissingServletTests30;
import com.ibm.ws.springboot.support.fat.MissingSslFeatureTests30;
import com.ibm.ws.springboot.support.fat.MissingWebsocketFeatureTests30;
import com.ibm.ws.springboot.support.fat.MultiContextTests30;
import com.ibm.ws.springboot.support.fat.MultiModuleProjectTests30;
import com.ibm.ws.springboot.support.fat.MultipleApplicationsNotSupported30;
import com.ibm.ws.springboot.support.fat.NeedSpringBootFeatureTests30;
import com.ibm.ws.springboot.support.fat.NoServletRequiredAppTests30;
import com.ibm.ws.springboot.support.fat.NonZipExtensionFilesInBootInfLibTests30;
import com.ibm.ws.springboot.support.fat.PreThinnedSpringBootTests30;
import com.ibm.ws.springboot.support.fat.SSLMutualAuthTests30;
import com.ibm.ws.springboot.support.fat.SSLTests30;
import com.ibm.ws.springboot.support.fat.SpringSecurityTests30;
import com.ibm.ws.springboot.support.fat.TemplateTests30;
import com.ibm.ws.springboot.support.fat.UnsupportedConfigWarningTest30;
import com.ibm.ws.springboot.support.fat.UseDefaultHostTest30;
import com.ibm.ws.springboot.support.fat.ValidationTests30;
import com.ibm.ws.springboot.support.fat.ValidationTests30War;
import com.ibm.ws.springboot.support.fat.WarmStartTests30;
import com.ibm.ws.springboot.support.fat.WebAnnotationTests30;
import com.ibm.ws.springboot.support.fat.WebSocketSpringBootAppTests30;
import com.ibm.ws.springboot.support.fat.WebSocketWebAppTests30;
import com.ibm.ws.springboot.support.fat.utility.SpringBootUtilityThinTest;

@RunWith(Suite.class)
@SuiteClasses({
                CommonWebServerTests30.class,
                CommonWebFluxTests30.class,
                UnsupportedConfigWarningTest30.class,
                ConfigDropinRootTests30.class,
                ConfigSpringBootApplicationTagTests30.class,
                ConfigSpringBootApplicationTagWarTests30.class,
                ConfigSpringBootApplicationClassloaderTests30.class,
                ConfigSpringBootApplicationWithArgsTests30.class,
                ConfigServerXMLOverrideTests30.class,
                ConfigActuatorXMLOverrideTests30.class,
                ApplicationArgsTests30.class,
                CDITests30.class,
                GenerateWebServerPluginTests30.class,
                UseDefaultHostTest30.class,
                PreThinnedSpringBootTests30.class,
                WarmStartTests30.class,
                SSLTests30.class,
                SSLMutualAuthTests30.class,
                SpringSecurityTests30.class,
                JSPTests30.class,
                MissingServletTests30.class,
                MissingSslFeatureTests30.class,
                MissingWebsocketFeatureTests30.class,
                MultiContextTests30.class,
                MultipleApplicationsNotSupported30.class,
                NeedSpringBootFeatureTests30.class,
                InvalidAppTests.class,
                NoServletRequiredAppTests30.class,
                SpringBootUtilityThinTest.class,
                WebAnnotationTests30.class,
                ExtractedAppTests30.class,
                WebSocketSpringBootAppTests30.class,
                WebSocketWebAppTests30.class,
                MimeMapping30.class,
                ErrorPage30Test.class,
                EnableSpringBootTraceTests30.class,
                ExceptionOccuredAfterAppIsAvailableTest30.class,
                JakartaFeatureTests30.class,
                TemplateTests30.class,
                NonZipExtensionFilesInBootInfLibTests30.class,
                MultiModuleProjectTests30.class,
                ApplicationStartedEventTests30.class,
                JTAAppTests30.class,
                JTAAppTests30War.class,
                JPAHibernateAppTests30War.class,
                JPALibertyAppTests30War.class,
                JPAEclipseLinkAppTests30War.class,
                JPAEclipseLinkWeavingAppTests30War.class,
                JPAEclipseLinkAppTests30.class,
                JPAEclipseLinkWeavingAppTests30.class,
                JPAHibernateAppTests30.class,
//                JPALibertyAppTests30.class,
                ConcurrencyAppTests30.class,
                ConcurrencyAppTests30War.class,
                ValidationTests30.class,
                ValidationTests30War.class,
                AopWebAppTests30.class,
                AopSpringBootAppTests30.class,
                MBeanAppTests30.class,
                MBeanAppTests30War.class,
                HTTPMetricsNoContextRootTest.class,
                HTTPMetricsWithContextRootTest.class
})

public class FATSuite {
    // Empty
}
