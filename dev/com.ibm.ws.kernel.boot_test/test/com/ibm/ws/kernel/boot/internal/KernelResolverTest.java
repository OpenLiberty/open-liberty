/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;
import test.shared.DumpTimerRule;
import test.shared.TestUtils;

import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.SharedBootstrapConfig;

/**
 *
 */
public class KernelResolverTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestRule outputRule = outputMgr;

    @Rule
    public TestRule dumpTimerRule = new DumpTimerRule(30000, new File("build/unittest"));

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestUtils.cleanTempFiles();
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        TestUtils.setKernelUtilsBootstrapLibDir(new File(testClassesDir + "/test data/lbr/lib"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        TestUtils.cleanTempFiles();
        TestUtils.setKernelUtilsBootstrapLibDir(null);
    }

    // Shared configuration places the instance directory in the build dir
    SharedBootstrapConfig config = SharedBootstrapConfig.createSharedConfig(outputMgr, "kernelResolver");
    File installDir = config.getInstallRoot();

    @Test
    public void testKernelResolverNoIncludesNoExtensions() throws Exception {

        // Read kernelCore and defaultLogging-- null os extensions
        // This should be happy/healthy, and should not throw
        KernelResolver resolver = new KernelResolver(installDir, null, "kernelCore-1.0", "defaultLogging-1.0", null);

        final List<URL> urlList = new ArrayList<URL>(4);

        // Add OSGi framework, log provider, and/or os extension "boot.jar" elements
        resolver.addBootJars(urlList);
        System.out.println(urlList);

        Assert.assertEquals("Should have two jars in the jar list by default", 2, urlList.size());
        Assert.assertTrue("Should have resolved x.y; version=\"[1.0.0,1.0.100)\"; type=\"boot.jar\" to x.y_1.0.jar", listContains(urlList, "x.y_1.0.jar"));
        Assert.assertTrue("Should have resolved a.b; version=\"[1,1.0.100)\"; type=\"boot.jar\" to a.b_1.0.1.v2.jar", listContains(urlList, "a.b_1.0.1.v2.jar"));
        Assert.assertEquals("Should have read the log provider from defaultLogging-1.0.jar", "com.ibm.ws.logging.internal.impl.LogProviderImpl", resolver.getLogProvider());

        // There should be a warning issued about ignoring iFix jar a.b_1.0.2.v1.jar
        Assert.assertTrue("Should have a warning message about skipping an iFix jar", outputMgr.checkForStandardOut("CWWKE0060W.*a.b_1.0.2.v1.jar"));
    }

    @Test
    public void testKernelResolverIncludesNoExtensions() throws Exception {

        // Read kernelCore and defaultLogging-- null os extensions
        // This should be happy/healthy, and should not throw
        KernelResolver resolver = new KernelResolver(installDir, null, "kernelCore-1.0", "binaryLogging-1.0", null);

        final List<URL> urlList = new ArrayList<URL>(4);

        resolver.addBootJars(urlList);
        System.out.println(urlList);

        // Conditions are mostly the same: the binaryLogging mf includes defaultLogging mf, but the log provider class is different
        Assert.assertEquals("Should have two jars in the jar list by default", 2, urlList.size());
        Assert.assertTrue("Should have resolved x.y; version=\"[1.0.0,1.0.100)\"; type=\"boot.jar\" to x.y_1.0.jar", listContains(urlList, "x.y_1.0.jar"));
        Assert.assertTrue("Should have resolved a.b; version=\"[1,1.0.100)\"; type=\"boot.jar\" to a.b_1.0.1.v2.jar", listContains(urlList, "a.b_1.0.1.v2.jar"));
        Assert.assertEquals("Should have read the log provider from defaultLogging-1.0.mf", "com.ibm.ws.logging.internal.hpel.HpelLogProviderImpl", resolver.getLogProvider());
    }

    @Test
    public void testKernelResolverExtensions() {

        // Read kernelCore and defaultLogging-- null os extensions
        // This should be happy/healthy, and should not throw
        KernelResolver resolver = new KernelResolver(installDir, null, "kernelCore-1.0", "emptyLogging-1.0", "extension-1.0");

        final List<URL> urlList = new ArrayList<URL>(4);

        resolver.addBootJars(urlList);
        System.out.println(urlList);

        // Conditions are mostly the same: emptyLogging has no boot.jars, but extension-1.0 does
        Assert.assertEquals("Should have two jars in the jar list by default", 2, urlList.size());
        Assert.assertTrue("Should have resolved x.y; version=\"[1.0.0,1.0.100)\"; type=\"boot.jar\" to x.y_1.0.jar", listContains(urlList, "x.y_1.0.jar"));
        Assert.assertTrue("Should have resolved a.b; version=\"[1,1.0.100)\"; type=\"boot.jar\" to a.b_1.0.1.v2.jar", listContains(urlList, "a.b_1.0.1.v2.jar"));
        Assert.assertEquals("Should have read the log provider from emptyLogging-1.0.mf", "dummy.LogProvider", resolver.getLogProvider());
    }

    @Test(expected = LaunchException.class)
    public void testKernelResolverNullKernel() {

        // Throw: null kernel definition
        new KernelResolver(installDir, null, null, null, null);
    }

    @Test(expected = LaunchException.class)
    public void testKernelResolverMissingKernel() {

        // Throw: null missing kernel definition
        new KernelResolver(installDir, null, "bogus-1.0", null, null);
    }

    @Test(expected = LaunchException.class)
    public void testKernelResolverNullLogProvider() {

        // Throw: null log provider definition
        new KernelResolver(installDir, null, "kernelCore-1.0", null, null);
    }

    @Test(expected = LaunchException.class)
    public void testKernelResolverMissingLogProvider() {

        // Throw: missing log provider definition
        new KernelResolver(installDir, null, "kernelCore-1.0", "bogus-1.0", null);
    }

    @Test(expected = LaunchException.class)
    public void testKernelResolverMissingBundle() {

        // Throw: we couldn't find one of the specified jars
        new KernelResolver(installDir, null, "kernelCore-1.0", "missingJar-1.0", null);
    }

    boolean listContains(List<URL> urlList, String filename) {
        for (URL url : urlList) {
            if (url.toString().contains(filename))
                return true;
        }
        return false;
    }
}
