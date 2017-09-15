/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;

import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.SharedBootstrapConfig;
import com.ibm.ws.kernel.boot.internal.KernelResolver;
import com.ibm.ws.kernel.boot.internal.KernelStartLevel;
import com.ibm.ws.kernel.launch.internal.Provisioner.InvalidBundleContextException;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

@RunWith(JMock.class)
public class ProvisionerTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    static SharedBootstrapConfig config;

    @Rule
    public TestName testName = new TestName();

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        TestUtils.cleanTempFiles();
        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        TestUtils.setKernelUtilsBootstrapLibDir(new File(testClassesDir + "/test data/lbr/lib"));
        TestUtils.setUtilsInstallDir(new File(testClassesDir + "/test data/lbr"));

        config = SharedBootstrapConfig.createSharedConfig(outputMgr);
        BundleRepositoryRegistry.initializeDefaults(null, true);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        TestUtils.cleanTempFiles();
        TestUtils.setKernelUtilsBootstrapLibDir(null);
    }

    /** Trivial interface that groups Bundle & BundleStartLevel so mock can push through the adapt method */
    interface TestBundleStartLevel extends Bundle, BundleStartLevel {}

    /** Trivial interface that groups Bundle & BundleRevision so mock can push through the adapt method */
    interface TestBundleRevision extends Bundle, BundleRevision {}

    /** Trivial interface that groups Bundle & FrameworkStartLevel so mock can push through the adapt method */
    interface TestFrameworkStartLevel extends Bundle, FrameworkStartLevel {}

    Mockery context = new Mockery();

    final Bundle mockBundle = context.mock(Bundle.class);
    final BundleContext mockBundleContext = context.mock(BundleContext.class);
    final Filter mockFilter = context.mock(Filter.class);
    final TestBundleRevision mockBundleRevision = context.mock(TestBundleRevision.class);
    final TestBundleStartLevel mockBundleStartLevel = context.mock(TestBundleStartLevel.class);
    final TestFrameworkStartLevel mockFrameworkStartLevel = context.mock(TestFrameworkStartLevel.class);

    final ProvisionerImpl provisioner = new ProvisionerImpl();

    @Before
    public void setUp() throws Exception {
        System.setProperty("java.protocol.handler.pkgs", "com.ibm.ws.kernel.internal");
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockBundleContext).getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                will(returnValue(mockFrameworkStartLevel));

                one(mockFrameworkStartLevel).adapt(FrameworkStartLevel.class);
                will(returnValue(mockFrameworkStartLevel));

                one(mockFrameworkStartLevel).setInitialBundleStartLevel(KernelStartLevel.ACTIVE.getLevel());

                allowing(mockBundleContext).createFilter(with(any(String.class)));
                will(returnValue(mockFilter));

                allowing(mockBundleContext).addServiceListener(with(any(ServiceListener.class)), with(any(String.class)));
                allowing(mockBundleContext).getServiceReferences("com.ibm.ws.kernel.provisioning.LibertyBootRuntime", null);
                will(returnValue(new ServiceReference[0]));
            }
        });
    }

    @Test
    public void testInstallNonexistentBundle() throws Exception {

        BundleInstallStatus iStatus;

        // B -- non-existent bundle

        KernelResolver resolver = new KernelResolver(config.getInstallRoot(), null, "kernelCoreMissing-1.0", "defaultLogging-1.0", null);
        config.setKernelResolver(resolver);

        provisioner.getServices(mockBundleContext);
        iStatus = provisioner.installBundles(config);

        String listMissingBundles = iStatus.listMissingBundles();

        assertTrue("B: Bundles notexist should be missing: " + listMissingBundles, iStatus.bundlesMissing());
        assertNotNull("B: listMissingBundles should not return null: " + listMissingBundles, listMissingBundles);
        assertTrue("B: List of missing bundles should include p.q: " + listMissingBundles, listMissingBundles.contains("p.q"));
    }

    @Test
    public void testInstallBundle() throws Exception {
        final String m = "testInstallBundle";

        BundleInstallStatus iStatus;

        context.checking(new Expectations() {
            {
                one(mockBundleContext).installBundle(with(any(String.class)), with(any(InputStream.class)));
                will(returnValue(mockBundle));

                one(mockBundle).adapt(BundleStartLevel.class);
                will(returnValue(mockBundleStartLevel));

                one(mockBundle).adapt(BundleRevision.class);
                will(returnValue(mockBundleRevision));

                one(mockBundleRevision).getTypes();
                will(returnValue(0));

                one(mockBundleStartLevel).getStartLevel();
                will(returnValue(1));

                one(mockBundleStartLevel).setStartLevel(KernelStartLevel.BOOTSTRAP.getLevel());
            }
        });

        KernelResolver resolver = new KernelResolver(config.getInstallRoot(), null, "kernelCore-1.0", "emptyLogging-1.0", null);
        config.setKernelResolver(resolver);

        provisioner.getServices(mockBundleContext);
        iStatus = provisioner.installBundles(config);

        if (iStatus.bundlesMissing())
            System.out.println(iStatus.listMissingBundles());

        assertFalse(m + " C: There should be no missing bundles", iStatus.bundlesMissing());
        assertNull(m + " C: The list of missing bundles should be null", iStatus.listMissingBundles());

        recordExceptions(iStatus); // print any unexpected exceptions for
        // debug

        assertFalse(m + " C: There should not be an install exception", iStatus.installExceptions());
        assertNull(m + " C: There should not be an exception to trace", iStatus.traceInstallExceptions());

        if (iStatus.bundlesToStart())
            System.out.println(iStatus.getBundlesToStart());

        assertTrue(m + " C: Bundles were installed", iStatus.bundlesToStart());
        assertTrue(m + " C: The list of bundles should contain the installed bundle", iStatus.getBundlesToStart().contains(mockBundle));
    }

    @Test
    public void testInstallExistingBundle() throws Exception {
        final String m = "testInstallExistingBundle";

        BundleInstallStatus iStatus;

        KernelResolver resolver = new KernelResolver(config.getInstallRoot(), null, "kernelCore-1.0", "kernelCore-1.0", null);
        config.setKernelResolver(resolver);

        String testClassesDir = System.getProperty("test.classesDir", "bin_test");
        File simple_1 = new File(testClassesDir + "/test data/lbr/lib", "x.y_1.0.jar");
        final String locationString = "reference:" + simple_1.toURI().toURL().toString();

        context.checking(new Expectations() {
            {
                one(mockBundleContext).installBundle("kernel@" + locationString, null);
                will(returnValue(mockBundle));

                one(mockBundle).adapt(BundleRevision.class);
                will(returnValue(mockBundleRevision));

                one(mockBundleRevision).getTypes();
                will(returnValue(0));

                one(mockBundle).adapt(BundleStartLevel.class);
                will(returnValue(mockBundleStartLevel));

                one(mockBundleStartLevel).getStartLevel();
                will(returnValue(1));

                one(mockBundleStartLevel).setStartLevel(KernelStartLevel.BOOTSTRAP.getLevel());
            }
        });

        provisioner.getServices(mockBundleContext);
        iStatus = provisioner.installBundles(config);

        if (iStatus.bundlesMissing())
            System.out.println(iStatus.listMissingBundles());

        assertFalse(m + " C: There should be no missing bundles", iStatus.bundlesMissing());
        assertNull(m + " C: The list of missing bundles should be null", iStatus.listMissingBundles());

        recordExceptions(iStatus); // print any unexpected exceptions for
        // debug

        assertFalse(m + " C: There should not be an install exception", iStatus.installExceptions());
        assertNull(m + " C: There should not be an exception to trace", iStatus.traceInstallExceptions());

        if (iStatus.bundlesToStart())
            System.out.println(iStatus.getBundlesToStart());

        assertTrue(m + " C: Bundles were installed", iStatus.bundlesToStart());
        assertTrue(m + " C: The list of bundles should contain the installed bundle", iStatus.getBundlesToStart().contains(mockBundle));
    }

    @Test
    public void testInstallBundleException() throws Exception {

        BundleInstallStatus iStatus;

        // D -- existing bundle with EXCEPTION on install;
        // one call to installBundle (that will throw exception)
        // no calls to getHeaders

        final BundleException testEx = new BundleException("Expected exception installing bundle");

        KernelResolver resolver = new KernelResolver(config.getInstallRoot(), null, "kernelCore-1.0", "emptyLogging-1.0", null);
        config.setKernelResolver(resolver);

        context.checking(new Expectations() {
            {
                one(mockBundleContext).installBundle(with(any(String.class)), with(any(InputStream.class)));
                will(throwException(testEx));

                never(mockBundle).adapt(BundleRevision.class);
                never(mockBundleRevision).getTypes();

                never(mockBundle).adapt(BundleStartLevel.class);
                never(mockBundleStartLevel).getStartLevel();
                never(mockBundleStartLevel).setStartLevel(with(any(int.class)));
            }
        });

        provisioner.getServices(mockBundleContext);
        iStatus = provisioner.installBundles(config);

        assertFalse("D: There should be no bundles installed", iStatus.bundlesToStart());
        assertTrue("D: There should be an install exception", iStatus.installExceptions());
        assertNotNull("D: There should be exceptions to trace", iStatus.traceInstallExceptions());

        Map<String, Throwable> badness = iStatus.getInstallExceptions();
        System.out.println(badness);

        assertTrue("D: There should be an exception associated with bad bundle", badness.keySet().contains("x.y;version=\"[1.0.0,1.0.100)\""));
        assertEquals("D: The exception in the map should match the one thrown", badness.get("x.y;version=\"[1.0.0,1.0.100)\""), testEx);
    }

    @Test
    public void testInstallFragment() throws Exception {

        BundleInstallStatus iStatus;

        KernelResolver resolver = new KernelResolver(config.getInstallRoot(), null, "kernelCore-1.0", "emptyLogging-1.0", null);
        config.setKernelResolver(resolver);

        // E -- existing FRAGMENT bundle;
        // should not be added to list of "installed" bundles
        context.checking(new Expectations() {
            {
                one(mockBundleContext).installBundle(with(any(String.class)), with(any(InputStream.class)));
                will(returnValue(mockBundle));

                one(mockBundle).adapt(BundleRevision.class);
                will(returnValue(mockBundleRevision));

                one(mockBundleRevision).getTypes();
                will(returnValue(BundleRevision.TYPE_FRAGMENT));

                never(mockBundle).adapt(BundleStartLevel.class);
                never(mockBundleStartLevel).getStartLevel();
                never(mockBundleStartLevel).setStartLevel(with(any(int.class)));
            }
        });

        provisioner.getServices(mockBundleContext);
        iStatus = provisioner.installBundles(config);

        assertFalse("E: The fragment bundle should not be in the list of installed bundles", iStatus.bundlesToStart());
    }

    @Test
    public void testStartNullList() {
        provisioner.getServices(mockBundleContext);

        // A -- null properties, empty array

        provisioner.startBundles(null);
        provisioner.startBundles(new ArrayList<Bundle>());
    }

    @Test
    public void testStartBundle() throws Exception {
        BundleStartStatus sStatus;
        provisioner.getServices(mockBundleContext);

        // B -- mock bundle;
        // one call to b.start, bundle already started

        List<Bundle> bList = new ArrayList<Bundle>();
        bList.add(mockBundle);

        context.checking(new Expectations() {
            {
                one(mockBundle).getState();
                will(returnValue(org.osgi.framework.Bundle.RESOLVED));

                one(mockBundle).start(with(any(int.class)));
            }
        });

        sStatus = provisioner.startBundles(bList);

        assertFalse(sStatus.startExceptions());
        assertNull(sStatus.traceStartExceptions());

        // C -- Felix path
        context.checking(new Expectations() {
            {
                one(mockFrameworkStartLevel).adapt(FrameworkStartLevel.class);
                will(returnValue(mockFrameworkStartLevel));

                one(mockFrameworkStartLevel).setInitialBundleStartLevel(KernelStartLevel.ACTIVE.getLevel());

                one(mockBundle).getState();
                will(returnValue(org.osgi.framework.Bundle.RESOLVED));

                one(mockBundle).start(with(any(int.class)));
            }
        });

        provisioner.getServices(mockBundleContext);

        sStatus = provisioner.startBundles(bList);
    }

    @Test
    public void testStartStartedBundle() throws Exception {
        BundleStartStatus sStatus;
        provisioner.getServices(mockBundleContext);

        // C -- mock bundle;
        // one call to b.start, works fine

        List<Bundle> bList = new ArrayList<Bundle>();
        bList.add(mockBundle);

        context.checking(new Expectations() {
            {
                one(mockBundle).getState();
                will(returnValue(org.osgi.framework.Bundle.STARTING));

                never(mockBundle).start(with(any(int.class)));
            }
        });

        sStatus = provisioner.startBundles(bList);

        assertFalse(sStatus.startExceptions());
        assertNull(sStatus.traceStartExceptions());
    }

    @Test
    public void testStartBundleException() throws Exception {
        BundleStartStatus sStatus;
        provisioner.getServices(mockBundleContext);

        // D -- existing bundle with EXCEPTION;
        // one call to b.start, throws bundle exception

        final BundleException testEx = new BundleException("Expected exception starting bundle");

        context.checking(new Expectations() {
            {
                one(mockBundle).getState();
                will(returnValue(org.osgi.framework.Bundle.RESOLVED));

                one(mockBundle).start(with(any(int.class)));
                will(throwException(testEx));

                one(mockBundle).getSymbolicName();
                will(returnValue("mockBundle"));
            }
        });

        List<Bundle> bList = new ArrayList<Bundle>();
        bList.add(mockBundle);

        sStatus = provisioner.startBundles(bList);

        assertTrue(sStatus.startExceptions());
        assertNotNull(sStatus.traceStartExceptions());

        Map<Bundle, Throwable> badness = sStatus.getStartExceptions();
        assertTrue(badness.keySet().contains(mockBundle));
        assertEquals(badness.get(mockBundle), testEx);
    }

    @Test(expected = LaunchException.class)
    public void testInstallStatusInstallException() {
        final String m = "testInstallStatusInstallException";
        try {
            final BundleInstallStatus iStatus = new BundleInstallStatus();
            iStatus.addInstallException("bundleName", new Throwable("pretend error"));

            provisioner.getServices(mockBundleContext);
            provisioner.checkInstallStatus(iStatus);
        } catch (LaunchException b) {
            if (!b.getMessage().contains("installing"))
                outputMgr.failWithThrowable(m, new AssertionError("exception message does not contain message about exceptions while installing bundles"));

            throw b; // expected
        }
    }

    @Test(expected = LaunchException.class)
    public void testInstallStatusMissingBundles() {
        final String m = "testInstallStatusMissingBundles";
        try {
            final BundleInstallStatus iStatus = new BundleInstallStatus();
            iStatus.addMissingBundle("missing");

            provisioner.getServices(mockBundleContext);
            provisioner.checkInstallStatus(iStatus);
        } catch (LaunchException b) {
            if (!b.getMessage().contains("Missing"))
                outputMgr.failWithThrowable(m, new AssertionError("exception message does not contain message about missing bundles"));

            throw b; // expected
        }
    }

    @Test(expected = LaunchException.class)
    public void testInstallStatusNoBundles() {
        final String m = "testInstallStatusNoBundles";

        try {
            final BundleInstallStatus iStatus = new BundleInstallStatus();

            // no bundles to start..
            provisioner.getServices(mockBundleContext);
            provisioner.checkInstallStatus(iStatus);
        } catch (LaunchException b) {
            if (!b.getMessage().contains("No required bundles"))
                outputMgr.failWithThrowable(m, new AssertionError("exception message does not contain message about no bundles to start"));

            throw b; // expected
        }
    }

    @Test(expected = LaunchException.class)
    public void testCheckStartStatusStartException() throws InvalidBundleContextException {

        context.checking(new Expectations() {
            {
                oneOf(mockBundle).getLocation();
                will(returnValue("bundle location"));
            }
        });

        BundleStartStatus startStatus = new BundleStartStatus();
        startStatus.addStartException(mockBundle, new BundleException("fake exception"));

        provisioner.getServices(mockBundleContext);
        provisioner.checkStartStatus(startStatus);
    }

    @Test(expected = InvalidBundleContextException.class)
    public void testCheckStartStatusInvalidContext() throws InvalidBundleContextException {
        BundleStartStatus startStatus = new BundleStartStatus();
        startStatus.markContextInvalid();

        provisioner.getServices(mockBundleContext);
        provisioner.checkStartStatus(startStatus);
    }

    protected void recordExceptions(BundleInstallStatus iStatus) {

        if (iStatus.installExceptions()) {
            Map<String, Throwable> exceptions = iStatus.getInstallExceptions();
            for (String key : exceptions.keySet()) {
                Throwable t = exceptions.get(key);
                System.out.println("+UNEXPECTED EXCEPTION for " + key);
                System.out.println("ex: " + t.getMessage());
                t.printStackTrace(System.out);
            }
        }
    }

}
