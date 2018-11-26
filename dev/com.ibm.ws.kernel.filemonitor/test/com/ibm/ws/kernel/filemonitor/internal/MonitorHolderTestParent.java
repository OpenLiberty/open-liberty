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
package com.ibm.ws.kernel.filemonitor.internal;

import static com.ibm.ws.kernel.filemonitor.internal.MapReader.readFromMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsCollectionContaining;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.kernel.filemonitor.internal.MonitorHolder.MonitorState;
import com.ibm.ws.kernel.filemonitor.internal.scan.ScanningCoreServiceImpl;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

import test.common.SharedOutputManager;
import test.utils.TestUtils;

/**
 *
 */
public abstract class MonitorHolderTestParent {

    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("fileMonitor=all=enabled").logTo(TestUtils.TEST_DATA);
    @Rule
    public final TestRule outputRule = outputMgr;

    Mockery context = new JUnit4Mockery();
    CoreService mockCoreService = context.mock(CoreService.class);
    Map<String, Object> props = new HashMap<String, Object>();

    ComponentContext mockComponentContext = context.mock(ComponentContext.class);
    BundleContext mockBundleContext = context.mock(BundleContext.class);
    Bundle mockBundle = context.mock(Bundle.class);
    @SuppressWarnings("unchecked")
    ServiceReference<FileMonitor> mockServiceReference = context.mock(ServiceReference.class);
    FileMonitor mockFileMonitor = context.mock(FileMonitor.class);
    WsLocationAdmin mockLocation = context.mock(WsLocationAdmin.class);

    ScheduledExecutorService mockScheduler = context.mock(ScheduledExecutorService.class);
    ScheduledFuture<?> mockFuture = context.mock(ScheduledFuture.class);

    Field scheduledFuture;
    Field monitors;
    Field monitorState;

    @Before
    public void setUp() {
        props.clear();
        props.put(FileMonitor.MONITOR_FILTER, null);
        props.put(FileMonitor.MONITOR_INTERVAL, "1s");
        props.put(FileMonitor.MONITOR_RECURSE, Boolean.FALSE);
        props.put(FileMonitor.MONITOR_INCLUDE_SELF, Boolean.FALSE);
        props.put(FileMonitor.MONITOR_FILES, null);
        props.put(FileMonitor.MONITOR_DIRECTORIES, null);

        try {
            scheduledFuture = MonitorHolder.class.getDeclaredField("scheduledFuture");
            scheduledFuture.setAccessible(true);
            monitors = MonitorHolder.class.getDeclaredField("updateMonitors");
            monitors.setAccessible(true);
            monitorState = MonitorHolder.class.getDeclaredField("monitorState");
            monitorState.setAccessible(true);
        } catch (Exception e) {
            outputMgr.failWithThrowable("setUp", e);
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#MonitorHolder(com.ibm.ws.kernel.filemonitor.internal.CoreService, org.osgi.framework.ServiceReference)}.
     */
    @Test(expected = NullPointerException.class)
    public void testMonitorHolderNullCoreService() {
        instantiateMonitor(null, null);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#MonitorHolder(com.ibm.ws.kernel.filemonitor.internal.CoreService, org.osgi.framework.ServiceReference)}.
     */
    @Test(expected = NullPointerException.class)
    public void testMonitorHolderNullServiceReference() {
        instantiateMonitor(mockCoreService, null);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#MonitorHolder(com.ibm.ws.kernel.filemonitor.internal.CoreService, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void testMonitorHolderBadFilter() {
        // To test the filter we need to be doing directory monitoring
        props.put(FileMonitor.MONITOR_DIRECTORIES, "somedirs");
        // Return an Integer value for the recurse property, which expects a String
        props.put(FileMonitor.MONITOR_FILTER, Integer.valueOf(1));

        createMonitorHolderAndVerifyInvalidArguments("testMonitorHolderBadFilter", "CWWKE0400W");
    }

    /**
     * Test method for
     * {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#MonitorHolder(com.ibm.ws.kernel.filemonitor.internal.CoreService, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void testMonitorHolderBadRecurse() {
        // Return an Integer value for the recurse property, which expects a Boolean
        props.put(FileMonitor.MONITOR_RECURSE, Integer.valueOf(6));
        createMonitorHolderAndVerifyInvalidArguments("testMonitorHolderBadRecurse", "CWWKE0103W");
    }

    /**
     * Test method for
     * {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#MonitorHolder(com.ibm.ws.kernel.filemonitor.internal.CoreService, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void testMonitorHolderBadIncludeSelf() {
        // Return an Integer value for the include self property, which expects a Boolean
        props.put(FileMonitor.MONITOR_INCLUDE_SELF, Integer.valueOf(6));
        createMonitorHolderAndVerifyInvalidArguments("testMonitorHolderBadIncludeSelf", "CWWKE0103W");
    }

    /**
     * Test method for
     * {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#MonitorHolder(com.ibm.ws.kernel.filemonitor.internal.CoreService, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void testMonitorHolderBadInterval() {
        // Return an Integer value for the filter property (which expects a String)
        props.put(FileMonitor.MONITOR_INTERVAL, "alphaonly");

        createMonitorHolderAndVerifyInvalidArguments("testMonitorHolderBadInterval", "CWWKE0401W");
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#parseTimeUnit}.
     */
    @Test
    public void testParseTimeUnit() {
        final String m = "testParseTimeUnit";
        try {
            // Verify time units return the expected/same TimeUnit types
            assertSame("ms should return TimeUnit.MILLISECONDS", TimeUnit.MILLISECONDS, MonitorHolder.parseTimeUnit("ms", null));
            assertSame("s should return TimeUnit.SECONDS", TimeUnit.SECONDS, MonitorHolder.parseTimeUnit("s", null));
            assertSame("m should return TimeUnit.MINUTES", TimeUnit.MINUTES, MonitorHolder.parseTimeUnit("m", null));
            assertSame("h should return TimeUnit.HOURS", TimeUnit.HOURS, MonitorHolder.parseTimeUnit("h", null));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#parseTimeUnit}.
     */
    @Test
    public void testParseBadTimeUnit() {
        final String m = "testParseBadTimeUnit";
        try {
            MonitorHolder.parseTimeUnit("mss", null);
        } catch (IllegalArgumentException e) {
            if (!outputMgr.checkForStandardOut("CWWKE0401W")) {
                outputMgr.failWithThrowable(m, e);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#parseInterval}.
     */
    @Test
    public void testParseBadInterval() {
        final String m = "testParseInterval";
        try {
            MonitorHolder.parseInterval("badLong", null);
        } catch (IllegalArgumentException e) {
            if (!outputMgr.checkForStandardOut("CWWKE0401W")) {
                outputMgr.failWithThrowable(m, e);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#init(java.io.File)}.
     */
    @Test
    public void testInit() {
        final String m = "testInit";

        // All valid properties: file & directory lists are both empty
        // future will not be scheduled (nothing to do)
        setConfigExpectations();
        setOnBaselineExpectations();

        try {
            MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);
            monitor.init();
            assertNull("ScheduledFuture should not be allocated: nothing to monitor", getFuture(monitor));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#init(java.io.File)}.
     */
    @Test
    public void testInitMonitorFileList() {
        final String m = "testInitMonitorFileList";

        try {
            final File f1 = TestUtils.createTempFile(m, ".1");
            final File f2 = TestUtils.createTempFile(m, ".2");

            // Monitor f1 and f2 for changes: because we now have files, the call to init
            // should start a monitor task
            props.put(FileMonitor.MONITOR_FILES, f1.getAbsolutePath() + "," + f2.getAbsolutePath());

            setConfigExpectations();
            setFullInitExpectations(f1, f2);

            MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);

            monitor.init();
            assertNotNull("ScheduledFuture should be allocated to monitor files", getFuture(monitor));
            assertEquals("Should only have one monitor due to throw when resolving .2",
                         1, getUpdateMonitors(monitor).size());
            assertTrue("CWWKE0403W should be present when exception occurs creating UpdateMonitor",
                       outputMgr.checkForStandardOut("CWWKE0403W"));

            monitor.stop();
            assertNull("ScheduledFuture should be null after stop()", getFuture(monitor));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#init(java.io.File)}.
     */
    @Test
    public void testInitMonitorDirectoryList() {
        final String m = "testInitMonitorDirectoryList";

        try {
            final File f1 = TestUtils.createTempDirectory(m + ".3");
            final File f2 = TestUtils.createTempDirectory(m + ".4");

            // Monitor f1 and f2 for changes: because we now have files, the call to init
            // should start a monitor task
            props.put(FileMonitor.MONITOR_DIRECTORIES, f1.getAbsolutePath() + "," + f2.getAbsolutePath());

            setConfigExpectations();
            setFullInitExpectations(f1, f2);
            context.checking(new Expectations() {
                {
                    allowing(mockLocation).resolveString(f1.getAbsolutePath());
                    will(returnValue(f1.getAbsolutePath()));

                    // Explicitly throw when trying to resolve/add the second file,
                    // this should not bomb the whole method, but should generate a
                    // warning.
                    allowing(mockLocation).resolveString(f2.getAbsolutePath());
                    will(throwException(new RuntimeException("Dummy forced exception")));
                }
            });

            MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);
            assertEquals("State should be INIT after constructor", MonitorState.INIT, getMonitorState(monitor));

            // Verify behavior with a bad file as the cache root:
            // expect a message about being forced to store information about resources last
            // seen in memory
            monitor.init();
            assertNotNull("ScheduledFuture should be allocated to monitor directories", getFuture(monitor));
            assertEquals("Should only have one monitor due to throw when resolving .4",
                         1, getUpdateMonitors(monitor).size());
            assertTrue("CWWKE0403W should be present when exception occurs creating UpdateMonitor",
                       outputMgr.checkForStandardOut("CWWKE0403W"));
            assertEquals("State should be ACTIVE after constructor", MonitorState.ACTIVE, getMonitorState(monitor));

            monitor.destroy();
            assertNull("ScheduledFuture should be null after stop()", getFuture(monitor));
            assertEquals("State should be DESTROYED after destroy() with no contention",
                         MonitorState.DESTROYED, getMonitorState(monitor));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#init(java.io.File)}.
     */
    @Test
    public void testBadInitMonitorDirectoryList() {
        final String m = "testBadInitMonitorDirectoryList";

        try {
            final File f1 = TestUtils.createTempDirectory(m + ".3");
            final File f2 = TestUtils.createTempDirectory(m + ".4");

            // Monitor f1 and f2 for changes: because we now have files, the call to init
            // should start a monitor task
            props.put(FileMonitor.MONITOR_DIRECTORIES, f1.getAbsolutePath() + "," + f2.getAbsolutePath());

            setConfigExpectations();
            setFullBadInitExpectations(f1, f2);
            context.checking(new Expectations() {
                {
                    allowing(mockLocation).resolveString(f1.getAbsolutePath());
                    will(returnValue(f1.getAbsolutePath()));

                    // Explicitly throw when trying to resolve/add the second file,
                    // this should not bomb the whole method, but should generate a
                    // warning.
                    allowing(mockLocation).resolveString(f2.getAbsolutePath());
                    will(throwException(new RuntimeException("Dummy forced exception")));
                }
            });

            MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);
            assertEquals("State should be INIT after constructor", MonitorState.INIT, getMonitorState(monitor));

            // Verify behavior with a bad file as the cache root:
            // expect a message about being forced to store information about resources last
            // seen in memory
            monitor.init();
            assertNull("ScheduledFuture should NOT be allocated to monitor directories", getFuture(monitor));
            assertEquals("Should NOT have a monitor due to throw when resolving .4",
                         0, getUpdateMonitors(monitor).size());
            //will not be this message, as the monitor will never power up, due to having a null service..
            assertTrue("CWWKE0403W should NOT present when exception occurs creating UpdateMonitor",
                       !outputMgr.checkForStandardOut("CWWKE0403W"));
            assertEquals("State should be DESTROYED after init", MonitorState.DESTROYED, getMonitorState(monitor));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.MonitorHolder#init(java.io.File)}.
     */
    @Test
    public void testInitMonitorDirectoryListRecurse() {
        final String m = "testInitMonitorDirectoryListRecurse";

        try {
            final File f1 = TestUtils.createTempDirectory(m + ".5");
            final File f2 = TestUtils.createTempDirectory(m + ".6");

            // Monitor f1 and f2 for changes: because we now have files, the call to init
            // should start a monitor task
            props.put(FileMonitor.MONITOR_DIRECTORIES, f1.getAbsolutePath() + "," + f2.getAbsolutePath());

            // Check again with recurse turned on
            props.put(FileMonitor.MONITOR_RECURSE, true);

            setConfigExpectations();
            setFullInitExpectations(f1, f2);

            MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);

            // Verify behavior with a bad file as the cache root:
            // expect a message about being forced to store information about resources last
            // seen in memory
            monitor.init();
            assertNotNull("ScheduledFuture should be allocated to monitor directories", getFuture(monitor));
            assertEquals("Should only have one monitor due to throw when resolving .4",
                         1, getUpdateMonitors(monitor).size());
            assertTrue("CWWKE0403W should be present when exception occurs creating UpdateMonitor",
                       outputMgr.checkForStandardOut("CWWKE0403W"));

            monitor.stop();
            assertNull("ScheduledFuture should be null after stop()", getFuture(monitor));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testCoreServiceSetMonitor() {
        final String m = "testCoreServiceSetMonitor";
        try {
            final File f1 = TestUtils.createTempFile(m, ".6");
            final Collection<File> empty = Collections.emptySet();

            setConfigExpectations();

            CoreServiceImpl impl = new ScanningCoreServiceImpl();
            impl.setMonitor(mockServiceReference);

            context.checking(new Expectations() {
                {
                    allowing(mockComponentContext).getBundleContext();
                    will(returnValue(mockBundleContext));

                    allowing(mockBundleContext).getDataFile(with(any(String.class)));
                    will(returnValue(f1));

                    allowing(mockServiceReference).getBundle();
                    will(returnValue(mockBundle));

                    allowing(mockBundle).getBundleId();
                    will(returnValue(1L));

                    oneOf(mockFileMonitor).onBaseline(empty);

                    oneOf(mockComponentContext).locateService(CoreServiceImpl.MONITOR, mockServiceReference);
                    will(returnValue(mockFileMonitor));
                }
            });
            impl.activate(mockComponentContext, props);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * This test is part of 48922 to make sure that when there the run has been executed on a different thread then the delete can still take place.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testDestroyWithMultipleThreads() {
        final String m = "testDestroyWithMultipleThreads";

        try {
            setConfigExpectations();
            // There is only a limited set of init expectations as we are not setting up any directories to be monitored
            context.checking(new Expectations() {
                {
                    atLeast(1).of(mockFileMonitor).onBaseline(with(any(Collection.class)));

                    atLeast(1).of(mockCoreService).getReferencedMonitor(mockServiceReference);
                    will(returnValue(mockFileMonitor));
                }
            });

            // Check the set up is completed successfully
            MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);
            assertEquals("State should be INIT after constructor", MonitorState.INIT, getMonitorState(monitor));
            monitor.init();
            assertEquals("State should be ACTIVE after constructor", MonitorState.ACTIVE, getMonitorState(monitor));

            // Call the run method on a separate thread
            Thread secondThread = new Thread(monitor);
            secondThread.start();
            int timeoutCounter = 0;
            while (secondThread.isAlive()) {
                if (timeoutCounter > 10) {
                    fail("Test timeout due to the run not completing within 5 seconds");
                }
                timeoutCounter++;
                Thread.sleep(500);
            }

            // Wait for this thread to complete then destroy, this should still be possible
            monitor.destroy();
            assertNull("ScheduledFuture should be null after stop()", getFuture(monitor));
            assertEquals("State should be DESTROYED after destroy() with no contention",
                         MonitorState.DESTROYED, getMonitorState(monitor));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTimedAndExternalScans() throws Exception {
        final String m = "testTimedNotificationWorksWithNoPropertySetting";
        try {
            final File temporaryFile = createFileAndSetExpectationsForInit();
            context.checking(new Expectations() {
                {
                    // expect to see the modify but not the delete
                    oneOf(mockFileMonitor).onChange(with(files()), with(files(temporaryFile)), with(files()));
                    never(mockFileMonitor).onChange(with(files()), with(files()), with(files(temporaryFile)));
                }
            });

            createMonitorAndDriveTimedAndExternalScans(temporaryFile);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }

    }

    @SuppressWarnings("unchecked")
    private static Matcher<Collection<File>> files(File... files) {
        Object result;
        switch (files.length) {
            case 0:
                result = Expectations.any(Collection.class);
                break;
            case 1:
                result = IsCollectionContaining.hasItem(files[0]);
                break;
            default:
                result = IsCollectionContaining.hasItems(files);
        }
        return (Matcher<Collection<File>>) result;
    }

    private static <T> Set<T> newSet(T... elems) {
        if (elems.length == 0)
            return Collections.emptySet();
        return new HashSet<T>(Arrays.asList(elems));
    }

    private void createMonitorAndDriveTimedAndExternalScans(File temporaryFile) throws IOException {
        // Check the set up is completed successfully
        MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);

        monitor.init();

        // modify the file, then trigger a 'timed' scan
        new FileWriter(temporaryFile).append("hello").close();
        monitor.scheduledScan();

        // delete the file then trigger an 'external' scan
        temporaryFile.delete();
        Set<String> paths = newSet(temporaryFile.getCanonicalPath());

        Set<File> canonicalCreated = PathUtils.getFixedPathFiles(paths);
        Set<File> canonicalDeleted = PathUtils.getFixedPathFiles(paths);
        Set<File> canonicalModified = PathUtils.getFixedPathFiles(paths);

        monitor.externalScan(canonicalCreated, canonicalDeleted, canonicalModified, true, null);

        // check that scanComplete() was called
        context.assertIsSatisfied();
    }

    /**
     * @return
     */
    protected abstract MonitorHolder instantiateMonitor(CoreService mockCoreService, ServiceReference<FileMonitor> mockServiceReference);

    private void createMonitorAndDriveTimedAndExternalScansForDir(File temporaryFile, File subDir, File subFile) throws IOException {
        // Check the set up is completed successfully
        MonitorHolder monitor = instantiateMonitor(mockCoreService, mockServiceReference);

        monitor.init();

        // create the subDirectory
        subDir.mkdirs();

        // create the file in the subdir, then trigger an external scan
        new FileWriter(subFile).append("hello").close();

        Set<String> paths = newSet(subFile.getCanonicalPath());

        Set<File> canonicalCreated = PathUtils.getFixedPathFiles(paths);
        Set<File> canonicalDeleted = PathUtils.getFixedPathFiles(paths);
        Set<File> canonicalModified = PathUtils.getFixedPathFiles(paths);

        monitor.externalScan(canonicalCreated, canonicalDeleted, canonicalModified, true, null);

        // delete the file then trigger an 'external' scan
        subFile.delete();
        subDir.delete();

        monitor.externalScan(canonicalCreated, canonicalDeleted, canonicalModified, true, null);

        // check that scanComplete() was called
        context.assertIsSatisfied();

    }

    @Test
    public void testTimedAndExternalScansWithMonitorTypeTimed() throws Exception {
        props.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_TIMED);
        testTimedAndExternalScans();
    }

    @Test
    public void testTimedAndExternalScansWithMonitorTypeExternal() throws Exception {
        final String m = "testTimedAndExternalScansWithMonitorTypeExternal";
        try {
            props.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_EXTERNAL);
            final File temporaryFile = createFileAndSetExpectationsForInit();
            context.checking(new Expectations() {
                {
                    // expect not to see the modify, but to see the delete
                    never(mockFileMonitor).onChange(with(files()), with(files(temporaryFile)), with(files()));
                    oneOf(mockFileMonitor).onChange(with(files()), with(files()), with(files(temporaryFile.getCanonicalFile())));
                }
            });

            createMonitorAndDriveTimedAndExternalScans(temporaryFile);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testTimedAndExternalScansForParentsWithMonitorTypeExternal() throws Exception {
        final String m = "testTimedAndExternalScansForParentsWithMonitorTypeExternal";
        try {
            props.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_EXTERNAL);
            final File temporaryFile = createDirAndSetExpectationsForInit();
            final File subDir = new File(temporaryFile, File.separator + "FISH");
            final File subFile = new File(subDir, "ZOMBIES");

            context.checking(new Expectations() {
                {
                    // expect not to see the modify, but to see the create and delete
                    never(mockFileMonitor).onChange(with(files()), with(files(temporaryFile)), with(files()));
                    oneOf(mockFileMonitor).onChange(with(files(subDir.getCanonicalFile())), with(files()), with(files()));
                    oneOf(mockFileMonitor).onChange(with(files()), with(files()), with(files(subDir.getCanonicalFile())));
                }
            });

            createMonitorAndDriveTimedAndExternalScansForDir(temporaryFile, subDir, subFile);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @SuppressWarnings("unchecked")
    private File createFileAndSetExpectationsForInit() throws IOException {
        // create the temporary file
        final File temporaryFile = File.createTempFile(MonitorHolder.class.getSimpleName(), ".tmp");
        temporaryFile.deleteOnExit();
        // stash the name in the appropriate property
        props.put(FileMonitor.MONITOR_FILES, temporaryFile.getAbsolutePath());
        // initialise the mock object properties
        setConfigExpectations();
        // This is subtly different from setFullInitExpectations()
        // They should be unified, but this is deferred indefinitely
        // as not critical to any of our function.
        context.checking(new Expectations() {
            {
                // allow the monitor to be retrieved
                atLeast(1).of(mockCoreService).getReferencedMonitor(mockServiceReference);
                will(returnValue(mockFileMonitor));

                // allow the scheduler to be retrieved
                allowing(mockCoreService).getScheduler();
                will(returnValue(mockScheduler));

                // allow the MonitorHolder to be scheduled
                allowing(mockScheduler).scheduleWithFixedDelay(with(any(MonitorHolder.class)), with(any(long.class)), with(any(long.class)), with(any(TimeUnit.class)));
                will(returnValue(mockFuture));

                // allow the scheduledFuture to be cancelled
                allowing(mockFuture).cancel(with(any(boolean.class)));

                // allow the file path to be retrieved
                allowing(mockCoreService).getLocationService();
                will(returnValue(mockLocation));
                allowing(mockLocation).resolveString(with(temporaryFile.getAbsolutePath()));
                will(returnValue(temporaryFile.getAbsolutePath()));

                // allow trace setting to be queried
                allowing(mockCoreService).isDetailedScanTraceEnabled();
                will(returnValue(true));

                // expect onBaseline() to be called
                atLeast(1).of(mockFileMonitor).onBaseline(with(any(Collection.class)));
            }
        });
        return temporaryFile;
    }

    @SuppressWarnings("unchecked")
    private File createDirAndSetExpectationsForInit() throws IOException {
        // create the temporary dir..
        final File temporaryFile = File.createTempFile(MonitorHolder.class.getSimpleName(), ".tmp");
        if (!temporaryFile.delete())
            throw new IOException("Test failure could not delete temp file to create temp dir.");
        if (!temporaryFile.mkdirs())
            throw new IOException("Test failure could not create temp dir.");
        temporaryFile.deleteOnExit();

        // stash the name in the appropriate property (dir, because we are using a dir here)
        props.put(FileMonitor.MONITOR_DIRECTORIES, temporaryFile.getAbsolutePath());
        // set recurse to false..
        props.put(FileMonitor.MONITOR_RECURSE, false);

        // initialise the mock object properties
        setConfigExpectations();
        // This is subtly different from setFullInitExpectations()
        // They should be unified, but this is deferred indefinitely
        // as not critical to any of our function.
        context.checking(new Expectations() {
            {
                // allow the monitor to be retrieved
                atLeast(1).of(mockCoreService).getReferencedMonitor(mockServiceReference);
                will(returnValue(mockFileMonitor));

                // allow the scheduler to be retrieved
                allowing(mockCoreService).getScheduler();
                will(returnValue(mockScheduler));

                // allow the MonitorHolder to be scheduled
                allowing(mockScheduler).scheduleWithFixedDelay(with(any(MonitorHolder.class)), with(any(long.class)), with(any(long.class)), with(any(TimeUnit.class)));
                will(returnValue(mockFuture));

                // allow the scheduledFuture to be cancelled
                allowing(mockFuture).cancel(with(any(boolean.class)));

                // allow the file path to be retrieved
                allowing(mockCoreService).getLocationService();
                will(returnValue(mockLocation));
                allowing(mockLocation).resolveString(with(temporaryFile.getAbsolutePath()));
                will(returnValue(temporaryFile.getAbsolutePath()));

                // allow trace setting to be queried
                allowing(mockCoreService).isDetailedScanTraceEnabled();
                will(returnValue(true));

                // expect onBaseline() to be called
                atLeast(1).of(mockFileMonitor).onBaseline(with(any(Collection.class)));
            }
        });
        return temporaryFile;
    }

    protected void createMonitorHolderAndVerifyInvalidArguments(String methodName, String expectedMsgKey) {
        try {
            setConfigExpectations();
            instantiateMonitor(mockCoreService, mockServiceReference);
        } catch (IllegalArgumentException e) {
            if (!outputMgr.checkForStandardOut(expectedMsgKey)) {
                // This may be the correct exception, but with a wrong message
                // (for example, if the message wasn't translated correctly)
                fail("An expected exception was received, but the key " + expectedMsgKey + " was not found in the logs. The exception is: " + e);
            }
            // Otherwise, we got what we expected and are good
            return;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
        // If we didn't get any exception at all, better fail
        fail("Creating a monitor holder did not produce the expected illegal argument exception");
    }

    protected void setConfigExpectations() {
        context.checking(new Expectations() {
            {
                allowing(mockServiceReference).getProperty(Constants.SERVICE_PID);
                will(returnValue(1));

                // Delegate all other property requests to our props map
                allowing(mockServiceReference).getProperty(with(any(String.class)));
                will(readFromMap(props));
            }
        });
    }

    protected void setOnBaselineExpectations() {
        final Collection<File> empty = Collections.emptySet();
        context.checking(new Expectations() {
            {
                oneOf(mockCoreService).getReferencedMonitor(mockServiceReference);
                will(returnValue(mockFileMonitor));

                oneOf(mockFileMonitor).onBaseline(empty);
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected void setFullInitExpectations(final File f1, final File f2) {
        context.checking(new Expectations() {
            {
                atLeast(1).of(mockCoreService).getLocationService();
                will(returnValue(mockLocation));

                allowing(mockLocation).resolveString(f1.getAbsolutePath());
                will(returnValue(f1.getAbsolutePath()));

                atLeast(1).of(mockFileMonitor).onBaseline(with(any(Collection.class)));

                atLeast(1).of(mockCoreService).getScheduler();
                will(returnValue(mockScheduler));

                atLeast(1).of(mockScheduler).scheduleWithFixedDelay(with(any(Runnable.class)),
                                                                    with(any(Long.class)),
                                                                    with(any(Long.class)),
                                                                    with(any(TimeUnit.class)));
                will(returnValue(mockFuture));

                atLeast(1).of(mockCoreService).getReferencedMonitor(mockServiceReference);
                will(returnValue(mockFileMonitor));

                atLeast(1).of(mockFuture).cancel(false);
            }
        });

        if (f2 != null) {
            context.checking(new Expectations() {
                {
                    // Explicitly throw when trying to resolve/add the second file,
                    // this should not bomb the whole method, but should generate a
                    // warning.
                    allowing(mockLocation).resolveString(f2.getAbsolutePath());
                    will(throwException(new RuntimeException("Dummy forced exception")));
                }
            });

        }
    }

    @SuppressWarnings("unchecked")
    protected void setFullBadInitExpectations(final File f1, final File f2) {
        context.checking(new Expectations() {
            {
                never(mockCoreService).getLocationService();

                allowing(mockLocation).resolveString(f1.getAbsolutePath());
                will(returnValue(f1.getAbsolutePath()));

                never(mockFileMonitor).onBaseline(with(any(Collection.class)));

                never(mockCoreService).getScheduler();

                never(mockScheduler).scheduleWithFixedDelay(with(any(Runnable.class)),
                                                            with(any(Long.class)),
                                                            with(any(Long.class)),
                                                            with(any(TimeUnit.class)));

                //bad init is testing null referenced monitor..
                //possible at runtime if supplying bundle went away after registration,
                //but after init invocation.
                atLeast(1).of(mockCoreService).getReferencedMonitor(mockServiceReference);
                will(returnValue(null));

                never(mockFuture).cancel(false);
            }
        });

        if (f2 != null) {
            context.checking(new Expectations() {
                {
                    // Explicitly throw when trying to resolve/add the second file,
                    // this should not bomb the whole method, but should generate a
                    // warning.
                    allowing(mockLocation).resolveString(f2.getAbsolutePath());
                    will(throwException(new RuntimeException("Dummy forced exception")));
                }
            });

        }
    }

    protected Object getFuture(MonitorHolder mh) throws Exception {
        return scheduledFuture.get(mh);
    }

    @SuppressWarnings("unchecked")
    protected Map<UpdateMonitor, UpdateMonitor> getUpdateMonitors(MonitorHolder mh) throws Exception {
        return (Map<UpdateMonitor, UpdateMonitor>) monitors.get(mh);
    }

    protected MonitorState getMonitorState(MonitorHolder mh) throws Exception {
        return MonitorState.values()[((AtomicInteger) monitorState.get(mh)).get()];
    }
}
