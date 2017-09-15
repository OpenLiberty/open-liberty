/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.utils.Utils;

import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

/**
 *
 */
public class VirtualRootResourceTest {
    static SharedOutputManager outputMgr;

    static File tempDirectory;
    static VirtualRootResource vRoot;
    static SymbolicRootResource aRoot;
    static SymbolicRootResource bRoot;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);

        File f = Utils.createTempFile("SymbolicRootTest", "tmp");
        f.delete();
        f.mkdir();

        tempDirectory = f;
        outputMgr.captureStreams();

        System.out.println("Using tmp directory: " + tempDirectory.getAbsolutePath());
        try {
            vRoot = new VirtualRootResource();
            aRoot = new SymbolicRootResource(tempDirectory.getAbsolutePath(), "A", vRoot);
            bRoot = new SymbolicRootResource(tempDirectory.getAbsolutePath(), "B", vRoot);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("setUp", t);
        }
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        Utils.recursiveClean(tempDirectory);
        SymbolRegistry.getRegistry().clear();
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.VirtualRootResource#getChild(java.lang.String)} .
     */
    @Test
    public void testGetChild() {
        assertNotNull("Child of virtual root ${A} should exist", vRoot.getChild("${A}"));
        assertEquals("Parent of child should be virtual root", vRoot, vRoot.getChild("${A}").getParent());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.VirtualRootResource#getChildren()} .
     */
    @Test
    public void testGetChildren() {
        Iterator<String> kids = vRoot.getChildren();
        assertNotNull("kids should be non-null", kids);
        assertTrue("kids should have two entries, this is the first", kids.hasNext());
        assertNotNull("first entry should exist", kids.next());
        assertTrue("kids should have two entries, this is the second", kids.hasNext());
        assertNotNull("second entry should exist", kids.next());
        assertFalse("kids should only have two entries", kids.hasNext());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.VirtualRootResource#getChildren(java.lang.String)} .
     */
    @Test
    public void testGetChildrenString() {
        assertNotNull("child ${A} should match pattern", vRoot.getChildren(".*A.*"));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.VirtualRootResource#toExternalURI()} .
     */
    @Test
    public void testToExternalURI() {
        assertNull("Virtual root has no external URI", vRoot.toExternalURI());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.VirtualRootResource#toRepositoryPath()} .
     */
    @Test
    public void testToRepositoryPath() {
        assertEquals("Virtual root should return its special repository path", WsLocationConstants.SYMBOL_ROOT_NODE, vRoot.toRepositoryPath());
    }
}
