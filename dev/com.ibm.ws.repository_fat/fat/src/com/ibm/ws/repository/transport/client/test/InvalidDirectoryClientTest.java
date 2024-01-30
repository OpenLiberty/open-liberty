/*******************************************************************************
 * Copyright (c) 2015, 2023 IBM Corporation and others.
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
package com.ibm.ws.repository.transport.client.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.ReadMode;
import com.ibm.ws.repository.transport.client.AbstractFileClient;
import com.ibm.ws.repository.transport.client.DirectoryClient;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

public class InvalidDirectoryClientTest {

    private final static File resourceDir = new File("lib/LibertyFATTestFiles");

    @Test
    public void testRepositoryStatusNoDirFound() throws Exception {
        AbstractFileClient noDirClient = new DirectoryClient(new File("doesnotexist"), ReadMode.DETECT_CHANGES);
        try {
            noDirClient.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (FileNotFoundException e) {
            // We should get a FileNotFoundException from checkRepositoryStatus, any other exception should be
            // allowed to propagate back up to cause the test to fail
        }
    }

    @Test
    public void testRepositoryStatusRootIsAFileNotADir() throws IOException, RequestFailureException {
        AbstractFileClient invalidFile = new DirectoryClient(new File(resourceDir, "TestAttachment.txt"), ReadMode.DETECT_CHANGES);
        try {
            invalidFile.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (IOException e) {
            assertTrue("Wrong exception messages,  expected \"is not a directory\" but got \"" + e.getMessage() + "\"",
                       e.getMessage().contains("is not a directory"));
        }
    }

}
