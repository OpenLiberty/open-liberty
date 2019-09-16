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
import static org.junit.Assert.assertNull;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.Utils;

import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 *
 */
public class ResourceUtilsTest {
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

        File f = Utils.createTempFile("ResourceUtilsTest", "tmp");
        f.delete();
        f.mkdir();

        tempDirectory = f;
        System.out.println("Using tmp directory: " + tempDirectory.getAbsolutePath());

        SharedLocationManager.createLocations(tempDirectory.getAbsolutePath());

        commonRoot = new SymbolicRootResource(tempDirectory.getAbsolutePath(), "A", null);
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
     * Test method for {@link ResourceUtils#isFileAChild(File, File)}
     */
    @Test
    public void testIsAChildPeerFalse() throws Exception {
        File f = Utils.createTempFile("testIsAChildPeerFalseParent", "tmp");
        File g = Utils.createTempFile("testIsAChildPeerFalseNotChild", "tmp");

        assertFalse("Peer file is not a child; f=" + f + ",g=" + g, ResourceUtils.isFileAChild(f, g));
    }

    /**
     * Test method for {@link ResourceUtils#isFileAChild(File, File)}
     */
    @Test
    public void testIsAChildSelfFalse() throws Exception {
        File f = Utils.createTempFile("testIsAChildFalseSelf", "tmp");

        assertFalse("File is not a child of itself; f=" + f, ResourceUtils.isFileAChild(f, f));
    }

    /**
     * Test method for {@link ResourceUtils#getParentResource(LocalFileResource, SymbolicRootResource)}
     */
    @Test
    public void testGetParentResource() {
        InternalWsResource r = commonRoot.createDescendantResource("a/b//c");
        assertEquals("Symbol and child name should be included in repository path, no trailing slash; r=" + r, "${A}/a/b/c", r.toRepositoryPath());

        r = ResourceUtils.getParentResource(r, commonRoot);
        assertEquals("Symbol and parent name should be included in repository path (trailing slash-- assumed directory); r=" + r, "${A}/a/b/", r.toRepositoryPath());

        r = ResourceUtils.getParentResource(r, commonRoot);
        assertEquals("Symbol and parent name should be included in repository path (trailing slash-- assumed directory); r=" + r, "${A}/a/", r.toRepositoryPath());

        r = ResourceUtils.getParentResource(r, commonRoot);
        assertEquals("Symbol for parent should match root (with trailing slash); r=" + r, "${A}/", r.toRepositoryPath());

        assertNull("Null should be returned when trying to access parent of root; r=" + r, r.getParent());
    }

    /**
     * Test method for {@link ResourceUtils#getChildResource(InternalWsResource, String)}
     */
    @Test
    public void testGetChildResourceNullPath() {
        assertNull("No child matching the name null", ResourceUtils.getChildResource(commonRoot, null));
    }

    /**
     * Test method for {@link ResourceUtils#getChildResource(InternalWsResource, String)}
     */
    @Test(expected = MalformedLocationException.class)
    public void testGetChildResourceSlash() {
        ResourceUtils.getChildResource(commonRoot, "../../");
    }

    /**
     * Test method for {@link ResourceUtils#getChildResource(InternalWsResource, String)}
     */
    @Test(expected = MalformedLocationException.class)
    public void testGetChildResourceAbsolute() {
        assertNull("No child matching the name null", ResourceUtils.getChildResource(commonRoot, "/"));
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(LocalFileResource, String)}
     */
    @Test(expected = java.lang.NullPointerException.class)
    public void testGetRelativeResourceNullBase() {
        ResourceUtils.getRelativeResource(null, null);
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(LocalFileResource, String)}
     */
    @Test
    public void testGetRelativeResourceNullPath() {
        assertNull("No relative resource when path is null", ResourceUtils.getRelativeResource(commonRoot, null));
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(LocalFileResource, String)}
     */
    @Test
    public void testGetRelativeResourceAbsoluteURI() {
        File f = new File(tempDirectory.getAbsolutePath(), "sample");
        assertNotNull("absolute resource should be resolved", ResourceUtils.getRelativeResource(commonRoot, f.toURI().toString()));
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(LocalFileResource, String)}
     */
    @Test
    public void testGetRelativeResourceAbsolutePath() {
        File f = new File(tempDirectory.getAbsolutePath(), "sample");
        assertNotNull("absolute resource should be resolved, even with null base", ResourceUtils.getRelativeResource(commonRoot, f.getAbsolutePath()));
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(LocalFileResource, String)}
     */
    @Test
    public void testGetRelativeResourceBadColons() {
        assertNotNull("resource with colons should be resolved as relative", ResourceUtils.getRelativeResource(commonRoot, "a:b"));
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(LocalFileResource, String)}.
     */
    @Test(expected = java.lang.IllegalStateException.class)
    @Ignore("Currently will not get an exception because we are allowing files to be loaded from outside server root")
    public void testGetRelativeResourceBadParent() {
        InternalWsResource r = commonRoot.createDescendantResource("e/f");
        ResourceUtils.getRelativeResource(r, "../../..");
    }

    /**
     * Test method for {@link ResourceUtils#getRelativeResource(String, InternalWsResource)}.
     */
    @Test
    public void testGetRelativeResource() {
        InternalWsResource r = commonRoot.createDescendantResource("e/f");
        WsResource g = ResourceUtils.getRelativeResource(r, "g");

        assertNotNull("relative peer should exist", g);
        assertEquals("relative resource should be peer of original", r.getParent(), g.getParent());
        assertEquals("resource repository API should have correct root", "${A}/e/g", g.toRepositoryPath());
    }
}
