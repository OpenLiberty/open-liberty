/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

import test.common.junit.rules.MaximumJavaLevelRule;
import test.shared.Constants;
import test.shared.TestUtils;

/**
 *
 */
public class PackageProcessorTest {

    @ClassRule
    public static MaximumJavaLevelRule rule = new MaximumJavaLevelRule(8);

    /**
     * Mock environment.
     */
    protected static Mockery mockery;

    /**
     * Create the mockery environment. Called before each test to setup a new
     * mockery environment for each test, which helps isolate Expectation sets
     * and makes it easier to read error log output when Expectations fail.
     */
    @Before
    public void before() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    /**
     * @return A manifest file to use for testing.
     */
    private File createTestManifestFile() throws IOException {
        File maniFile = TestUtils.createTempFile("PackageProcessorTest.MANIFEST", ".mf");
        maniFile.deleteOnExit();

        Manifest mf = new Manifest();
        Attributes atts = mf.getMainAttributes();

        atts.putValue("Manifest-Version", "1.0");
        atts.putValue("Archive-Content-Type", "install");
        atts.putValue("Archive-Root", "wlp/");
        atts.putValue("Bnd-LastModified", "1389725452656");
        atts.putValue("Bundle-Copyright", "The Program materials contained in this file are IBM c" +
                                          "opyright materials. WLP Copyright International Business Machines Corp." +
                                          " 1999, 2013 All Rights Reserved * Licensed Materials - Property of IBM " +
                                          "US Government Users Restricted Rights - Use, duplication or disclosure " +
                                          "restricted by GSA ADP Schedule Contract with IBM Corp.");
        atts.putValue("Bundle-Vendor", "IBM");
        atts.putValue("Created-By", "1.6.0 (IBM Corporation)");
        atts.putValue("Extract-Installer", "true");
        atts.putValue("Import-Package", "javax.xml.parsers,org.w3c.dom,org.xml.sax");
        atts.putValue("License-Agreement", "wlp/lafiles/LA");
        atts.putValue("License-Information", "wlp/lafiles/LI");
        atts.putValue("Main-Class", "wlp.lib.extract.SelfExtract");

        mf.write(new FileOutputStream(maniFile));

        return maniFile;
    }

    /**
     *
     */
    @Test
    public void testBuildManifestForIncludeEqualsUsr() throws Exception {

        final BootstrapConfig mockBootConfig = mockery.mock(BootstrapConfig.class);

        // Methods called on BootstratpConfig from PackageProcessor.CTOR.
        mockery.checking(new Expectations() {
            {
                allowing(mockBootConfig).getInstallRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getUserRoot();
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).getConfigFile(null);
                will(returnValue(Constants.TEST_TMP_ROOT_FILE));

                allowing(mockBootConfig).get(BootstrapConstants.LOC_PROPERTY_SRVTMP_DIR);
                will(returnValue(Constants.TEST_TMP_ROOT));

                allowing(mockBootConfig).getProcessType();
                will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));
            }
        });

        // Run the code under test.
        File newManiFile = new PackageProcessor(null, null, mockBootConfig, null, null).buildManifestForIncludeEqualsUsr(createTestManifestFile());
        newManiFile.deleteOnExit();

        // Verify content of new manifest file
        Manifest mf = new Manifest();
        mf.read(new FileInputStream(newManiFile));

        Attributes atts = mf.getMainAttributes();
        assertNull(atts.getValue("License-Information"));
        assertNull(atts.getValue("License-Agreement"));
        assertEquals("com.ibm.websphere.appserver", atts.getValue("Applies-To"));
        assertEquals("false", atts.getValue("Extract-Installer"));
    }
}
