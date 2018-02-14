/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import com.ibm.ws.repository.transport.client.AbstractFileClient;
import com.ibm.ws.repository.transport.client.ZipClient;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;

public class InvalidZipClientTest {

    private final static File resourceDir = new File("lib/LibertyFATTestFiles");

    @Test
    public void testRepositoryStatusNoZipFound() throws Exception {
        AbstractFileClient noZipClient = new ZipClient(new File("doesnotexist.zip"));
        try {
            noZipClient.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (FileNotFoundException e) {
            // We should get a FileNotFoundException from checkRepositoryStatus, any other exception should be
            // allowed to propagate back up to cause the test to fail
        }
    }

    @Test
    public void testRepositoryStatusInvalidZipFile() throws IOException, RequestFailureException {
        AbstractFileClient invalidFile = new ZipClient(new File(resourceDir, "TestAttachment.txt"));
        try {
            invalidFile.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (IOException e) {
            assertTrue("Wrong exception messages,  expected \"error in opening zip file\" but got \"" + e.getMessage() + "\"",
                       e.getMessage().contains("error in opening zip file"));
        }
    }
}
