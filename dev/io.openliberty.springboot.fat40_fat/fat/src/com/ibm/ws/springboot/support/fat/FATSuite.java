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
                //NeedSpringBootFeatureTests40.class,     // Disabling reason: See class for comments
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
                JmsSpringBootAppTests40.class,
                JmsWebAppTests40.class,
                JTAAppTests40.class,
                JTAAppTests40War.class,
                JPAEclipseLinkAppTests40War.class,
                JPAEclipseLinkWeavingAppTests40War.class,
                JPAHibernateAppTests40War.class,
                JPALibertyAppTests40War.class,
                JPAEclipseLinkAppTests40.class,
                JPAEclipseLinkWeavingAppTests40.class,
                JPAHibernateAppTests40.class,
                ValidationTests40.class,
                ValidationTests40War.class
//JPALibertyAppTests40.class

})

public class FATSuite {
}
