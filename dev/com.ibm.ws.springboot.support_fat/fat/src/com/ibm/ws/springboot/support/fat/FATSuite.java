/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
                CommonWebServerTests15.class,
                CommonWebServerTests15Servlet40.class,
                CommonWebServerTests20.class,
                CommonWebServerTests20Servlet40.class,
                CommonWebFluxTests20.class,
                CommonWebFluxTests20Servlet40.class,
                CommonWebServerTests21.class,
                CommonWebServerTests22.class,
                CommonWebServerTests23.class,
                WebFluxWrongSpringBootVersionTests.class,
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
                MultiModuleProjectTests20.class
})

public class FATSuite {

}
