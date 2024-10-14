/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.fat.util.FatLogHandler;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.org.apache.myfaces41.fat.test.CompositeExtensionTest;
import io.openliberty.org.apache.myfaces41.fat.test.ConfigTests;
import io.openliberty.org.apache.myfaces41.fat.test.ContentLengthTest;
import io.openliberty.org.apache.myfaces41.fat.test.Faces41CDITests;
import io.openliberty.org.apache.myfaces41.fat.test.FacesConfigVersion41Test;
import io.openliberty.org.apache.myfaces41.fat.test.FacesMessagesTest;
import io.openliberty.org.apache.myfaces41.fat.test.RenderIdTest;
import io.openliberty.org.apache.myfaces41.fat.test.RowStatePreservedTest;
import io.openliberty.org.apache.myfaces41.fat.test.UUIDConverterTest;

@RunWith(Suite.class)
@SuiteClasses({
                ConfigTests.class,
                ContentLengthTest.class,
                CompositeExtensionTest.class,
                UUIDConverterTest.class,
                Faces41CDITests.class,
                FacesMessagesTest.class,
                RenderIdTest.class,
                RowStatePreservedTest.class,
                FacesConfigVersion41Test.class
})

public class FATSuite {

    /**
     * Utility method that will write xmlContent to output.txt and
     * when running locally will also write to a file under output/servers/[yourServer]/logs/output/
     *
     * @param xmlContent - Content from an XmlPage
     * @param fileName   - Name of the file, typically in the form [testname].[subtest].html
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
