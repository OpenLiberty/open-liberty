/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
                    // SpringSecurityTests30.class,
                    // Disabled due to application failures.
                JSPTests30.class,
                    // MissingServletTests30.class,
                    // Disabled: The jakarta servlet class is being provisioned even without the servlet feature!
                MissingSslFeatureTests30.class,
                    // MissingWebsocketFeatureTests30.class,
                    // Disabled: The jakarta web socket class is being provisioned even without the web socket feature!
                MultiContextTests30.class,
                MultipleApplicationsNotSupported30.class,
                NeedSpringBootFeatureTests30.class,
                InvalidAppTests.class,
                NoServletRequiredAppTests30.class,
                SpringBootUtilityThinTest.class,
                WebAnnotationTests30.class,
                ExtractedAppTests30.class,
                WebSocketTests30.class,
                MimeMapping30.class,
                ErrorPage30Test.class,
                    // EnableSpringBootTraceTests30.class,
                    // Disabled: Failing to start web server.  See the test class for details.
                ExceptionOccuredAfterAppIsAvailableTest30.class,
                JakartaFeatureTests30.class,
                TemplateTests30.class,
                NonZipExtensionFilesInBootInfLibTests30.class,
                MultiModuleProjectTests30.class,
                ApplicationStartedEventTests30.class
})

public class FATSuite {
    // Empty
}
