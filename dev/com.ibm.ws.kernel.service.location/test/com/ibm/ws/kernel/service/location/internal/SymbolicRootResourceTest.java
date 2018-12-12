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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.utils.Utils;

import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class SymbolicRootResourceTest {
    static SharedOutputManager outputMgr;

    static File tempDirectory;

    static SymbolicRootResource commonRoot;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();

        File f = Utils.createTempFile("SymbolicRootTest", "tmp");
        f.delete();
        f.mkdir();

        tempDirectory = f;
        System.out.println("Using tmp directory: " + tempDirectory.getAbsolutePath());

        commonRoot = new SymbolicRootResource(tempDirectory.getAbsolutePath(), "common", null);
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

    @After
    public void tearDown() {
        SymbolRegistry.getRegistry().removeSymbol("A");
        SymbolRegistry.getRegistry().removeSymbol("B");
    }

    /**
     * Test method for {@link SymbolicRootResource#SymbolicRootResource(String, String, VirtualRootResource)}
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testSymbolicRootResourceNullFileName() {
        new SymbolicRootResource(null, null, null);
    }

    /**
     * Test method for {@link SymbolicRootResource#SymbolicRootResource(String, String, VirtualRootResource)}
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testSymbolicRootResourceNullSymbolicName() {
        new SymbolicRootResource("filename", null, null);
    }

    /**
     * Test method for {@link SymbolicRootResource#SymbolicRootResource(String, String, VirtualRootResource)}
     */
    @Test
    public void testSymbolicRootResource() throws Exception {
        final String m = "testSymbolicRootResource";
        try {
            File file = new File(tempDirectory, "testResourceRoot");

            SymbolicRootResource root;

            // Create a file where the resource root will be
            file.createNewFile();
            try {
                root = new SymbolicRootResource(file.getAbsolutePath(), "A", null);
                fail("Construction of resource root did not fail when root already existed as a file");
            } catch (IllegalArgumentException e) {
                // Expected
            }
            // Clean up the pre-existing file
            file.delete();

            // Creating the root should succeed now
            root = new SymbolicRootResource(file.getAbsolutePath(), "A", null);

            // On windows, normalized paths (from URI) will be preceeded by a /
            String expected = file.toURI().normalize().getPath() + "/";
            assertTrue("normalized paths should match: \n\texpected=" + expected + "\n\tcalculated=" + root.getNormalizedPath(), expected.endsWith(root.getNormalizedPath()));
            assertEquals("symbolic name should match", "${A}", root.getSymbolicName());
            assertFalse("Root should not exist (not created)", root.exists());

            assertTrue("toString contains '${A}/', toString=" + root.toString(), root.toString().contains("${A}/"));
            assertTrue("toString contains normalized path", root.toString().contains(root.getNormalizedPath()));
            
            File rootAsFile = root.asFile();
            assertNotNull("File cannot be null", rootAsFile);
            assertEquals("File name not equal", file.getAbsolutePath(), rootAsFile.getAbsolutePath());

            file.mkdir();
            assertTrue("Root should exist (created)", root.exists());
            file.delete();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link SymbolicRootResource#contains(String)}
     */
    @Test
    public void testContains() {
        final String m = "testContains";
        try {
            File dir = new File(tempDirectory, "testContains");
            File child = new File(dir, "child");
            File peer = new File(tempDirectory, "testContainsPeer");

            SymbolicRootResource root = new SymbolicRootResource(dir.getAbsolutePath(), "A", null);

            assertTrue("root should contain child", root.contains(PathUtils.normalize(child.getAbsolutePath())));
            assertFalse("root should not contain peer", root.contains(PathUtils.normalize(peer.getAbsolutePath())));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link SymbolicRootResource#createDescendantResource(String)}
     */
    @Test
    public void testCreateDescendentWithNull() {
        final String m = "testCreateDescendentWithNull";
        try {
            InternalWsResource r = commonRoot.createDescendantResource(null);
            assertEquals("Empty path should equal root", r, commonRoot);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link SymbolicRootResource#createDescendantResource(String)}
     */
    @Test
    public void testCreateDescendentWithEmpty() {
        final String m = "testCreateDescendentWithEmpty";
        try {
            InternalWsResource r = commonRoot.createDescendantResource("");
            assertEquals("Empty path should equal root", r, commonRoot);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link SymbolicRootResource#createDescendantResource(String)}
     */
    @Test(expected = com.ibm.wsspi.kernel.service.location.MalformedLocationException.class)
    public void testCreateDescendentWithAbsolute() {
        commonRoot.createDescendantResource("/absolute");
    }

    /**
     * Test method for {@link SymbolicRootResource#createDescendantResource(String)}
     */
    @Test(expected = com.ibm.wsspi.kernel.service.location.MalformedLocationException.class)
    public void testCreateDescendentWithAbsoluteWindows() {
        commonRoot.createDescendantResource("c:\\testTHAT");
    }

    /**
     * Test method for {@link SymbolicRootResource#createDescendantResource(String)}
     */
    @Test(expected = com.ibm.wsspi.kernel.service.location.MalformedLocationException.class)
    public void testCreateDescendentWithBadRelative() {
        commonRoot.createDescendantResource("../sneaky");
    }

    /**
     * Test method for {@link SymbolicRootResource#createDescendantResource(String)}
     */
    @Test
    public void testCreateDescendentResource() {
        final String m = "testCreateDescendentResource";
        try {
            File child = new File(tempDirectory, "child");

            InternalWsResource r = commonRoot.createDescendantResource("child");
            assertEquals("Symbol and child name should be included in repository path, no trailing slash", "${common}/child", r.toRepositoryPath());

            String expected = child.toURI().normalize().getPath();
            assertTrue("createDescendent should return the same child: \n\texpected=" + expected + "\n\tcalculated=" + r.getNormalizedPath(),
                       expected.endsWith(r.getNormalizedPath()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

/**
   * Test method for {@link SymbolicRootResource#hashCode() Test method for
   * 
   * @link SymbolicRootResource#equals()
   */
    @Test
    public void testEqualsHashCode() throws Exception {
        final String m = "testEqualsHashCode";

        try {
            File dir = new File(tempDirectory, "testEqualsDir1");
            File dir2 = new File(tempDirectory, "testEqualsDir2");

            SymbolicRootResource rootA1 = new SymbolicRootResource(dir.getAbsolutePath(), "A", null);
            SymbolicRootResource rootB = new SymbolicRootResource(dir.getAbsolutePath(), "B", null);

            // --- Contract for equals ---

            assertFalse("equals(null) must return false", rootA1.equals(null));
            assertFalse("File and SymbolicRoot should not be equal (different class)", rootA1.equals(dir));

            assertTrue("Reflexive: x.equals(x) is true", rootA1.equals(rootA1));

            assertFalse("Different symbolic root should be different", rootA1.equals(rootB));
            assertEquals("Symmetric (not equal): x.equals(y) must be the same as y.equals(x)", rootA1.equals(rootB), rootB.equals(rootA1));

            // --- Contract for hashCode ---

            assertTrue("Resource with different symbol should have different hashCode", rootA1.hashCode() != rootB.hashCode());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

}
