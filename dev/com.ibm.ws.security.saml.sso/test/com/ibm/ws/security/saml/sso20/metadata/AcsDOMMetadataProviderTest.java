/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.ibm.ws.security.saml.error.SamlException;

/**
 *
 */
public class AcsDOMMetadataProviderTest {

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    static Element element = mockery.mock(Element.class);
    static File firstFile = mockery.mock(File.class, "firstFile");
    static File secondFile = mockery.mock(File.class, "secondFile");

    static AcsDOMMetadataProvider metadataProvider;

    final static String PATH_FILE = "C:/test";
    final static long FILE_LENGTH = 13900;
    final static long LAST_MODIFICATION = 12400201;

    @Before
    public void setUp() throws SamlException {
        mockery.checking(new Expectations() {
            {
                expectationsFirstFile();
            }
        });

        metadataProvider = new AcsDOMMetadataProvider(element, firstFile);
    }

    @AfterClass
    public static void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void getMetadataFileNameTest() {
        metadataProvider.getMetadataFilename();
        assertTrue("Filename is not the expected one", metadataProvider.getMetadataFilename().equals(PATH_FILE));
    }

    @Test
    public void sameIdpFileTestFalse() throws SamlException, ParserConfigurationException, SAXException, IOException {
        mockery.checking(new Expectations() {
            {

                expectationsFirstFile();
                one(secondFile).getPath();
                will(returnValue(PATH_FILE));
                one(secondFile).exists();
                will(returnValue(true));
                one(secondFile).length();
                will(returnValue(FILE_LENGTH));
                one(secondFile).lastModified();
                will(returnValue(LAST_MODIFICATION));

            }
        });

        metadataProvider = new AcsDOMMetadataProvider(element, firstFile);
        assertFalse("Is the same file", metadataProvider.sameIdpFile(secondFile));

    }

    @Test
    public void sameIdpFileTestTrue() throws SamlException, ParserConfigurationException, SAXException, IOException {
        mockery.checking(new Expectations() {
            {
                expectationsFirstFile();
                expectationsFirstFile();
            }
        });

        metadataProvider = new AcsDOMMetadataProvider(element, firstFile);
        assertTrue("Is not the same file", metadataProvider.sameIdpFile(firstFile));
    }

    private void expectationsFirstFile() {

        mockery.checking(new Expectations() {
            {
                one(firstFile).getPath();
                will(returnValue(PATH_FILE));
                one(firstFile).exists();
                will(returnValue(true));
                one(firstFile).length();
                will(returnValue(FILE_LENGTH));
                one(firstFile).lastModified();
                will(returnValue(LAST_MODIFICATION));
            }
        });
    }
}
