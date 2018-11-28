/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.archive;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.artifact.loose.internal.LooseContainerFactoryHelper;
import com.ibm.wsspi.artifact.ArtifactContainer;

import junit.framework.Assert;
import test.common.SharedOutputManager;

/**
 * Tests for the Loose Config implementation of the artifact API
 */
public class LooseConfigTest {

    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Test null returned for invalid loose XML. Done for defect 52399.
     */
    @Test
    public void testInvalidXml() {
        // Try to parse invalid XML and make sure we get null back
        LooseContainerFactoryHelper factory = new LooseContainerFactoryHelper();
        File invalidXmlFile = new File("test/InvalidXml.xml");
        Assert.assertTrue("Test XML file does not exist", invalidXmlFile.exists());
        //passing null as the cachedir is seriously discouraged..
        //but since we don't actually plan to use the Container.. it'll do.
        ArtifactContainer container = factory.createContainer(null, invalidXmlFile);
        Assert.assertNull("Able to create a container even with invalid XML", container);

    }
}
