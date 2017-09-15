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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.Utils;

import com.ibm.wsspi.kernel.service.location.MalformedLocationException;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 *
 */
public class WsLocationAdminImplTest {
    static SharedOutputManager outputMgr;
    static String NORMALIZED_ROOT;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.trace("*=event=enabled");
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();

        NORMALIZED_ROOT = PathUtils.normalize(Utils.TEST_DATA.getAbsolutePath());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        SymbolRegistry.getRegistry().clear();
        SharedLocationManager.resetWsLocationAdmin();

        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    WsLocationAdminImpl impl;

    @Before
    public void setUp() {
        try {
            SymbolRegistry.getRegistry().clear();
            SharedLocationManager.createLocations(Utils.TEST_DATA_DIR);
            impl = WsLocationAdminImpl.getInstance();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("setUp", t);
        }
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#getInstance} .
     */
    @Test(expected = java.lang.IllegalStateException.class)
    public void testGetInstance() {
        SharedLocationManager.resetWsLocationAdmin(); // make sure instance is null
        WsLocationAdminImpl.getInstance();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#getServerName()} .
     */
    @Test
    public void testServerName() {
        // Check name of default server in impl created by setUp()
        assertEquals("Server name should be server", "defaultServer", impl.getServerName());

        // Create a new impl w/ a different server name
        SharedLocationManager.createLocations(Utils.TEST_DATA_DIR, "silly");
        impl = WsLocationAdminImpl.getInstance();
        assertEquals("Server name should be silly", "silly", impl.serverName);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#getBundleFile(java.lang.Object, java.lang.String)} .
     */
    @Test
    public void testGetBundleFile() {
        try {
            File f = impl.getBundleFile(getClass(), "testFile");

            // We have no framework, so the FrameworkUtil method will fail
            // the file should be created in the server workarea:
            WsResource res = impl.resolveResource("${server.workarea.dir}/bundle-0/testFile");

            assertEquals("Fallback to workarea should find the bundle file", res.toExternalURI(), f.toURI());

            f = impl.getBundleFile(getClass(), "testDir/");
            f.mkdirs();

            // We have no framework, so the FrameworkUtil method will fail
            // the file should be created in the server workarea:
            res = impl.resolveResource("${server.workarea.dir}/bundle-0/testDir/");

            assertEquals("Fallback to workarea should find the bundle directory", res.toExternalURI(), f.toURI());
        } finally {
            Utils.recursiveClean(new File(impl.resolveResource("${server.config.dir}").toExternalURI()));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#getServerResource(java.lang.String)} .
     * 
     * @throws IOException
     */
    @Test
    public void testGetServerFile() throws IOException {
        WsResource f = impl.resolveResource("${server.config.dir}/testFile");
        assertFalse(f.exists());

        File fixed = new File(NORMALIZED_ROOT, "servers/defaultServer/testFile");
        assertFalse(fixed.exists());

        assertEquals("URI for file created via getServerFile should match URI of file created manually", fixed.toURI(), f.toExternalURI());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#getServerWorkareaResource(java.lang.String)} .
     */
    @Test
    public void testGetServerWorkareaFile() throws Exception {
        WsResource f = impl.resolveResource("${server.workarea.dir}/testFile");
        assertFalse(f.exists());

        File fixed = new File(NORMALIZED_ROOT, "servers/defaultServer/workarea/testFile");
        assertFalse(fixed.exists());

        assertEquals("URI for file created via getServerWorkareaFile should match URI of file created manually", fixed.toURI(), f.toExternalURI());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#resolveResource(String)}
     */
    @Test
    public void testResolveResourceNull() {
        assertNull("Null resource should be returned for null URI", impl.resolveResource((String) null));
        assertNull("Null resource should be returned for null URI", impl.resolveResource((URI) null));
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)

     */
    @Test(expected = MalformedLocationException.class)
    public void testResolveResourceUnknownSymbol() {
        // Resolve resource containing a symbolic(does not exist)
        impl.resolveResource("${garbage}");
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test(expected = IllegalStateException.class)
    @Ignore("will not get an exception because we are allowing files to be loaded from outside server root")
    public void testResolveResourceArbitraryFileSystemWindows() {
        // Resolve resource containing absolute path (does not exist)
        impl.resolveResource("c:\\temp.fake\\notexist");
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test(expected = IllegalStateException.class)
    @Ignore("will not get an exception because we are allowing files to be loaded from outside server root")
    public void testResolveResourceArbitraryFileSystemUnix() {
        // Resolve resource containing absolute path (does not exist)
        impl.resolveResource("/tmp.fake/notexist");
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test
    public void testResolveResource() {
        final String m = "testResolveResource";
        try {
            WsResource r;

            // Resolve resource containing absolute path (does not exist)
            r = impl.resolveResource(NORMALIZED_ROOT + "/notexist");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertFalse("resource should not exist; " + r.toString(), r.exists());

            // Resolve resource with backslash (doesn't exist)
            r = impl.resolveResource(NORMALIZED_ROOT + "/notexist\\otherstuff");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertFalse("resource should not exist; " + r.toString(), r.exists());

            // Resolve path with relative segments
            r = impl.resolveResource("${server.config.dir}/../other.dir/");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertEquals(".. should be allowed to traverse to parent (also symbolic path)", "${wlp.user.dir}/servers/other.dir/", r.toRepositoryPath());

            r = impl.resolveResource("${server.config.dir}/./other.dir/");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertEquals(".. should be allowed to traverse to parent (also symbolic path)", "${server.config.dir}/other.dir/", r.toRepositoryPath());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test(expected = IllegalStateException.class)
    @Ignore("Currently will not get an exception because we are allowing files to be loaded from outside server root")
    public void testResolveResourceURIArbitraryFileSystemWindows() {
        // Resolve resource containing absolute path (does not exist)
        File f = new File("d:\\tmp.fake\\notexist");
        impl.resolveResource(f.toURI());
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test(expected = IllegalStateException.class)
    @Ignore("Currently will not get an exception because we are allowing files to be loaded from outside server root")
    public void testResolveResourceURIArbitraryFileSystemUnix() {
        // Resolve resource containing absolute path (does not exist)
        File f = new File("/tmp.fake/notexist");
        impl.resolveResource(f.toURI());
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test(expected = IllegalStateException.class)
    @Ignore("Currently not throwing an exception to allow for loading resources outside of the server root")
    public void testResolveStringArbitraryFileSystemWindows() {
        // Resolve resource containing absolute path (does not exist)
        impl.resolveString("d:\\tmp.fake\\notexist");
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test
    public void testResolveString() {
        final String m = "testResolveString";
        try {
            String r;

            // Resolve resource containing absolute path (does not exist)
            r = impl.resolveString(NORMALIZED_ROOT + "/notexist");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);

            // Resolve resource with backslash (doesn't exist)
            r = impl.resolveString(NORMALIZED_ROOT + "/notexist\\otherstuff");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);

            // Resolve path with relative segments
            r = impl.resolveString("${server.config.dir}/../other.dir/");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertEquals(".. should be allowed to traverse to parent (also symbolic path): " + r, NORMALIZED_ROOT + "/servers/other.dir/", r);

            r = impl.resolveString("${server.config.dir}/./other.dir/");
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertEquals(".. should be allowed to traverse to parent (also symbolic path)", NORMALIZED_ROOT + "/servers/defaultServer/other.dir/", r);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link WsLocationAdminImpl#resolveResource(String)}
     */
    @Test(expected = IllegalStateException.class)
    @Ignore("Currently not throwing an exception to allow for loading resources outside of the server root")
    public void testResolveStringArbitraryFileSystemUnix() {
        // Resolve resource containing absolute path (does not exist)
        impl.resolveString("/tmp.fake/notexist");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#resolveResource(String)}
     */
    @Test
    public void testResolveStringUnknownSymbol() throws Exception {
        impl.resolveString("$(garbage)");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#resolveResource(String)}
     */
    @Test
    public void testResolveResourceURI() throws Exception {
        String m = "testResolveResourceURI";
        WsResource r;
        File f;
        try {
            f = new File(NORMALIZED_ROOT + "/notexist");
            r = impl.resolveResource(f.toURI());
            assertNotNull("Non-null resource should be returned for non-existent resource", r);
            assertFalse("resource should not exist; " + r.toString(), r.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.location.internal.WsLocationAdminImpl#getResource(String, String)}
     * 
     * @throws IOException
     */
    @Test
    public void testMatchResource() throws IOException {
        final String m = "testMatchResource";

        try {
            File f = new File(NORMALIZED_ROOT, "lib/features/featureset_good.blst");
            assertTrue(f.exists());

            Iterator<WsResource> i = impl.matchResource("feature", "featureset_good.*", 1);
            assertNotNull("Iterator should be returned", i);
            assertTrue("Iterator should have an element", i.hasNext());
            assertTrue(i.next().isType(WsResource.Type.FILE));

            f = new File(NORMALIZED_ROOT, "lib/file name.test");
            f.createNewFile();
            f.deleteOnExit();
            assertTrue(f.exists());

            i = impl.matchResource("notFeature", ".*.test", 1);
            assertNotNull("Iterator should be returned", i);
            assertTrue("Iterator should have an element", i.hasNext());
            assertTrue(i.next().isType(WsResource.Type.FILE));

            assertFalse("Missing resource should give empty result", impl.matchResource("dontcare", "notexist", 1).hasNext());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testMatchResourceNullGroup() {
        impl.matchResource(null, null, 1);
    }

    @Test
    public void testResolveResourceVariable() throws Exception {
        System.setProperty("my.user.dir.1", NORMALIZED_ROOT);
        System.setProperty("my.user.dir.2", "${wlp.user.dir}");
        System.setProperty("my.config.dir", "configuration");
        System.setProperty("my.config.file", "${my.config.dir}/good.properties");

        final String m = "testResolveResourceVariable";
        try {
            WsResource r;

            // Resolve path with variables
            r = impl.resolveResource("${wlp.user.dir}/${my.config.file}");
            assertTrue("Resource should resolve", r.exists());

            r = impl.resolveResource(NORMALIZED_ROOT + "/${my.config.file}");
            assertTrue("Resource should resolve", r.exists());

            r = impl.resolveResource("${my.user.dir.1}/${my.config.file}");
            assertTrue("Resource should resolve", r.exists());

            r = impl.resolveResource("${my.user.dir.2}/${my.config.file}");
            assertTrue("Resource should resolve", r.exists());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testAddLocationNullFile() {
        impl.addLocation(null, null);
    }

    @Test(expected = java.lang.NullPointerException.class)
    public void testAddLocationNullSymbolicName() {
        impl.addLocation("junk", null);
    }

    @Test
    public void testAddLocation() {
        System.setProperty("my.config.dir", "configuration");
        System.setProperty("my.config.file", "${my.config.dir}/good.properties");
        WsResource r;
        r = impl.addLocation(NORMALIZED_ROOT, "testit.extension.dir");
        assertTrue("Resource should resolve", r.exists());
        r = impl.resolveResource("${testit.extension.dir}");
        assertTrue("Resource should resolve", r.exists());

        r = impl.resolveResource("${testit.extension.dir}" + "/${my.config.file}");
        assertTrue("Resource should resolve", r.exists());
    }

    @Test(expected = com.ibm.wsspi.kernel.service.location.SymbolException.class)
    public void testAddLocationAlreadyAdded() {
        WsResource r;
        r = impl.addLocation(NORMALIZED_ROOT, "testit.extension.dir");
        assertTrue("Resource should resolve", r.exists());
        // should fail if try to add again
        impl.addLocation(NORMALIZED_ROOT, "testit.extension.dir");
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void testAddLocationIsFile() {
        impl.addLocation(NORMALIZED_ROOT + "/configuration/good.properties", "testit.extension.dir");
    }

}
