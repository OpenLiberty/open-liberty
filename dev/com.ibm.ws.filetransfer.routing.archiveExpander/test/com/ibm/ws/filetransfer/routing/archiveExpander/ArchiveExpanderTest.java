/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.filetransfer.routing.archiveExpander;

import com.ibm.ws.filetransfer.routing.archiveExpander.ArchiveExpander;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArchiveExpanderTest {

    @Rule
    public TemporaryFolder f = new TemporaryFolder();

    @Test
    public void goodTest() throws IOException {
        File goodZip = makeZip("good.zip", "goodfile.txt", "good-too.txt");
        ArchiveExpander.coreExpandArchive(goodZip.getPath(), f.newFolder().getPath());
    }

    @Test
    public void badTest() throws IOException {
        File badZip = makeZip("bad.zip", "../badfile.txt", "good.txt");
        try {
            ArchiveExpander.coreExpandArchive(badZip.getPath(), f.newFolder().getPath());
        } catch (IOException e) {
            return;
        }
        Assert.fail("The expected exception was not thrown");
    }

    public File makeZip(String path, String... contents) throws IOException {
        File outZip = f.newFile(path);
        ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outZip));
        for (String name: contents) {
            byte[] contentBytes = name.getBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(contentBytes);
            ZipEntry zipEntry = new ZipEntry(name);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = bais.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            bais.close();
        }
        zipOut.close();
        return outZip;
    }
}
