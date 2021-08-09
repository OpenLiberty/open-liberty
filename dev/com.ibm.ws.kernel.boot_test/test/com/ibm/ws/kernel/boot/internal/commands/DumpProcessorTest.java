/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.shared.Constants;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.ReturnCode;

/**
 *
 */
@Ignore
public class DumpProcessorTest {

    private static SharedOutputManager outputMgr;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // capture system output
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testUnableToFindJavaDumps() throws Exception {
        final String serverName = "server-testUnableToFindJavaDumps";
        final File tempServerDir = new File(new File(Constants.TEST_TMP_ROOT_FILE, "servers"), serverName);
        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);

        // Methods called on BootstratpConfig from DumpProcessor execute method.
        mockery.checking(new Expectations() {
            {
                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getOutputFile(null);
                will(returnValue(tempServerDir));
            }
        });

        File dumpFile = new File(Constants.TEST_TMP_ROOT_FILE, "dump.testUnableToFindJavaDumps.zip").getAbsoluteFile();

        assertTrue("Precondition Failed - unable to create temp server directory", tempServerDir.mkdirs());

        dumpFile.createNewFile();

        // create paths to heap and system dumps that do not exist
        List<String> javaDumps = new ArrayList<String>();
        javaDumps.add(new File(Constants.TEST_TMP_ROOT, "NON_EXISTENT_HEAP_DUMP.phd").getAbsolutePath());
        javaDumps.add(new File(Constants.TEST_TMP_ROOT, "NON_EXISTENT_SYSTEM_DUMP.dmp").getAbsolutePath());

        try {
            DumpProcessor dumpProcessor = new DumpProcessor(serverName, dumpFile, mockBootConfig, javaDumps);
            assertEquals(ReturnCode.OK, dumpProcessor.execute());

            // verify that the user is alerted to missing java dump files:
            assertTrue("Did not find expected error message for missing heap dump file", outputMgr.checkForStandardOut("CWWKE0009E.*NON_EXISTENT_HEAP_DUMP.phd"));
            assertTrue("Did not find expected error message for missing system dump file", outputMgr.checkForStandardOut("CWWKE0009E.*NON_EXISTENT_SYSTEM_DUMP.dmp"));
        } finally {
            if (dumpFile.exists()) {
                dumpFile.delete();
            }
            if (tempServerDir.exists()) {
                tempServerDir.delete();
            }
        }
    }
}
