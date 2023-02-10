/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.org.apache.myfaces40.fat;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces40.fat.tests.AcceptInputFileTest;
import io.openliberty.org.apache.myfaces40.fat.tests.AjaxRenderExecuteThisTest;
import io.openliberty.org.apache.myfaces40.fat.tests.AnnotationLiteralsTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExtensionlessMappingTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExternalContextAddResponseCookieTest;
import io.openliberty.org.apache.myfaces40.fat.tests.Faces40ThirdPartyApiTests;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesConfigTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesContextGetLifecycleTest;
import io.openliberty.org.apache.myfaces40.fat.tests.InputTextTypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.LayoutAttributeTests;
import io.openliberty.org.apache.myfaces40.fat.tests.MultipleInputFileTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ProgrammaticFaceletTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SelectItemTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SimpleTest;
import io.openliberty.org.apache.myfaces40.fat.tests.UIViewRootGetDoctypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.WebSocketTests;

@RunWith(Suite.class)
@SuiteClasses({
                AcceptInputFileTest.class,
                AjaxRenderExecuteThisTest.class,
                AnnotationLiteralsTest.class,
                SimpleTest.class,
                ExternalContextAddResponseCookieTest.class,
                FacesConfigTest.class,
                InputTextTypeTest.class,
                LayoutAttributeTests.class,
                MultipleInputFileTest.class,
                ExtensionlessMappingTest.class,
                SelectItemTests.class,
                FacesContextGetLifecycleTest.class,
                UIViewRootGetDoctypeTest.class,
                Faces40ThirdPartyApiTests.class,
                ProgrammaticFaceletTests.class,
                WebSocketTests.class,
})
public class FATSuite {

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

}
