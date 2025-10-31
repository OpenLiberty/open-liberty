/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.File;
import java.util.Formatter;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class FolderStructureGeneratorTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    private static final String testClassesDir = System.getProperty("test.classesDir", "bin_test");
    private static final File dataDir = new File(testClassesDir + "/test data/dump");

    @Rule
    public TestRule outputRule = outputMgr;

    @Test
    public void testSha512() {
        String sha512 = new ServerDumpPackager.FolderStructureGenerator().hash(new File(dataDir, "/sha512-1.txt"));
        Assert.assertEquals("1f47c433a2b1d1aef1c6f6d645af845374f3e0707cf44f11edeaabb9d376ecb948c9f7bafc7ba14320f1ff1de189cb137e834444d0b4e832af68f39cfb4cfe7e", sha512);
    }

    @Test
    public void testPrintFileList() throws Exception {
        StringBuilder builder = new StringBuilder();
        Formatter formatter = new Formatter(builder);
        new ServerDumpPackager.FolderStructureGenerator().printFileList(dataDir, formatter, dataDir.getAbsolutePath().length() + 1);
        String[] lines = builder.toString().split("\r?\n");
        String dateTime = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";
        String sep = Pattern.quote(File.separator);

        int i = 0;
        Assert.assertTrue(lines[i], lines[i].matches("d              " + dateTime + "                                    lib" + sep));
        i++;
        Assert.assertTrue(lines[i],
                          lines[i].matches("f          54  " + dateTime
                                           + "  cb7bfefbe2e5c5321c345d25b4b61c31919a5c68e2a24013ebc2c4a9e5c478a1936d8724dafc78ee49354dccbb0c3cd3dd3b477ec8369a420f38c43a7c14e55c  lib"
                                           + sep + "sha512-2.txt"));
        i++;
        Assert.assertTrue(lines[i],
                          lines[i].matches("f          46  " + dateTime
                                           + "  1f47c433a2b1d1aef1c6f6d645af845374f3e0707cf44f11edeaabb9d376ecb948c9f7bafc7ba14320f1ff1de189cb137e834444d0b4e832af68f39cfb4cfe7e  sha512-1.txt"));
        i++;
        Assert.assertTrue(lines[i], lines[i].matches("d              " + dateTime + "                                    usr" + sep));
        i++;
        // Files beneath usr should not be printed.
        Assert.assertEquals(i, lines.length);
    }
}
