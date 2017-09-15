/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileTransferHelperTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void getTempArchiveName() throws IllegalArgumentException, IOException {
        assertEquals("c:/myPath/file_original.zip", FileTransferHelper.getTempArchiveName("c:/myPath/file.zip"));
        assertEquals("c:/file_original.zip", FileTransferHelper.getTempArchiveName("c:/file.zip"));
        assertEquals("c:/file.jar_original.zip", FileTransferHelper.getTempArchiveName("c:/file.jar.zip"));
        assertEquals("/home/user/myFile_original.war", FileTransferHelper.getTempArchiveName("/home/user/myFile.war"));
        assertEquals("/myFile_original.zip", FileTransferHelper.getTempArchiveName("/myFile.zip"));
        assertEquals("/myFile.tar_original.gz", FileTransferHelper.getTempArchiveName("/myFile.tar.gz"));
        assertEquals("/myFile_original", FileTransferHelper.getTempArchiveName("/myFile"));
        assertEquals("/myFile.image/targetDir_original", FileTransferHelper.getTempArchiveName("/myFile.image/targetDir"));
        assertEquals("/myFile.image/targetDir_original.zip", FileTransferHelper.getTempArchiveName("/myFile.image/targetDir.zip"));
        assertEquals("C:\\myFile.image\\targetDir_original", FileTransferHelper.getTempArchiveName("C:\\myFile.image\\targetDir"));
        assertEquals("C:\\myFile.image\\targetDir_original.zip", FileTransferHelper.getTempArchiveName("C:\\myFile.image\\targetDir.zip"));
        assertEquals("C:\\myFile.image/blah/targetDir_original", FileTransferHelper.getTempArchiveName("C:\\myFile.image/blah/targetDir"));
        assertEquals("C:\\myFile.image/blah/targetDir_original.zip", FileTransferHelper.getTempArchiveName("C:\\myFile.image/blah/targetDir.zip"));
    }

    @Test
    public void getParentDir() {
        assertEquals("C:/temp", FileTransferHelper.getParentDir("C:/temp/wlp.zip"));
        assertEquals("C:/temp", FileTransferHelper.getParentDir("C:/temp/wlp"));
        assertEquals("C:/", FileTransferHelper.getParentDir("C:/temp.zip"));
        assertEquals("C:/", FileTransferHelper.getParentDir("C:/temp"));
        assertEquals("/home", FileTransferHelper.getParentDir("/home/myDir.zip"));
        assertEquals("/home", FileTransferHelper.getParentDir("/home/myDir"));
        assertEquals("/", FileTransferHelper.getParentDir("/home"));
        assertEquals("/", FileTransferHelper.getParentDir("/home.zip"));

    }
}
