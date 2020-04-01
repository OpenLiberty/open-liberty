/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlFileInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
// import com.ibm.ws.fat.Props;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22InputFileTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22InputFile";
    // static Props testProps = Props.getInstance();

    protected static final Class<?> c = JSF22InputFileTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer2, "JSF22InputFile.war", "com.ibm.ws.jsf22.fat.input");

        jsfTestServer2.startServer(JSF22InputFileTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    /**
     * inputFile defect - This test copies a file to the ServerRoot (so we have a full path) and then
     * sets the file to be uploaded. It then attempts to upload the file, if it works, the word 'SUCCESS'
     * will be present in the subsequently loaded page.
     *
     * Note: Even though we are copying the file to 'ServerRoot' that is just so we can figure out/know where
     * it is. This lets us create the path to it so it can be selected later on.
     *
     * @throws Exception
     */
    @Test
    public void testInputFile() throws Exception {
        jsfTestServer2.copyFileToLibertyServerRoot("JSF22InputFileCONTENT.txt");

        Log.info(c, name.getMethodName(), jsfTestServer2.getServerRoot());

        File fileToUpload = new File(jsfTestServer2.getServerRoot() + File.separator + "JSF22InputFileCONTENT.txt");

        Log.info(c, name.getMethodName(), "File to Upload --  Using FILE --> " + fileToUpload.toString());
        if (fileToUpload.exists()) {
            Log.info(c, name.getMethodName(), "File to Upload -->  Found file: " + fileToUpload.toString());
        }

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "fileUploadTest.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            HtmlFileInput fileInput = (HtmlFileInput) page.getElementById("form1:file1");
            fileInput.setValueAttribute(fileToUpload.toString());
            HtmlSubmitInput uploadButton = (HtmlSubmitInput) page.getElementById("form1:uploadButton");

            HtmlPage page2 = uploadButton.click();
            Log.info(c, name.getMethodName(), page2.asText());
            assertTrue(page2.asText().contains("SUCCESS"));
        }
    }
}
