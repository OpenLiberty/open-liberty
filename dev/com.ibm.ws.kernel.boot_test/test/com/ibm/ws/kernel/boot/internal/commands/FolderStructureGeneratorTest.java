/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
    public void testMD5() {
        String md5 = new ServerDumpPackager.FolderStructureGenerator().md5(new File(dataDir, "/md5-1.txt"));
        Assert.assertEquals("a62c519aaabfce3f6d02bfc983f26098", md5);
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
        Assert.assertTrue(lines[i], lines[i].matches("f          54  " + dateTime + "  c327688b13b293d7d42c60d39e59f8a5  lib" + sep + "md5-2.txt"));
        i++;
        Assert.assertTrue(lines[i], lines[i].matches("f          46  " + dateTime + "  a62c519aaabfce3f6d02bfc983f26098  md5-1.txt"));
        i++;
        Assert.assertTrue(lines[i], lines[i].matches("d              " + dateTime + "                                    usr" + sep));
        i++;
        // Files beneath usr should not be printed.
        Assert.assertEquals(i, lines.length);
    }
}
