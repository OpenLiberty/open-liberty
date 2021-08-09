/*******************************************************************************
 * Copyright (c) 2011,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.adaptable.module.structure.StructureHelper;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.BundleFileSystem;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.DirFileSystem;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.FileSystem;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.ArtifactUtils;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.FileSystemUtils;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.JarFileSystem;
import com.ibm.ws.artifact.fat_bvt.servlet.filesystem.LooseFileSystem;
import com.ibm.ws.artifact.fat_bvt.servlet.notification.NotificationTestRunner;
import com.ibm.ws.artifact.zip.cache.ZipCachingProperties;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.AddEntryToOverlay;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.InterpretedContainer;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.EnclosedEntity;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public class ArtifactAPIServlet extends HttpServlet {
    private static final long serialVersionUID = 8783096430712965126L;

    // Clean up the physical path deprecation warnings in one place.
    @SuppressWarnings("deprecation")
    private static String getPhysicalPath(EnclosedEntity ee) {
        return ee.getPhysicalPath();
    }

    // Clean up the physical path deprecation warnings in one place.
    @SuppressWarnings("deprecation")
    private static String getPhysicalPath(ArtifactEntry ae) {
        return ae.getPhysicalPath();
    }

    // Clean up the physical path deprecation warnings in one place.
    @SuppressWarnings("deprecation")
    private static String getPhysicalPath(ArtifactContainer ac) {
        return ac.getPhysicalPath();
    }

    //

    private static String dir = null;
    private static String bundleLocation = null;
    private static String badBundleLocation = null;
    private static String bundleFragmentLocation = null;
    private static String bundleTestDirLocation = null;
    private static String cacheDir = null;
    private static String cacheDirAdapt = null;
    private static String cacheDirOverlay = null;
    private static String jar_b = null;
    private static String jar_a = null;
    /** Only use this for the zipMultiTest - if it is used for any other test this will invalidate this test */
    private static String jar_multi = null;
    private static String jar_dir = null;
    private static String rar = null;
    private static String looseXml = null;
    private static String customContainerData = null;

    private static WsLocationAdmin al = null;
    private static ArtifactContainerFactory cf = null;
    private static OverlayContainerFactory ocf = null;
    private static AdaptableModuleFactory amf = null;
    private static ZipCachingService zcs = null;

    private static void initLocations(WsLocationAdmin al) {
        dir = al.resolveString("${server.config.dir}/TESTDATA");
        jar_b = al.resolveString("${server.config.dir}/TESTDATA/c/b.jar");
        jar_a = al.resolveString("${server.config.dir}/TEST.JAR");
        jar_multi = al.resolveString("${server.config.dir}/TESTMULTI.JAR");
        jar_dir = al.resolveString("${server.config.dir}TESTDIR.jar");
        rar = al.resolveString("${server.config.dir}/TEST.RAR");
        looseXml = al.resolveString("${server.config.dir}/virtualFileSystem.xml");
        customContainerData = al.resolveString("${server.config.dir}/customContainer.custom");
        bundleLocation = al.resolveString("${server.config.dir}/TESTDATA_bundle.jar");
        badBundleLocation = al.resolveString("${server.config.dir}/BADPATHTEST.jar");
        bundleFragmentLocation = al.resolveString("${server.config.dir}/TESTDATA_fragment.jar");
        bundleTestDirLocation = al.resolveString("${server.config.dir}/TestDirEntries.jar");
        cacheDir = al.resolveString("${server.config.dir}/cacheDir");
        cacheDirAdapt = al.resolveString("${server.config.dir}/cacheDirAdapt");
        cacheDirOverlay = al.resolveString("${server.config.dir}/cacheDirOverlay");

        File cacheDirFile = new File(cacheDir);
        cacheDirFile.mkdirs();

        File cacheDirAdaptFile = new File(cacheDirAdapt);
        cacheDirAdaptFile.mkdirs();

        File cacheDirOverlayFile = new File(cacheDirOverlay);
        cacheDirOverlayFile.mkdirs();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");

        writer.println("This is WOPR. Welcome Dr Falken.");

        String test = req.getQueryString();
        if ( test == null ) {
            writer.println("FAIL: No test specified.");
            writer.println("Selecting Global Thermonuclear War.");
            return;
        } else {
            writer.println("Selecting [ " + test + " ]");
        }

        BundleContext bc = FrameworkUtil.getBundle(Servlet.class).getBundleContext();

        ServiceReference<WsLocationAdmin> alsr = bc.getServiceReference(WsLocationAdmin.class);

        ServiceReference<ArtifactContainerFactory> cfsr = bc.getServiceReference(ArtifactContainerFactory.class);
        ServiceReference<ZipCachingService> zcsr = bc.getServiceReference(ZipCachingService.class);

        ServiceReference<OverlayContainerFactory> ocfsr = bc.getServiceReference(OverlayContainerFactory.class);
        ServiceReference<AdaptableModuleFactory> amfsr = bc.getServiceReference(AdaptableModuleFactory.class);

        try {
            if ( alsr == null ) {
                writer.println("FAIL: Location service reference not available");
                return;
            } else {
                al = bc.getService(alsr);
                if ( al == null ) {
                    writer.println("FAIL: Location service not available");
                }
            }

            if ( cfsr == null ) {
                writer.println("FAIL: Artifact container service reference not available");
                return;
            } else {
                cf = bc.getService(cfsr);
                if ( cf == null ) {
                    writer.println("FAIL: Artifact container service not available");
                    return;
                }
            }
            if ( zcsr == null ) {
                writer.println("FAIL: Zip caching service reference not available");
                return;
            } else {
                zcs = bc.getService(zcsr);
                if ( zcs == null ) {
                    writer.println("FAIL: Zip caching service not available");
                }
            }

            if ( ocfsr == null ) {
                writer.println("FAIL: Overlay container service reference not available");
                return;
            } else {
                ocf = bc.getService(ocfsr);
                if ( ocf == null ) {
                    writer.println("FAIL: Overlay container service not available");
                    return;
                }
            }
            if ( amfsr == null ) {
                writer.println("FAIL: Adaptable module service reference not available");
                return;
            } else {
                amf = bc.getService(amfsr);
                if ( amf == null ) {
                    writer.println("FAIL: Adaptable module service not available");
                    return;
                }
            }

            initLocations(al);

            if ( test.equals("testDir") ) {
                testDir(writer);
            } else if ( test.equals("testJar") ) {
                testJar(writer);
            } else if ( test.equals("testRar") ) {
                testRar(writer);
            } else if ( test.equals("testDirMedium") ) {
                testDirMedium(writer);
            } else if ( test.equals("testJarMedium") ) {
                testJarMedium(writer);
            } else if (test.equals("testJarNested")) {
                testJarNested(writer);
            }

            if ( test.equals("testDirNavigation") ) {
                testNavigation(getContainerForDirectory(), writer);
            } else if ( test.equals("testZipNavigation") ) {
                testNavigation(getContainerForZip(), writer);
            }

            if ( test.equals("g") ) { // This doesn't seem to be in use.
                testOverlay(writer);
            } else if ( test.equals("testDirOverlay")) {
                testDirOverlay(writer);
            }

            try {
                if ( test.equals("testAdapt") ) {
                    testAdaptableApiSimple(writer);
                } else if ( test.equals("j") ) { // This doesn't seem to be in use.
                    testAdaptableApiNavigate(writer);
                } else if ( test.equals("unableToAdapt") ) {
                    testUnableToAdapt(writer);
                } else if ( test.equals("testInterpretedAdaptable") ) {
                    testInterpretedAdaptable(writer);
                } else if ( test.equals("testInterpretedAdaptableRoots") ) {
                    testInterpretedAdapterRoots(writer);
                } else if ( test.equals("testAddEntryToOverlay") ) {
                    testAddEntryToOverlay(writer);
                }
            } catch ( UnableToAdaptException e ) {
                writer.println("FAIL: Adapt exception: " + e.getMessage());
                e.printStackTrace(writer);
            }

            if ( test.equals("testLooseRead") ) {
                writer.println("XML test has begun!");
                ArtifactContainer c = getContainerForLooseXML();
                dumpContainerRecursive(0, c, "/", writer);
                writer.println("mapping complete");

                if (testLooseApiXmlParsing(c, writer) && testUriFromXmlParsing(c, writer)) {
                    writer.println("PASS: all tests passed");
                } else {
                    writer.println("FAIL: one or more tests have failed");
                }
            }

            if ( test.equals("testFileSystem") ) {
                try {
                    testFsLayer(writer);
                } catch ( Exception e ) {
                    writer.println("FAIL: Exception during file system test: " + e.getMessage());
                    e.printStackTrace(writer);
                }
            } else if ( test.equals("listDir") ) { // Debug only; no test entry.
                try {
                    ArtifactContainer c = getContainerForDirectory();
                    dumpContainerRecursiveFs(0, c, "/", writer);
                } catch ( Exception e ) {
                    e.printStackTrace(writer);
                }
            } else if ( test.equals("listZip") ) { // Debug only; no test entry.
                try {
                    ArtifactContainer c = getContainerForZip();
                    dumpContainerRecursiveFs(0, c, "/", writer);
                } catch ( Exception e ) {
                    e.printStackTrace(writer);
                }
            } else if ( test.equals("listLoose") ) { // Debug only; no test entry.
                try {
                    ArtifactContainer c = getContainerForLooseXML();
                    dumpContainerRecursiveFs(0, c, "/", writer);
                } catch ( Exception e ) {
                    e.printStackTrace(writer);
                }
            } else if ( test.equals("listBundle") ) { // Debug only; no test entry.
                try {
                    ArtifactContainer c = getContainerForBundle(writer);
                    dumpContainerRecursiveFs(0, c, "/", writer);
                } catch ( Exception e ) {
                    e.printStackTrace(writer);
                }
            }

            if ( test.equals("testNotify") ) {
                NotificationTestRunner nt = new NotificationTestRunner();
                nt.runNotificationTests(new File(dir), cf, ocf, writer);
            }

            if ( test.equals("testUri") ) {
                if ( testUri(writer) ) {
                    writer.println("PASS: URI test");
                } else {
                    writer.println("FAIL: URI test");
                }
            }

            if ( test.equals("testPhysicalPath") ) {
                if ( testPhysicalPath(writer) ) {
                    writer.println("PASS: Physical path test");
                } else {
                    writer.println("FAIL: Physical path test");
                }
            }

            if ( test.equals("testLooseCaseSensitivity") ) {
                ArtifactContainer c = getContainerForLooseXML();
                if ( testCaseSensitivity_loose(c, writer) ) {
                    writer.println("PASS: Loose case sensitivity test");
                } else {
                    writer.println("Fail: Loose case sensitivity test");
                }
            } else if ( test.equals("testFileCaseSensitivity") ) {
                ArtifactContainer ArtifactContainer = getContainerForDirectory();
                if ( testCaseSesnitivityForDirs(ArtifactContainer, writer, "") ) {
                    writer.println("PASS: File case sensitivity test");
                } else {
                    writer.println("FAIL: File case sensitivity test");
                }
            }

            // if (test.equals("testCaseSensitivity_overlay")) {
            //     ArtifactContainer ArtifactContainer = getContainerForDirectory();
            //     OverlayContainer overlayContainer = ocf.createOverlay(OverlayContainer.class, ArtifactContainer);
            //     if (testCaseSesnitivityForDirs(overlayContainer, writer, "")) {
            //         writer.println("PASS: all tests for testCaseSensitivity_file passed");
            //     } else {
            //         writer.println("FAIL: one or more tests for testCaseSensitivity_file failed");
            //     }
            // }

            if ( test.equals("testGetResourceForLooseEntry") ) {
                ArtifactContainer c = getContainerForLooseXML();
                testGetResourceForLooseEntry(c, writer);
            } else if ( test.equals("testGetResourceForZipAndFile") ) {
                testGetResourceForZipAndFile(cf, dir, writer);
            } else if ( test.equals("testDotDotPath") ) {
                testDotDotPath(cf, dir, new File(looseXml), writer);
            } else if ( test.equals("testImpliedZipDir") ) {
                testImpliedZipDir(writer);
            } else if (test.equals("testSimpleBundleArtifactApi") ) { 
                simpleBundleArtifactApiTest(writer, cf);
            } else if ( test.equals("testGetEnclosingContainerOnBundle") ) {
                testGetEnclosingContainerOnBundle(writer);
            } else if ( test.equals("testBadBundlePathIteration") ) {
                testBadBundlePathIteration(writer);
            } else if ( test.equals("testZipCachingService") ) {
                testZipCachingService(writer);
            } else if ( test.equals("testZipMulti") ) {
                testMultithreadedZipArtifactionInitialisation(writer);
            } else if ( test.equals("testCustomContainer") ) {
                testCustomContainer(writer);
            }

        } finally {
            if ( cfsr != null ) {
                bc.ungetService(cfsr);
            }
            if ( alsr != null ) {
                bc.ungetService(alsr);
            }
            if ( ocfsr != null ) {
                bc.ungetService(ocfsr);
            }
            if ( zcsr != null ) {
                bc.ungetService(zcsr);
            }
        }
    }

    @SuppressWarnings("unused")
    private void dumpRecursive(int depth, FileSystem f, PrintWriter out) {
        String pad = "";
        for (int i = 0; i < depth; i++) {
            pad += " ";
        }
        out.println(pad + " " + f.getType() + " " + f.getName() + " " + f.getPath());
        if (f.getChildren() != null) {
            for (FileSystem child : f.getChildren()) {
                dumpRecursive(depth + 1, child, out);
            }
        }
    }

    private static final String ZFH_OPEN_COUNT_FIELD_NAME = "openCount";
    private static final int ERROR_OPEN_COUNT = -1;

    private int getOpenCount(ZipFileHandle zfh) {
        try {
            Field f = zfh.getClass().getDeclaredField(ZFH_OPEN_COUNT_FIELD_NAME);
            f.setAccessible(true);
            return f.getInt(zfh);
        } catch ( Exception e ) {
            return ERROR_OPEN_COUNT;
        }
    }

    private byte[] getBuffer(InputStream is) {
        if ( !(is instanceof ByteArrayInputStream) ) {
            return null;
        }

        ByteArrayInputStream bais = (ByteArrayInputStream) is;

        try {
            Field f = bais.getClass().getDeclaredField("buf");
            f.setAccessible(true);
            return (byte[]) f.get(bais);

        } catch ( Exception e ) {
            return null;
        }
    }

    private void testCustomContainer(PrintWriter writer) {
        ArtifactContainer custom = getContainerForCustom();
        if (custom != null) {
            ArtifactEntry ae = custom.getEntry("/TESTENTRY");
            if (ae == null) {
                writer.println("FAIL: testCustomContainer did not retrieve entry");
            } else {
                writer.println("PASS: customContainer worked as expected");
            }
        } else {
            writer.println("FAIL: testCustomContainer was unable to open *.custom as container");
        }
    }

    /**
     * A test to check for concurrency issues when we create the cache - defect 100419
     * Uses its own jar (has to be it's own jar so it's uninitialised) this jar contains another jar
     * of non trivial size (7 M) that takes a while to deal with.
     * 
     * @param writer
     */
    private void testMultithreadedZipArtifactionInitialisation(PrintWriter writer) {
        ArtifactContainer preTest = getContainerForMulti();

        int numthreads = 50; // 500 is known to be too much for some systems
        boolean sandboy = true;

        for (int j = 1; j < 50 && sandboy; j++) {
            ArtifactEntry ae = preTest.getEntry("a/TestbigishJar" + j + ".jar");
            ArtifactContainer test = ae.convertToContainer();

            CountDownLatch cdl = new CountDownLatch(numthreads);
            //spawn threads all of them invoke get entry on test.
            ThreadedArtifactTester threads[] = new ThreadedArtifactTester[numthreads + 1];
            for (int i = 1; i <= numthreads; i++) {
                threads[i] = new ThreadedArtifactTester(test, "Thread" + i, cdl);
            }
            for (int i = 1; i <= numthreads; i++) {
                threads[i].start();
            }

            try {
                if (cdl.await(300, TimeUnit.SECONDS)) {
                    writer.println("jar" + j + ": All threads returned (may or may not have been happy but they returned)");
                } else {
                    writer.println("FAIL:" + cdl.getCount() + " of our threads unravelled and didn't decrement the coundown");
                    sandboy = false;
                }
            } catch (InterruptedException e) {
                writer.println("FAIL: testZipInitMulti was interupted while awaiting");
            }
            for (int i = 1; i <= numthreads; i++) {
                if (threads[i].getResults().startsWith("FAIL")) {
                    writer.print(j + "," + i + ":\n" + threads[i].getResults());
                    sandboy = false;
                } else {
                    //writer.print(j + "," + i + ":" + threads[i].getResults());
                }
            }
        }
        if (sandboy) {
            writer.println("\nPASS:");
        }
    }

    private void testZipCachingService(PrintWriter writer) {
        String methodName = "testZipCachingService";

        String generalPath = bundleFragmentLocation;

        writer.println(methodName);

        writer.println("ZipCache Service Parameters:");
        
        int handleCacheSize = ZipCachingProperties.ZIP_CACHE_HANDLE_MAX;
        writer.println("  Max Zip Handles [ " + Integer.valueOf(handleCacheSize) + " ]");

        int entryCacheSize = ZipCachingProperties.ZIP_CACHE_ENTRY_MAX;
        writer.println("  Max Entry Cache Entry Size Limit [ " + Integer.valueOf(ZipCachingProperties.ZIP_CACHE_ENTRY_LIMIT) + " ]");
        writer.println("  Max Entry Cache Size [ " + Integer.valueOf(entryCacheSize) + " ]");

        writer.println("  Max Pending Zip Closes [ " + Integer.valueOf(ZipCachingProperties.ZIP_CACHE_REAPER_MAX_PENDING) + " ]");

        writer.println("  Zip Close Min Quick Pend [ " + Long.valueOf(ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MIN) + " ]");
        writer.println("  Zip Close Max Quick Pend [ " + Long.valueOf(ZipCachingProperties.ZIP_CACHE_REAPER_QUICK_PEND_MAX) + " ]");        
        writer.println("  Zip Close Min Slow Pend [ " + Long.valueOf(ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MIN) + " ]");
        writer.println("  Zip Close Max Slow  Pend [ " + Long.valueOf(ZipCachingProperties.ZIP_CACHE_REAPER_SLOW_PEND_MAX) + " ]");                

        boolean pass = true;

        // 'bundleFragmentLocation' is safe to use; no other code
        // should be accessing the fragment using the zip handle service.

        try {
            File fragmentFile = new File(bundleFragmentLocation);
            String canonicalPath = fragmentFile.getCanonicalPath(); // throws IOException

            writer.println("Test archive path [ " + generalPath + " ]");            
            writer.println("Test archive canonical path [ " + canonicalPath + " ]");

            ZipFileHandle handle = zcs.openZipFile(canonicalPath);
            ZipFileHandle duplicateHandle = zcs.openZipFile(canonicalPath);
            if ( duplicateHandle != handle ) {
                pass = false;
                writer.println("FAIL: Non-unique handle for [ " + canonicalPath + " ]");
            }

            int initialOpenCount = getOpenCount(handle);
            if ( initialOpenCount != 0 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " initial open count is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ " + Integer.valueOf(0) + " ]");
            }

            ArtifactContainer zipContainer =
                cf.getContainer( new File(cacheDir), new File(bundleFragmentLocation) );

            int afterOpenCount = getOpenCount(handle);
            if ( afterOpenCount != 0 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " open count after obtaining container is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ " + Integer.valueOf(0) + " ]");
            }

            zipContainer.useFastMode();

            int startFastModeOpenCount = getOpenCount(handle);
            if ( startFastModeOpenCount != 1 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " open count after enabling fast mode is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ 1 ]");
            }

            zipContainer.stopUsingFastMode();

            int stopFastModeOpenCount = getOpenCount(handle);

            if ( stopFastModeOpenCount != 0 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " open count after disabling fast mode is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ " + Integer.valueOf(0) + " ]");
            }

            zipContainer.stopUsingFastMode();
            int stopAgainFastModeOpenCount = getOpenCount(handle);
            if ( stopAgainFastModeOpenCount != 0 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " open count after twice disabling fast mode is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ " + Integer.valueOf(0) + " ]");
            }

            ArtifactEntry entry = zipContainer.getEntry("/META-INF/MANIFEST.MF");
            InputStream entryStream = entry.getInputStream(); // throws IOException

            int streamOpenOpenCount = getOpenCount(handle);
            if ( streamOpenOpenCount != 1 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " open count after stream open is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ " + Integer.valueOf(1) + " ]");
            }

            entryStream.close(); // throws IOException

            int streamCloseOpenCount = getOpenCount(handle);
            if ( streamCloseOpenCount != 0 ) {
                pass = false;
                writer.println("FAIL: Handle [ " + canonicalPath + " ]" +
                               " open count after stream close is [ " + Integer.valueOf(initialOpenCount) + " ]" +
                               " but should be [ " + Integer.valueOf(0) + " ]");
            }

            if ( entryCacheSize > 0 ) {
                // The handle must be open for calls to 'getInputStream'.
                ZipFile zipFile1 = handle.open();
                ZipEntry ze = zipFile1.getEntry("META-INF/MANIFEST.MF");

                InputStream zis1 = handle.getInputStream(zipFile1, ze);
                byte zisBytes1[] = getBuffer(zis1);
                zis1.close();

                // When the entry buffer is enabled, the byte arrays for
                // small entries are cached, and the stream which is
                // retrieved is constructed directly as a ByteArrayInputStream
                // on the cached byte array.  ByteArrayInputStream stores
                // the byte array directly.

                InputStream zis2 = handle.getInputStream(zipFile1, ze);
                byte zisBytes2[] = getBuffer(zis2);
                zis2.close();

                handle.close();

                // A null byte array means the entry was not a candidate
                // for caching.  That is, if it had more than the cache
                // size limit bytes, or was not a .class or META-INF/MANIFEST.MF.
                //
                // If the entry is cacheable, it is returned as a ByteArrayInputStream,
                // even on a cache put.

                if ( (zisBytes1 == null) || (zisBytes2 == null) ) {
                    pass = false;
                    writer.println("FAIL: Handle [ " + canonicalPath + " ] entry [ " + ze.getName() + " ]" +
                                   " did not obtain byte array input streams");
                } else if ( zisBytes1 != zisBytes2 ) {
                    pass = false;
                    writer.println("FAIL: Handle [ " + canonicalPath + " ] entry [ " + ze.getName() + " ]" +
                                   " did not obtain identical byte arrays");
                }

            } else {
                writer.println("Skipping entry tests: Entry caching is disabled");
            }

            for ( int handleNo = 0; handleNo < handleCacheSize + 1; handleNo++ ) {
                String nextPath = canonicalPath + "." + handleNo;
                ZipFileHandle zipHandle = zcs.openZipFile(nextPath);
                if ( zipHandle == null ) {
                    pass = false;
                    writer.println("FAIL: Failed to obtain handle [ " + nextPath + " ]");
                }
            }

            ZipFileHandle nonDuplicateHandle = zcs.openZipFile(canonicalPath);
            if ( nonDuplicateHandle == handle ) {
                pass = false;
                writer.println("FAIL: Unique handle after overflow for [ " + canonicalPath + " ]");
            }

        } catch ( IOException e ) {
            pass = false;
            writer.println("FAIL: Exception: " + e.getMessage());
            e.printStackTrace(writer);
        }

        if ( pass ) {
            writer.println("PASS: " + methodName);
        }
    }

    private boolean testFsLayerViaOverlay(PrintWriter out, ArtifactContainer container, String type, FileSystem testdata) {
        if (container == null) {
            out.println("FAIL: Unable to perform overlay test, as container came back null for " + type);
            return false;
        }
        OverlayContainer oc = ocf.createOverlay(OverlayContainer.class, container);
        if (oc == null) {
            out.println("FAIL: Unable to obtain overlay ArtifactContainer via factory");
            return false;
        }

        boolean result = false;
        File temp = new File("OVERLAYTEST" + System.currentTimeMillis());
        try {
            if (!temp.mkdirs()) {
                out.println("FAIL: Unable to create directory for overlay test for " + type + " , not an overlay issue");
                return false;
            }
            out.println("Using " + temp.getAbsolutePath() + " as the overlay test dir");
            oc.setOverlayDirectory(new File(cacheDirOverlay), temp);

            ArtifactUtils.counter = 0;
            long start = System.currentTimeMillis();
            result = ArtifactUtils.compare(oc, testdata, out);
            if (result) {
                out.println("PASS: woohoo.. completed " + ArtifactUtils.counter + " tests on overlay for " + type + " in " + (System.currentTimeMillis() - start) + "ms");
            }
        } finally {
            out.println("Cleaning up " + temp.getAbsolutePath() + " for " + type);
            killDirectory(temp, out);
        }

        return result;
    }

    /**
     * Recursively validate a container and its children.
     * 
     * Recurse into entries which adapt as containers.
     *
     * Validate the full path of each container and of each entry.
     * 
     * Validate the result of adapting each container to string and of
     * adapting each entry to string.
     *
     * @param container The container to validate.
     * @param writer A writer on which to print diagnostic messages.
     * 
     * @return True or false telling if the container and its contents are valid.
     */
    private boolean recursiveCheck(Container container, PrintWriter writer) {
        boolean pass = true;

        try {
            String containerID = ContainerUtils.getContainerID(container);
            String containerID_noGetRoot = ContainerUtils.getContainerIDNoGetRoot(container);
            writer.println("Container ID [ " + containerID + " ]");
            writer.println("Container ID (no getRoot) [ " + containerID_noGetRoot + " ]");
            if ( !containerID.equals(containerID_noGetRoot) ) {
                writer.println("Error: Container ID [ " + containerID + " ]" +
                               " does not match container ID (no getRoot) [ " + containerID_noGetRoot + " ]");
                pass = false;
            }

            String containerString = container.adapt(String.class);
            writer.println("Container String [ " + containerString + " ]");
            if ( containerString == null ) {
                writer.println("Error: Null container [ " + containerID + " ] String");
                pass = false;
            } else if ( containerString.indexOf("FAIL") != -1 ) {
                writer.println("Error: FAIL detected in container [ " + containerID + " ] String [ " + containerString + " ]");
                pass = false;
            }

            for ( Entry entry : container ) {
                String entryID = ContainerUtils.getEntryID(entry);
                String entryID_noGetRoot = ContainerUtils.getEntryIDNoGetRoot(entry);

                writer.println("Entry ID [ " + entryID + " ]");
                writer.println("Entry ID (no getRoot) [ " + entryID_noGetRoot + " ]");

                if ( !entryID.equals(entryID_noGetRoot) ) {
                    writer.println("Error: Entry ID [ " + entryID + " ]" +
                                   " does not match entry ID (no getRoot) [ " + entryID_noGetRoot + " ]");
                    pass = false;
                }

                String entryString = entry.adapt(String.class);
                writer.println("Entry String [ " + entryString + " ]");
                if ( entryString == null ) {
                    writer.println("Error: Null entry [ " + entryID + " ] String");
                    pass = false;
                } else if ( entryString.indexOf("FAIL") != -1 ) {
                    writer.println("Error: FAIL detected in entry [ " + entryID + " ] String [ " + entryString + " ]");
                    pass = false;
                }

                Container entryAsContainer = entry.adapt(Container.class);
                if ( entryAsContainer != null ) {
                    if ( !recursiveCheck(entryAsContainer, writer) ) {
                        pass = false;
                    }
                }
            }

            return pass;

        } catch ( UnableToAdaptException e ) {
            writer.println("FAIL: Adapt Exception: " + e.getMessage());
            e.printStackTrace(writer);

            return false;
        }
    }

    private void testInterpretedAdapterRoots(PrintWriter writer) throws UnableToAdaptException {
        ArtifactContainer c = getContainerForDirectory();

        Container adaptContainer = amf.getContainer(new File(cacheDirAdapt), new File(cacheDirOverlay), c);

        if (adaptContainer != null) {
            writer.println("Got ArtifactContainer " + adaptContainer);
            InterpretedContainer interpreted = adaptContainer.adapt(InterpretedContainer.class);

            StructureHelper sh = new StructureHelper() {
                @Override
                public boolean isRoot(ArtifactContainer e) {
                    if ("aa".equals(e.getName()))
                        return true;
                    if ("ab".equals(e.getName()))
                        return true;
                    if ("ba".equals(e.getName())) {
                        return true;
                    }
                    if ("baa".equals(e.getName())) {
                        return true;
                    }
                    return e.isRoot();
                }

                @Override
                public boolean isValid(ArtifactContainer e, String path) {
                    //correct absolute paths down to paths beneath 'e'
                    if (path.startsWith("/")) {
                        path = path.substring(e.getPath().length());
                    }
                    //does the path contain containers?
                    if (path.indexOf("/") != -1) {
                        String pathComponents[] = path.split("/");
                        //iterate through all path components except the last
                        //there will be at least two, because the path had a / in it.
                        for (int i = 0; i < (pathComponents.length - 1); i++) {
                            if ("aa".equals(pathComponents[i]))
                                return false;
                            if ("ab".equals(pathComponents[i]))
                                return false;
                            if ("ba".equals(pathComponents[i])) {
                                return false;
                            }
                            if ("baa".equals(pathComponents[i])) {
                                return false;
                            }
                        }
                        //we didnt match a container that we're converting into a root, 
                        //so the path is valid.
                        return true;
                    } else {
                        //no containers, nothing to hide.. 
                        return true;
                    }
                }
            };
            interpreted.setStructureHelper(sh);

            //test method will dive through every container/entry 
            //and adapt it to string, driving the verifier adapters.
            boolean pass = recursiveCheck(interpreted, writer);

            if (pass)
                writer.println("PASS: all done with checks");
        }

    }

    // Helpers for the overlay test ...

    private class ContainerAction {
        public final Container container;
        public final String path;
        public final boolean expected;

        public ContainerAction(Container container, String path, boolean expected) {
            if ( container == null ) {
                throw new IllegalArgumentException("Container cannot be null.");
            } else if ( path == null ) {
                throw new IllegalArgumentException("Path cannot be null.");
            }

            this.container = container;
            this.path = path;

            this.expected = expected;
        }

        @Override
        public String toString() {
            return "ContainerAction(container " + container.getPath() + ", path " + path + ", expected " + Boolean.valueOf(expected) +")";
        }
    }

    private String standardAddData() {
        Date date = new Date();
        return date.getTime() + ", " + date.toString();
    }

    private boolean addAndGetEntry(ContainerAction addAction, ContainerAction getAction, PrintWriter writer)
        throws IOException, UnableToAdaptException {

        String addContent;
        if ( addAction != null ) {
            writer.println("Add [ " + addAction + " ]");
            addContent = standardAddData();
            writer.println("Add content [" + addContent + "]");
            AddEntryToOverlay adder = addAction.container.adapt(AddEntryToOverlay.class);
            boolean actual = adder.add(addAction.path, addContent);
            if ( actual != addAction.expected ) {
                writer.println("FAIL: " + (addAction.expected ? "Failed to but expected to add" : "Added but expected failed to add"));
                return false;
            } else {
                writer.println("Add result [ " + (addAction.expected ? "Did add" : "Did not add") + " ]");
            }
        } else {
            writer.println("No add action");
            addContent = null;
        }

        Entry getEntry;
        if ( getAction != null ) {
            writer.println("Get [ " + getAction + " ]");
            getEntry = getAction.container.getEntry(getAction.path);
            boolean actual = (getEntry != null);
            if ( actual != getAction.expected ) {
                writer.println("FAIL: " + (getAction.expected ? "Failed to get but expected get" : "Got but expected failed to get"));
                return false;
            } else {
                writer.println("Get result [ " + (getAction.expected ? "Did get" : "Did not get") + " ]");
            }
        } else {
            writer.println("No get action");
            getEntry = null;
        }

        String getContent;
        if ( (addContent != null) && (getEntry != null) ) {
            getContent = readFirstLine(getEntry);
            writer.println("Get content [" + getContent + "]");
            if ( !addContent.equals(getContent) ) {
                writer.println("FAIL: Add content [ " + addContent + " ] does not match get content [ " + getContent + " ]");
                return false;
            }
        } else {
            writer.println("No content comparison");
            getContent = null;
        }

        // Do NOT add "PASS": This is one step among many.  Other steps may still fail.
        return true;
    }

    private String readFirstLine(Entry entry) throws UnableToAdaptException, IOException {
        InputStream inputStream = entry.adapt(InputStream.class); // throws UnableToAdaptException

        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            return bufferedReader.readLine(); // throws IOException

        } finally {
            inputStream.close(); // throws IOException
        }
    }

    // this structure helper is specifically for use with the ${server.config.dir}/TESTDATA dir.

    private static class AddStructureHelper implements StructureHelper {

        private boolean isContainedInJar(ArtifactContainer c) {
            // there are a couple of jars under the TESTDATA file hierarchy, so there is a root 
            // container at the top of the hierarchy and at each jar, but the jar containers are
            // in turn under another container as they are within the file hierarchy whereas the
            // filesystem root container is not.
            return c.getRoot().getEnclosingContainer() != null;
        }

        @Override
        public boolean isRoot(ArtifactContainer c) {
            // if it's the "ba" directory on the filesystem (and not one inside a jar in the
            // filesystem), treat as root.
            if (c.getName().equals("ba") && !isContainedInJar(c))
                return true;
            // otherwise go with the default behaviour.
            else
                return c.isRoot();
        }

        @Override
        public boolean isValid(ArtifactContainer c, String path) {
            // hide paths that include but don't end in "ba" that are not inside a jar, i.e. are
            // paths traversing under "ba" on the filesystem (note also that paths that are 
            // returned by getPath() do not have trailing /'s, so we never expect to see a path 
            // of the form .../ba/ referring to the ba directory itself).
            if (path.contains("/ba/") && !isContainedInJar(c))
                return false;
            else
                return true;
        }
    }

    private void populateActions(
        List<ContainerAction> addActions, List<ContainerAction> getActions,
        Container rootPlainContainer, InterpretedContainer rootInterpretedContainer) 
        throws UnableToAdaptException {

        // now use adaptable entries and containers to test add entry adapter, users of the 
        // adapter will typically deal with interpreted containers, but may as well test plain
        // non-interpreted containers too; first build up the list of required tests. 
        ContainerAction cpr;

        // check the top-level root container doesn't allow absolute paths.
        addActions.add(new ContainerAction(rootPlainContainer, "/plain-abspath-1.txt", false));
        getActions.add(null);

        addActions.add(new ContainerAction(rootInterpretedContainer, "/interpreted-abspath-1.txt", false));
        getActions.add(null);

        // check a non-root container under the top-level parent root container doesn't allow 
        // absolute paths.
        addActions.add(new ContainerAction(rootPlainContainer.getEntry("a/ab").adapt(Container.class), "/plain-abspath-2.txt", false));
        getActions.add(null);

        addActions.add(new ContainerAction(rootInterpretedContainer.getEntry("a/ab").adapt(Container.class), "/interpreted-abspath-2.txt", false));
        getActions.add(null);

        // check the top-level root container.
        cpr = new ContainerAction(rootPlainContainer, "plain-1.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/plain-1.txt", true));

        cpr = new ContainerAction(rootInterpretedContainer, "interpreted-1.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/interpreted-1.txt", true));

        // check a non-root container under the top-level parent root container.
        cpr = new ContainerAction(rootPlainContainer.getEntry("a/ab").adapt(Container.class), "plain-2.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        cpr = new ContainerAction(rootInterpretedContainer.getEntry("a/ab").adapt(Container.class), "interpreted-2.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        // check a non-root container under the top-level parent root container, retrieving the 
        // added entry from the container above. 
        addActions.add(new ContainerAction(rootPlainContainer.getEntry("a/ab/aba").adapt(Container.class), "plain-3.txt", true));
        getActions.add(new ContainerAction(rootPlainContainer.getEntry("a/ab").adapt(Container.class), "aba/plain-3.txt", true));

        addActions.add(new ContainerAction(rootInterpretedContainer.getEntry("a/ab/aba").adapt(Container.class), "interpreted-3.txt", true));
        getActions.add(new ContainerAction(rootInterpretedContainer.getEntry("a/ab").adapt(Container.class), "aba/interpreted-3.txt", true));

        // check a root container under the top-level parent root container, enforced by the 
        // structure helper.
        cpr = new ContainerAction(rootPlainContainer.getEntry("b/ba").adapt(Container.class), "plain-4.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        cpr = new ContainerAction(rootInterpretedContainer.getEntry("b/ba").adapt(Container.class), "interpreted-4.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        // check a root container under the top-level parent root container, due to being an 
        // archive.
        cpr = new ContainerAction(rootPlainContainer.getEntry("c/a.jar").adapt(Container.class), "plain-5.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        cpr = new ContainerAction(rootInterpretedContainer.getEntry("c/a.jar").adapt(Container.class), "interpreted-5.txt", true);
        addActions.add(cpr);
        getActions.add(cpr);

        // add to a root container under the top-level parent root container, enforced by the 
        // structure helper, and ensure an entry of the same name cannot be retrieved from the 
        // top-level root container.
        addActions.add(new ContainerAction(rootPlainContainer.getEntry("b/ba").adapt(Container.class), "plain-6.txt", true));
        getActions.add(new ContainerAction(rootPlainContainer, "plain-6.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/plain-6.txt", false));

        addActions.add(new ContainerAction(rootInterpretedContainer.getEntry("b/ba").adapt(Container.class), "interpreted-6.txt", true));
        getActions.add(new ContainerAction(rootInterpretedContainer, "interpreted-6.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/interpreted-6.txt", false));

        // add to a root container under the top-level parent root container, due to being an 
        // archive, and ensure an entry of the same name cannot be retrieved from the top-level 
        // root container.
        addActions.add(new ContainerAction(rootPlainContainer.getEntry("c/a.jar").adapt(Container.class), "plain-7.txt", true));
        getActions.add(new ContainerAction(rootPlainContainer, "plain-7.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/plain-7.txt", false));

        addActions.add(new ContainerAction(rootInterpretedContainer.getEntry("c/a.jar").adapt(Container.class), "interpreted-7.txt", true));
        getActions.add(new ContainerAction(rootInterpretedContainer, "interpreted-7.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/interpreted-7.txt", false));

        // add to the plain container (non-interpreted) that corresponds to a root container 
        // under the top-level parent root container, that was made root by the structure 
        // helper, and ensure the entry can be retrieved or not as expected from both the plain 
        // and interpreted views.
        cpr = new ContainerAction(rootPlainContainer.getEntry("b/ba").adapt(Container.class), "plain-8.txt", true);
        addActions.add(cpr); // checks content as well as entry existence, when addAt is null only entry existence is checked
        getActions.add(cpr);

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "b/ba/plain-8.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/b/ba/plain-8.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "b/ba/plain-8.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/b/ba/plain-8.txt", false));

        addActions.add(null); // should be identical to first check, but will try again anyway
        getActions.add(new ContainerAction(rootPlainContainer.getEntry("b/ba").adapt(Container.class), "plain-8.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer.getEntry("b/ba").adapt(Container.class), "plain-8.txt", true));

        // add to the interpreted container (not plain) that corresponds to a root container 
        // under the top-level parent root container, that was made root by the structure 
        // helper, and ensure the entry can be retrieved or not as expected from both the plain 
        // and interpreted views.
        cpr = new ContainerAction(rootInterpretedContainer.getEntry("b/ba").adapt(Container.class), "interpreted-8.txt", true);
        addActions.add(cpr); // checks content as well as entry existence, when addAt is null only entry existence is checked
        getActions.add(cpr);

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "b/ba/interpreted-8.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/b/ba/interpreted-8.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "b/ba/interpreted-8.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/b/ba/interpreted-8.txt", true));

        addActions.add(null); // should be identical to first check, but will try again anyway
        getActions.add(new ContainerAction(rootInterpretedContainer.getEntry("b/ba").adapt(Container.class), "interpreted-8.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer.getEntry("b/ba").adapt(Container.class), "interpreted-8.txt", true));

        // add to the plain container (non-interpreted) that corresponds to a root container 
        // under the top-level parent root container, that was made root due to being an 
        // archive, and ensure the entry can be retrieved or not as expected from both the plain
        // and interpreted views.
        cpr = new ContainerAction(rootPlainContainer.getEntry("c/a.jar").adapt(Container.class), "plain-9.txt", true);
        addActions.add(cpr); // checks content as well as entry existence, when addAt is null only entry existence is checked
        getActions.add(cpr);

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "c/a.jar/plain-9.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/c/a.jar/plain-9.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "c/a.jar/plain-9.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/c/a.jar/plain-9.txt", false));

        addActions.add(null); // should be identical to first check, but will try again anyway
        getActions.add(new ContainerAction(rootPlainContainer.getEntry("c/a.jar").adapt(Container.class), "plain-9.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer.getEntry("c/a.jar").adapt(Container.class), "plain-9.txt", true));

        // add to the plain container (non-interpreted) that corresponds to a root container 
        // under the top-level parent root container, that was made root due to being an 
        // archive, and ensure the entry can be retrieved or not as expected from both the plain
        // and interpreted views.
        cpr = new ContainerAction(rootInterpretedContainer.getEntry("c/a.jar").adapt(Container.class), "interpreted-9.txt", true);
        addActions.add(cpr); // checks content as well as entry existence, when addAt is null only entry existence is checked
        getActions.add(cpr);

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "c/a.jar/interpreted-9.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootInterpretedContainer, "/c/a.jar/interpreted-9.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "c/a.jar/interpreted-9.txt", false));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer, "/c/a.jar/interpreted-9.txt", false));

        addActions.add(null); // should be identical to first check, but will try again anyway
        getActions.add(new ContainerAction(rootInterpretedContainer.getEntry("c/a.jar").adapt(Container.class), "interpreted-9.txt", true));

        addActions.add(null);
        getActions.add(new ContainerAction(rootPlainContainer.getEntry("c/a.jar").adapt(Container.class), "interpreted-9.txt", true));
    }

    private void testAddEntryToOverlay(PrintWriter writer)
        throws UnableToAdaptException, IOException {

        writer.println("testAddEntryToOverlay");
        writer.println("  CacheDir [ " + cacheDirAdapt + " ]");
        writer.println("  CacheOverlay [ " + cacheDirOverlay + " ]");

        ArtifactContainer rootArtifactContainer = getContainerForDirectory();

        Container rootOverlayContainer = amf.getContainer(
            new File(cacheDirAdapt), new File(cacheDirOverlay),
            rootArtifactContainer);

        InterpretedContainer rootInterpretedContainer = rootOverlayContainer.adapt(InterpretedContainer.class);

        rootInterpretedContainer.setStructureHelper( new AddStructureHelper() );

        // Sanity-check: Ensure we can define a root container using the structure helper,
        // shouldn't be able to retrieve entries under that root container from outside it.

        if ( rootInterpretedContainer.getEntry("b/ba/baa/baa1.txt") != null ) {
            writer.println("FAIL: Retrieved entry from root created by structure helper");
            return;
        }

        List<ContainerAction> addActions = new ArrayList<ContainerAction>();
        List<ContainerAction> getActions = new ArrayList<ContainerAction>();

        populateActions(addActions, getActions, rootOverlayContainer, rootInterpretedContainer);

        int numAddActions = addActions.size();
        int numGetActions = getActions.size();
        if ( numAddActions != numGetActions ) {
            writer.println("FAIL: Unequal actions:" +
                           " Add [ " + Integer.valueOf(numAddActions) + " ]" +
                           " Get [ " + Integer.valueOf(numGetActions) + " ]");
            return;
        }

        boolean passed = true;

        try {
            for ( int actionNo = 0; actionNo < numAddActions; actionNo++ ) {
                ContainerAction addAction = addActions.get(actionNo);
                ContainerAction getAction = getActions.get(actionNo);
                if ( !(passed = addAndGetEntry(addAction, getAction, writer)) ) {
                    break;
                }
            }

        } finally {
            // clean-up overlaid entries, in brute-force manner (may well delete paths like a/b 
            // before a/b/c, removing the latter by implicitly before we attempt to do it 
            // explicitly, but this doesn't matter as the explicit operation will just be a no-op).

            for ( int actionNo = 0; actionNo < numAddActions; actionNo++) {
                ContainerAction addAction = addActions.get(actionNo);
                if ( addAction == null ) {
                    continue;
                }

                OverlayContainer overlayContainer = addAction.container.adapt(OverlayContainer.class);
                if ( overlayContainer == null ) {
                    passed = false;
                    writer.println("FAIL: Container [ " + addAction.container.getPath() + " ] has no overlay container");
                    continue;
                }

                for ( String overlaidPath : overlayContainer.getOverlaidPaths() ) {
                    overlayContainer.removeFromOverlay(overlaidPath);
                }
            }

            for ( int actionNo = 0; actionNo < numAddActions; actionNo++ ) {
                ContainerAction addAction = addActions.get(actionNo);
                if ( addAction == null ) {
                    continue;
                }

                OverlayContainer overlayContainer = addAction.container.adapt(OverlayContainer.class);
                if ( overlayContainer == null ) {
                    passed = false;
                    writer.println("FAIL: Container [ " + addAction.container.getPath() + " ] has no overlay container");
                    continue;
                }

                for ( String overlaidPath : overlayContainer.getOverlaidPaths() ) {
                    passed = false;
                    writer.println("FAIL: Container [ " + addAction.container.getPath() + " ] still has overlay [ " + overlaidPath + " ]");
                }
            }
        }

        if ( passed ) {
            writer.println("PASS: entries can be added to a container.");
        }
    }

    private void testInterpretedAdaptable(PrintWriter writer) throws UnableToAdaptException {
        ArtifactContainer c = getContainerForDirectory();
        Container adaptContainer = amf.getContainer(new File(cacheDirAdapt), new File(cacheDirOverlay), c);
        if (adaptContainer != null) {
            writer.println("Got ArtifactContainer " + adaptContainer);
            InterpretedContainer interpreted = adaptContainer.adapt(InterpretedContainer.class);

            StructureHelper sh = new StructureHelper() {
                /** {@inheritDoc} */
                @Override
                public boolean isRoot(ArtifactContainer e) {
                    if ("aa".equals(e.getName()))
                        return true;
                    if ("ba".equals(e.getName())) {
                        return true;
                    }
                    if ("baa".equals(e.getName())) {
                        return true;
                    }
                    return e.isRoot();
                }

                @Override
                public boolean isValid(ArtifactContainer e, String path) {
                    //correct absolute paths down to paths beneath 'e'
                    if (path.startsWith("/")) {
                        path = path.substring(e.getPath().length());
                    }
                    //does the path contain containers?
                    if (path.indexOf("/") != -1) {
                        String pathComponents[] = path.split("/");
                        //iterate through all path components except the last
                        //there will be at least two, because the path had a / in it.
                        for (int i = 0; i < (pathComponents.length - 1); i++) {
                            if ("aa".equals(pathComponents[i]))
                                return false;
                            if ("ba".equals(pathComponents[i])) {
                                return false;
                            }
                            if ("baa".equals(pathComponents[i])) {
                                return false;
                            }
                        }
                        //we didnt match a container that we're converting into a root, 
                        //so the path is valid.
                        return true;
                    } else {
                        //no containers, nothing to hide.. 
                        return true;
                    }
                }
            };
            interpreted.setStructureHelper(sh);

            writer.println("Before interpret.. ");
            dumpContainerRecursive(0, adaptContainer, "/", writer);
            writer.println("After interpret.. ");
            dumpContainerRecursive(0, interpreted, "/", writer);

            boolean pass = true;

            //single root.. no nested overrides.. 

            Entry fail = interpreted.getEntry("/a/aa/aa.txt");
            if (fail != null) {
                writer.println("FAIL: able to see content /a/aa/aa.txt that should be masked by fake root.");
                pass = false;
            }

            Entry e = interpreted.getEntry("/a/aa");
            Container aa = e.adapt(Container.class);
            writer.println("aa is root? " + aa.isRoot());
            if (!aa.isRoot()) {
                writer.println("FAIL: aa should have been root, as set by structure handler.");
                pass = false;
            }
            writer.println("aa enclosing : " + aa.getEnclosingContainer().getPath());
            if (!aa.getEnclosingContainer().getPath().equals("/a")) {
                writer.println("FAIL: aa enclosing path should have been /a");
                pass = false;
            }
            writer.println("aa.getEntry(aa.txt) not null ? " + (aa.getEntry("aa.txt") != null));
            if (aa.getEntry("aa.txt") == null) {
                writer.println("FAIL: aa could not see it's local child of aa.txt");
                pass = false;
            } else {
                writer.println("aa.getEntry(aa.txt) path=/aa.txt ? " + (aa.getEntry("aa.txt").getPath()));
                if (!aa.getEntry("aa.txt").getPath().equals("/aa.txt")) {
                    writer.println("FAIL: path for aa.txt under fake root of /aa should have been, /aa.txt was " + aa.getEntry("aa.txt").getPath());
                    pass = false;
                }
            }
            writer.println("aa.getEntry(/aa.txt) not null ? " + (aa.getEntry("/aa.txt") != null));
            if (aa.getEntry("/aa.txt") == null) {
                writer.println("FAIL: aa could not see  aa.txt via the fake root path of /aa.txt");
                pass = false;
            } else {
                writer.println("aa.getEntry(/aa.txt) path=/aa.txt ? " + aa.getEntry("/aa.txt").getPath());
                if (!aa.getEntry("/aa.txt").getPath().equals("/aa.txt")) {
                    writer.println("FAIL: path for aa.txt under fake root of /aa should have been, /aa.txt was " + aa.getEntry("/aa.txt").getPath());
                    pass = false;
                }
            }

            //root, with a nested override as a child.. 

            e = interpreted.getEntry("/b/ba");
            Container ba = e.adapt(Container.class);
            writer.println("ba is root? " + ba.isRoot());
            if (!ba.isRoot()) {
                writer.println("FAIL: ba should have been root, as set by structure handler.");
                pass = false;
            }
            writer.println("ba enclosing : " + ba.getEnclosingContainer().getPath());
            if (!ba.getEnclosingContainer().getPath().equals("/b")) {
                writer.println("FAIL: ba enclosing path should have been /b");
                pass = false;
            }
            writer.println("ba.getEntry(baa) not null ? " + (ba.getEntry("baa") != null));
            if (ba.getEntry("baa") == null) {
                writer.println("FAIL: ba could not see it's local child of baa");
                pass = false;
            } else {
                writer.println("ba.getEntry(baa) path=/baa ? " + (ba.getEntry("baa").getPath()));
                if (!ba.getEntry("baa").getPath().equals("/baa")) {
                    writer.println("FAIL: path for baa under fake root of /ba should have been, /baa was " + ba.getEntry("baa").getPath());
                    pass = false;
                }
            }
            writer.println("ba.getEntry(/baa) not null ? " + (ba.getEntry("/baa") != null));
            if (ba.getEntry("/baa") == null) {
                writer.println("FAIL: ba could not see baa via the fake root path of /baa");
                pass = false;
            } else {
                writer.println("ba.getEntry(/baa) path=/baa ? " + ba.getEntry("/baa").getPath());
                if (!ba.getEntry("/baa").getPath().equals("/baa")) {
                    writer.println("FAIL: path for baa under fake root of /ba should have been, /baa was " + ba.getEntry("/baa").getPath());
                    pass = false;
                }
            }

            //testing of the nested override.. 
            e = ba.getEntry("/baa");
            Container baa = e.adapt(Container.class);
            writer.println("baa is root? " + baa.isRoot());
            if (!baa.isRoot()) {
                writer.println("FAIL: baa should have been root, as set by structure handler.");
                pass = false;
            }
            writer.println("baa enclosing : " + baa.getEnclosingContainer().getPath());
            if (!baa.getEnclosingContainer().getPath().equals("/")) {
                writer.println("FAIL: baa enclosing path should have been /");
                pass = false;
            }
            writer.println("baa.getEntry(/baa1.txt) not null ? " + (baa.getEntry("baa1.txt") != null));
            if (baa.getEntry("baa1.txt") == null) {
                writer.println("FAIL: baa could not see it's local child of baa1.txt");
                pass = false;
            } else {
                writer.println("baa.getEntry(baa1.txt) path=/baa1.txt ? " + (baa.getEntry("baa1.txt").getPath()));
                if (!baa.getEntry("baa1.txt").getPath().equals("/baa1.txt")) {
                    writer.println("FAIL: path for baa1.txt under fake root of /baa should have been, /baa1.txt was " + baa.getEntry("baa1.txt").getPath());
                    pass = false;
                }
            }
            writer.println("baa.getEntry(/baa1.txt) not null ? " + (baa.getEntry("/baa1.txt") != null));
            if (baa.getEntry("/baa1.txt") == null) {
                writer.println("FAIL: baa could not see baa1.txt via the fake root path of /baa1.txt");
                pass = false;
            } else {
                writer.println("baa.getEntry(/baa1.txt) path=/baa1.txt ? " + baa.getEntry("/baa1.txt").getPath());
                if (!baa.getEntry("/baa1.txt").getPath().equals("/baa1.txt")) {
                    writer.println("FAIL: path for baa1.txt under fake root of /baa should have been, /baa1.txt was " + baa.getEntry("/baa1.txt").getPath());
                    pass = false;
                }
            }
            if (pass) {
                writer.println("PASS: adaptable with structure handler passed.");
            }
        }

    }

    private void testFsLayer(PrintWriter out) throws Exception {
        out.println("Testing fs layer... ");

        out.println("\n\nrunning dir compare..");
        ArtifactContainer dirc = getContainerForDirectory();
        ArtifactUtils.counter = 0;
        long start = System.currentTimeMillis();
        boolean res1 = ArtifactUtils.compare(dirc, DirFileSystem.TESTDATA, out);
        if (res1) {
            out.println("PASS: woohoo.. completed " + ArtifactUtils.counter + " tests in " + (System.currentTimeMillis() - start) + "ms");
        }
        testFsLayerViaOverlay(out, dirc, "Directory", DirFileSystem.TESTDATA);

        out.println("\n\nrunning zip compare..");
        ArtifactContainer zipc = getContainerForZip();
        ArtifactUtils.counter = 0;
        start = System.currentTimeMillis();
        boolean res2 = ArtifactUtils.compare(zipc, JarFileSystem.TESTDATA, out);
        if (res2) {
            out.println("PASS: woohoo.. completed " + ArtifactUtils.counter + " tests in " + (System.currentTimeMillis() - start) + "ms ");
        }
        testFsLayerViaOverlay(out, zipc, "ZipData", JarFileSystem.TESTDATA);

        out.println("\n\nrunning loose compare..");
        ArtifactContainer loosec = getContainerForLooseXML();
        ArtifactUtils.counter = 0;
        start = System.currentTimeMillis();
        boolean res3 = ArtifactUtils.compare(loosec, LooseFileSystem.TESTDATA, out);
        if (res3) {
            out.println("PASS: woohoo.. completed " + ArtifactUtils.counter + " tests in " + (System.currentTimeMillis() - start) + "ms ");
        }
        testFsLayerViaOverlay(out, loosec, "LooseData", LooseFileSystem.TESTDATA);

        //because I still need to 'fix' the bundle impl architecture..
        ArtifactUtils.TEST_INTERFACE_IMPLS_ARE_DIFFERENT = false;

        out.println("\n\nrunning bundle compare..");
        ArtifactContainer bundlec = getContainerForBundle(out);
        ArtifactUtils.counter = 0;
        start = System.currentTimeMillis();
        boolean res4 = ArtifactUtils.compare(bundlec, BundleFileSystem.TESTDATA, out);
        if (res4) {
            out.println("PASS: woohoo.. completed " + ArtifactUtils.counter + " tests in " + (System.currentTimeMillis() - start) + "ms ");
        }
        testFsLayerViaOverlay(out, bundlec, "BundleData", BundleFileSystem.TESTDATA);

        ArtifactUtils.TEST_INTERFACE_IMPLS_ARE_DIFFERENT = true;

    }

    private ArtifactContainer getContainerForDirectory() {
        File dirFile = new File(dir);
        ArtifactContainer ArtifactContainer = cf.getContainer(new File(cacheDir), dirFile);
        return ArtifactContainer;
    }

    private ArtifactContainer getContainerForZip() {
        File zipFile = new File(jar_a);
        ArtifactContainer c = cf.getContainer(new File(cacheDir), zipFile);
        return c;
    }

    private ArtifactContainer getContainerForMulti() {
        File zipFile = new File(jar_multi);
        ArtifactContainer c = cf.getContainer(new File(cacheDir), zipFile);
        return c;
    }

    private ArtifactContainer getContainerForCustom() {
        File containerFile = new File(customContainerData);
        ArtifactContainer c = cf.getContainer(new File(cacheDir), containerFile);
        return c;
    }

    private ArtifactContainer getContainerForSmallerZip() {
        File zipFile = new File(jar_b);
        ArtifactContainer c = cf.getContainer(new File(cacheDir), zipFile);
        return c;
    }

    private ArtifactContainer getContainerForDirZip() {
        File zipFile = new File(jar_dir);
        ArtifactContainer c = cf.getContainer(new File(cacheDir), zipFile);
        return c;
    }

    private ArtifactContainer getContainerForLooseXML() {
        ArtifactContainer c = cf.getContainer(new File(cacheDir), new File(looseXml));
        return c;
    }

    private ArtifactContainer getContainerForRar() {
        File rarFile = new File(rar);
        ArtifactContainer c = cf.getContainer(new File(cacheDir), rarFile);
        return c;
    }

    private ArtifactContainer getContainerForBundle(PrintWriter writer) throws Exception {
        // Create the bundle
        Bundle bundle = installBundle(writer, bundleLocation);
        installBundle(writer, bundleFragmentLocation);
        ArtifactContainer artifactContainer = cf.getContainer(new File(cacheDir), bundle);
        return artifactContainer;
    }

    private ArtifactContainer getContainerForBadBundle(PrintWriter writer) throws Exception {
        // Create the bundle
        Bundle bundle = installBundle(writer, badBundleLocation);
        ArtifactContainer artifactContainer = cf.getContainer(new File(cacheDir), bundle);
        return artifactContainer;
    }

    private ArtifactContainer getContainerForBundleDir() throws Exception {
        // Create the bundle
        String fileLocation = (new File(bundleTestDirLocation)).toURI().toURL().toString();
        //from javadoc for installBundle.. 
        //"If a bundle containing the same location identifier is already installed, the Bundle object for that bundle is returned."
        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class).getBundleContext().installBundle(fileLocation);
        ArtifactContainer ArtifactContainer = cf.getContainer(new File(cacheDir), bundle);
        return ArtifactContainer;
    }

    /**
     * This test uses a bundle that has been build with 'bad' path names inside, where a path name is prefixed with /
     * this seems to cause osgi a few issues.. these were found via 82145, and were breaking the artifact api layered over the bundle.
     * 82145 included a patch to prevent the bad behavior causing the problems, and this is it's associated test.<p>
     * 
     * Note: BADPATHTEST.jar used as the bundle for this test, has been built programatically to insert the bad path, using JarOutputStream.
     * 
     * @param writer
     * @param bundleDirTestLocation
     */
    private void testBadBundlePathIteration(PrintWriter writer) {
        boolean passed = true;
        try {
            writer.println("Executing test..");
            writer.flush();
            ArtifactContainer container = getContainerForBadBundle(writer);
            writer.println("Got container..");
            writer.flush();
            for (ArtifactEntry e : container) {
                if (e.getPath().equals("/")) {
                    writer.println("FAIL: testBadBundlePathiteration: root level iteration included bad entry with root path");
                    passed = false;
                }
            }
            writer.println("Done..");
            writer.flush();
            if (passed) {
                writer.println("PASS: testBadBundlePathiteration: All testBadBundlePathiteration tests passed");
            }
        } catch (Exception e) {
            writer.println("FAIL: testBadBundlePathiteration: Caught exception: ");
            e.printStackTrace(writer);
        }

    }

    /**
     * This is a basic test of the bundle artifact API implementation to make sure that it can load a ArtifactContainer from an OSGi bundle. It is just a temporary test whilst the
     * artifact
     * This test takes an artifact API for a bundle with two file entries one in a zip directory entry and one not and makes sure both can get a container.
     * 
     * @param writer
     * @param bundleDirTestLocation
     */
    private void testGetEnclosingContainerOnBundle(PrintWriter writer) {
        boolean passed = true;
        try {
            ArtifactContainer container = getContainerForBundleDir();

            // First test the file in a directory entry
            ArtifactEntry dirFileEntry = container.getEntry("/dirEntry/file.txt");
            ArtifactContainer dirFileEntryEnclosingContainer = dirFileEntry.getEnclosingContainer();
            if (dirFileEntryEnclosingContainer == null) {
                passed = false;
                writer.println("FAIL: No container found for /dirEntry/file.txt");
            }

            // No test the file that isn't in a zip directory
            ArtifactEntry noDirFileEntry = container.getEntry("/noDirEntry/file.txt");
            ArtifactContainer noDirFileEntryEnclosingContainer = noDirFileEntry.getEnclosingContainer();
            if (noDirFileEntryEnclosingContainer == null) {
                passed = false;
                writer.println("FAIL: No container found for /noDirEntry/file.txt");
            }

            if (passed) {
                writer.println("PASS: All testGetEnclosingContainerOnBundle tests passed");
            }
        } catch (Exception e) {
            writer.println("FAIL: Caught exception: ");
            e.printStackTrace(writer);
        }
    }

    /**
     * This is a basic test of the bundle artifact API implementation to make sure that it can load a container from an OSGi bundle. It is just a temporary test whilst the artifact
     * API test framework is re-written as part of task 50058.
     */
    private void simpleBundleArtifactApiTest(PrintWriter writer, ArtifactContainerFactory cf) {
        boolean passed = true;
        try {
            // Create the bundle
            ArtifactContainer artifactContainer = getContainerForBundle(writer);

            // Test txt ArtifactEntry
            ArtifactEntry aTxtEntry = artifactContainer.getEntry("/a/a.txt");
            if (aTxtEntry == null) {
                writer.println("FAIL: No ArtifactEntry for a.txt");
                passed = false;
            } else {
                InputStream is = aTxtEntry.getInputStream();
                if (is == null) {
                    writer.println("FAIL: No input stream for a.txt");
                    passed = false;
                } else {
                    is.close();
                    writer.println("SUCCESS: ArtifactEntry and stream found for a.txt");
                }

                if (aTxtEntry.convertToContainer() != null) {
                    writer.println("FAIL: Was able to convert a.txt to a ArtifactContainer");
                    passed = false;
                }
            }

            // Test internal ArtifactContainer
            ArtifactEntry internalContainerEntry = artifactContainer.getEntry("/b");
            if (internalContainerEntry == null) {
                writer.println("FAIL: No ArtifactEntry for b");
                passed = false;
            } else {
                ArtifactContainer internalContainer = internalContainerEntry.convertToContainer();
                if (internalContainer == null) {
                    writer.println("FAIL: Unable to convert b to ArtifactContainer");
                    passed = false;
                } else {
                    Iterator<ArtifactEntry> ArtifactEntryIterator = internalContainer.iterator();
                    boolean foundBa = false;
                    boolean foundBb = false;
                    while (ArtifactEntryIterator.hasNext()) {
                        String name = ArtifactEntryIterator.next().getName();
                        if ("ba".equals(name)) {
                            foundBa = true;
                        } else if ("bb".equals(name)) {
                            foundBb = true;
                        } else {
                            writer.println("FAIL: Unkown ArtifactEntry: " + name + " found in b");
                            passed = false;
                        }
                    }
                    if (!foundBa) {
                        writer.println("FAIL: Did not find ba found in b");
                        passed = false;
                    }
                    if (!foundBb) {
                        writer.println("FAIL: Did not find bb found in b");
                        passed = false;
                    }
                    if (foundBa && foundBb) {
                        writer.println("SUCCESS: ArtifactContainer found with correct content for b");
                    }
                }
            }

            // Test external ArtifactContainer
            ArtifactEntry externalContainerEntry = artifactContainer.getEntry("/c/a.jar");
            if (externalContainerEntry == null) {
                writer.println("FAIL: No ArtifactEntry for /c/a.jar");
                passed = false;
            } else {
                ArtifactContainer externalContainer = externalContainerEntry.convertToContainer();
                if (externalContainer == null) {
                    writer.println("FAIL: Unable to convert /c/a.jar to ArtifactContainer");
                    passed = false;
                } else {
                    writer.println("SUCCESS: ArtifactContainer found for /c/a.jar");
                }
            }

            // Finally test the iterator on the root
            Iterator<ArtifactEntry> ArtifactEntryIterator = artifactContainer.iterator();
            boolean foundA = false;
            boolean foundB = false;
            boolean foundC = false;
            boolean foundD = false;
            boolean foundMetaInf = false;
            int countMetaInf = 0;
            while (ArtifactEntryIterator.hasNext()) {
                String name = ArtifactEntryIterator.next().getName();
                if ("a".equals(name)) {
                    foundA = true;
                } else if ("b".equals(name)) {
                    foundB = true;
                } else if ("c".equals(name)) {
                    foundC = true;
                } else if ("META-INF".equals(name)) {
                    countMetaInf++;
                    foundMetaInf = true;
                } else if ("d".equals(name)) {
                    foundD = true;
                } else {
                    writer.println("FAIL: Unkown ArtifactEntry: " + name + " found in root iterator");
                    passed = false;
                }
            }
            if (!foundA) {
                writer.println("FAIL: Did not find a found in root");
                passed = false;
            }
            if (!foundB) {
                writer.println("FAIL: Did not find b found in root");
                passed = false;
            }
            if (!foundC) {
                writer.println("FAIL: Did not find c found in root");
                passed = false;
            }
            if (!foundMetaInf) {
                writer.println("FAIL: Did not find META-INF found in root");
                passed = false;
            }
            if (countMetaInf > 1) {
                writer.println("FAIL: More than one occurence of META-INF found in root");
                passed = false;
            }
            if (!foundD) {
                writer.println("FAIL: Did not find the fragment entry in the iterator");
                passed = false;
            }
            if (foundA && foundB && foundC && foundD && foundMetaInf) {
                writer.println("SUCCESS: Iterator for root was correct");
            }

            // Also test getEntry for fragment
            ArtifactEntry dTxt = artifactContainer.getEntry("/d/d.txt");
            if (dTxt != null) {
                writer.println("SUCCESS: Got entry for d.txt");
            } else {
                writer.println("FAIL: Could not get entry for d.txt");
                passed = false;
            }

            // Also test iteration of META-INF (test removal of dupes.. since a fragment will add a 2nd manifest)
            ArtifactEntry metaInf = artifactContainer.getEntry("META-INF");
            if (metaInf != null) {
                ArtifactContainer metaInfDir = metaInf.convertToContainer(true);
                if (metaInfDir != null) {
                    int manifestCount = 0;
                    for (ArtifactEntry manifestDirEntry : metaInfDir) {
                        if (manifestDirEntry.getName().equals("MANIFEST.MF")) {
                            manifestCount++;
                        }
                    }
                    if (manifestCount > 1) {
                        writer.println("FAIL: More than one occurence of META-INF/MANIFEST found in ArtifactContainer");
                    }
                }
            }

            if (passed) {
                writer.println("PASS: All simpleBundleArtifactApiTest tests passed");
            }
        } catch (Exception e) {
            writer.println("FAIL: Caught exception:");
            e.printStackTrace(writer);
        }
    }

    /**
     * Attempts to install a bundle and the supplied location into the bundle framework
     * 
     * @param writer A writer to write messages to
     * @param location The location of the bundle
     * @return The bundle
     * @throws BundleException If the bundle can't be installed
     * @throws MalformedURLException If the location is illegal
     */
    private Bundle installBundle(PrintWriter writer, String location) throws BundleException, MalformedURLException {
        writer.println("Attempting to install:");
        String fileLocation = (new File(location)).toURI().toURL().toString();
        writer.println(fileLocation);
        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class).getBundleContext().installBundle(fileLocation);
        return bundle;
    }

    /**
     * This tests that you can call getEntry("..") on a container to get its parent
     * 
     * @param cf The factory for ArtifactContainers
     * @param dir The dir for a directory archive
     * @param looseConfigFile The loose archive XML file
     * @param writer The writer to write messages to
     */
    private void testDotDotPath(ArtifactContainerFactory cf, String dir, File looseConfigFile, PrintWriter writer) {
        boolean passed = true;

        // First need a ArtifactContainer
        ArtifactContainer ArtifactContainer = getContainerForDirectory();

        // Test a sub-dir
        writer.println("Testing using .. on a directory");
        testDotDotPath(ArtifactContainer, "/a/ab", "aa", writer);

        // Test a zip
        try {
            // Test a zip
            writer.println("Testing using .. on a zip");
            ArtifactEntry zipEntry = ArtifactContainer.getEntry("/c/a.jar");
            ArtifactContainer zipContainer = zipEntry.convertToContainer();
            testDotDotPath(zipContainer, "/ab/aba", "ab.txt", writer);
        } catch (Throwable t) {
            writer.println("FAIL: (testDotDotPath) Exception (" + t.getMessage() + ") thrown trying to navigate to dir to test in a zip. Exception is:");
            t.printStackTrace(writer);
            passed = false;
        }

        // Now test loose
        writer.println("Testing using .. on a loose config");
        ArtifactContainer looseContainer = getContainerForLooseXML();
        testDotDotPath(looseContainer, "/META-INF/b", "c/a.jar", writer);

        if (passed) {
            writer.println("PASS: (testDotDotPath) able to get .. entries for ArtifactContainers");
        }
    }

    /**
     * This test will make sure that it is possible to get a parent ArtifactContainer by passing in .. to the getEntry method and then make sure you can't leave the root
     * ArtifactContainer by
     * passing in ...
     * 
     * @param ArtifactContainer The root ArtifactContainer being tested
     * @param subContainerPath A path to a sub ArtifactContainer that we are then going to call .. on. This ArtifactContainer must be two levels down (so that when we try to leave
     *            the root
     *            ArtifactContainer we'll actually be leaving)
     * @param siblingName This is the name of a file path to a file that is a sibling of the <code>subContainerPath</code>. It should just be the file name part relative to the
     *            parent of the both of the siblings, not an absolute path
     * @param writer The writer to write messages to
     * @return <code>true</code> if all the tests pass
     */
    private boolean testDotDotPath(ArtifactContainer ArtifactContainer, String subContainerPath, String siblingName, PrintWriter writer) {
        boolean passed = true;
        try {
            // Test a dir
            ArtifactEntry dirEntry = ArtifactContainer.getEntry(subContainerPath);
            ArtifactContainer subContainer = dirEntry.convertToContainer();

            // Get the parent ArtifactEntry
            String parentPath = subContainerPath.substring(0, subContainerPath.lastIndexOf("/"));
            ArtifactEntry parentEntry = subContainer.getEntry("..");
            if (parentEntry != null) {
                // Make sure the path was right
                if (parentPath.equals(parentEntry.getPath())) {
                    writer.println("SUCCESS: (testDotDotPath) The correct parent path returned for " + parentPath);
                } else {
                    writer.println("FAIL: (testDotDotPath) Wrong path for " + parentPath + " got " + parentEntry.getPath());
                    passed = false;
                }
            } else {
                writer.println("FAIL: (testDotDotPath) Unable to get parent ArtifactEntry by using .. for dir: " + subContainerPath);
                passed = false;
            }

            // Now try to get a sibling
            String siblingPath = "../" + siblingName;
            ArtifactEntry siblingEntry = subContainer.getEntry(siblingPath);
            if (siblingEntry != null) {
                // Make sure the path was right
                String expectedPath = parentPath + "/" + siblingName;
                if (expectedPath.equals(siblingEntry.getPath())) {
                    writer.println("SUCCESS: (testDotDotPath) The correct sibling path returned for " + expectedPath);
                } else {
                    writer.println("FAIL: (testDotDotPath) Wrong path for " + expectedPath + " got " + siblingEntry.getPath());
                    passed = false;
                }
            } else {
                writer.println("FAIL: (testDotDotPath) Unable to get sibling ArtifactEntry by using the path " + siblingPath + " for dir: " + subContainerPath);
                passed = false;
            }

            // Also try to leave the ArtifactContainer, should throw an illegal argument exception
            try {
                ArtifactEntry leftContainerEntry = subContainer.getEntry("../../..");
                if (leftContainerEntry == null) {
                    writer.println("SUCCESS: Got null trying to leave the ArtifactContainer as expected");
                } else {
                    writer.println("FAIL: We tried to leave the ArtifactContainer but an Artifact entry was returned. " + leftContainerEntry);
                    passed = false;
                }
            } catch (IllegalArgumentException e) {
                writer.println("FAIL: We tried to leave the ArtifactContainer but an exception was thrown saying this isn't allowed.");
                passed = false;
            }
        } catch (Throwable t) {
            writer.println("FAIL: (testDotDotPath) Exception (" + t.getMessage() + ") thrown testing .. notation in getEntry. Exception is:");
            t.printStackTrace(writer);
            passed = false;
        }
        return passed;
    }

    private void testImpliedZipDir(PrintWriter writer) {
        boolean passed = true;

        ArtifactContainer ac = getContainerForDirZip();
        ArtifactEntry ae;

        writer.println("Testing dir");
        if ((ae = ac.getEntry("dir")) == null) {
            writer.println("FAIL: expected dir to exist");
            passed = false;
        } else {
            writer.println("SUCCESS: " + ae);
        }

        writer.println("Testing dir/file");
        if ((ae = ac.getEntry("dir/file")) == null) {
            writer.println("SUCCESS: dir/file does not exist");
        } else {
            writer.println("FAIL: did not expect dir/file to exist: " + ae);
            passed = false;
        }

        writer.println("Testing dir2");
        if ((ae = ac.getEntry("dir2")) == null) {
            writer.println("SUCCESS: dir2 does not exist");
        } else {
            writer.println("FAIL: did not expect dir2 to exist: " + ae);
            passed = false;
        }

        writer.println("Testing dir3");
        if ((ae = ac.getEntry("dir3")) == null) {
            writer.println("FAIL: expected dir3 to exist");
            passed = false;
        } else {
            writer.println("SUCCESS: " + ae);
        }

        if (passed) {
            writer.println("PASS: passed implied directory tests");
        }
    }

    /**
     * @param c The ArtifactContainer to check
     * @param writer The writer to write the results to
     * @return
     */
    private boolean testCaseSensitivity_loose(ArtifactContainer c, PrintWriter writer) {
        // First test ArtifactContainers and files in dirs
        boolean passed = testCaseSesnitivityForDirs(c, writer, "/META-INF");

        // Now test a file ArtifactEntry
        if (c.getEntry("/myjar.JAR") != null) {
            writer.println("FAIL: Able to get file even with the wrong case on the file name");
            passed = false;
        }

        // And an archive ArtifactEntry
        if (c.getEntry("webApp.WAR") != null) {
            writer.println("FAIL: Able to get archive even with the wrong case on the archive name");
            passed = false;
        }

        return passed;
    }

    /**
     * This test makes sure that dirs and files from within the TESTDATA dir are not case sensitive within the supplied ArtifactContainer.
     * 
     * @param c
     * @param writer
     * @param prefix A prefix to add in front of the directory name when calling getEntry. This is mainly for loose config, if there is not one then "" should be supplied.
     * @return
     */
    private boolean testCaseSesnitivityForDirs(ArtifactContainer c, PrintWriter writer, String prefix) {
        boolean passed = true;
        prefix = (prefix == null) ? "" : prefix;

        // Try to get a file in the a dir with the wrong case, first for the dir then for the file
        if (c.getEntry(prefix + "/b/ba/baa/BAA1.txt") != null) {
            writer.println("FAIL: Able to get file from dir even with the wrong case on the file name");
            passed = false;
        }

        if (c.getEntry(prefix + "/b/ba/BAA/baa1.txt") != null) {
            writer.println("FAIL: Able to get file from dir even with the wrong case on the directory name");
            passed = false;
        }

        // For directories this should work with and without a "/" on the end, make sure we can still get dirs with the right case for boths types
        if (c.getEntry(prefix + "/b/ba/BAA") != null) {
            writer.println("FAIL: Able to get directory even with the wrong case on the directory name");
            passed = false;
        }

        if (c.getEntry(prefix + "/b/ba/BAA/") != null) {
            writer.println("FAIL: Able to get directory even with the wrong case on the directory name and a / on the end");
            passed = false;
        }

        if (c.getEntry(prefix + "/b/ba/baa") == null) {
            writer.println("FAIL: Unable to get directory with the right case");
            passed = false;
        }

        if (c.getEntry(prefix + "/b/ba/baa/") == null) {
            writer.println("FAIL: Unable to get directory with the right case and a / on the end");
            passed = false;
        }
        return passed;
    }

    private void testAdaptableApiNavigate(PrintWriter writer) throws UnableToAdaptException {
        ArtifactContainer c = getContainerForDirectory();
        com.ibm.wsspi.adaptable.module.Container adaptContainer = amf.getContainer(new File(cacheDirAdapt), new File(cacheDirOverlay), c);
        if (adaptContainer != null) {
            writer.println("Got ArtifactContainer " + adaptContainer);
            Set<String> expected = new HashSet<String>(Arrays.asList(new String[] { "a", "b", "c" }));
            Set<String> surplus = new HashSet<String>();
            for (com.ibm.wsspi.adaptable.module.Entry e : adaptContainer) {
                if (expected.contains(e.getName())) {
                    expected.remove(e.getName());
                } else {
                    surplus.add(e.getName());
                }
            }
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: adapt traversal expected: " + expected + " surplus: " + surplus);
                return;
            }
            expected = new HashSet<String>(Arrays.asList(new String[] { "/a",
                                                                       "/a/aa",
                                                                       "/a/aa/aa.txt",
                                                                       "/a/ab",
                                                                       "/a/ab/aba",
                                                                       "/a/ab/aba/aba.txt",
                                                                       "/a/ab/ab.txt",
                                                                       "/a/a.txt",
                                                                       "/b",
                                                                       "/b/ba",
                                                                       "/b/ba/baa",
                                                                       "/b/ba/baa/baa1.txt",
                                                                       "/b/ba/baa/baa2.txt",
                                                                       "/b/bb",
                                                                       "/b/bb/a.jar",
                                                                       "/c",
                                                                       "/c/a.jar",
                                                                       "/c/b.jar"
            }));
            surplus = new HashSet<String>();
            validatePaths(expected, surplus, adaptContainer);
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: expected: " + expected + " surplus: " + surplus);
                return;
            }

            writer.println("PASS");
        }
    }

    /**
     * Tests the xml parsing loose config api to make sure it reads the xml and maps the file system correctly.
     * 
     * @param rootLooseArchiveContainer
     * @param writer
     * @return if all tests passed or not.
     */
    private boolean testLooseApiXmlParsing(ArtifactContainer rootLooseArchiveContainer, PrintWriter writer) {
        boolean passed = true;
        ArtifactContainer testJarFromXml = null;

        //get a ArtifactContainer in root
        if (rootLooseArchiveContainer.getEntry("/TEST.jar") == null) {
            //test failed
            writer.println("FAIL: unable to get a handle of TEST.jar from root");
            passed = false;
        } else {
            testJarFromXml = rootLooseArchiveContainer.getEntry("/TEST.jar").convertToContainer();
            if (testJarFromXml != null) {
                writer.println("SUCCESS: grab test.jar in root passed");
            } else {
                //test failed
                writer.println("FAIL: unable to convert a grabbed TEST.jar from root into a ArtifactContainer");
                passed = false;
            }
        }

        ArtifactContainer webAppWarFromXml = null;

        //get another ArtifactContainer in root
        if (rootLooseArchiveContainer.getEntry("/webApp.war") == null) {
            //test failed
            writer.println("FAIL: unable to get a handle of webApp.war from root");
            passed = false;
        } else {
            webAppWarFromXml = rootLooseArchiveContainer.getEntry("/webApp.war").convertToContainer();
            if (webAppWarFromXml != null) {
                writer.println("SUCCESS: grab webApp.war in root passed");
            } else {
                //test failed
                writer.println("FAIL: unable to convert a grabbed webApp.war from root into a ArtifactContainer");
                passed = false;
            }
        }

        //make sure excluded file is excluded
        if (webAppWarFromXml == null || webAppWarFromXml.getEntry("/a/a.txt") != null) {
            //test failed
            writer.println("FAIL: when making sure /a/a.txt was excluded from the webApp.war ArtifactContainer the test failed");
            passed = false;
        } else {
            writer.println("SUCCESS: ensuring /a/a.txt was excluded from webApp.war passed");
        }

        //make sure excluded file is excluded when using different excludes expression
        if (webAppWarFromXml == null || webAppWarFromXml.getEntry("/WEB-INF/classes/b/ba/baa/baa1.txt") != null) {
            //test failed
            writer.println("FAIL: when making sure /WEB-INF/classes/b/ba/baa/baa1.txt was excluded from the webApp.war ArtifactContainer the test failed");
            passed = false;
        } else {
            writer.println("SUCCESS: ensuring /WEB-INF/classes/b/ba/baa/baa1.txt was excluded from webApp.war passed");
        }

        //get a file in the root of a ArtifactContainer in root
        if (testJarFromXml == null || testJarFromXml.getEntry("/a.txt") == null) {
            //test failed
            writer.println("FAIL: grabbing /a.txt from TEST.jar failed");
            passed = false;
        } else {
            writer.println("SUCCESS: grabbing /a.txt from TEST.jar passed");
        }

        //get a file 2 folders deep in a ArtifactContainer in root
        if (testJarFromXml.getEntry("/ab/aba/aba.txt") == null) {
            //test failed
            writer.println("FAIL: grabbing /ab/aba/aba.txt from TEST.jar failed");
            passed = false;
        } else {
            writer.println("SUCCESS: grabbing /ab/aba/aba.txt from TEST.jar passed");
        }

        //get a ArtifactContainer 3 folders deep in root
        ArtifactContainer aJar = null;
        if (rootLooseArchiveContainer.getEntry("/META-INF/b/bb/a.jar") == null) {
            //test failed
            writer.println("FAIL: unable to grab /META-INF/b/bb/a.jar from root");
            passed = false;
        } else {
            aJar = rootLooseArchiveContainer.getEntry("/META-INF/b/bb/a.jar").convertToContainer();
            if (aJar != null) {
                writer.println("SUCCESS: was able to grab and convert /META-INF/b/bb/a.jar from root");
            } else {
                //test failed
                writer.println("FAIL: unable to convert /META-INF/b/bb/a.jar from root into a ArtifactContainer");
                passed = false;
            }
        }

        //get a file from a ArtifactContainer in root
        if (aJar == null || aJar.getEntry("/ab/aba/aba.txt") == null) {
            //test failed
            writer.println("FAIL: couldn't grab /ab/aba/aba.txt from a.jar ArtifactContainer");
            passed = false;
        } else {
            writer.println("SUCCESS: was able to grab /ab/aba/aba.txt from a.jar");
        }

        //get a file deep in a path in root
        if (rootLooseArchiveContainer.getEntry("/META-INF/b/ba/baa/baa1.txt") == null) {
            //test failed
            writer.println("FAIL: unable to grab /META-INF/b/ba/baa/baa1.txt from root");
            passed = false;
        } else {
            writer.println("SUCCESS: was able to grab /META-INF/b/ba/baa/baa1.txt from root");
        }

        //ensure a file deep in a path in root is excluded
        if (rootLooseArchiveContainer.getEntry("/META-INF/a/ab/aba/aba.txt") != null) {
            //test failed
            writer.println("FAIL: able to grab /META-INF/a/ab/aba/aba.txt from root when it should be excluded");
            passed = false;
        } else {
            writer.println("SUCCESS: was unable to grab /META-INF/a/ab/aba/aba.txt from root when it is excluded");
        }

        //ensure a file deep in a path in root which shouldn't be excluded is there as it is similar to one which is excluded
        if (rootLooseArchiveContainer.getEntry("/META-INF/b/ba/baa/baa1.txt") == null) {
            //test failed
            writer.println("FAIL: unable to grab /META-INF/b/ba/baa/baa1.txt from root when it shouldn't be excluded");
            passed = false;
        } else {
            writer.println("SUCCESS: was able to grab /META-INF/b/ba/baa/baa1.txt from root (it shouldn't be excluded but is similar to one which is)");
        }

        //get a ArtifactContainer in a folder in root
        ArtifactContainer testJarNotFromXml = null;
        if (rootLooseArchiveContainer.getEntry("/META-INF/c/a.jar") == null) {
            //test failed
            writer.println("FAIL: unable to grab /META-INF/c/a.jar from root");
            passed = false;
        } else {
            ArtifactEntry e = rootLooseArchiveContainer.getEntry("/META-INF/c/a.jar");
            testJarNotFromXml = e.convertToContainer();
            if (testJarNotFromXml != null &&
                testJarNotFromXml.getEnclosingContainer().getEntry(e.getName()) != null &&
                testJarNotFromXml.getEnclosingContainer().getEntry(e.getName()).getPath().equals(e.getPath())) {
                writer.println("SUCCESS: was able to grab and convert /META-INF/c/a.jar from root");
            } else {
                //test failed
                writer.println("FAIL: unable to grab and convert the correct /META-INF/c/a.jar from root into a ArtifactContainer");
                writer.println("failed because " + testJarNotFromXml.getPath() + " did not start with "
                               + testJarNotFromXml.getEnclosingContainer().getPath());
                passed = false;
            }
        }

        //tests if you can get a file in a folder with the same name as a folder in root but
        //from a ArtifactContainer that is not root (same folder name, different location depth wise)
        if (testJarNotFromXml == null || testJarNotFromXml.getEntry("/META-INF/MANIFEST.MF") == null) {
            //test failed
            writer.println("FAIL: unable to grab /META-INF/MANIFEST.MF from /META-INF/c/a.jar ArtifactContainer");
            passed = false;
        } else {
            writer.println("SUCCESS: able to grab /META-INF/MANIFEST.MF from /META-INF/c/a.jar ArtifactContainer");
        }

        //test that you can get the ArtifactContainer of a ArtifactContainer in root (gets root)
        if (testJarFromXml == null || testJarFromXml.getEnclosingContainer() == null) {
            //test failed
            writer.println("FAIL: was unable to get ArtifactContainer of a ArtifactContainer");
            passed = false;
        } else {
            writer.println("SUCCESS: able to get ArtifactContainer of a ArtifactContainer");
        }

        //get a file deep in a ArtifactContainer in root
        if (testJarNotFromXml == null || testJarNotFromXml.getEntry("/ab/aba/aba.txt") == null) {
            //test failed
            writer.println("FAIL: unable to get /ab/aba/aba.txt from /META-INF/c/a.jar ArtifactContainer");
            passed = false;
        } else {
            writer.println("SUCCESS: able to get /ab/aba/aba.txt from /META-INF/c/a.jar ArtifactContainer");
        }

        //get a folder implied by a child file in a ArtifactContainer in root
        if (testJarNotFromXml == null || testJarNotFromXml.getEntry("/ab/aba") == null) {
            //test failed
            writer.println("FAIL: unable to find /ab/aba folder in /META-INF/c/a.jar ArtifactContainer");
            passed = false;
        } else {
            writer.println("SUCCESS: able to find /ab/aba folder in /META-INF/c/a.jar ArtifactContainer");
        }

        //get a dir in a ArtifactContainer in root in a merged folder path
        if (testJarFromXml == null || testJarFromXml.getEntry("/ab/aba") == null) {
            //test failed
            writer.println("FAIL: unable to get a merged folder /ab/aba from TEST.jar in root");
            passed = false;
        } else {
            writer.println("SUCCESS: able to get a merged folder /ab/aba from TEST.jar in root");
        }

        //get a ArtifactContainer in root
        ArtifactContainer bJar = null;
        if (rootLooseArchiveContainer.getEntry("/META-INF/c/b.jar") == null) {
            //test failed
            writer.println("FAIL: unable to find /META-INF/c/b.jar in root");
            passed = false;
        } else {
            ArtifactEntry e = rootLooseArchiveContainer.getEntry("/META-INF/c/b.jar");

            bJar = rootLooseArchiveContainer.getEntry("/META-INF/c/b.jar").convertToContainer();
            // make sure that path of parent is the start of the path of the current (child)
            if (bJar.getEnclosingContainer().getEntry(e.getName()) != null &&
                bJar.getEnclosingContainer().getEntry(e.getName()).getPath().equals(e.getPath())) {
                writer.println("SUCCESS: able to find and convert /META-INF/c/b.jar in root into a contaner which matches the ArtifactContainer it should be");
            } else {
                //test failed
                writer.println("FAIL: unable to convert /META-INF/c/b.jar in root into a ArtifactContainer which equals the ArtifactContainer it should be");
                passed = false;
            }
        }

        //get a ArtifactContainer in a ArtifactContainer in root
        ArtifactContainer newAJar = null;
        if (bJar == null || bJar.getEntry("/bb/a.jar") == null) {
            //test failed
            writer.println("FAIL: unable to get /bb/a.jar from within /META-INF/c/b.jar ArtifactContainer in root");
            passed = false;
        } else {
            writer.println("SUCCESS: able to get /bb/a.jar from within /META-INF/c/b.jar ArtifactContainer in root");
            newAJar = bJar.getEntry("/bb/a.jar").convertToContainer();
        }

        //test double enclosing ArtifactContainer - parent of parent should be root (checks that it is root)
        if (newAJar == null
            || (newAJar.getEnclosingContainer().getEnclosingContainer() == null)
            && (newAJar.getEnclosingContainer().getEnclosingContainer().isRoot())) {
            //test failed
            writer.println("FAIL: when checking the ArtifactContainer of a ArtifactContainer to get root something didn't work");
            passed = false;
        } else {
            writer.println("SUCCESS: able to confirm the ArtifactContainer of a conteiner which should be root is, in fact, root.");
        }

        //find a folder dictated in xml
        if (rootLooseArchiveContainer.getEntry("/META-INF") == null) {
            //test failed
            writer.println("FAIL: unable to find /META-INF in root");
            passed = false;
        } else {
            if (rootLooseArchiveContainer.getEntry("/META-INF").convertToContainer().getEnclosingContainer().getEnclosingContainer() == null) {
                writer.println("SUCCESS: was able to confirm getting the ArtifactContainer of root gave null, and not some mystery root of root (this test comes from a file in root to get root)");
            } else {
                //test failed
                writer.println("FAIL: when trying to get the ArtifactContainer of root we got something other than null - this should not happen.(this test comes from a file in root to get root)");
                passed = false;
            }
        }

        //find a file in a ArtifactContainer in a ArtifactContainer in root (deep ArtifactContainer encapsulation test)
        if (newAJar == null || newAJar.getEntry("/ab/aba/aba.txt") == null) {
            //test failed
            writer.println("FAIL: unable to grab a /ab/aba/aba.txt from /bb/a.jar ArtifactContainer in the /META-INF/c/b.jar ArtifactContainer in root");
            passed = false;
        } else {
            writer.println("SUCCESS: able to grab a /ab/aba/aba.txt from /bb/a.jar ArtifactContainer in the /META-INF/c/b.jar ArtifactContainer in root");
        }

        //test fail case - getting enclosing ArtifactContainer of root (which should not exist)
        if (rootLooseArchiveContainer.getEnclosingContainer() != null) {
            //test failed
            writer.println("FAIL: when trying to get the ArtifactContainer of root we got something other than null");
            passed = false;
        } else {
            writer.println("SUCCESS: was able to confirm getting the ArtifactContainer of root gave null");
        }

        //test relative path check
        if (rootLooseArchiveContainer.getEntry("META-INF") == null) {
            //test failed
            writer.println("FAIL: was unable to get META-INF without any slashes in request path from root");
            passed = false;
        } else {
            writer.println("SUCCESS: able to get META-INF without any slashes in request path from root");
        }

        ArtifactContainer myUtilityJar = null;
        //grab myutility from within webapp.war so next test can test the final type of exclude is working
        if (webAppWarFromXml.getEntry("/WEB-INF/lib/myutility.jar") == null) {
            //test failed
            writer.println("FAIL: unable to get a handle of /WEB-INF/lib/myutility.jar from webApp.war ArtifactContainer");
            passed = false;
        } else {
            myUtilityJar = webAppWarFromXml.getEntry("/WEB-INF/lib/myutility.jar").convertToContainer();
            if (myUtilityJar != null) {
                writer.println("SUCCESS: grab /WEB-INF/lib/myutility.jar in webApp.war in root passed");
            } else {
                //test failed
                writer.println("FAIL: unable to convert a grabbed /WEB-INF/lib/myutility.jar from webApp.war in root into a ArtifactContainer");
                passed = false;
            }
        }

        //Check that the txt file in my utility is excluded.
        if (myUtilityJar == null || myUtilityJar.getEntry("/b/ba/baa/baa2.txt") != null) {
            //test failed
            writer.println("FAIL: able to grab the exclued file /b/ba/baa/baa2.txt from myutility.jar ArtifactContainer in the webApp.war ArtifactContainer in root");
            passed = false;
        } else {
            writer.println("SUCCESS: unable to grab the excluded file /b/ba/baa/baa2.txt from myutility.jar ArtifactContainer in the webApp.war ArtifactContainer in root");
        }

        //check that we can find the .jar file in my utility, to make sure only txt files are gone
        if (myUtilityJar == null || myUtilityJar.getEntry("/b/bb/a.jar") == null) {
            //test failed
            writer.println("FAIL: unable to grab /b/bb/a.jar from myutility.jar ArtifactContainer in the webApp.war ArtifactContainer in root");
            passed = false;
        } else {
            writer.println("SUCCESS: able to grab /b/bb/a.jar from myutility.jar ArtifactContainer in the webApp.war ArtifactContainer in root");
        }

        //test that the file /a.txt is the first file we hit
        ArtifactEntry aFile = testJarFromXml.getEntry("a.txt");
        if (aFile != null) {
            try {
                InputStream stream = aFile.getInputStream();
                if (stream != null) {
                    Writer stringWriter = new StringWriter();
                    char[] buffer = new char[1024];
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                        int n;
                        while ((n = reader.read(buffer)) != -1) {
                            stringWriter.write(buffer, 0, n);
                        }
                    } finally {
                        stream.close();
                    }
                    if (stringWriter.toString().contains("xml content is present.")) {
                        writer.println("SUCCESS: was able to confirm that the first TEST.jar/a.txt declared was the one returned");
                    } else {
                        writer.println("FAIL: the TEST.jar/a.txt file in root did not have the contents we were expecting. We got: '" + stringWriter.toString() + "'");
                        passed = false;
                    }
                } else {
                    writer.println("FAIL: the TEST.jar/a.txt file in root did not provide us an input stream");
                    passed = false;
                }
            } catch (IOException e) {
                writer.println("FAIL: we hit an IOE when trying to get an input stream on TEST.jar/a.txt");
                passed = false;
            }
        } else {
            writer.println("FAIL: we couldn't find TEST.jar/a.txt in virtual file system.");
            passed = false;
        }

        // Try getting a dir registered with a "/" on the end but without putting  the "/" on the end
        ArtifactEntry withSlash = rootLooseArchiveContainer.getEntry("/withSlash");
        if (withSlash == null) {
            writer.println("FAIL: we couldn't find /withSlash/ when asking for /withSlash");
            passed = false;
        } else {
            writer.println("SUCCESS: we found /withSlash/ when asking for /withSlash");
            ArtifactContainer withSlashContainer = withSlash.convertToContainer();
            if (withSlashContainer == null) {
                writer.println("FAIL: we couldn't convert /withSlash/ to a ArtifactContainer");
                passed = false;
            } else {
                Iterator<ArtifactEntry> withSlashIterator = withSlashContainer.iterator();
                if (!withSlashIterator.hasNext()) {
                    writer.println("FAIL: No content provided when loading dir register as /withSlash/ when asking for /withSlash");
                    passed = false;
                } else {
                    boolean foundA = false;
                    boolean foundB = false;
                    boolean foundC = false;
                    while (withSlashIterator.hasNext()) {
                        ArtifactEntry ArtifactEntry = withSlashIterator.next();
                        if ("/withSlash/a".equals(ArtifactEntry.getPath())) {
                            foundA = true;
                        } else if ("/withSlash/b".equals(ArtifactEntry.getPath())) {
                            foundB = true;
                        } else if ("/withSlash/c".equals(ArtifactEntry.getPath())) {
                            foundC = true;
                        } else {
                            passed = false;
                            writer.println("FAIL: Unexpected ArtifactEntry found when iteratorating through the content of /withSlash/: " + ArtifactEntry.getPath());
                        }
                    }
                    if (!foundA) {
                        passed = false;
                        writer.println("FAIL: Unable to find /withSlash/a in the iterator for /withSlash");
                    }
                    if (!foundB) {
                        passed = false;
                        writer.println("FAIL: Unable to find /withSlash/b in the iterator for /withSlash");
                    }
                    if (!foundC) {
                        passed = false;
                        writer.println("FAIL: Unable to find /withSlash/c in the iterator for /withSlash");
                    }
                }
            }
        }

        return passed;
    }

    /**
     * Tests that the URLs for /META-INF/b/ba/baa/baa1.txt and /myjar.jar can be loaded
     * 
     * @param rootLooseArchiveContainer
     * @param writer
     */
    private void testGetResourceForLooseEntry(ArtifactContainer rootLooseArchiveContainer, PrintWriter writer) {
        // Make sure we can get URIs for a file in a directory and one registered directly as a file
        boolean passed = testEntryUrl(rootLooseArchiveContainer, "/META-INF/b/ba/baa/baa1.txt", "TESTDATA/b/ba/baa/baa1.txt", writer);
        passed = passed & testEntryUrl(rootLooseArchiveContainer, "/myjar.jar", "TEST.JAR", writer);
        if (passed) {
            writer.println("PASS: testGetResourceForLooseEntry");
        } else {
            writer.println("FAIL: testGetResourceForLooseEntry");
        }
    }

    /**
     * Test to make sure that the GetResource method returns the right thing for the zip and file implementations
     * 
     * @param ArtifactContainer
     * @param writer
     */
    private void testGetResourceForZipAndFile(ArtifactContainerFactory factory, String dir, PrintWriter writer) {
        boolean passed = true;

        File dirFile = new File(dir);
        try {
            ArtifactContainer ArtifactContainer = getContainerForDirectory();
            passed = passed & testEntryUrl(ArtifactContainer, "/a/a.txt", "TESTDATA/a/a.txt", writer);

            // Now try a JAR file
            ArtifactEntry jarEntry = ArtifactContainer.getEntry("/c/b.jar");
            ArtifactContainer jarContainer = jarEntry.convertToContainer();
            String jarFileUri = dirFile.toURI().toString() + "c/b.jar";
            passed = passed & testEntryUrl(jarContainer, "/ba/baa/baa1.txt", "jar:" + jarFileUri + "!/ba/baa/baa1.txt", writer);
        } catch (Throwable t) {
            passed = false;
            writer.println("FAIL: Exception thrown in testGetResourceForZipAndFile, trace is:");
            t.printStackTrace(writer);
        }
        if (passed) {
            writer.println("PASS: testGetResourceForZipAndFile");
        }
    }

    private boolean testEntryUrl(ArtifactContainer ArtifactContainer, String path, String expectedEnding, PrintWriter writer) {
        boolean passed = true;
        try {
            ArtifactEntry baaEntry = ArtifactContainer.getEntry(path);
            URL baaUrl = baaEntry.getResource();
            if (!baaUrl.toString().endsWith(expectedEnding)) {
                passed = false;
                writer.println("FAIL: The URL for the ArtifactEntry " + path + " was wrong, epected:\t" + expectedEnding + "\tgot\t" + baaUrl);
            } else {
                writer.println("SUCCESS: Have found the URL for " + path);
            }
        } catch (Throwable t) {
            passed = false;
            writer.println("FAIL: Exception when looking for the URL for " + path + ", text is:");
            t.printStackTrace(writer);
        }
        return passed;
    }

    private boolean testUriFromXmlParsing(ArtifactContainer rootLooseArchiveContainer, PrintWriter writer) {
        boolean passed = true;
        // Test that we can get URIs for various paths in the archive, start with the root for the archive which shouldn't have any entries
        Collection<URL> rootUrls = rootLooseArchiveContainer.getURLs();
        if (rootUrls == null) {
            //test failed
            writer.println("FAIL: null returned for the root URIs for the root loose archive");
            passed = false;
        } else {
            /*
             * There are no directories that map to the root of the archive so the URIs should be empty
             */
            if (rootUrls.isEmpty()) {
                writer.println("SUCCESS: no URIs returned for the root loose archive");
            } else {
                writer.println("FAIL: the following URIs were returned for the root of the loose archive: " + rootUrls.toString());
            }
        }

        // Now try getting the URIs for a ArtifactContainer
        if (!testGetURI(rootLooseArchiveContainer, "/META-INF", Collections.singleton("TESTDATA/"), writer, true)) {
            passed = false;
        }

        // Nested ArtifactContainer
        if (!testGetURI(rootLooseArchiveContainer, "/META-INF/b/ba", Collections.singleton("TESTDATA/b/ba/"), writer, true)) {
            passed = false;
        }

        // Nested archive
        Set<String> expectedPathsForTestJar = new HashSet<String>();
        expectedPathsForTestJar.add("TESTDATA/a/");
        expectedPathsForTestJar.add("TESTDATA/a/aa/");
        expectedPathsForTestJar.add("xmlContentTestData/a/");
        if (!testGetURI(rootLooseArchiveContainer, "/TEST.jar", expectedPathsForTestJar, writer, true)) {
            passed = false;
        }

        // File
        if (!testGetURI(rootLooseArchiveContainer, "/myjar.jar", Collections.singleton("TEST.JAR"), writer, false)) {
            passed = false;
        }

        return passed;
    }

    /**
     * This method will test that the {@link ArtifactContainer#getUri()} method will return the expected URIs for cetain ArtifactContainers. It finds the ArtifactContainer at the
     * give <code>path</code>
     * within the <code>rootArchive</code> (or using the <code>rootArchive</code> itself if <code>path</code> is <code>null</code>) then it calls getURI and compares the end of the
     * returned URIs against the <code>expectedEndStrings</code>.
     * 
     * @param rootArchive The root archive to get the ArtifactContainer from or the ArtifactContainer itself
     * @param path The path to the ArtifactContainer or <code>null</code> if the rootArchive should be used as the ArtifactContainer
     * @param expectedEndStrings The expected end parts of the URIs to compare with
     * @param writer The writer to write error and success messages to
     * @param testFile <code>true</code> if this should try to create a File object for each of the URIs
     * @return <code>true</code> if everything was present as expected
     */
    private boolean testGetURI(ArtifactContainer rootArchive, String path, Set<String> expectedEndStrings, PrintWriter writer, boolean testFile) {
        boolean passed = true;
        if (path != null && rootArchive.getEntry(path) == null) {
            //test failed
            writer.println("FAIL: unable to find " + path + " in root so can't test getURI on it");
            passed = false;
        } else {
            ArtifactContainer ArtifactContainer = (path == null) ? rootArchive : rootArchive.getEntry(path).convertToContainer();
            if (ArtifactContainer == null) {
                writer.println("FAIL: unable to convert " + path + " into a ArtifactContainer");
                passed = false;
            } else {
                Collection<URL> urls = ArtifactContainer.getURLs();
                if (urls == null) {
                    //test failed
                    writer.println("FAIL: null returned for the " + path + " URIs");
                    passed = false;
                } else {
                    // There should only be one
                    if (urls.size() != expectedEndStrings.size()) {
                        writer.println("FAIL: The wrong number of URIs returned for " + path + ": " + urls.toString());
                        passed = false;
                    } else {
                        // Now make sure the expected URIs are all there, make a copy of the expected strings and then remove them as they are found so the set should end up with no entries 
                        Set<String> missingStrings = new HashSet<String>(expectedEndStrings);
                        for (URL url : urls) {
                            String urlString = url.toString();
                            boolean expectedUrl = false;
                            Iterator<String> expectedStringIterator = missingStrings.iterator();
                            while (expectedStringIterator.hasNext()) {
                                String expectedString = expectedStringIterator.next();
                                if (urlString.endsWith(expectedString)) {
                                    expectedStringIterator.remove();
                                    expectedUrl = true;
                                    break;
                                }
                            }
                            if (!expectedUrl) {
                                writer.println("FAIL: The URL " + urlString + " for path " + path + " was not expected");
                                passed = false;
                            } else {
                                if (testFile) {
                                    // Make sure the URI points to a valid location (we can't do this for things inside a jar hence the boolean param)

                                    try {
                                        File file = new File(url.toURI());
                                        if (!file.exists()) {
                                            writer.println("FAIL: The URL: " + urlString + " in the ArtifactContainer " + path + " does not point to a file");
                                            passed = false;
                                        }

                                    } catch (URISyntaxException e) {
                                        writer.println("FAIL: The URL: " + urlString + " in the ArtifactContainer " + path + " failed conversion. " + e);
                                        passed = false;
                                    }
                                }
                            }
                        }

                        // If there are still strings left in the missingStrings then they weren't found
                        if (missingStrings.size() > 0) {
                            writer.println("FAIL: The URIs " + missingStrings.toString() + " in the " + path + " were expected but not found");
                            passed = false;
                        }
                    }
                }
            }
        }

        // Put out a success method if necessary
        if (passed) {
            writer.println("SUCCESS: All of the expected URIs were returned for " + path);
        }
        return passed;
    }

    private void testUnableToAdapt(PrintWriter writer) throws UnableToAdaptException {
        ArtifactContainer container = getContainerForDirectory();

        Container overlayContainer = amf.getContainer( new File(cacheDirAdapt), new File(cacheDirOverlay), container );
        if ( overlayContainer == null ) {
            writer.println("FAIL: Failed to obtain overlay container");
            return;
        }

        Entry aaEntry = overlayContainer.getEntry("/a/aa");
        Entry abEntry = overlayContainer.getEntry("/a/ab");
        Entry bbEntry = overlayContainer.getEntry("/b/bb");

        if ( (aaEntry == null) || (abEntry == null) || (bbEntry == null) ) {
            writer.println("FAIL: Failed to obtain test entries");
            return;
        }

        Container aaContainer = aaEntry.adapt(Container.class);
        Container abContainer = abEntry.adapt(Container.class);
        Container bbContainer = bbEntry.adapt(Container.class);

        if  ( (aaContainer == null) || (abContainer == null) || (bbContainer == null) ) {
            writer.println("FAIL: Failed to obtain test containers");
            return;
        }

        // This test tests adapter sequencing by service rank, and checks that if an adapter returns
        // null, that the next one is invoked, and if an adapter throws the exception, that control returns
        //immediately, and subsequent adapters are not called.

        // Reference test data.
        //   aa entry gave 10.0
        //   ab entry gave NODE:/a#/a/ab with java.io.FileNotFoundException
        //   bb entry gave 999.0
        //   aa container gave 10.0
        //   ab container gave NODE:/a/ab#/a/ab with java.io.FileNotFoundException
        //   bb container gave 999.0

        Float ten = new Float(10.0);
        Float nineninenine = new Float(999.0);

        // ok.. setup done.. lets try some tests..

        boolean pass = true;

        try {
            Float f = aaEntry.adapt(Float.class);
            if ( f == null ) {
                writer.println("FAIL: Incorrect adapter invoked for entry aa, expected " + ten + " got null");
                pass = false;
            } else if (!f.equals(ten)) {
                writer.println("FAIL: Incorrect adapter invoked for entry aa, expected " + ten + " got " + f);
                pass = false;
            }
        } catch ( UnableToAdaptException e ) {
            writer.println("FAIL: aa entry threw UnableToAdaptException " + e.getMessage() + " with " + e.getCause().getClass().getName());
            pass = false;
        }

        try {
            Float f = abEntry.adapt(Float.class);
            writer.println("FAIL: ab entry gave " + f + " expected exception");
            pass = false;
        } catch ( UnableToAdaptException e ) {
            //expected.
        }

        try {
            Float f = bbEntry.adapt(Float.class);
            if ( f == null ) {
                writer.println("FAIL: Incorrect adapter invoked for entry bb, expected " + nineninenine + " got null");
                pass = false;
            } else if (!f.equals(nineninenine)) {
                writer.println("FAIL: Incorrect adapter invoked for entry bb expected " + nineninenine + " got " + f);
                pass = false;
            }
        } catch ( UnableToAdaptException e ) {
            writer.println("FAIL: bb entry threw UnableToAdaptException " + e.getMessage() + " with " + e.getCause().getClass().getName());
            pass = false;
        }

        try {
            Float f = aaContainer.adapt(Float.class);
            if ( f == null ) {
                writer.println("FAIL: Incorrect adapter invoked for entry aa, expected " + ten + " got null");
                pass = false;
            } else if (!f.equals(ten)) {
                writer.println("FAIL: Incorrect adapter invoked for container aa, expected " + ten + " got " + f);
                pass = false;
            }
        } catch ( UnableToAdaptException e ) {
            writer.println("FAIL: aa container threw UnableToAdaptException " + e.getMessage() + " with " + e.getCause().getClass().getName());
            pass = false;
        }

        try {
            Float f = abContainer.adapt(Float.class);
            writer.println("FAIL: ab container gave " + f + " expected exception");
            pass = false;
        } catch ( UnableToAdaptException e ) {
            //expected.
        }

        try {
            Float f = bbContainer.adapt(Float.class);
            if ( f == null ) {
                writer.println("FAIL: Incorrect adapter invoked for entry bb, expected " + nineninenine + " got null");
                pass = false;
            } else if (!f.equals(nineninenine)) {
                writer.println("FAIL: Incorrect adapter invoked for container bb, expected " + nineninenine + " got " + f);
                pass = false;
            }
        } catch ( UnableToAdaptException e ) {
            writer.println("FAIL: aa container threw UnableToAdaptException " + e.getMessage() + " with " + e.getCause().getClass().getName());
            pass = false;
        }

        if ( pass ) {
            writer.println("PASS: adapter sequencing, and unable to adapt tests passed");
        }
    }

    private void testAdaptableApiSimple(PrintWriter writer) throws UnableToAdaptException {
        ArtifactContainer artifactContainer = getContainerForDirectory();

        com.ibm.wsspi.adaptable.module.Container adaptableContainer =
            amf.getContainer(new File(cacheDirAdapt), new File(cacheDirOverlay), artifactContainer);
        if ( adaptableContainer == null ) {
            writer.println("FAIL: Failed to create adaptable container");
            return;
        }

        com.ibm.wsspi.adaptable.module.Entry adaptEntry =
            adaptableContainer.getEntry("/a/a.txt");
        if ( adaptEntry == null ) {
            writer.println("FAIL: Failed to obtain entry [ /a/a.txt ]");
            return;
        }

        Integer entryInteger = adaptEntry.adapt(Integer.class);
        if ( entryInteger == null ) {
            writer.println("FAIL: Failed to adapt entry to Integer");
            return;
        }

        if ( entryInteger.intValue() != adaptEntry.getName().length()) {
            writer.println("FAIL: Adapted value [ " + entryInteger + " ]" +
                            " should be [ " + Integer.valueOf(adaptEntry.getName().length()) + " ]");
            return;
        }

        com.ibm.wsspi.adaptable.module.Container adaptParent =
            adaptEntry.getEnclosingContainer();
        if ( adaptParent == null ) {
            writer.println("FAIL: Failed to obtain adaptable parent");
            return;
        }

        Integer containerInteger = adaptParent.adapt(Integer.class);
        if ( containerInteger == null ) {
            writer.println("FAIL: Failed to adapt container to Integer");
            return;
        }

        if ( containerInteger.intValue() != adaptParent.getPath().length() ) {
            writer.println("FAIL: Adapted value [ " + containerInteger + " ]" +
                            " should be [ " + Integer.valueOf(adaptParent.getName().length()) + " ]");
            return;
        }


        writer.println("PASS");
    }

    private class RelocatedEntry implements ArtifactEntry {
        ArtifactEntry delegate;
        String name;
        String path;

        RelocatedEntry(ArtifactEntry e, String newName, String newPath) {
            this.delegate = e;
            this.name = newName;
            this.path = newPath;
        }

        /** {@inheritDoc} */
        @Override
        public ArtifactContainer convertToContainer() {
            return delegate.convertToContainer();
        }

        /** {@inheritDoc} */
        @Override
        public ArtifactContainer convertToContainer(boolean localOnly) {
            return delegate.convertToContainer(localOnly);
        }

        /** {@inheritDoc} */
        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        /** {@inheritDoc} */
        @Override
        public ArtifactContainer getEnclosingContainer() {
            //this really doesn't matter.. the overlay won't use it.
            return delegate.getEnclosingContainer();
        }

        /** {@inheritDoc} */
        @Override
        public String getPath() {
            return path;
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public ArtifactContainer getRoot() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public long getLastModified() {
            return delegate.getLastModified();
        }

        /** {@inheritDoc} */
        @Override
        public URL getResource() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public String getPhysicalPath() {
            return null;
        }

    }

    private boolean testOverlayNavigation(OverlayContainer oc, ArtifactContainer c, PrintWriter writer) {
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: initial overlaid set was not empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: initial masked set was not empty as expected");
            return false;
        }
        ArtifactEntry aa = c.getEntry("/a/aa");
        if (aa == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry aa from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        ArtifactEntry ab = c.getEntry("/a/ab");
        if (ab == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry ab from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        //add ac using old approach to proxying the location
        ArtifactEntry raa = new RelocatedEntry(aa, "ac", "/a/ac");
        oc.addToOverlay(raa);

        //add ad using new approach to proxying location.
        oc.addToOverlay(ab, "/a/ad", false);

        //non overlaid retrieval test
        ArtifactEntry aaTxt = oc.getEntry("/a/aa/aa.txt");
        if (aaTxt == null) {
            writer.println("FAIL: unable to obtain /a/aa/aa.txt from overlay");
            return false;
        }

        //overlaid retrieval
        ArtifactEntry acTxt = oc.getEntry("/a/ac/aa.txt");
        if (acTxt == null) {
            writer.println("FAIL: unable to obtain /a/ac/aa.txt from overlay");
            return false;
        }

        //navigation walk from overlaid ArtifactEntry back to non overlaid node.
        ArtifactContainer acParentC = acTxt.getEnclosingContainer();
        if (acParentC == null) {
            writer.println("FAIL: unable to obtain parent for virtual ArtifactEntry /a/ac/aa.txt");
            return false;
        }
        if (!acParentC.getName().equals("ac")) {
            writer.println("FAIL: parent for virtual ArtifactEntry /a/ac/aa.txt had incorrect name, expected ac, found " + acParentC.getName());
            return false;
        }
        if (!acParentC.getPath().equals("/a/ac")) {
            writer.println("FAIL: parent for virtual ArtifactEntry /a/ac/aa.txt had incorrect path, expected /a/ac, found " + acParentC.getPath());
            return false;
        }
        ArtifactContainer ac = acParentC.getEnclosingContainer();
        if (ac == null) {
            writer.println("FAIL: unable to obtain navigated parent for virtual ArtifactEntry /a/ac");
            return false;
        }
        if (!ac.getName().equals("a")) {
            writer.println("FAIL: navigated parent for virtual ArtifactEntry /a/ac had incorrect name, expected a, found " + ac.getName());
            return false;
        }
        if (!ac.getPath().equals("/a")) {
            writer.println("FAIL: navigated parent for virtual ArtifactEntry /a/ac had incorrect path, expected /a, found " + ac.getPath());
            return false;
        }

        //getEntry query from overlaid node to non-overlaid node.
        ArtifactEntry aaViaOverlaid = acParentC.getEntry("/a/aa");
        if (aaViaOverlaid == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry /a/aa via overlay from /a/ac");
            return false;
        }
        if (!aaViaOverlaid.getName().equals("aa")) {
            writer.println("FAIL: located ArtifactEntry /a/aa had incorrect name, expected aa, found " + aaViaOverlaid.getName());
            return false;
        }
        if (!aaViaOverlaid.getPath().equals("/a/aa")) {
            writer.println("FAIL: located ArtifactEntry /a/aa had incorrect path, expected /a/aa, found " + aaViaOverlaid.getPath());
            return false;
        }

        //getEntry query from overlaid node to overlaid node.
        ArtifactEntry adViaOverlaid = acParentC.getEntry("/a/ad");
        if (adViaOverlaid == null) {
            writer.println("FAIL: unable to obtain virtual ArtifactEntry /a/ad via overlay from /a/ac");
            return false;
        }
        if (!adViaOverlaid.getName().equals("ad")) {
            writer.println("FAIL: located ArtifactEntry /a/ad had incorrect name, expected ad, found " + adViaOverlaid.getName());
            return false;
        }
        if (!adViaOverlaid.getPath().equals("/a/ad")) {
            writer.println("FAIL: located ArtifactEntry /a/ad had incorrect path, expected /a/ad, found " + adViaOverlaid.getPath());
            return false;
        }

        //retrieve an overlaid nested archive & open it & navigate upwards..
        ArtifactEntry nestedUnderOverlay = oc.getEntry("/c/b.jar");
        if (nestedUnderOverlay == null) {
            writer.println("FAIL: unable to locate /c/b.jar in the overlay");
            return false;
        }

        ArtifactContainer nestedOpened = nestedUnderOverlay.convertToContainer();
        if (nestedOpened == null) {
            writer.println("FAIL: unable to open /c/b.jar in the overlay as a ArtifactContainer");
            return false;
        }

        nestedOpened.useFastMode();

        try {
            if (nestedOpened.getEnclosingContainer().getEntry("b.jar") == null) {
                writer.println("FAIL: couldn't find self in parent for nested fs under overlay");
                return false;
            }

            //now for a nested nested..
            ArtifactEntry nestedNestedUnderOverlay = nestedOpened.getEntry("/bb/a.jar");
            if (nestedNestedUnderOverlay == null) {
                writer.println("FAIL: unable to locate /bb/a.jar in nested b.jar in the overlay");
                return false;
            }

            ArtifactContainer nestedNestedOpened = nestedNestedUnderOverlay.convertToContainer();
            if (nestedNestedOpened == null) {
                writer.println("FAIL: unable to open /bb/a.jar in nested b.jar in the overlay as a ArtifactContainer");
                return false;
            }

            nestedNestedOpened.useFastMode();

            try {
                if (nestedNestedOpened.getEnclosingContainer().getEntry("a.jar") == null) {
                    writer.println("FAIL: couldn't find self in parent for nested nested fs under overlay");
                    return false;
                }

            } finally {
                nestedNestedOpened.stopUsingFastMode();
            }

        } finally {
            nestedOpened.stopUsingFastMode();
        }

        return true;
    }

    private boolean testOverlayEntrySetManipulation(OverlayContainer oc, ArtifactContainer c, PrintWriter writer) {
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: initial overlaid set was not empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: initial masked set was not empty as expected");
            return false;
        }
        ArtifactEntry aa = c.getEntry("/a/aa");
        if (aa == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry aa from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        ArtifactEntry ab = c.getEntry("/a/ab");
        if (ab == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry ab from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        oc.addToOverlay(aa);
        oc.addToOverlay(ab);

        List<String> wanted = Arrays.asList(new String[] { "/a/aa", "/a/ab", "/a/aa/aa.txt", "/a/ab/ab.txt", "/a/ab/aba", "/a/ab/aba/aba.txt" });

        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: masked set was not empty as expected");
            return false;
        }
        if (!oc.getOverlaidPaths().containsAll(wanted)) {
            writer.println("FAIL: overlaid set did not contain updated values expected " + wanted + " got " + oc.getOverlaidPaths());
            return false;
        }
        oc.removeFromOverlay("/a/aa");
        wanted = Arrays.asList(new String[] { "/a/ab", "/a/ab/ab.txt", "/a/ab/aba", "/a/ab/aba/aba.txt" });
        List<String> notWanted = Arrays.asList(new String[] { "/a/aa", "/a/aa/aa.txt" });
        for (String w : notWanted) {
            if (oc.getOverlaidPaths().contains(w)) {
                writer.println("FAIL: overlaid contain unwanted value " + w + " got " + oc.getOverlaidPaths());
                return false;
            }
        }
        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: masked set was not still empty as expected");
            return false;
        }
        if (!oc.getOverlaidPaths().containsAll(wanted)) {
            writer.println("FAIL: masked set did not contain remaining values expected " + wanted + " got " + oc.getOverlaidPaths());
            return false;
        }
        oc.removeFromOverlay("/a/ab");
        notWanted = Arrays.asList(new String[] { "/a/ab", "/a/ab/ab.txt", "/a/ab/aba", "/a/ab/aba/aba.txt", "/a/aa", "/a/aa/aa.txt" });
        for (String w : notWanted) {
            if (oc.getOverlaidPaths().contains(w)) {
                writer.println("FAIL: overlaid set still contained unexpected item " + w);
                return false;
            }
        }

        for (String s : oc.getOverlaidPaths()) {
            oc.removeFromOverlay(s);
        }

        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: masked set was not empty as expected");
            return false;
        }
        return true;
    }

    private boolean testOverlayMaskSetManipulation(OverlayContainer oc, PrintWriter writer) {
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: initial overlaid set was not empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: initial masked set was not empty as expected");
            return false;
        }
        oc.mask("/mask/one");
        oc.mask("/mask/two");
        if (oc.getMaskedPaths().size() != 2) {
            writer.println("FAIL: masked set did not grow as expected, wanted size 2, got " + oc.getMaskedPaths().size());
            return false;
        }
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: overlaid set was not empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().containsAll(Arrays.asList(new String[] { "/mask/one", "/mask/two" }))) {
            writer.println("FAIL: masked set did not contain updated values");
            return false;
        }
        oc.unMask("/mask/one");
        if (oc.getMaskedPaths().size() != 1) {
            writer.println("FAIL: masked set did not shrink as expected");
            return false;
        }
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: overlaid set was not still empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().containsAll(Arrays.asList(new String[] { "/mask/two" }))) {
            writer.println("FAIL: masked set did not contain remaining value");
            return false;
        }
        oc.unMask("/mask/two");
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: overlaid set not empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: masked set was not empty as expected");
            return false;
        }
        return true;
    }

    private boolean testOverlayUsingOtherEntries(OverlayContainer oc, ArtifactContainer c, PrintWriter writer) {
        if (!oc.getOverlaidPaths().isEmpty()) {
            writer.println("FAIL: initial overlaid set was not empty as expected");
            return false;
        }
        if (!oc.getMaskedPaths().isEmpty()) {
            writer.println("FAIL: initial masked set was not empty as expected");
            return false;
        }
        ArtifactEntry aa = c.getEntry("/a/aa");
        if (aa == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry aa from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        ArtifactEntry ab = c.getEntry("/a/ab");
        if (ab == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry ab from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }

        ArtifactEntry rab = new RelocatedEntry(ab, "ad", "/a/ad");
        oc.addToOverlay(rab);

        oc.addToOverlay(aa, "/a/ac", false);

        List<String> wanted = Arrays.asList(new String[] { "/a/ac", "/a/ad", "/a/ac/aa.txt", "/a/ad/ab.txt", "/a/ad/aba", "/a/ad/aba/aba.txt" });
        //using < because the overlay may have added additional paths, like /a depending on it's implementation.
        //(/a may be added because its a parent of the overlaid paths..)
        if (oc.getOverlaidPaths().size() < wanted.size()) {
            writer.println("FAIL: overlaid set did not grow as expected (" + oc.getOverlaidPaths().size() + "/" + wanted.size() + ") with relocated entries: wanted " + wanted
                           + " got:" + oc.getOverlaidPaths());
            return false;
        }

        ArtifactEntry a = oc.getEntry("/a");
        if (a == null) {
            writer.println("FAIL: unable to obtain /a from overlay");
            return false;
        }
        ArtifactContainer ac = a.convertToContainer();
        if (ac == null) {
            writer.println("FAIL: unable to obtain dir from /a from overlay");
            return false;
        }
        Set<String> expected = new HashSet<String>(Arrays.asList(new String[] { "aa", "ab", "ac", "ad", "a.txt" }));
        Set<String> seen = new HashSet<String>();
        for (ArtifactEntry e : ac) {
            seen.add(e.getName());
        }
        if (expected.size() != seen.size() || !seen.containsAll(expected)) {
            writer.println("FAIL: virtual nodes were not located via iterator of parent node in overlay, "
                           + "expected " + expected + " saw " + seen);
            return false;
        }
        ArtifactEntry o1 = oc.getEntry("/a/ac");
        if (o1 == null) {
            writer.println("FAIL: unable to obtain /a/ac from overlay");
            return false;
        }
        ArtifactEntry o2 = oc.getEntry("/a/ad");
        if (o2 == null) {
            writer.println("FAIL: unable to obtain /a/ad from overlay");
            return false;
        }
        ArtifactEntry o3 = ac.getEntry("/a/ac");
        if (o3 == null) {
            writer.println("FAIL: unable to obtain /a/ac from overlay via node using absolute");
            return false;
        }
        ArtifactEntry o4 = ac.getEntry("/a/ad");
        if (o4 == null) {
            writer.println("FAIL: unable to obtain /a/ad from overlay via node using absolute");
            return false;
        }
        ArtifactEntry o5 = ac.getEntry("ac");
        if (o5 == null) {
            writer.println("FAIL: unable to obtain /a/ac from overlay via node using relative");
            return false;
        }
        ArtifactEntry o6 = ac.getEntry("ad");
        if (o6 == null) {
            writer.println("FAIL: unable to obtain /a/ad from overlay via node using relative");
            return false;
        }

        //cleanup..
        oc.removeFromOverlay("/a/ac");
        oc.removeFromOverlay("/a/ad");
        for (String p : oc.getOverlaidPaths()) {
            if (p.startsWith("/a/ac") || p.startsWith("/a/ad"))
                writer.println("FAIL: unable to remove entries added for otherentry test");
            else
                oc.removeFromOverlay(p);
        }

        return true;
    }

    private void recursiveDump(String pad, String root, File f, PrintWriter writer) {
        if (f.isDirectory()) {
            writer.println(pad + f.getAbsolutePath().substring(root.length()) + " [DIR] ");
            File children[] = f.listFiles();
            for (File child : children) {
                recursiveDump(pad, root, child, writer);
            }
        } else {
            writer.println(pad + f.getAbsolutePath().substring(root.length()));
        }
    }

    private void testDirOverlay(PrintWriter writer) {
        ArtifactContainer c = getContainerForDirectory();
        if (c == null) {
            writer.println("FAIL: Unable to obtain ArtifactContainer for overlay ArtifactContainer test (unable to test overlay ArtifactContainer, not an overlay issue)");
            return;
        }
        ArtifactContainer j = getContainerForZip();
        if (j == null) {
            writer.println("FAIL: Unable to obtain 2nd ArtifactContainer for overlay ArtifactContainer test (unable to test overlay ArtifactContainer, not an overlay issue)");
            return;
        }
        OverlayContainer oc = ocf.createOverlay(OverlayContainer.class, c);
        if (oc == null) {
            writer.println("FAIL: Unable to obtain overlay ArtifactContainer via factory");
            return;
        }

        File temp = new File("OVERLAYTEST" + System.currentTimeMillis());
        try {
            if (!temp.mkdirs()) {
                writer.println("FAIL: Unable to create directory for overlay test, not an overlay issue");
            }
            writer.println("using " + temp.getAbsolutePath() + " as the overlay test dir");
            oc.setOverlayDirectory(new File(cacheDirOverlay), temp);

            //the overlay should be in a clean state..
            writer.println("Comparing overlay with dir testdata..");
            ArtifactUtils.compare(oc, DirFileSystem.TESTDATA, writer);

            writer.println("Before : ");
            recursiveDump(" ", temp.getAbsolutePath(), temp, writer);
            writer.println("Overlaid entries before: ");
            for (String s : oc.getOverlaidPaths()) {
                writer.println("  " + s);
            }

            overlayTests(oc, c, j, true, writer);

            writer.println("After : ");
            recursiveDump(" ", temp.getAbsolutePath(), temp, writer);
            writer.println("Overlaid entries after: ");
            for (String s : oc.getOverlaidPaths()) {
                writer.println("  " + s);
            }

            //make sure everything is gone before next step..
            for (String s : oc.getOverlaidPaths()) {
                writer.println("Test removing " + s + " from overlay...");
                oc.removeFromOverlay(s);
            }

            writer.println("After Cleanup : ");
            recursiveDump(" ", temp.getAbsolutePath(), temp, writer);
            writer.println("Overlaid entries after cleanup: ");
            for (String s : oc.getOverlaidPaths()) {
                writer.println("  " + s);
            }

            //the overlay should be back to a clean state..
            writer.println("Comparing overlay with dir testdata after overlay tests");
            ArtifactUtils.compare(oc, DirFileSystem.TESTDATA, writer);

            //test if the overlay sees content already in the dir, even if added later.
            File overlay = new File(temp, ".overlay");
            try {
                File testFile = new File(overlay, "WIBBLE");
                testFile.createNewFile();
                File otherTest = new File(overlay, "WOBBLE/fred/wilma/barney");
                otherTest.mkdirs();
                File otherFile = new File(otherTest, "betty");
                otherFile.createNewFile();
            } catch (IOException io) {
                writer.println("FAIL: unable to create test content into overlay fs");
            }

            FileSystem overlayContent =
                FileSystem.root( null, null, null, false, null, 0, null, 0, new String[] {},
                    FileSystem.File( "WIBBLE", "/WIBBLE", false, null, 0, null, null ),
                    FileSystem.dir( "WOBBLE", "/WOBBLE", null, null, 0, new String[] {},
                        FileSystem.dir( "fred", "/WOBBLE/fred", null, null, 0, new String[] {},
                            FileSystem.dir( "wilma", "/WOBBLE/fred/wilma", null, null, 0, new String[] {},
                                FileSystem.dir( "barney", "/WOBBLE/fred/wilma/barney", null, null, 0, new String[] {},
                                    FileSystem.File( "betty", "/WOBBLE/fred/wilma/barney/betty", false, null, 0, null, null ) ) ) ) ) );

            FileSystem merged = FileSystemUtils.merge(DirFileSystem.TESTDATA, overlayContent);

            //the overlay should now validate against the merged data..
            writer.println("Comparing overlay with merged testdata");
            ArtifactUtils.compare(oc, merged, writer);

            if (oc.getEntry("/WIBBLE") == null) {
                writer.println("FAIL: to read content existing in the overlay dir, placed by hand.");
            }

            if (oc.getEntry("/WOBBLE/fred/wilma/barney/betty") == null) {
                writer.println("FAIL: to read nested content existing in the overlay dir, placed by hand.");
            }

            writer.println("Overlaid paths : " + oc.getOverlaidPaths());

            //now test nested overlays.. 
            ArtifactEntry nested = oc.getEntry("/c/b.jar");
            OverlayContainer correspondingOverlay = oc.getOverlayForEntryPath(nested.getPath());
            ArtifactEntry aa = oc.getEntry("/a/aa");
            correspondingOverlay.addToOverlay(aa, "/newentry", false);

            //we use fs to add newentry at the right place in the total tree.. not in the subtree 
            //represented by corresponding overlay, this lets the fs layer check the new subtree
            //is still correctly linked to the main tree.
            FileSystem nestedOverlayContent =
                FileSystem.root( null, null, null, false, null, 0, null, 0, new String[] {},
                    FileSystem.dir( "c", "/c", null, null, 0, new String[] {},
                        FileSystem.root( "b.jar", "/c/b.jar", null, false, null, 0, null, 0, new String[] {},
                            FileSystem.dir( "newentry", "/newentry", null, null, 0, new String[] {},
                                FileSystem.File( "aa.txt", "/newentry/aa.txt", true, "wibble", 6, null, null ) ) ) ) );

            FileSystem nestedMerged = FileSystemUtils.merge(merged, nestedOverlayContent);

            //writer.println("nestedMerged");
            //dumpRecursive(0, nestedMerged, writer);

            //the overlay should now validate against the merged data..
            writer.println("Comparing overlay with nested merged testdata");
            ArtifactUtils.compare(oc, nestedMerged, writer);

            //quick navigation test.
            ArtifactContainer nestedContainer = nested.convertToContainer();
            if (nestedContainer.getEntry("/newentry/aa.txt") == null) {
                writer.println("FAIL: to read content added to nested container via overlay");
            }

            //quick cache test.
            Object myObject = new Object();
            Object myOtherObject = new Object();
            oc.addToNonPersistentCache("/test", this.getClass(), myObject);
            correspondingOverlay.addToNonPersistentCache("/test", this.getClass(), myOtherObject);

            if (oc.getFromNonPersistentCache("/test", this.getClass()) != myObject) {
                writer.println("FAIL: object from cache was incorrect");
            }
            if (oc.getFromNonPersistentCache("/test2", this.getClass()) != null) {
                writer.println("FAIL: object retrievable from cache with bad path");
            }
            if (oc.getFromNonPersistentCache("/test", String.class) != null) {
                writer.println("FAIL: object retrievable from cache with bad caller obj");
            }
            oc.removeFromNonPersistentCache("/test", this.getClass());
            if (oc.getFromNonPersistentCache("/test", this.getClass()) != null) {
                writer.println("FAIL: object retrievable from cache after remove");
            }
            //re-obtain the sub overlay.. 
            OverlayContainer newInstanceOfSubOverlay = oc.getOverlayForEntryPath("/c/b.jar");
            if (newInstanceOfSubOverlay.getFromNonPersistentCache("/test", this.getClass()) != myOtherObject) {
                writer.println("FAIL: object not retrievable from sub overlay via new instance");
            }

        } finally {
            writer.println("Cleaning up " + temp.getAbsolutePath());
            killDirectory(temp, writer);
        }
    }

    private void killDirectory(File f, PrintWriter writer) {
        if (f.isDirectory()) {
            //writer.println("Cleaning up dir contents from " + f.getAbsolutePath());
            for (File c : f.listFiles()) {
                killDirectory(c, writer);
            }
            //writer.println("Contents dealt with.. removing dir " + f.getAbsolutePath());
            if (!f.delete()) {
                writer.println("FAIL: (not an overlay failure) Error .. unable to cleanup dir " + f.getAbsolutePath());
                f.deleteOnExit();
            } else {
                //writer.println("dir deleted ok from " + f.getAbsolutePath());
            }
        } else {
            //writer.println("Cleaning up file " + f.getAbsolutePath());
            if (f.exists()) {
                //writer.println("file exists at " + f.getAbsolutePath());
                if (!f.delete()) {
                    writer.println("FAIL: (not an overlay failure) Error .. unable to cleanup " + f.getAbsolutePath());
                    f.deleteOnExit();
                } else {
                    //writer.println("file deleted ok from " + f.getAbsolutePath());
                }
            } else {
                //writer.println("file not found for deletion at " + f.getAbsolutePath());
            }
        }
    }

    private void testOverlay(PrintWriter writer) {
        writer.println("Processing ArtifactContainer for : " + dir);
        ArtifactContainer c = getContainerForDirectory();
        if (c == null) {
            writer.println("FAIL: Unable to obtain ArtifactContainer for overlay ArtifactContainer test (unable to test overlay ArtifactContainer, not an overlay issue)");
            return;
        }
        ArtifactContainer j = getContainerForZip();
        if (j == null) {
            writer.println("FAIL: Unable to obtain 2nd ArtifactContainer for overlay ArtifactContainer test (unable to test overlay ArtifactContainer, not an overlay issue)");
            return;
        }
        OverlayContainer oc = ocf.createOverlay(OverlayContainer.class, c);
        if (oc == null) {
            writer.println("FAIL: Unable to obtain overlay ArtifactContainer via factory");
            return;
        }

        overlayTests(oc, c, j, false, writer);
    }

    /**
     * @param oc
     * @param c
     * @param j
     * @param copiesDirectories <code>true</code> if the implementation of the overlay ArtifactContainer will copy directories to a new location when they are added to the overlay
     * @param writer
     */
    private void overlayTests(OverlayContainer oc, ArtifactContainer c, ArtifactContainer j, boolean copiesDirectories, PrintWriter writer) {

        if (oc.getContainerBeingOverlaid() != c) {
            writer.println("FAIL: Incorrect ArtifactContainer returned from getContainerBeingOverlaid");
            return;
        }
        if (oc.getEnclosingContainer() != c.getEnclosingContainer()) {
            writer.println("FAIL: Mismatch for getEnclosingContainer on OverlayContainer");
            return;
        }
        if (oc.getName() != c.getName()) {
            writer.println("FAIL: Mismatch for getName on OverlayContainer (wanted " + c.getName() + " got " + oc.getName() + " )");
            return;
        }
        if (oc.getPath() != c.getPath()) {
            writer.println("FAIL: Mismatch for getPath on OverlayContainer");
            return;
        }

        if (!testOverlayMaskSetManipulation(oc, writer)) {
            return;
        }
        if (!testOverlayEntrySetManipulation(oc, j, writer)) {
            return;
        }
        if (!testOverlayUsingOtherEntries(oc, j, writer)) {
            return;
        }
        if (!testOverlayNavigation(oc, j, writer)) {
            return;
        }
        if (!testOverlayContent(oc, j, copiesDirectories, writer)) {
            return;
        }

        writer.println("PASS");
    }

    private boolean testOverlayContent(OverlayContainer oc, ArtifactContainer j, boolean isDirectory, PrintWriter writer) {
        ArtifactEntry aa = j.getEntry("/a/aa");
        if (aa == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry aa from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        ArtifactEntry ab = oc.getContainerBeingOverlaid().getEntry("/b/ba/baa");
        if (ab == null) {
            writer.println("FAIL: unable to obtain ArtifactEntry ab from 2ndry ArtifactContainer for overlay test (not overlay failure)");
            return false;
        }
        if (!oc.addToOverlay(ab, "/a/ad", false)) {
            writer.println("FAIL: unable to add ArtifactEntry to overlay at new path /a/ad");
            return false;
        }
        if (!oc.addToOverlay(aa, "/a/ac", false)) {
            writer.println("FAIL: unable to add ArtifactEntry to overlay at new path /a/ac");
            return false;
        }

        ArtifactEntry e = oc.getEntry("/a/aa/aa.txt");
        if (!validateEntry(e, "/a/aa/aa.txt", "aa.txt", "wibble", writer)) {
            return false;
        }
        if (!testEntryUrl(oc, "/a/aa/aa.txt", "/a/aa/aa.txt", writer)) {
            return false;
        }

        e = oc.getEntry("/a/ac/aa.txt");
        if (!validateEntry(e, "/a/ac/aa.txt", "aa.txt", "", writer)) {
            return false;
        }

        // If this is a directory overlay then it should of copied the whole directory into a new location
        String expectedFileForaa = isDirectory ? "/a/ac/aa.txt" : "/a/aa/aa.txt";
        if (!testEntryUrl(oc, "/a/ac/aa.txt", expectedFileForaa, writer)) {
            return false;
        }

        e = oc.getEntry("/a/ad/baa1.txt");
        if (!validateEntry(e, "/a/ad/baa1.txt", "baa1.txt", "minion", writer)) {
            return false;
        }
        String expectedFileForbaa1 = isDirectory ? "/a/ad/baa1.txt" : "/b/ba/baa/baa1.txt";
        if (!testEntryUrl(oc, "/a/ad/baa1.txt", expectedFileForbaa1, writer)) {
            return false;
        }

        return true;
    }

    private boolean validateEntry(ArtifactEntry e, String path, String name, String content, PrintWriter writer) {
        if (e == null) {
            writer.println("FAIL: unable to get ArtifactEntry " + path + " from overlay");
            return false;
        }
        if (!e.getName().equals(name)) {
            writer.println("FAIL: ArtifactEntry for " + path + " has name " + e.getName() + " expected " + name);
            return false;
        }
        if (!e.getPath().equals(path)) {
            writer.println("FAIL: ArtifactEntry for " + path + " has path " + e.getPath() + " expected " + path);
            return false;
        }
        InputStream i = null;
        try {
            i = e.getInputStream();

            //if no content we can exit here.
            if (content == null && i == null) {
                if (e.getSize() != 0) {
                    writer.println("FAIL: Size was not zero for null inputstream for " + path);
                    return false;
                }
                return true;
            }

            //supposed to be content.. check it out..
            if (i == null) {
                writer.println("FAIL: unable to get inputstream for " + path + "t from overlay");
                return false;
            }
            StringBuffer s = new StringBuffer();
            int maxlen = 64; //safety to avoid test going nuts.
            int b = i.read();
            while (b != -1 & maxlen > 0) {
                s.append((char) b);
                b = i.read();
                maxlen--;
            }
            if (!content.equals(s.toString())) {
                writer.println("FAIL: incorrect content for " + path + " from overlay. Expected: " + content + " Got: " + s.toString());
                return false;
            }
            if (e.getSize() != content.length()) {
                writer.println("FAIL: wanted length " + content.length() + " for ArtifactEntry " + path + " and got " + e.getSize());
                return false;
            }
        } catch (IOException e1) {
            writer.println("FAIL: unable to get inputstream for " + path + " from overlay due to" + e1);
            return false;
        } finally {
            if (i != null) {
                try {
                    i.close();
                } catch (IOException e2) {
                }
            }
        }
        return true;
    }

    private void testNavigation(ArtifactContainer c, PrintWriter writer) {
        if ( c == null ) {
            writer.println("FAIL: Null Container");
            return;
        }
        String cText = "[ " + c.getPath() + " : " + c.getName() + " ]";
        writer.println("Examining root container " + cText);
        if ( !c.isRoot() ) {
            writer.println("FAIL: Container " + cText + " is not root");
            return;
        } else if ( c.getEnclosingContainer() != null ) {
            ArtifactContainer enclosing = c.getEnclosingContainer();
            writer.println("FAIL: Root Container " + cText +
                           " has enclosing container [ " + enclosing.getPath() + " : " + enclosing.getName() + " ]");
            return;
        } else if ( !c.getPath().equals("/") ) {
            writer.println("FAIL: Root container " + cText + " has unexpected path " + cText);
            return;
        } else if ( !c.getName().equals("/") ) {
            writer.println("FAIL: Root container " + cText + " has unexpected name" + cText);
            return;
        }

        // Test retrieval of absolute immediate entry of root container.

        ArtifactEntry a = c.getEntry("/a");
        if ( a == null ) {
            writer.println("FAIL: Container " + cText + " does not container entry [ /a ]");
            return;
        } 
        String aText = "[ " + a.getPath() + " : " + a.getName() + " ]";
        writer.println("Examining absolute " + aText);
        if ( !a.getPath().equals("/a") ) {
            writer.println("FAIL: Container " + cText + " entry [ /a ] has unexpected path " + aText);
            return;
        } else if ( !a.getName().equals("a") ) {
            writer.println("FAIL: Container " + cText + " entry [ /a ] has unexpected name" + aText);
            return;
        }

        // Test retrieval of relative immediate entry of root container.

        ArtifactEntry aRel = c.getEntry("a");
        if ( aRel == null ) {
            writer.println("FAIL: Container " + cText + " does not container entry [ a ]");
            return;
        } 
        String aRelText = "[ " + aRel.getPath() + " : " + aRel.getName() + " ]";
        writer.println("Examining relative " + aRelText);
        if ( !a.getPath().equals("/a") ) {
            writer.println("FAIL: Container " + cText + " entry [ a ] has unexpected path " + aRelText);
            return;
        } else if ( !a.getName().equals("a") ) {
            writer.println("FAIL: Container " + cText + " entry [ a ] has unexpected name" + aRelText);
            return;
        }

        // The two entries are equal, but usually not identical.  Don't do an identity test.

        // Test conversion of nested (non-root) container.
        ArtifactContainer a_c = a.convertToContainer();
        if ( a_c == null ) {
            writer.println("FAIL: Container " + cText + " entry " + aText + " failed to convert to container");
            return;
        }
        String a_cText = "[ " + a_c.getPath() + " : " + a_c.getName() + " ]";
        writer.println("Examining converted " + a_cText);
        if ( a_c.isRoot() ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText + " is root");
            return;
        } else if ( !a_c.getPath().equals("/a") ) {
            writer.println("FAIL: Container " + cText + " child container [ /a ] has unexpected path " + a_cText);
            return;
        } else if (!a_c.getName().equals("a") ) {
            writer.println("FAIL: Container " + cText + " child container [ /a ] has unexpected name " + a_cText);
            return;
        }

        // Test retrieval of immediate relative entry of a nested container.
        ArtifactEntry a_c_aa = a_c.getEntry("aa");
        if (a_c_aa == null ) {
            writer.println("FAIL: Container " + cText + " container " + a_cText + " failed to locate entry [ aa ]");
            return;
        }
        String a_c_aaText = "[ " + a_c_aa.getPath() + " : " + a_c_aa.getName() + " ]";
        writer.println("Examining entry " + a_c_aaText);
        if (  !a_c_aa.getPath().equals("/a/aa")) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry [ aa ] has unexpected path " + a_c_aaText);
            return;
        } else if ( !a_c_aa.getName().equals("aa") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry [ aa ] has unexpected name " + a_c_aaText);
            return;
        }

        // Test retrieval of absent immediate relative entry.
        ArtifactEntry invalid = a_c.getEntry("fish");
        if ( invalid != null ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " found absent entry [ " + invalid.getPath() + " : " + invalid.getName() + " ]");
            return;
        }

        // Test retrieval of deep relative entry from nested container.
        ArtifactEntry deep = a_c.getEntry("ab/aba/aba.txt");
        if ( deep == null ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " failed to locate entry [ ab/aba/aba.txt ]");
            return;
        }
        String deepText = "[ " + deep.getPath() + " : " + deep.getName() + " ]";
        writer.println("Examining entry " + deepText);
        if ( !deep.getPath().equals("/a/ab/aba/aba.txt") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry [ ab/aba/aba.txt ] has unexpected path " + deepText);
            return;
        } else if ( !deep.getName().equals("aba.txt") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry [ ab/aba/aba.txt ] has unexpected name " + deepText);
            return;
        }

        ArtifactContainer deep_p = deep.getEnclosingContainer();
        if ( deep_p == null ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " has no parent");
            return;
        }
        String deep_pText = "[ " + deep_p.getPath() + " : " + deep_p.getName() + " ]";
        writer.println("Examining container " + deep_pText);
        if ( deep_p.isRoot() ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent " + deep_pText + " is root");
            return;
        } else if ( !deep_p.getPath().equals("/a/ab/aba") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent " + deep_pText + " has incorrect path");
            return;
        } else if ( !deep_p.getName().equals("aba") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent " + deep_pText + " has incorrect name");
            return;
        }

        ArtifactContainer deep_p_p = deep_p.getEnclosingContainer();
        if ( deep_p_p == null ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deep_pText + " has no parent");
            return;
        }
        String deep_p_pText = "[ " + deep_p_p.getPath() + " : " + deep_p_p.getName() + " ]";
        writer.println("Examining container " + deep_p_pText);
        if ( deep_p_p.isRoot() ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent " + deep_p_pText + " is root");
            return;
        } else if ( !deep_p_p.getPath().equals("/a/ab") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent " + deep_p_pText + " has incorrect path");
            return;
        } else if ( !deep_p_p.getName().equals("ab") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent " + deep_p_pText + " has incorrect name");
            return;
        }

        ArtifactEntry deep_deep = deep_p_p.getEntry("aba/aba.txt");
        if ( deep_deep == null ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent " + deep_p_pText +
                           " fails to locate [  aba/aba.txt ]");
            return;
        }
        String deep_deepText = "[ " + deep_deep.getPath() + " : " + deep_deep.getName() + " ]";
        writer.println("Examining entry " + deep_deepText);
        if ( !deep_deep.getPath().equals("/a/ab/aba/aba.txt") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent " + deep_p_pText +
                           " entry [ aba/aba.txt ] has incorrect path " + deep_deepText);
            return;
        }
        if ( !deep_deep.getName().equals("aba.txt") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent " + deep_p_pText +
                           " entry [ aba/aba.txt ] has incorrect name " + deep_deepText);
            return;
        }

        ArtifactEntry veryDeep = deep_p_p.getEntry("/a/ab/aba/aba.txt");
        if ( veryDeep == null ) {
            writer.println("FAIL: Container " + cText + " child container " + deep_p_pText +
                            " failed to locate [ /a/ab/aba/aba.txt ]");
            return;
        }
        String veryDeepText = "[ " + veryDeep.getPath() + " : " + veryDeep.getName() + " ]";
        writer.println("Examining entry " + veryDeepText);
        if ( !veryDeep.getPath().equals("/a/ab/aba/aba.txt") ) {
            writer.println("FAIL: Container " + cText + " child container " + deep_p_pText +
                           " entry [ /a/ab/aba/aba.txt ] has incorrect path " + veryDeepText);
            return;
        } else if ( !veryDeep.getName().equals("aba.txt") ) {
            writer.println("FAIL: Container " + cText + " child container " + deep_p_pText +
                           " entry [ /a/ab/aba/aba.txt ] has incorrect name " + veryDeepText);
            return;
        }

        ArtifactContainer deep_p_p_p = deep_p_p.getEnclosingContainer();
        if ( deep_p_p_p == null ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deep_p_pText + " has no parent");
            return;
        }
        String deep_p_p_pText = "[ " + deep_p_p_p.getPath() + " : " + deep_p_p_p.getName() + " ]";
        writer.println("Examining container " + deep_p_p_pText);
        if ( deep_p_p_p.isRoot() ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent of parent " + deep_p_p_pText + " is root");
            return;
        } else if ( !deep_p_p_p.getPath().equals("/a") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent of parent " + deep_p_p_pText + " has incorrect path");
            return;
        } else if ( !deep_p_p_p.getName().equals("a") ) {
            writer.println("FAIL: Container " + cText + " child container " + a_cText +
                           " entry " + deepText + " parent of parent of parent " + deep_p_p_pText + " has incorrect name");
            return;
        }

        ArtifactContainer root = deep_p_p_p.getEnclosingContainer();
        if ( root == null ) {
            writer.println("FAIL: Container " + cText + " child container " + deep_p_p_pText + " has no parent");
            return;
        }
        String rootText = "[ " + root.getPath() + " : " + root.getName() + " ]";
        writer.println("Examining root container " + rootText);
        if ( !root.isRoot() ) {
            writer.println("FAIL: Non-root Container " + rootText);
            return;
        } else if ( root.getEnclosingContainer() != null ) {
            ArtifactContainer enclosing = root.getEnclosingContainer();
            writer.println("FAIL: Root Container " + rootText +
                           " has enclosing container [ " + enclosing.getPath() + " : " + enclosing.getName() + " ]");
            return;
        } else if ( !root.getPath().equals("/") ) {
            writer.println("FAIL: Root container " + rootText + " has unexpected path");
            return;
        } else if ( !root.getName().equals("/") ) {
            writer.println("FAIL: Root container " + rootText + " has unexpected name");
            return;
        }

        writer.println("PASS");
    }

    /**
     * @param cf
     * @param dir
     * @param writer
     */
    private void testJarMedium(PrintWriter writer) {
        writer.println("Processing ArtifactContainer for : " + jar_b);
        ArtifactContainer c = getContainerForSmallerZip();

        if (c != null) {
            writer.println("Got ArtifactContainer " + c);

            HashSet<String> expected = new HashSet<String>(Arrays.asList(new String[] { "/bb",
                                                                                       "/bb/a.jar",
                                                                                       "/META-INF",
                                                                                       "/META-INF/MANIFEST.MF",
                                                                                       "/ba",
                                                                                       "/ba/baa",
                                                                                       "/ba/baa/baa1.txt",
                                                                                       "/ba/baa/baa2.txt",
            }));
            HashSet<String> surplus = new HashSet<String>();
            validatePaths(expected, surplus, c);
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: expected: " + expected + " surplus: " + surplus);
                return;
            }

            writer.println("PASS");

        } else {
            writer.println("FAIL: Null ArtifactContainer for " + jar_b);
        }
    }

    /**
     * This test makes sure you can get URIs for dirs, JARs, directories in JARs and nested JARs
     * 
     * @param cf
     * @param dir
     * @param writer
     */
    private boolean testUri(PrintWriter writer) {
        boolean passed = true;
        // First test a dir
        writer.println("Testing URIs for dir " + dir);
        File dirFile = new File(dir);
        ArtifactContainer ArtifactContainer = getContainerForDirectory();
        if (ArtifactContainer != null) {
            if (!testGetURI(ArtifactContainer, null, Collections.singleton(dirFile.toURI().toString()), writer, true)) {
                passed = false;
            }

            // Now try a JAR file
            String jarFileUri = "jar:" + dirFile.toURI().toString() + "c/b.jar!/";
            if (!testGetURI(ArtifactContainer, "/c/b.jar", Collections.singleton(jarFileUri), writer, false)) {
                passed = false;
            }

            // Now try a folder within the JAR file, this should be surrounded by the jar:!/ syntax
            ArtifactEntry jarEntry = ArtifactContainer.getEntry("/c/b.jar");
            if (jarEntry == null) {
                writer.println("FAIL: Unable to find JAR /c/b.jar");
                passed = false;
            } else {
                ArtifactContainer jarContainer = jarEntry.convertToContainer();
                if (jarContainer == null) {
                    writer.println("FAIL: Unable to convert JAR /c/b.jar to a ArtifactContainer");
                    passed = false;
                } else {
                    if (!testGetURI(jarContainer, "/ba", Collections.singleton(jarFileUri + "ba/"), writer, false)) {
                        passed = false;
                    }

                    // Now test a nested JAR in the JAR ArtifactContainer
                    if (!testGetURI(jarContainer, "/bb/a.jar", Collections.singleton(jarFileUri + "bb/a.jar"), writer, false)) {
                        passed = false;
                    }
                }
            }
        } else {
            writer.println("FAIL: unable to test URIs because the ArtifactContainer cannot be created for " + dir);
            passed = false;
        }
        return passed;
    }

    private boolean testPhysicalPath(EnclosedEntity c, String real, PrintWriter writer) {
        String result = getPhysicalPath(c);
        if (real == null && result != null) {
            writer.println("FAIL: Got the path " + result + " for " + c + " at " + c.getPath() + " but was expecting null");
            return false;
        } else if (real != null && result == null) {
            writer.println("FAIL: The path was null for the entity " + c + " at " + c.getPath() + " but was expecting " + real);
            return false;
        } else {
            writer.println("PASS: got path of " + result + " for " + (real != null ? real : c.getPath()));
            return true;
        }
    }

    /**
     * This test makes sure you can get Paths for dirs, JARs
     * 
     * @param cf
     * @param dir
     * @param writer
     */
    private boolean testPhysicalPath(PrintWriter writer) {
        boolean passed = true;
        // First test a dir
        writer.println("Testing Paths for dir " + dir);
        File dirFile = new File(dir);
        ArtifactContainer ArtifactContainer = getContainerForDirectory();
        //dumpContainerRecursive(0, ArtifactContainer, "root", writer);
        if (ArtifactContainer != null) {
            passed &= testPhysicalPath(ArtifactContainer, dir, writer);
            // Now try a JAR file
            ArtifactEntry e = ArtifactContainer.getEntry("/c/b.jar");
            passed &= testPhysicalPath(e, dirFile + File.separator + "c" + File.separator + "b.jar", writer);
            ArtifactContainer c2 = e.convertToContainer();
            if (c2 != null) {
                passed &= testPhysicalPath(c2, dirFile + File.separator + "c" + File.separator + "b.jar", writer);
                //test that things inside jars return null.. 
                ArtifactEntry e2 = c2.getEntry("/bb");
                if (e2 != null) {
                    passed &= testPhysicalPath(e2, null, writer);
                } else {
                    writer.println("FAIL: unable to locate /bb inside c/b.jar as expected for test (test data fail)");
                    passed = false;
                }

            } else {
                writer.println("FAIL: unable to open jar as ArtifactContainer (test data fail)");
                passed = false;
            }
        } else {
            writer.println("FAIL: unable to open dir as ArtifactContainer (test data fail)");
            passed = false;
        }
        //now try loose.. 
        ArtifactContainer l = getContainerForLooseXML();
        if (l != null) {
            // The root of the archive is a virtual ArtifactContainer so doesn't have a physical resource so pass in null
            passed &= testPhysicalPath(l, null, writer);

            ArtifactEntry e3 = l.getEntry("/META-INF/b/ba/baa/baa1.txt");
            if (e3 != null) {
                passed &= testPhysicalPath(e3, "somewhere, but not null", writer);
            } else {
                writer.println("FAIL: Unable to open ArtifactEntry /META-INF/b/ba/baa/baa1.txt inside loose ArtifactContainer (test data fail)");
                passed = false;
            }

            // Also test a real ArtifactContainer in loose config
            ArtifactEntry looseContainerEntry = l.getEntry("/META-INF/b");
            if (looseContainerEntry != null) {
                passed &= testPhysicalPath(looseContainerEntry, "somewhere, but not null", writer);
                ArtifactContainer looseContainer = looseContainerEntry.convertToContainer();
                if (looseContainer != null) {
                    passed &= testPhysicalPath(looseContainer, "somewhere, but not null", writer);
                }
            } else {
                writer.println("FAIL: Unable to open ArtifactEntry /META-INF/b inside loose ArtifactContainer (test data fail)");
                passed = false;
            }

            // Now test a virtual ArtifactEntry and ArtifactContainer, should always be null, we need to go to the war to get a virtual ArtifactContainer
            try {
                ArtifactEntry warEntry = l.getEntry("/webApp.war");
                ArtifactContainer warContainer = warEntry.convertToContainer();
                ArtifactEntry looseContainerVirtualEntry = warContainer.getEntry("/WEB-INF");
                passed &= testPhysicalPath(looseContainerVirtualEntry, null, writer);
                ArtifactContainer looseVirtualContainer = looseContainerVirtualEntry.convertToContainer();
                if (looseVirtualContainer != null) {
                    passed &= testPhysicalPath(looseVirtualContainer, null, writer);
                }
            } catch (Exception e) {
                writer.println("FAIL: Exception (" + e.getMessage() + ")caught navigating to /webApp.war/META-INF inside loose ArtifactContainer (test data fail), full trace is:");
                e.printStackTrace(writer);
                passed = false;
            }
        } else {
            writer.println("FAIL: unable to open xml as ArtifactContainer (test data fail)");
            passed = false;
        }
        return passed;
    }

    /**
     * @param cf
     * @param dir
     * @param writer
     */
    private void testJarNested(PrintWriter writer) {
        writer.println("Processing ArtifactContainer for : " + jar_b);
        ArtifactContainer c = getContainerForSmallerZip();

        if (c != null) {
            writer.println("Got ArtifactContainer " + c);
            Set<String> expected = new HashSet<String>(Arrays.asList(new String[] { "ba", "bb", "META-INF" }));
            Set<String> surplus = new HashSet<String>();
            for (ArtifactEntry e : c) {
                if (expected.contains(e.getName())) {
                    expected.remove(e.getName());
                } else {
                    surplus.add(e.getName());
                }
            }
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: expected: " + expected + " surplus: " + surplus);
                return;
            }
            expected = new HashSet<String>(Arrays.asList(new String[] { "b.jar:/bb",
                                                                       "b.jar:/bb/a.jar",
                                                                       "b.jar:/META-INF",
                                                                       "b.jar:/META-INF/MANIFEST.MF",
                                                                       "b.jar:/ba",
                                                                       "b.jar:/ba/baa",
                                                                       "b.jar:/ba/baa/baa1.txt",
                                                                       "b.jar:/ba/baa/baa2.txt",
                                                                       "a.jar:/aa",
                                                                       "a.jar:/aa/aa.txt",
                                                                       "a.jar:/ab",
                                                                       "a.jar:/ab/aba",
                                                                       "a.jar:/ab/aba/aba.txt",
                                                                       "a.jar:/ab/ab.txt",
                                                                       "a.jar:/a.txt",
                                                                       "a.jar:/META-INF",
                                                                       "a.jar:/META-INF/MANIFEST.MF"
            }));
            surplus = new HashSet<String>();
            validatePathsAndSubContainers(expected, surplus, c, "b.jar");
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: expected: " + expected + " surplus: " + surplus);
                return;
            }

            writer.println("PASS");
        } else {
            writer.println("FAIL: Null ArtifactContainer for " + jar_b);
        }
    }

    /**
     * @param cf
     * @param dir
     * @param writer
     */
    private void testDirMedium(PrintWriter writer) {
        writer.println("Processing ArtifactContainer for : " + dir);
        ArtifactContainer c = getContainerForDirectory();

        if (c != null) {
            writer.println("Got ArtifactContainer " + c);
            Set<String> expected = new HashSet<String>(Arrays.asList(new String[] { "a", "b", "c" }));
            Set<String> surplus = new HashSet<String>();
            for (ArtifactEntry e : c) {
                if (expected.contains(e.getName())) {
                    expected.remove(e.getName());
                } else {
                    surplus.add(e.getName());
                }
            }
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: expected: " + expected + " surplus: " + surplus);
                return;
            }
            expected = new HashSet<String>(Arrays.asList(new String[] { "/a",
                                                                       "/a/aa",
                                                                       "/a/aa/aa.txt",
                                                                       "/a/ab",
                                                                       "/a/ab/aba",
                                                                       "/a/ab/aba/aba.txt",
                                                                       "/a/ab/ab.txt",
                                                                       "/a/a.txt",
                                                                       "/b",
                                                                       "/b/ba",
                                                                       "/b/ba/baa",
                                                                       "/b/ba/baa/baa1.txt",
                                                                       "/b/ba/baa/baa2.txt",
                                                                       "/b/bb",
                                                                       "/b/bb/a.jar",
                                                                       "/c",
                                                                       "/c/a.jar",
                                                                       "/c/b.jar"
            }));
            surplus = new HashSet<String>();
            validatePaths(expected, surplus, c);
            if (expected.size() > 0 && surplus.size() > 0) {
                writer.println("FAIL: expected: " + expected + " surplus: " + surplus);
                return;
            }

            writer.println("PASS");
        } else {
            writer.println("FAIL: Null ArtifactContainer for " + dir);
        }

    }

    private void validatePathsAndSubContainers(Set<String> expected, Set<String> surplus, ArtifactContainer c, String root) {
        for (ArtifactEntry e : c) {
            if (expected.contains(root + ":" + e.getPath())) {
                expected.remove(root + ":" + e.getPath());
            } else {
                surplus.add(root + ":" + e.getPath());
            }
            ArtifactContainer nested = e.convertToContainer();
            if (nested != null) {
                validatePathsAndSubContainers(expected, surplus, nested, (nested.isRoot() ? e.getName() : root));
            }
        }
    }

    private void validatePaths(Set<String> expected, Set<String> surplus, ArtifactContainer c) {
        for (ArtifactEntry e : c) {
            if (expected.contains(e.getPath())) {
                expected.remove(e.getPath());
            } else {
                surplus.add(e.getPath());
            }
            ArtifactContainer nested = e.convertToContainer();
            //isRoot=false will keep us from entering nested ArtifactContainers not part of this fs.
            if (nested != null && !nested.isRoot()) {
                validatePaths(expected, surplus, nested);
            }
        }
    }

    private void validatePaths(Set<String> expected, Set<String> surplus, Container c) throws UnableToAdaptException {
        for (com.ibm.wsspi.adaptable.module.Entry e : c) {
            if (expected.contains(e.getPath())) {
                expected.remove(e.getPath());
            } else {
                surplus.add(e.getPath());
            }
            Container nested = e.adapt(Container.class);
            //test convert back to entry..
            if (nested != null) {
                Entry back = nested.adapt(Entry.class);
                if (back == null) {
                    //abuse the surplus set to report the failue.. 
                    //future tests will use FsAdaptableUtils to validate adapt back to Entry.
                    surplus.add("Unable to convert " + nested.getPath() + " back to Entry!");
                } else {
                    if (!back.getName().equals(e.getName()) || !back.getPath().equals(e.getPath())) {
                        //abuse the surplus set to report the failue.. 
                        //future tests will use FsAdaptableUtils to validate adapt back to Entry.
                        surplus.add("Unable to validate converted Entry for " + nested.getPath());
                    }
                }
            }
            //isRoot=false will keep us from entering nested ArtifactContainers not part of this fs.
            if (nested != null && !nested.isRoot()) {
                validatePaths(expected, surplus, nested);
            }
        }
    }

    private void testDir(PrintWriter out) {
        out.println("Processing ArtifactContainer for : " + dir);
        ArtifactContainer c = getContainerForDirectory();

        out.println("Got ArtifactContainer " + c);
        if (c != null) {
            out.println("PASS");
        } else {
            out.println("FAIL: Null ArtifactContainer for " + dir);
        }
    }

    private void testJar(PrintWriter out) {
        out.println("Processing ArtifactContainer for : " + jar_b);
        ArtifactContainer c = getContainerForSmallerZip();

        out.println("Got ArtifactContainer " + c);
        if (c != null) {
            out.println("PASS");
        } else {
            out.println("FAIL: Null ArtifactContainer for " + jar_b);
        }
    }

    private void testRar(PrintWriter out) {
        out.println("Processing ArtifactContainer for : " + rar);
        ArtifactContainer c = getContainerForRar();

        out.println("Got ArtifactContainer " + c);
        if (c != null) {
            out.println("PASS");
        } else {
            out.println("FAIL: Null ArtifactContainer for " + rar);
        }
    }

    private void dumpContainerRecursive(int depth, ArtifactContainer zc, String root, PrintWriter out) {
        out.println("Container @ " + String.valueOf(getPhysicalPath(zc)));
        for (ArtifactEntry e : zc) {
            out.format("%3d: %30s %s\n", depth, root, e.getPath());
            out.println(" @ " + String.valueOf(getPhysicalPath(e)));
            ArtifactContainer zc1 = e.convertToContainer();
            if (zc1 != null) {
                dumpContainerRecursive(depth + 1, zc1, zc1.isRoot() ? e.getName() : root, out);
            }
        }
    }

    private void dumpContainerRecursive(int depth, Container zc, String root, PrintWriter out) throws UnableToAdaptException {
        for (Entry e : zc) {
            out.format("%3d: %30s %s\n", depth, root, e.getPath());
            Container zc1 = e.adapt(Container.class);
            if (zc1 != null) {
                dumpContainerRecursive(depth + 1, zc1, zc1.isRoot() ? e.getName() : root, out);
            }
        }
    }

    private String getContentForEntry(ArtifactEntry e) throws IOException {
        InputStream i = null;
        try {
            i = e.getInputStream();
            if (i == null)
                return null;

            StringBuffer s = new StringBuffer();
            int maxlen = 64; //safety to avoid test going nuts.
            int b = i.read();
            while (b != -1 & maxlen > 0) {
                if (b == '\n') {
                    s.append("\\n");
                } else if (b == '\r') {
                    s.append("\\r");
                } else {
                    s.append((char) b);
                }
                b = i.read();
                maxlen--;
            }

            return s.toString();
        } finally {
            if (i != null)
                i.close();
        }
    }

    private void dumpContainerRecursiveFs(int depth, ArtifactContainer zc, String root, PrintWriter out) {

        String pad = "";
        for (int i = 0; i < depth; i++) {
            pad += " ";
        }

        if ( zc.isRoot() ) {
            if ( zc.getEnclosingContainer() == null ) {
                out.println("// ###################################");
                out.println("// Fs for " + zc.getClass().getName());
                out.println("// ###################################");
                out.println(pad + "Fs.Root( null, null, null, false, null, 0, null, 0, new String[] {} )");
            }
        }

        String zcphyspath = getPhysicalPath(zc) == null ? "null" : "\"" + getPhysicalPath(zc).replace("\\", "\\\\") + "\"";
        out.println(", " + zcphyspath);

        Collection<URL> zcurls = zc.getURLs();
        String zcuricount = zcurls == null ? "-1" : "" + zcurls.size();
        out.println(", " + zcuricount);
        String zcuristr = "null";
        if (zcurls != null) {
            zcuristr = "new String[] { ";
            for (URL url : zcurls) {
                zcuristr += "\"" + url.toString() + "\",";
            }
            //remove trailing , if needed
            if (zcurls.size() > 0) {
                zcuristr = zcuristr.substring(0, zcuristr.length() - 1);
            }
            zcuristr += "}";
        }
        out.println(", " + zcuristr);

        pad += " ";
        for (ArtifactEntry e : zc) {
            ArtifactContainer zc1 = e.convertToContainer();
            if (zc1 != null) {
                //handle root here before we forget it's paths!
                if (zc1.isRoot()) {
                    out.println(pad + ", Fs.Root( \"" + e.getName() +     "\"" +
                                               ", \"" + e.getPath() +     "\"" +
                                               ", \"" + e.getResource() + "\"" +
                                               ", false, null, " + e.getSize() +
                                               ", null, 0, new String[] {} )");
                } else {
                    out.println(pad + ", Fs.Dir( \"" + e.getName() + "\"" +
                                              ", \"" + e.getPath() + "\"" +
                                              ", \"" + e.getResource() + "\" )");
                }
                dumpContainerRecursiveFs(depth + 1, zc1, zc1.isRoot() ? e.getName() : root, out);
            } else {
                //Fs.File("ab.txt", "/ab/ab.txt", true, "", 0)
                String content = null;
                boolean hasContent = false;
                try {
                    content = getContentForEntry(e);
                    if (content != null) {
                        hasContent = true;
                    }
                    //we wont validate jar content..
                    if (e.getName().toUpperCase().endsWith(".JAR")) {
                        content = null;
                    }
                } catch (IOException io) {
                    hasContent = true;
                }
                if (content == null) {
                    content = "null";
                } else {
                    content = "\"" + content + "\"";
                }

                String resource =
                    ( (e.getResource() == null) ? "null" : ("\"" + e.getResource().toString() + "\"") );
                String physpath =
                    ( (getPhysicalPath(e) == null) ? "null" : ("\"" + getPhysicalPath(e).replace("\\", "\\\\") + "\"") );

                out.println(pad + ", Fs.File( \"" + e.getName() + "\"" +
                                            ",\"" + e.getPath() + "\"" +
                                             ", " + hasContent + ", " + content + ", " + e.getSize() +
                                             ", " + resource + ", " + physpath + " )");
            }
        }
        pad = pad.substring(1);
        out.print(pad + ")");

        if (depth == 0) {
            out.println(";");
        } else {
            out.println("");
        }
    }

    @SuppressWarnings("unused")
    private void dumpContainerRecursive2(int depth, ArtifactContainer zc, String root, PrintWriter out) {
        out.println("Processing ArtifactContainer of class " + zc.getClass().getName());
        out.println("Container has path " + zc.getPath());
        for (ArtifactEntry e : zc) {
            out.println("entry class: " + e.getClass().getName());
            //out.format("%3d: %30s %s\n", depth, root.getName(), e.getPath());

            ArtifactContainer zc1 = e.convertToContainer();
            out.format("c?%7s %3d: %30s %s\n", Boolean.valueOf(zc1 != null).toString(), depth, root, e.getPath());

            if (zc1 != null) {
                if (e.getPath().equals("/b/bb/a.jar")) {
                    Iterator<ArtifactEntry> ei = zc1.iterator();
                }
                out.println("container class: " + e.getClass());
                dumpContainerRecursive2(depth + 1, zc1, zc1.isRoot() ? e.getName() : root, out);
            }
        }
    }
}
