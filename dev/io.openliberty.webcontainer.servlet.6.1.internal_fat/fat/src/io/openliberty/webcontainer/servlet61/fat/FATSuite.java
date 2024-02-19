/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61AddAndSetHeaderTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61CharsetEncodingTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61DispatcherErrorMethodAttributeTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61DoTraceRemoveSensitiveHeadersTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61EmptyURLPatternMappingTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61HTTPResponseCodesTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61HTTPServletMappingTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61RequestParameterTest;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61ResponseNoOpAfterCommit;
import io.openliberty.webcontainer.servlet61.fat.tests.Servlet61ResponseSendRedirectTest;

@RunWith(Suite.class)
@SuiteClasses({
    Servlet61AddAndSetHeaderTest.class,
    Servlet61CharsetEncodingTest.class,
    Servlet61DispatcherErrorMethodAttributeTest.class,
    Servlet61DoTraceRemoveSensitiveHeadersTest.class,
    Servlet61EmptyURLPatternMappingTest.class,
    Servlet61HTTPResponseCodesTest.class,
    Servlet61HTTPServletMappingTest.class,
    Servlet61RequestParameterTest.class,
    Servlet61ResponseNoOpAfterCommit.class,
    Servlet61ResponseSendRedirectTest.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
