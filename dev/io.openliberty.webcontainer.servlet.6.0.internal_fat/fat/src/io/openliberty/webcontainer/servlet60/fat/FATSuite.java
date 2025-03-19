/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet60.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60CookieSetAttributeTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60GetMappingAsyncDispatchTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60GetRealPathDirectoryBrowsingTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60GetRealPathTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60PartitionedAttributeTests;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60RequestConnectionTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60RequestCookieHeaderTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60ResponseNullCharacterEncodingTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60SessionCookieConfigSCITest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60SessionCookieConfigXMLTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60URIPathCanonicalizationBadRequestTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60URIPathCanonicalizationInvalidWebXMLTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60URIPathCanonicalizationServerXMLTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60URIPathCanonicalizationTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60URIPathCanonicalizationWebXMLTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60XPoweredByHeaderTest;

@RunWith(Suite.class)
@SuiteClasses({
                Servlet60XPoweredByHeaderTest.class,
                Servlet60GetMappingAsyncDispatchTest.class,
                Servlet60GetRealPathDirectoryBrowsingTest.class,
                Servlet60GetRealPathTest.class,
                Servlet60CookieSetAttributeTest.class,
                Servlet60RequestConnectionTest.class,
                Servlet60SessionCookieConfigXMLTest.class,
                Servlet60SessionCookieConfigSCITest.class,
                Servlet60RequestCookieHeaderTest.class,
                Servlet60ResponseNullCharacterEncodingTest.class,
                Servlet60URIPathCanonicalizationBadRequestTest.class,
                Servlet60URIPathCanonicalizationInvalidWebXMLTest.class,
                Servlet60URIPathCanonicalizationServerXMLTest.class,
                Servlet60URIPathCanonicalizationTest.class,
                Servlet60URIPathCanonicalizationWebXMLTest.class,
                Servlet60PartitionedAttributeTests.class
})
public class FATSuite {

    // EE11 requires Java 17
    // If we only specify EE11 for lite mode it will cause no tests to run with lower Java versions which causes an error.
    // If we are running on a Java version less than 17, have EE10 (EmptyAction) be the lite mode test to run.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES().setSkipTransformation(true));

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
