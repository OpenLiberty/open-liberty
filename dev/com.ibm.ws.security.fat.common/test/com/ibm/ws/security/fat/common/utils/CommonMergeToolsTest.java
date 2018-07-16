/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.security.fat.common.utils.CommonMergeTools;

public class CommonMergeToolsTest {

    /**
     * This test will validate that the merge method will merge a server XML file with multiple
     * layers of includes (includes that whose file also has includes).
     */
    @Test
    public void successfulMerge() throws Exception {
        //work around for issue
        System.setProperty("javax.xml.transform.TransformerFactory",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        
        
        assertTrue(new CommonMergeTools().mergeFile("test-files/imports/server-01.xml", ".", "."));

        List<String> expected = new ArrayList<String>();
        /* server-01.xml */ expected.add("<!-- Original Server.xml: server-01.xml --><server description=\"server-01.xml\">");
        /* server-01.xml */ expected.add("    <featureManager>");
        /* server-01.xml */ expected.add("        <feature>feature01</feature>");
        /* server-02.xml */ expected.add("        <feature>feature02</feature>");
        /* server-03.xml */ expected.add("        <feature>feature03</feature>");
        /* server-04.xml */ expected.add("        <feature>feature04</feature>");
        /* server-01.xml */ expected.add("    </featureManager>");
        /* server-01.xml */ expected.add("    <element1 attribute1=\"value1\">element1</element1>");
        /*   generated   */ expected.add("");
        /*   generated   */ expected.add("    <!-- begin include: test-files/imports/server-02.xml -->");
        /*   generated   */ expected.add("");
        /* server-02.xml */ expected.add("    <element2 attribute2=\"value2\">element2</element2>");
        /*   generated   */ expected.add("");
        /*   generated   */ expected.add("    <!-- end include:   test-files/imports/server-02.xml -->");
        /*   generated   */ expected.add("");
        /*   generated   */ expected.add("    <!-- begin include: test-files/imports/server-03.xml -->");
        /*   generated   */ expected.add("");
        /* server-03.xml */ expected.add("    <element3 attribute3=\"value3\">element3</element3>");
        /*   generated   */ expected.add("");
        /*   generated   */ expected.add("    <!-- begin include: test-files/imports/server-04.xml -->");
        /*   generated   */ expected.add("");
        /* server-04.xml */ expected.add("    <element4 attribute4=\"value4\">element4</element4>");
        /*   generated   */ expected.add("");
        /*   generated   */ expected.add("    <!-- end include:   test-files/imports/server-04.xml -->");
        /*   generated   */ expected.add("");
        /*   generated   */ expected.add("    <!-- end include:   test-files/imports/server-03.xml -->");
        /*   generated   */ expected.add("");
        /* server-01.xml */ expected.add("    <element1 attribute1=\"value2\">element1</element1>");
        /* server-01.xml */ expected.add("</server>");

        List<String> fileLines = Files.readAllLines(new File("test-files/imports/server-01_Merged.xml").toPath(), StandardCharsets.UTF_8);

        assertEquals(expected, fileLines);
    }

    /**
     * Verify handling of a non-existent file.
     */
    @Test
    public void noSuchFile() {
        assertFalse(new CommonMergeTools().mergeFile("test-files/imports/noSuchFile.xml", ".", "."));
    }

    /**
     * Verify handling of an empty file.
     */
    @Test
    public void emptyFile() {
        assertFalse(new CommonMergeTools().mergeFile("test-files/imports/emptyFile.xml", ".", "."));
    }

    /**
     * Verify handling of a file that has a root and no children.
     */
    @Test
    public void rootWithoutChildren() {
        assertFalse(new CommonMergeTools().mergeFile("test-files/imports/rootWithoutChildren.xml", ".", "."));
    }
}
