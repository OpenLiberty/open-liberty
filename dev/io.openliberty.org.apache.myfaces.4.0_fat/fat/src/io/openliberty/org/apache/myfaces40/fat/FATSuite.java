/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces40.fat;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces40.fat.tests.AcceptInputFileTest;
import io.openliberty.org.apache.myfaces40.fat.tests.AjaxRenderExecuteThisTest;
import io.openliberty.org.apache.myfaces40.fat.tests.AnnotationLiteralsTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ClientWindowScopedTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExtensionlessMappingTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExternalContextAddResponseCookieTest;
import io.openliberty.org.apache.myfaces40.fat.tests.Faces40ThirdPartyApiTests;
import io.openliberty.org.apache.myfaces40.fat.tests.Faces40URNTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesConfigTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesContextGetLifecycleTest;
import io.openliberty.org.apache.myfaces40.fat.tests.Html5Tests;
import io.openliberty.org.apache.myfaces40.fat.tests.InputTextTypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.LayoutAttributeTests;
import io.openliberty.org.apache.myfaces40.fat.tests.MultipleInputFileTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ProgrammaticFaceletTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SelectItemTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SubscribeToEventTest;
import io.openliberty.org.apache.myfaces40.fat.tests.UIViewRootGetDoctypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.WebSocketTests;
import io.openliberty.org.apache.myfaces40.fat.tests.bugfixes.MyFaces4628Test;

@RunWith(Suite.class)
@SuiteClasses({
                AcceptInputFileTest.class,
                AjaxRenderExecuteThisTest.class,
                AnnotationLiteralsTest.class,
                ClientWindowScopedTest.class,
                ExtensionlessMappingTest.class,
                ExternalContextAddResponseCookieTest.class,
                Faces40ThirdPartyApiTests.class,
                Faces40URNTest.class,
                FacesConfigTest.class,
                FacesContextGetLifecycleTest.class,
                InputTextTypeTest.class,
                LayoutAttributeTests.class,
                MultipleInputFileTest.class,
                ProgrammaticFaceletTests.class,
                SelectItemTests.class,
                SubscribeToEventTest.class,
                UIViewRootGetDoctypeTest.class,
                Faces40URNTest.class,
                WebSocketTests.class,
                Html5Tests.class,
                MyFaces4628Test.class

})

public class FATSuite {

    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new EmptyAction().conditionalFullFATOnly(EmptyAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                                                  .andWith(FeatureReplacementAction.EE11_FEATURES());

    /**
     * Utility method that will write xmlContent to output.txt and
     * when running locally will also write to a file under output/servers/[yourServer]/logs/output/
     *
     * @param xmlContent - Content from an XmlPage
     * @param fileName - Name of the file, typically in the form [testname].[subtest].html
     */
    public static final void logOutputForDebugging(LibertyServer server, String xmlContent, String fileName) {
        //always output to log
        Log.info(FATSuite.class, "writeOutputToFile", xmlContent);

        if (!FATRunner.FAT_TEST_LOCALRUN) {
            return;
        }

        //log to separate file locally
        File outputDir = new File(server.getLogsRoot(), "output");
        File outputFile = new File(outputDir, fileName);
        outputDir.mkdirs();

        try (FileOutputStream fos = new FileOutputStream(outputFile, true)) {
            fos.write(xmlContent.getBytes());
        } catch (Exception e) {
            //ignore only using for debugging
        }
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
