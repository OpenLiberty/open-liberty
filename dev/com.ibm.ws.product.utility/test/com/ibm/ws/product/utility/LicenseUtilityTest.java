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
package com.ibm.ws.product.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

/**
 * Units tests for LicenseUtility that displays License Information and License Agreement
 */
public class LicenseUtilityTest {

    private final LicenseUtility utility = new LicenseUtility();
    String TEST_FILE_NAME = "lafiles/testfile1";
    String LICENSE_DIR = "lafiles";
    String EMPTY_LICENSE_DIR = "emptyLAFiles";
    String INVALID_TEST_FILE_NAME = "lafiles/invalidFile";

    private Mockery mockery;
    private CommandConsole console;

    /**
     * This method is run before each test to make the {@link #mockery} object with a mocked {@link #console} and {@link #context} and you can retrieve the console via a call
     * {@link ExecutionContext#getCommandConsole()}
     */
    @Before
    public void createMockery() {
        this.mockery = new Mockery();
        this.console = mockery.mock(CommandConsole.class);
    }

    @Test
    public void testShowLicenseFile() throws FileNotFoundException, URISyntaxException {
        File testFile = new File(ClassLoader.getSystemResource(TEST_FILE_NAME).toURI());
        InputStream is = new FileInputStream(testFile);

        this.mockery.checking(new Expectations() {
            {
                one(console).printlnInfoMessage("hewey");
                one(console).printlnInfoMessage("dewey");
                one(console).printlnInfoMessage("luey");
                one(console).printlnInfoMessage("");
            }
        });
        utility.showLicenseFile(is, console);
    }

    @Test
    public void testDisplayLicenseFile() throws FileNotFoundException, URISyntaxException {
        File testFile = new File(ClassLoader.getSystemResource(TEST_FILE_NAME).toURI());
        InputStream is = new FileInputStream(testFile);

        this.mockery.checking(new Expectations() {
            {
                one(console).printlnInfoMessage("hewey");
                one(console).printlnInfoMessage("dewey");
                one(console).printlnInfoMessage("luey");
                one(console).printlnInfoMessage("");
            }
        });
        utility.displayLicenseFile(is, console);

        createMockery();
        this.mockery.checking(new Expectations() {
            {
                one(console).printErrorMessage(with(any(String.class)));
            }
        });
        utility.displayLicenseFile(null, console);

    }

    @Test
    public void testGetLicenseFile() throws URISyntaxException, IOException {

        File licenseDir = new File(ClassLoader.getSystemResource(LICENSE_DIR).toURI());
        File licenseInfo = utility.getLicenseFile(licenseDir, "LI");
        assertTrue("FAIL: License info doesnt exist:", licenseInfo.exists());

        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();

        assertEquals("License locale is not en or not LI:", "LI_" + lang, licenseInfo.getName());

        File licenseAgreement = utility.getLicenseFile(licenseDir, "LA");
        assertTrue("FAIL: License agreement doesnt exist:", licenseAgreement.exists());
        assertEquals("License locale is not en or not LA:", "LA_" + lang, licenseAgreement.getName());
    }

    @Test
    @SuppressWarnings("static-access")
    public void testWordWrap() {

        String fullLine = "This is a test string for testing the license agreement and license information word wrap";
        List<String> actualLine = utility.wordWrap(fullLine, null);

        List<String> expectedLine = new ArrayList<String>();
        expectedLine.add("This is a test string for testing the license agreement and license, information word wrap");
        assertEquals("FAIL: wordWrap is not wrapping line properly:", actualLine.toString(), expectedLine.toString());

        List<String> actualLine1 = utility.wordWrap("", null);
        assertEquals("FAIL: wordWrap is not returning empty:", actualLine1.toString(), "[]");
    }
}
