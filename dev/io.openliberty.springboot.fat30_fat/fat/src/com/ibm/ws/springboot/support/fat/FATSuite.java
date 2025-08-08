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
package com.ibm.ws.springboot.support.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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
                JmsWebAppTests30.class,
                JmsSpringBootAppTests30.class,
                ConcurrencyAppTests30.class,
                ConcurrencyAppTests30War.class,
                ValidationTests30.class,
                ValidationTests30War.class,
                AopWebAppTests30.class,
                AopSpringBootAppTests30.class,
                MBeanAppTests20.class,
                MBeanAppTests20War.class
})

public class FATSuite {
    // Empty
}
