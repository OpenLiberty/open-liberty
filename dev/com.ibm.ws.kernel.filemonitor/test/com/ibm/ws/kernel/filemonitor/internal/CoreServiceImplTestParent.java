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

import static com.ibm.ws.kernel.filemonitor.internal.ArgumentEchoer.echoArgument;
import static com.ibm.ws.kernel.filemonitor.internal.MapReader.readFromMap;
import static java.util.Collections.EMPTY_LIST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import test.common.SharedOutputManager;
import test.utils.TestUtils;

/**
 * Higher level tests which test that the core service drives its injected FileMonitors appropriately
 * when monitored files are updated or deleted.
 */
public abstract class CoreServiceImplTestParent {

    static final TraceComponent tc = Tr.register(CoreServiceImplTestParent.class);

    // In the event of a failure, rerun the failing test one more time
    // In my Window 7 64 bit env (quad core, SSD HD), looping these tests, I see
    // a failure about every 15 minutes. This retry block will retry the
    // failing test again. I haven't seen a test fail back to back, though
    // it can happen this should bring the intermittent failure rate down
    // a bit.
    // 152229 - commenting out.  We shouldn't need to retry anymore.
    //@Rule
    //public Retry retry = new Retry(1); // Try 1 more time after first failure

    private static final DateFormat df = new SimpleDateFormat("HH:mm:ss:SSS");

    private enum ChangeType {
        CREATE(0, "created"),
        MODIFY(1, "modified"),
        DELETE(2, "deleted"),
        NOCHANGE(3, "no change");

        private final int position;
        private final String description;

        ChangeType(int position, String description) {
            this.position = position;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }

    }

    private enum FILE_TYPE {
        DIRECTORY,
        FILE
    }

    /**
     * A little bit of time for the calls to propagate and logic to execute
     * (in addition to the time interval and known pauses)
     * We need a fairly large fudge time or the tests fail intermittently.
     * Measured in ms.
     */
    private static final int FUDGE_TIME = 160;

    private static final int DEFAULT_TIME_INTERVAL = 100;

    private static final int TIME_OUT = 60000; // 1 minute
    private final Synchroniser synchroniser = new Synchroniser();
    private Mockery context = new JUnit4Mockery() {
        {
            setThreadingPolicy(synchroniser);
        }
    };

    private CoreServiceImpl coreService;
    private ScheduledExecutorService scheduler;

    private ComponentContext mockComponentContext;

    private WsLocationAdmin mockLocation;
    private Map<String, Object> props;
    private int timeInterval;

    /** A little variable for disambiguating mocked instance names */
    private int counter;

    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all").logTo(TestUtils.TEST_DATA);
    @Rule
    public final TestRule outputRule = outputMgr;

    @Before
    public void setUp() {
        counter = 1;
        context = new JUnit4Mockery() {
            {
                setThreadingPolicy(synchroniser);
            }
        };

        mockComponentContext = context.mock(ComponentContext.class);

        mockLocation = context.mock(WsLocationAdmin.class);

        context.checking(new Expectations() {

            {
                allowing(mockLocation).resolveString(with(any(String.class)));
                will(echoArgument());
            }
        });

        props = generateDefaultProperties();

        coreService = createCoreService();

    }

    @After
    public void tearDown() {
        // Make sure old file monitors don't hang around, producing trace and eating up resources
        coreService.deactivate(0);
        coreService.unsetScheduler(scheduler);
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.CoreServiceImpl#notifyFileChanges(java.util.Collection, java.util.Collection, java.util.Collection)}. Makes
     * sure it doesn't {@link NullPointerException} if you pass in <code>null</code>.
     */
    @Test
    public void testNotifyFileChangesWithNullArguments() {
        CoreServiceImpl testObject = createCoreService();
        testObject.notifyFileChanges(null, null, null);
        // Not throwing an exception is a pass

    }

    @Test
    public void testFileMonitorGetsRightContentsOnInit() throws Exception {

        File rootDir = createDirectory();

        File childFile = createFile(rootDir); // rootDir/childFile
        File childDir = createDirectory(rootDir); // rootDir/childDir
        File grandchildFile = createFile(childDir); // rootDir/childDir/grandchildFile

        final File nonExistentDir = createDirectory();
        final File nonExistentFile = createFile(nonExistentDir); // nonExistentDir/nonExistentFile
        nonExistentFile.delete();
        nonExistentDir.delete();
        assertFalse("A file we just deleted shouldn't exist.", nonExistentFile.exists());
        assertFalse("A directory we just deleted shouldn't exist.", nonExistentDir.exists());

        // Make sure if we add a child file directly it doesn't get given back to us twice
        addFileToPropsForMonitoring(props, nonExistentFile, childFile);
        addDirToPropsForMonitoring(props, rootDir, nonExistentDir);

        List<Collection<File>> initFiles = new ArrayList<Collection<File>>();

        final FileMonitor mockFileMonitor = registerFileMonitor(props, initFiles);
        activateCoreService();

        Collection<File> expectedBaselineFiles = new HashSet<File>();
        expectedBaselineFiles.add(childFile);
        expectedBaselineFiles.add(childDir);
        expectedBaselineFiles.add(grandchildFile);
        expectedBaselineFiles.add(rootDir);

        assertEqualsOrderless("The wrong files were given as a baseline.", expectedBaselineFiles, initFiles.get(0));

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.CREATE) {

            // callback method for creating the directory and file
            @Override
            public void doCrud() throws IOException {
                nonExistentDir.mkdir();
                nonExistentFile.createNewFile();
                expectedFileList.add(nonExistentDir);
                expectedFileList.add(nonExistentFile);
            }
        };
    }

    /**
     * Higher-level test which makes sure if we tell the core service about a file monitor,
     * and then change a file, the file monitor gets told.
     *
     * @throws Exception
     */
    @Test
    public void testFileMonitorIsNotifiedOfChanges() throws Exception {

        final File file = createFile();
        addFileToPropsForMonitoring(props, file);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        modifyFilesAndAssert(mockFileMonitor, file);
    }

    @Test
    public void testFileMonitorIsNotifiedOfChangesIfActivationHappensBeforeAddingMonitor() throws Exception {

        final File file = createFile();
        addFileToPropsForMonitoring(props, file);
        props.put("monitor.directories", EMPTY_LIST);

        // This sequence isn't the order the rest of the tests use
        activateCoreService();
        FileMonitor mockFileMonitor = registerFileMonitor(props);

        // Wait for everything to get initialized before changing anything, to make sure our change gets noticed
        waitForMonitorsToStabilise();

        modifyFilesAndAssert(mockFileMonitor, file);
    }

    @Test
    public void testFileMonitorIsNotifiedOfFileCreation() throws Exception {

        final File file = createFile();
        // Delete the file we just created so it doesn't exist :)
        deleteFile(file);

        addFileToPropsForMonitoring(props, file);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        createFilesAndAssert(mockFileMonitor, FILE_TYPE.FILE, file);
    }

    @Test
    public void testFileMonitorIsNotifiedOfChangesToMultipleFiles() throws Exception {

        final File firstFile = createFile();
        final File secondFile = createFile();
        final File thirdFile = createFile();

        addFileToPropsForMonitoring(props, firstFile, secondFile, thirdFile);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        modifyFilesAndAssert(mockFileMonitor, firstFile, thirdFile);
    }

    @Test
    public void testFileMonitorIsNotifiedOfChangesToMultipleDirectories() throws Exception {

        final File firstDir = createDirectory();
        final File secondDir = createDirectory();
        final File thirdDir = createDirectory();

        addDirToPropsForMonitoring(props, firstDir, secondDir, thirdDir);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.CREATE) {

            // callback method for creating the file
            @Override
            public void doCrud() throws IOException {
                File file = createFile(secondDir); // Create a file under secondDir
                expectedFileList.add(file);
            }
        };
    }

    /**
     * Higher-level test which makes sure if we tell the core service about a file monitor,
     * and then change a file, the file monitor gets told.
     *
     * @throws Exception
     */
    @Test
    public void testFileMonitorIsNotifiedOfCreatedFile() throws Exception {

        final File file = createFile();
        addFileToPropsForMonitoring(props, file);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // Yes modify, not created.  We're testing modifying a file just created
        modifyFilesAndAssert(mockFileMonitor, file);
    }

    @Test
    public void testFileMonitorIsNotifiedOfDeletedFile() throws Exception {

        final File file = createFile();
        addFileToPropsForMonitoring(props, file);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        deleteFilesAndAssertDeleted(mockFileMonitor, file);
    }

    /**
     * Catch the case covered by defect 52611. This problem seems to be caused when a file is deleted, the timestamp/file size from the file pre deletion remained stored. When a
     * file is put back the monitor could compare the timestamp/file size and if they haven't changed it might not run the scan.
     *
     * @throws Exception
     */
    @Test
    public void testFileMonitorIsNotifiedOfDeletedFileWhichEventuallyGetsPutBack() throws Exception {

        final File file = createFile();
        addFileToPropsForMonitoring(props, file);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // Rename the file, which we hope will preserve timestamps
        final File newFile = new File(file.getAbsolutePath() + ".renamed");

        // Wait for the monitor to notice our file gets deleted by doing an assertion

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.DELETE) {

            // callback method for deleting the file
            @Override
            public void doCrud() throws IOException {
                file.renameTo(newFile);
                expectedFileList.add(file);
            }
        };

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.CREATE) {

            // callback method to restore the original file
            @Override
            public void doCrud() throws IOException {
                newFile.renameTo(file); // Now put the file back
                expectedFileList.add(file);
            }
        };
    }

    /**
     * We know that repeated file notifications only count as one modification
     * if they occur within 2 100 ms intervals of one another.
     * That's tested by {@link #testFileModificationThenModificationCountsAsOnlyOneModification()}.
     * However, if the modifications happened with a decent gap between them, we
     * should be notified twice.
     *
     * @throws Exception
     */
    @Test
    public void testRepeatedFileModificationsGetNoticed() throws Exception {

        final File file = createFile();

        addFileToPropsForMonitoring(props, file);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        for (int i = 0; i < 4; i++) {
            modifyFilesAndAssert(mockFileMonitor, file);
        }
    }

    /**
     * Test which makes sure that if one registered FileMonitor throws an exception, it doesn't
     * bring the whole system down, and other file monitors still get notified of changes.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testRogueFileMonitorDoesntRuinThingsForEveryoneElse() throws Exception {
        String methodName = "testRogueFileMonitorDoesntRuinThingsForEveryoneElse(): ";

        activateCoreService();

        // Create a mock FileMonitor which behaves normally
        File normalFile = createFile("normal");
        addFileToPropsForMonitoring(props, normalFile);
        final FileMonitor normalMockFileMonitor = registerFileMonitor(props);

        // Make a "rogue" mock FileMonitor which throws exceptions when it's onChange(...) method is called.
        // In the "Expectations" we will tell this one to throw a RuntimeException when it's notified
        Map<String, Object> rogueProps = generateDefaultProperties();
        File rogueFile = createFile("rogue");
        addFileToPropsForMonitoring(rogueProps, rogueFile);
        final FileMonitor rogueMockFileMonitor = registerFileMonitor(rogueProps);

        // Modify the rogueFile and verify that the rogueMockFileMonitor handled by throwing an exception.
        modifyFileAndAssertException(rogueMockFileMonitor, rogueFile);

        // Even though the rogue monitor threw an exception, we should still see later changes from other FileMonitors
        modifyFilesAndAssert(normalMockFileMonitor, normalFile);

        // Even the rogue monitor should get called again ... Note that the method we are calling this time will
        // set the expectations such that the FileMonitor will not throw an exception, but will behave normally.
        modifyFilesAndAssert(rogueMockFileMonitor, rogueFile);

        // Here we try to get the mock rogueFileMonitor to throw enough exceptions to get itself disabled.
        // The exceptionCount in the MonitorHolder should have been reset to 0 since the rogue mock FileMonitor
        // behaved normally on the last call.
        for (int i = 0; i < MonitorHolder.NUMBER_OF_EXCEPTIONS_BEFORE_DISABLING_MONITOR; i++) {
            Tr.debug(tc, methodName + "Attempt to cause 'rogue' FileMonitor to throw too many exceptions. i=" + i);
            modifyFileAndAssertException(rogueMockFileMonitor, rogueFile);
        }

        // Going to sleep for a bit to give time for the rogueFileMonitor to be destroyed
        Thread.sleep(1000);

        // Now the rogue file monitor should be disabled. So we don't expect any more notifications.
        Tr.debug(tc, methodName + "'Rogue' FileMonitor should be disabled at this point");

        context.checking(new Expectations() {
            {
                // We are not expecting the onChange method to be called this time.
                exactly(0).of(rogueMockFileMonitor).onChange(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class)));
            }
        });

        modifyFile(rogueFile);

        //  In this case we are not waiting on something to happen.  We are just waiting a sufficient
        //  amount of time, and then we make sure that the onChange method was not called.
        Thread.sleep(1000);
        context.assertIsSatisfied();

        // After the rogue monitor is disabled, the good monitor should still work
        modifyFilesAndAssert(normalMockFileMonitor, normalFile);
    }

    @Test
    public void testFileMonitorIsNotifiedOfDeletionOfDirectoryWhenMonitoringSelf() throws Exception {

        final File dir = createDirectory();

        addDirToPropsForMonitoring(props, dir);
        props.put(FileMonitor.MONITOR_INCLUDE_SELF, true);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // We should get told the directory got deleted
        deleteFilesAndAssertDeleted(mockFileMonitor, dir);
    }

    @Test
    public void testFileMonitorIsNotNotifiedOfDeletionOfDirectoryWhenNotMonitoringSelf() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir);

        addDirToPropsForMonitoring(props, dir);
        props.put(FileMonitor.MONITOR_INCLUDE_SELF, false); // only monitor files in the directory, but not the directory

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // If we delete files inside the directory, we should still be told about them being deleted
        deleteFilesAndAssertDeleted(mockFileMonitor, file);

        // We're not monitoring the directory itself, so we shouldn't get any notifications
        deleteFilesAndAssertNoChange(mockFileMonitor, dir);
    }

    @Test
    public void testFileMonitorIsNotifiedOfChangesInDirectory() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir); //  create file under directory

        addDirToPropsForMonitoring(props, dir);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        modifyFilesAndAssert(mockFileMonitor, file);
    }

    @Test
    public void testFileMonitorIsNotifiedOfChangesInDirectoryWhichDoesntExistAtStartOfMonitoring() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir); // Add a file in the directory

        // Delete the directory & file so we can create it again :)
        file.delete();
        dir.delete();

        addDirToPropsForMonitoring(props, dir);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // We should get told about both the directory and the file being created
        new CrudDetectionVerifier(mockFileMonitor, ChangeType.CREATE) {

            // callback method to make the directory and create the file
            @Override
            public void doCrud() throws IOException, InterruptedException {

                dir.mkdir(); // Now make the directory
                file.createNewFile(); // And add a file in the directory

                expectedFileList.add(dir);
                expectedFileList.add(file);
            }
        };
    }

    @Test
    public void testFileMonitorIsNotifiedOfChangesInDirectoryWhichDoesntExistAtStartOfMonitoringButThenAppearsShortlyAfter() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir); // Add a file in the directory

        // Delete the directory & file so we can create it again :)
        file.delete();
        dir.delete();

        addDirToPropsForMonitoring(props, dir);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // We should get told about the directory which didn't exist when the monitor started
        createFilesAndAssert(mockFileMonitor, FILE_TYPE.DIRECTORY, dir);

        // And we should get told about files in that directory
        createFilesAndAssert(mockFileMonitor, FILE_TYPE.FILE, file);
    }

    @Test
    public void testFileMonitorIsOnlyNotifiedOfChangesToFilteredFile() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir);
        final File excludedFile = createFile(dir);

        addDirToPropsForMonitoring(props, dir);
        // Use the middle of the file's name as a filter
        String filter = ".*" + file.getName().substring(2, file.getName().length() - 4) + ".*";
        props.put(FileMonitor.MONITOR_FILTER, filter);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.MODIFY) {

            // callback method to modify the files
            @Override
            public void doCrud() throws IOException, InterruptedException {
                modifyFile(file);
                modifyFile(excludedFile);
                expectedFileList.add(file); // We should only get told about
                                            // the file covered by our filter
            }
        };
    }

    @Test
    public void testFileMonitorIsNotNotifiedOfDeletionOfDirectoryUsingFileOnlyFilter() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir);
        final File childDir = createDirectory(dir);

        addDirToPropsForMonitoring(props, dir);
        props.put(FileMonitor.MONITOR_FILTER, FileMonitor.MONITOR_FILTER_FILES_ONLY);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // If we delete files inside the directory, we should still be told about them being deleted
        deleteFilesAndAssertDeleted(mockFileMonitor, file);

        // We shouldn't get notified if we delete the child directory, because of our filter
        deleteFilesAndAssertNoChange(mockFileMonitor, childDir);
    }

    @Test
    public void testFileMonitorIsNotNotifiedOfDeletionOfFileUsingDirectoryOnlyFilter() throws Exception {

        final File dir = createDirectory();
        final File file = createFile(dir);
        final File childDir = createDirectory(dir);

        addDirToPropsForMonitoring(props, dir);
        props.put(FileMonitor.MONITOR_FILTER, FileMonitor.MONITOR_FILTER_DIRECTORIES_ONLY);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // If we delete directories inside the directory, we should be told about them being deleted
        deleteFilesAndAssertDeleted(mockFileMonitor, childDir);

        // If we delete files inside the directory, we shouldn't get told
        deleteFilesAndAssertNoChange(mockFileMonitor, file);
    }

    @Test
    public void testFileMonitorIsNotNotifiedOfDeletionOfFileInsideChildDirectoryWithRecursionDisabled() throws Exception {

        final File dir = createDirectory();
        final File childDir = createDirectory(dir);
        final File grandChildFile = createFile(childDir);

        addDirToPropsForMonitoring(props, dir);
        props.put(FileMonitor.MONITOR_RECURSE, false);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // If we delete files inside the child directory, we shouldn't get told
        deleteFilesAndAssertNoChange(mockFileMonitor, grandChildFile);
    }

    @Test
    public void testFileMonitorIsNotifiedOfDeletionOfFileInsideChildDirectoryWithRecursionEnabled() throws Exception {

        final File dir = createDirectory();
        final File childFile = createFile(dir);
        final File childDir = createDirectory(dir);
        final File grandChildFile = createFile(childDir);

        addDirToPropsForMonitoring(props, dir);
        props.put(FileMonitor.MONITOR_RECURSE, true);

        FileMonitor mockFileMonitor = registerFileMonitor(props);
        activateCoreService();

        // If we delete files inside the child directory, we better get told
        deleteFilesAndAssertDeleted(mockFileMonitor, childFile, grandChildFile);
    }

    private Map<String, Object> generateDefaultProperties() {
        Map<String, Object> props = new HashMap<String, Object>();

        // Fill in some sensible defaults (we can always override them in a test)
        props.put("service.pid", "piddypid");
        props.put(FileMonitor.MONITOR_FILTER, null);
        props.put(FileMonitor.MONITOR_RECURSE, true);
        props.put(FileMonitor.MONITOR_INCLUDE_SELF, true);

        props.put("monitor.files", EMPTY_LIST);
        props.put("monitor.directories", EMPTY_LIST);

        setTimeInterval(props, DEFAULT_TIME_INTERVAL);

        return props;
    }

    private void waitForNotification() throws InterruptedException {
        // Wait for the polling plus a decent chunk of time for stuff to happen
        // The monitor holder scans, sees changes, waits 100 ms, then scans twice more at 100 ms intervals, so we need
        // to wait at least 1 time interval plus 3 x 100 ms for a notification. Then we really
        // need to allow a little bit of time for the calls to propagate and logic to execute
        Thread.sleep(FUDGE_TIME + 3 * MonitorHolder.TIME_TO_WAIT_FOR_COPY_TO_COMPLETE + timeInterval);
    }

    /**
     * OK, so this is how you would wait if JMock did not have a state machine.
     * Basically divide the time you want to wait into really small intervals, and
     * then just wait for the length of the interval. Then check the parameters to
     * see detect whether anything changed. That way you can break out without
     * having to wait for the entire time. But since JMock does this for you,
     * we don't need to do this.
     *
     * Waits for a really generous amount of time, but will return early
     * if a notification arrives.
     *
     */
    /*
     * private void waitForNotification(List<Collection<File>> parameters) throws InterruptedException {
     * // Allow twice the time we'd normally expect a notification to arrive in. This means
     * // we're much less stringent in what we're requiring in terms of notification speed,
     * // but gain test reliability on slower systems.
     * int totalTime = 2 * (FUDGE_TIME + 3 * MonitorHolder.TIME_TO_WAIT_FOR_COPY_TO_COMPLETE + timeInterval);
     * boolean gotAChange = false;
     * int elapsedTime = 0;
     * int increment = totalTime / 10;
     * while (!gotAChange && elapsedTime < totalTime) {
     * elapsedTime += increment;
     * for (Collection<File> collection : parameters) {
     * if (!collection.isEmpty()) {
     * gotAChange = true;
     * }
     * }
     * Thread.sleep(increment);
     * }
     * }
     */

    /**
     * Waits for everything to get initialized before changing anything, to make sure our change gets noticed.
     */
    private void waitForMonitorsToStabilise() throws InterruptedException {
        Thread.sleep(FUDGE_TIME + 2 * timeInterval);
    }

    private void addDirToPropsForMonitoring(Map<String, Object> props, File... dirs) {
        final Collection<String> monitoredDirs = new HashSet<String>();
        for (File dir : dirs) {
            monitoredDirs.add(dir.getAbsolutePath());
        }
        props.put(FileMonitor.MONITOR_DIRECTORIES, monitoredDirs);
    }

    /**
     * @param props the properties being passed to the core service
     * @param file the file to monitor
     */
    private void addFileToPropsForMonitoring(Map<String, Object> props, File... files) {
        final Collection<String> monitoredFiles = new HashSet<String>();
        for (File file : files) {
            monitoredFiles.add(file.getAbsolutePath());
        }
        props.put("monitor.files", monitoredFiles);
    }

    /**
     * Sets the time interval by adding it to our properties.
     *
     * @param props
     *
     * @param timeInterval
     *
     */
    private void setTimeInterval(Map<String, Object> props, int newTimeInterval) {
        this.timeInterval = newTimeInterval;
        props.put(FileMonitor.MONITOR_INTERVAL, timeInterval + "ms");
    }

    /**
     * This is our own implementation of ScheduledThreadPoolExecutor which will catch and log
     * runtime exceptions. That's the only reason for this. We were using ScheduledThreadPoolExecutor
     * directly before, but that made debugging very difficult.
     */
    private static class MyScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        MyScheduledThreadPoolExecutor() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return super.schedule(wrapRunnable(command), delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
        }

        private Runnable wrapRunnable(final Runnable r) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        r.run();
                    } catch (Throwable t) {
                        Tr.warning(tc, "Caught unexpected exception while running a task via ScheduledExecutorService", t);
                    }
                }
            };
        }
    }

    /**
     * @return
     */
    protected CoreServiceImpl createCoreService() {
        coreService = instantiateCoreService();
        coreService.setLocation(mockLocation);

        // Don't mock the scheduled executor, which needs to do real stuff
        if (scheduler == null) {
            scheduler = new MyScheduledThreadPoolExecutor();
            coreService.setScheduler(scheduler);
        }
        return coreService;

    }

    /**
     * This shouldn't be called directly, except by {@link #createCoreService()}
     *
     * @return an uninitialized core service
     */
    protected abstract CoreServiceImpl instantiateCoreService();

    /**
     * Tells the core service about our mock monitor and then simulates the activate()
     * call that DS would make.
     *
     * @param coreService
     * @throws Exception
     */
    protected void activateCoreService() throws Exception {
        coreService.activate(mockComponentContext, null);

        // Wait for everything to get initialized before changing anything, to make sure our change gets noticed
        waitForMonitorsToStabilise();
    }

    protected FileMonitor registerFileMonitor(final Map<String, Object> props) {
        // A place to store what gets passed to initComplete();
        List<Collection<File>> parameters = new ArrayList<Collection<File>>();
        return registerFileMonitor(props, parameters);
    }

    /**
     * @param initFiles A collection into which all the files initComplete() gets told about get put.
     */
    @SuppressWarnings("unchecked")
    protected FileMonitor registerFileMonitor(final Map<String, Object> props, final List<Collection<File>> parameters) {
        // Make sure each mock has unique names, since they default to the class names
        final FileMonitor mockFileMonitor = context.mock(FileMonitor.class, "fileMonitor" + counter);
        final ServiceReference<FileMonitor> mockServiceReference = context.mock(ServiceReference.class, "serviceReference" + counter);
        counter++;

        context.checking(new Expectations() {
            {
                // To reduce the verbosity of our expectations, use a map for all the service properties
                allowing(mockServiceReference).getProperty(with(any(String.class)));
                will(readFromMap(props));

                oneOf(mockFileMonitor).onBaseline(with(any(Collection.class)));
                will(grabParameters(parameters));

                // The service reference will point to the mock file monitor
                allowing(mockComponentContext).locateService(CoreServiceImpl.MONITOR, mockServiceReference);
                will(returnValue(mockFileMonitor));

            }
        });
        coreService.setMonitor(mockServiceReference);

        return mockFileMonitor;

    }

    /**
     * Compares two collections for equality, ignoring the original order of elements
     */
    private void assertEqualsOrderless(String message, Collection<File> expected, Collection<File> actual) {
        // Compare by sorting before comparing

        // Put into a set to eliminate duplicates (we'll check for those in a moment)
        Set<File> actualSet = convertToSet(actual);
        Set<File> expectedSet = convertToSet(expected);

        // Convert to a list so we get ordering
        List<File> expectedList = convertToSortedList(expectedSet);
        List<File> actualList = convertToSortedList(actualSet);

        assertEquals(message, expectedList, actualList);

        // Now check for duplicates (unless we had the same duplicates in the expected list)
        assertEquals("Although we were notified about the correct files, the same file was unexpectedly included multiple times in the list of changed files.", expectedList,
                     convertToSortedList(actual));

    }

    /**
     * @param collection
     * @return
     */
    private List<File> convertToSortedList(Collection<File> collection) {
        List<File> list = new ArrayList<File>();
        list.addAll(collection);
        Collections.sort(list);

        return list;
    }

    /**
     * @param collection
     * @return
     */
    private Set<File> convertToSet(Collection<File> collection) {
        Set<File> set = new HashSet<File>();
        set.addAll(collection);
        return set;
    }

    /**
     * Accepts a list of either files or directories. One or the other; not both.
     * It uses a CrudDetectionVerifier to create the files or directories and
     * to verify the mock file monitor picked up the changes.
     *
     * @param mockFileMonitor
     * @param fileType
     * @param files
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void createFilesAndAssert(final FileMonitor mockFileMonitor,
                                      final FILE_TYPE fileType,
                                      final File... files) throws IOException, InterruptedException {

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.CREATE) {

            // callback method to create the files or directories
            @Override
            public void doCrud() throws IOException, InterruptedException {

                for (File file : files) {
                    if (FILE_TYPE.DIRECTORY == fileType) {
                        file.mkdir();
                    } else {
                        file.createNewFile();
                    }
                    expectedFileList.add(file);
                }
            }
        };
    }

    /**
     * Accepts a list of either files or directories. One or the other; not both.
     * It uses a CrudDetectionVerifier to delete the files or directories and
     * to verify the mock file monitor picked up the changes.
     *
     * @param mockFileMonitor
     * @param files
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void deleteFilesAndAssertDeleted(final FileMonitor mockFileMonitor,
                                             final File... files) throws IOException, InterruptedException {

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.DELETE) {

            // callback method to delete the files
            @Override
            public void doCrud() throws IOException, InterruptedException {

                for (File file : files) {
                    deleteFile(file);
                    expectedFileList.add(file);
                }
            }
        };
    }

    /**
     * Accepts a list of either files or directories. One or the other; not both.
     * It uses a CrudDetectionVerifier to delete the files or directories and
     * to verify that no change is detected.
     *
     * For use when the delete should not be noticed; i.e. when a filter is applied.
     *
     * @param mockFileMonitor
     * @param files
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void deleteFilesAndAssertNoChange(final FileMonitor mockFileMonitor,
                                              final File... files) throws IOException, InterruptedException {

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.NOCHANGE) {

            // callback method to delete the files
            @Override
            public void doCrud() throws IOException, InterruptedException {

                for (File file : files) {
                    deleteFile(file);
                    // Not expecting a change to be detected so don't add
                    //expectedFileList.add(file);
                }
            }
        };
    }

    /**
     * Sets expectations for a "rogue" mock FileMonitor, so that it throws an exception
     * when its onChange(...) method is called.
     * Then modifies the file and asserts that the expectations were satisfied.
     *
     * @param mockFileMonitor - JMock of FileMonitor
     * @param files - files to monitor (can handle multiple files, but normally just one is passed)
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    private void modifyFileAndAssertException(final FileMonitor mockFileMonitor,
                                              File... files) throws IOException, InterruptedException {

        String methodName = "modifyFileAndAssertException: ";

        // Set expectations so that a RuntimeException will be thrown when the onChange(...) method is called
        // Also the state will change from "notDone" to "done"
        final States stateMachine = context.states("stateMachine").startsAs("notDone");
        context.checking(new Expectations() {
            {
                // Here we are telling the mock FileMonitor to expect the onChange(...) method
                // to be called and we are telling it to throw a RuntimeException when that happens.
                oneOf(mockFileMonitor).onChange(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class)));
                then(stateMachine.is("done"));
                will(throwException(new RuntimeException("Mock FileMonitor throwing RuntimeException as expected.")));
            }
        });

        // Modifying the files monitored by the mock rogue FileMonitor should cause the "onChange" method
        // to be invoked in the rogue mockFileMonitor. This should cause the 1st RuntimeException
        // to be thrown by the rogue mockFileMonitor.  The exception is caught by the MontiorHolder.
        final Collection<File> listOfMonitoredFiles = new ArrayList<File>();
        for (File file : files) {
            listOfMonitoredFiles.add(file);
            modifyFile(file);
            Tr.debug(tc, methodName + "modified file: [" + file.getName() + "]");
        }

        Tr.debug(tc, methodName + "synchroniser.waitUntil(stateMachine.is(done));");
        synchroniser.waitUntil(stateMachine.is("done"), TIME_OUT);
        Tr.debug(tc, methodName + "synchroniser back");

        context.assertIsSatisfied();
        Tr.debug(tc, methodName + "Assert satisfied, onChange called, exception thrown");
    }

    /**
     * Accepts a list of either files or directories. One or the other; not both.
     * It uses a CrudDetectionVerifier to modify the files or directories and
     * to verify the mock file monitor picked up the changes.
     *
     * This method assumes that the caller has already done some setup:
     * For example:
     * addFileToPropsForMonitoring(props, file);
     * FileMonitor mockFileMonitor = registerFileMonitor(props);
     * activateCoreService();
     *
     * @param mockFileMonitor - JMock of FileMonitor
     * @param files - files to monitor (can handle multiple files, but normally just one is passed)
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void modifyFilesAndAssert(final FileMonitor mockFileMonitor,
                                      final File... files) throws IOException, InterruptedException {

        new CrudDetectionVerifier(mockFileMonitor, ChangeType.MODIFY) {

            // callback method to modify the files
            @Override
            public void doCrud() throws IOException, InterruptedException {

                for (File file : files) {
                    modifyFile(file);
                    expectedFileList.add(file);
                }
            }
        };
    }

    /**
     * CrudDetectionVerifier: Verifies that we can detect CRUD
     *
     * This class is for making CRUD (Create Read Update Delete) changes to
     * the file system, and then verifying that the file monitor actually detected
     * the change. OK, so actually it should be CUD, since we aren't trying to
     * detect READs. But CRUD just sounds better.
     *
     * This is intended to be used as an anonymous inner class inside a test method.
     * The test must override the doCrud() method and make the changes to the file
     * system there because this class has no idea what changes the test needs
     * to make to the file system. So the test needs to make the file changes (Create,
     * Update, or Delete or some combination of those), and then tell us what to expect
     * by adding files to the "expectedFileList" and passing in the "ChangeType" on the
     * constructor.
     *
     * The assertCreated & assertDeleted methods should not need to be overridden.
     */
    public abstract class CrudDetectionVerifier {

        /**
         * expectedFileList: the files that we are expecting to be changed (Either
         * created, updated, or deleted.) The overridden doCrud() method creates this list.
         */
        final Collection<File> expectedFileList = new ArrayList<File>();

        /**
         * parameters are the copies of the parameters that are passed to the file mock
         * monitor's onChange(...) method. Basically, the scanner passes 3 collections of
         * files to the onChange(...) method; the files that the scanner detected as
         * created, updated, and deleted.
         */
        final List<Collection<File>> parameters = new ArrayList<Collection<File>>();

        /**
         * This is what type of file system change we should expect.
         */
        final ChangeType changeType;

        /**
         * We use JMock to implement a mocked up version of a FileMonitor for the test.
         */
        final FileMonitor mockFileMonitor;

        /**
         * stateMachine is a JMock thing. It allows us to wait on a state change with a timeout.
         * Without the state machine, we would always have to wait the full amount of time.
         */
        final States stateMachine = context.states("stateMachine").startsAs("notDone");

        /**
         * Constructor - Basically, you just create a CrudDetectionVerifier, and it does everything
         * but the file system manipulation. The test case has to override to the doCrud() method
         * manipulate the file system.
         * 1. It creates the "expectations" for the file mock monitor.
         * 2. Calls the doCrud() method which is test-case specific.
         * 3. Waits for notification from JMock that the scanner had called the on change
         * method. We get the parameters back from that.
         * 4. Verifies the parameters match our original expectations.
         *
         * @param mockFileMonitor
         * @param changeType
         * @throws InterruptedException
         * @throws IOException
         */
        CrudDetectionVerifier(final FileMonitor mockFileMonitor,
                              ChangeType changeType) throws InterruptedException, IOException {

            this.changeType = changeType;
            this.mockFileMonitor = mockFileMonitor;
            if (ChangeType.NOCHANGE == changeType) {
                setNullExpectationsModifyAndWait();
            } else {
                setExpectationsModifyAndWait();
            }
            assertExpectationsMet();
        }

        /**
         * Override this method to make the changes to the file system that are appropriate
         * for the test case. The doCrud() method must update the expectedFileList member so
         * that we know what changes to expect.
         *
         * @throws IOException
         * @throws InterruptedException
         */
        abstract void doCrud() throws IOException, InterruptedException;

        /**
         *
         */
        public void assertExpectationsMet() {
            if (changeType == ChangeType.CREATE) {
                assertCreated(parameters);
            } else if (changeType == ChangeType.MODIFY) {
                assertModified(parameters);
            } else if (changeType == ChangeType.DELETE) {
                assertDeleted(parameters);
            } else if (changeType == ChangeType.NOCHANGE) {
                assertNoChange(parameters);
            } else {
                throw new RuntimeException("Invalid changeType [" + changeType + "] passed to doCrudAndVerify() ");
            }
        }

        /**
         *
         * @param collectedListOfFiles
         */
        public void assertCreated(List<Collection<File>> collectedListOfFiles) {

            // Verify the file(s) were created
            assertNone(collectedListOfFiles, ChangeType.MODIFY); // no changes of type modify
            assertNone(collectedListOfFiles, ChangeType.DELETE);
            assertEqualsOrderless("The wrong file was reported as created.",
                                  expectedFileList,
                                  collectedListOfFiles.get(ChangeType.CREATE.position));
        }

        /**
         *
         * @param collectedListOfFiles
         */
        public void assertDeleted(List<Collection<File>> collectedListOfFiles) {

            assertNone(collectedListOfFiles, ChangeType.MODIFY);
            assertNone(collectedListOfFiles, ChangeType.CREATE);
            assertEqualsOrderless("The wrong file was reported as deleted.",
                                  expectedFileList,
                                  collectedListOfFiles.get(ChangeType.DELETE.position));
        }

        /**
         *
         * @param collectedListOfFiles
         */
        public void assertModified(List<Collection<File>> collectedListOfFiles) {
            // That just checked we got notified of something, so now check it was actually a modification

            assertNone(collectedListOfFiles, ChangeType.CREATE);
            assertNone(collectedListOfFiles, ChangeType.DELETE);
            assertEqualsOrderless("The wrong file was reported as modified.",
                                  expectedFileList,
                                  collectedListOfFiles.get(ChangeType.MODIFY.position));
        }

        /**
         * Verifies that no change occurred.
         *
         * @param collectedListOfFiles
         */
        public void assertNoChange(List<Collection<File>> collectedListOfFiles) {
            assertNone(collectedListOfFiles, ChangeType.CREATE);
            assertNone(collectedListOfFiles, ChangeType.MODIFY);
            assertNone(collectedListOfFiles, ChangeType.DELETE);
        }

        private void setNullExpectationsModifyAndWait() throws InterruptedException, IOException {
            // Let the call happen if it's going to ...
            final List<Collection<File>> parameters = new ArrayList<Collection<File>>();
            context.checking(new Expectations() {
                {
                    // Use allowing, not "one of", since we are not actually expecting onChange to be called
                    allowing(mockFileMonitor).onChange(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class)));
                    will(grabParameters(parameters));
                }
            });

            doCrud();

            // We are expecting nothing to happen.  So there is no event to wait for.
            // Just wait a "reasonable" amount of time.
            waitForNotification();

            // This assertion probably isn't necessary.  It just checks that the JMock expectations were met,
            // but all we set was an "allowing onChange".  The real checks are performed below and by the caller.
            context.assertIsSatisfied();

            // Now we should be able to collect the parameters, which better be empty
            int parametersSize = parameters.size() / ChangeType.values().length;
            assertEquals("The file monitor was invoked when it shouldn't have been.", 0, parametersSize);

            // Debug:  Display the parameters to see what unexpected change occurred
            if (parametersSize > 0) {
                String[] labels = { "Created", "Modified", "Deleted" };
                int i = 0;
                for (Collection<File> listOfFiles : parameters) {
                    if (i < labels.length) {
                        System.out.println(labels[i]);
                    } else {
                        System.out.println("!!! Unexpected number of parameters !!!");
                    }
                    i++;

                    for (File file : listOfFiles) {
                        System.out.println(file.getAbsolutePath());
                    }
                }
            }
        }

        /**
         *
         * @return
         * @throws InterruptedException
         * @throws IOException
         */
        @SuppressWarnings("unchecked")
        public List<Collection<File>> setExpectationsModifyAndWait() throws InterruptedException, IOException {

            // 1. Begin with the state machine in the "notDone" state.
            // 2. Set expectation for the mock file monitor's onChange method to be called.
            // 3. When that happens, JMock will set the state to done.
            // 4. grabParameters will capture the parameters that were passed to the onChange
            //    method and store them in the member variable "parameters"
            stateMachine.become("notDone");
            context.checking(new Expectations() {
                {
                    oneOf(mockFileMonitor).onChange(with(any(Collection.class)), with(any(Collection.class)), with(any(Collection.class)));
                    then(stateMachine.is("done"));
                    will(grabParameters(parameters));
                }
            });

            // Expectations are set.  Now create, modify or update files
            doCrud();

            // Wait for notification and check results
            synchroniser.waitUntil(stateMachine.is("done"), TIME_OUT);
            context.assertIsSatisfied(); // asserts all JMock expectations have been met,
                                         // but doesn't verify the expected parameter values.
                                         // Caller must do that.
            return parameters;
        }
    }

    private void assertNone(final List<Collection<File>> parameters, ChangeType changeType) {
        if (parameters.size() > 0) {
            Collection<File> collection = parameters.get(changeType.position);
            assertTrue("The following files were unexpectedly reported as " + changeType + ": " + collection,
                       collection.isEmpty());
        } else {
            // No changes of any type.
        }
    }

    /**
     * Creates a temporary file.
     */
    private File createFile() throws IOException {
        File file = createFile("test"); // "test" is the default identifier to insert into the file name
        return file;
    }

    /**
     * Creates a temporary file.
     *
     * @param identifier - text to be inserted into the actual file name, to differentiate from
     *            other temp files.
     */
    private File createFile(String identifier) throws IOException {
        File file = File.createTempFile("coreservice", identifier + counter);
        return file;

    }

    /**
     * @param parentDir - location to create the file.
     */
    private File createFile(File parentDir) throws IOException {
        File file = File.createTempFile("coreservice", "test" + counter, parentDir);
        return file;

    }

    private void modifyFile(File file) throws IOException {
        FileWriter writer = new FileWriter(file, true);
        writer.append("Modified " + df.format(new Date()));
        writer.close();
    }

    private void deleteFile(File file) throws IOException {
        file.delete();
        assertFalse("Problem with test code. Could not delete " + file, file.exists());
    }

    private File createDirectory() throws IOException {

        File file = File.createTempFile("coreservice", "dir" + counter);
        // Delete the file so we can recreate it as a directory
        file.delete();
        file.mkdir();
        return file;

    }

    private File createDirectory(File parentDir) throws IOException {
        File file = File.createTempFile("coreservice", "childdir" + counter, parentDir);
        // Delete the file so we can recreate it as a directory
        file.delete();
        file.mkdir();
        return file;

    }

    /**
     * The jMock assertion messages don't distinguish between a method which was never called, and one which
     * was called but with the wrong arguments. This is important to us, so add our own action
     * to give better diagnostics.
     */
    private static class ParameterGrabber implements Action {

        private final List<Collection<File>> parameters;

        /**
         * The only way we have of communicating back is through a collection.
         * Clunky but true.
         */
        public ParameterGrabber(List<Collection<File>> parameters) {
            this.parameters = parameters;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
         */
        @Override
        public void describeTo(Description description) {
            description.appendText("validating arguments separately");
        }

        /*
         * (non-Javadoc)
         *
         * @see org.jmock.api.Invokable#invoke(org.jmock.api.Invocation)
         */
        @SuppressWarnings("unchecked")
        @Override
        public Object invoke(Invocation inv) throws Throwable {
            // We can't use assertions because they turn into an exception,
            // which seems to come from the FileMonitor itself.
            // Instead. just sock away a record of what we got passed for later verification.

            Object[] parametersAsArray = inv.getParametersAsArray();
            for (Object parameter : parametersAsArray) {
                parameters.add((Collection<File>) parameter);
            }
            return null;
        }
    }

    public static Action grabParameters(List<Collection<File>> parameters) {
        return new ParameterGrabber(parameters);
    }

    public class Retry implements TestRule {
        private final int retryCount;

        public Retry(int retryCount) {
            this.retryCount = retryCount;
        }

        @Override
        public Statement apply(Statement base, org.junit.runner.Description description) {
            return statement(base, description);
        }

        private Statement statement(final Statement base, final org.junit.runner.Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Throwable caughtThrowable = null;

                    for (int i = 0; i < (retryCount + 1); i++) {
                        try {
                            base.evaluate();
                            return;
                        } catch (Throwable t) {
                            caughtThrowable = t;
                            System.err.println(description.getDisplayName() + ": run " + (i + 1) + " failed");
                        }
                    }
                    System.err.println(description.getDisplayName() + ": giving up after " + (retryCount + 1) + " failure(s)");
                    throw caughtThrowable;
                }
            };
        }
    }
}
