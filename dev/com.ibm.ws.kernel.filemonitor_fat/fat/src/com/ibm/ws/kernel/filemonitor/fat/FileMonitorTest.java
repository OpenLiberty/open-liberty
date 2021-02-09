/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class FileMonitorTest extends AbstractNotificationTest {

    /**
     * Cut-and-pasted from the test feature. No direct compile-time or run-time
     * communication is possible, so we're fragile.
     */
    private static final MonitorReader fileMonitor = new MonitorReader("-FILETESTMONITOROUTPUT-", "file monitor");
    private static final MonitorReader nonexistentFileMonitor = new MonitorReader("-FILENONEXISTENTTESTMONITOROUTPUT-", "file monitor for file which doesn't exist");
    private static final MonitorReader confusedFileMonitor = new MonitorReader("-FILEBUTACTUALLYMONITORINGDIRECTORYTESTMONITOROUTPUT-", "file monitor for a file which is actually a folder");
    private static final MonitorReader recursiveMonitor = new MonitorReader("-RECURSIVETESTMONITOROUTPUT-", "recursive folder monitor");
    private static final MonitorReader nonexistentFolderMonitor = new MonitorReader("-NONEXISTENTFOLDERTESTMONITOROUTPUT-", "folder monitor for folder which doesn't exist");
    private static final MonitorReader confusedFolderMonitor = new MonitorReader("-RECURSIVEBUTACTUALLYMONITORINGFILETESTMONITOROUTPUT-", "folder monitor for a folder which is actually a file");
    private static final MonitorReader filterMonitor = new MonitorReader("-FILTEREDTESTMONITOROUTPUT-", "filtered folder monitor");
    private static final MonitorReader filefilterMonitor = new MonitorReader("-FILEFILTERTESTMONITOROUTPUT-", "only-files filtered folder monitor");
    private static final MonitorReader directoryfilterMonitor = new MonitorReader("-DIRECTORYFILTERTESTMONITOROUTPUT-", "only-directories filtered folder monitor");
    private static final MonitorReader nonRecursiveMonitor = new MonitorReader("-NONRECURSINGTESTMONITOROUTPUT-", "non-recursive folder monitor");
    private static final MonitorReader nonRecursiveMonitorSelfMonitor = new MonitorReader("-NONRECURSEMONITORSELFTESTMONITOROUTPUT-", "non-recursive self-monitoring folder monitor");
    private static final MonitorReader monitorSelfMonitor = new MonitorReader("-MONITORSELFTESTMONITOROUTPUT-", "self-monitoring folder monitor");

    @After
    public void tearDown() throws Exception {
        // Clear up after any tests which may have deleted the monitored folder
        if (!monitoredFolder.exists()) {
            monitoredFolder.mkdirs();
            // Get rid of any notifications from reinstating the folder
            // This flush assumes that the monitor self monitor works 
            flushNotifications(monitorSelfMonitor);
        }

        // Get rid of any dangling notifications from tests, using the monitor most likely to catch something
        flushNotifications(recursiveMonitor);

    }

    @Test
    public void testBaselineIsReported() throws Exception {

        recursiveMonitor.scrapeLogsForBaseline();

        assertFalse("The baseline should not be empty", recursiveMonitor.baseline.isEmpty());
        // We're not monitoring the directory itself, so it won't get included in the baseline
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBaselineIncludesNestedFolders() throws Exception {
        File baseline = new File(monitoredFolder, "baseline");
        File baseline1 = new File(monitoredFolder, "baseline1");
        File nested = new File(monitoredFolder, "nestedBaselineFolder");
        File nestedFile = new File(nested, "nestedBaselineFile");

        recursiveMonitor.scrapeLogsForBaseline();

        assertFalse("The baseline should not be empty", recursiveMonitor.baseline.isEmpty());
        assertEqualsOrderless("The baseline didn't have the expected files in it.", new File[] { baseline, baseline1, nested, nestedFile }, recursiveMonitor.baseline);
    }

    @Test
    public void testBaselineDoesNotIncludeNestedFoldersInNonRecursiveCase() throws Exception {
        File baseline = new File(monitoredFolder, "baseline");
        File baseline1 = new File(monitoredFolder, "baseline1");
        File nested = new File(monitoredFolder, "nestedBaselineFolder");

        nonRecursiveMonitor.scrapeLogsForBaseline();

        assertFalse("The baseline should not be empty", nonRecursiveMonitor.baseline.isEmpty());
        assertEqualsOrderless("The baseline didn't have the expected files in it.", new File[] { baseline, baseline1, nested }, nonRecursiveMonitor.baseline);
    }

    @Test
    public void testBaselineIncludesSelfInSelfDirectoryCase() throws Exception {
        File baseline = new File(monitoredFolder, "baseline");
        File baseline1 = new File(monitoredFolder, "baseline1");
        File nested = new File(monitoredFolder, "nestedBaselineFolder");
        File nestedFile = new File(nested, "nestedBaselineFile");

        // Set the mark to the beginning of the log, since the baseline will happen at the beginning but 
        // this test could run in any order
        // This will reset the marks, too
        server.resetLogOffsets();
        monitorSelfMonitor.scrapeLogsForBaseline();

        assertFalse("The baseline should not be empty", monitorSelfMonitor.baseline.isEmpty());
        assertEqualsOrderless("The baseline didn't have the expected files in it.", new File[] { monitoredFolder, baseline, baseline1, nested, nestedFile },
                              monitorSelfMonitor.baseline);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testBaselineIncludesSelfInSelfNonRecursiveDirectoryCase() throws Exception {
        File baseline = new File(monitoredFolder, "baseline");
        File baseline1 = new File(monitoredFolder, "baseline1");
        File nested = new File(monitoredFolder, "nestedBaselineFolder");

        nonRecursiveMonitorSelfMonitor.scrapeLogsForBaseline();

        assertFalse("The baseline should not be empty", nonRecursiveMonitorSelfMonitor.baseline.isEmpty());
        assertEqualsOrderless("The baseline didn't have the expected files in it.", new File[] { monitoredFolder, baseline, baseline1, nested },
                              nonRecursiveMonitorSelfMonitor.baseline);
    }

    @Test
    public void testBaselineIsEmptyForNonExistentFile() throws Exception {
        nonexistentFileMonitor.scrapeLogsForBaseline();

        assertEquals("A baseline should have been reported, and been empty.", 0, nonexistentFileMonitor.baseline.size());
    }

    @Test
    public void testFileCreationIsNotified() throws Exception {
        // Don't assume test ordering
        if (monitoredFile.exists()) {
            assertTrue(monitoredFile.delete());
            flushNotifications(fileMonitor);
        }
        createFile(monitoredFile);

        fileMonitor.scrapeLogsForChanges();

        assertCreated(fileMonitor, monitoredFile);
    }

    @Test
    public void testFileModificationIsNotified() throws Exception {

        // Make the file to modify
        if (!monitoredFile.exists()) {
            createFile(monitoredFile);
            fileMonitor.scrapeLogsForChanges();
            server.setMarkToEndOfLog();
        }

        appendSomething(monitoredFile);

        fileMonitor.scrapeLogsForChanges();
        assertModified(fileMonitor, monitoredFile);
    }

    @Test
    public void testFileCreationAndModificationIsNotifiedAsCreation() throws Exception {

        // Don't assume test ordering
        if (monitoredFile.exists()) {
            assertTrue(monitoredFile.delete());
            fileMonitor.scrapeLogsForChanges();
            server.setMarkToEndOfLog();
        }

        createFile(monitoredFile);
        appendSomething(monitoredFile);

        fileMonitor.scrapeLogsForChanges();
        assertCreated(fileMonitor, monitoredFile);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTimestampChanging() throws Exception {
        // Make a file to delete (and make sure we noticed it)
        if (!monitoredFile.exists()) {
            createFile(monitoredFile);
            fileMonitor.scrapeLogsForChanges();
            server.setMarkToEndOfLog();
        }

        // Make it a lot older (~30min) than it is (should still be considered modified)
        monitoredFile.setLastModified(System.currentTimeMillis() - (30 * 60 * 1000));

        fileMonitor.scrapeLogsForChanges();
        assertModified(fileMonitor, monitoredFile);

    }

    @Test
    public void testFileDeletionIsNotified() throws Exception {
        // Make a file to delete (and make sure we noticed it)
        if (!monitoredFile.exists()) {
            createFile(monitoredFile);
            fileMonitor.scrapeLogsForChanges();
            server.setMarkToEndOfLog();
        }

        // Delete the file: it should be added to the 'deleted' list
        assertTrue("File should be deleted on the filesystem", monitoredFile.delete());
        fileMonitor.scrapeLogsForChanges();
        assertDeleted(fileMonitor, monitoredFile);

    }

    /**
     * An easy test, as a sanity baseline.
     */
    @Test
    public void testNoNotificationsHappenWhenNothingChanges() throws Exception {
        // Do nothing :)
        int count = recursiveMonitor.scrapeLogsForChanges();
        // Sanity check - we didn't see anything, right?
        assertEquals("We didn't change any files so we shouldn't have had any monitoring activity.", 0, count);
        assertNothingCreated(recursiveMonitor);
        assertNothingDeleted(recursiveMonitor);
        assertNothingModified(recursiveMonitor);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderCreationIsNotified() throws Exception {
        File f = new File(monitoredFolder, "monitoredFile");
        createFile(f);

        recursiveMonitor.scrapeLogsForChanges();

        assertCreated(recursiveMonitor, f);
    }

    @Test
    public void testMultipleFileCreationInFolderIsNotified() throws Exception {
        File f1 = new File(monitoredFolder, "testMultipleFileCreationIsNotified1");
        File f2 = new File(monitoredFolder, "testMultipleFileCreationIsNotified2");
        createFile(f1);
        createFile(f2);

        recursiveMonitor.scrapeLogsForChanges();

        assertCreated(recursiveMonitor, f1, f2);
    }

    @Test
    public void testFileCreationInNestedFoldersIsNotifiedDependingOnRecursionSetting() throws Exception {
        File folder = new File(monitoredFolder, "nestedFolder");
        createFolder(folder);
        File f = new File(folder, "testFileCreationInNestedFoldersIsNotifiedDependingOnRecursionSetting");
        createFile(f);

        nonRecursiveMonitor.scrapeLogsForChanges();
        recursiveMonitor.scrapeLogsForChanges();

        assertCreated(recursiveMonitor, folder, f);
        // The non-recursive monitor should only see the folder, not its contents
        assertCreated(nonRecursiveMonitor, folder);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderModificationIsNotified() throws Exception {

        // Make a file to modify (and make sure we noticed it)
        File f = new File(monitoredFolder, "testFileModificationIsNotified");
        createFile(f);
        flushNotifications(recursiveMonitor);

        appendSomething(f);

        recursiveMonitor.scrapeLogsForChanges();
        assertModified(recursiveMonitor, f);
    }

    @Test
    public void testFileInNestedFolderModificationIsNotifiedDependingOnRecursionSettings() throws Exception {

        // Make a file to modify (and make sure we noticed it)
        // Make a file to delete (and make sure we noticed it)
        File folder = new File(monitoredFolder, "nestedFolderForModification");
        createFolder(folder);
        File f = new File(folder, "testFileInNestedFolderModificationIsNotifiedDependingOnRecursionSettings");
        createFile(f);
        assertTrue("We should find a created notification to flush", flushNotifications(recursiveMonitor) > 0);
        flushNotifications(nonRecursiveMonitor);

        appendSomething(f);

        recursiveMonitor.scrapeLogsForChanges();
        nonRecursiveMonitor.scrapeLogsForChanges();

        assertModified(recursiveMonitor, f);
        assertNothingModified(nonRecursiveMonitor);

    }

    @Test
    public void testFileInNestedFolderCreationIsNotNotifiedWhenFileDoesNotMatchFilter() throws Exception {

        File folder = new File(monitoredFolder, "nestedFolderForFilteredCreation");
        createFolder(folder);

        File f = new File(folder, "testFileInNestedFolderCreationIsNotNotifiedWhenItDoesNotMatchFilterexcludedfile");
        createFile(f);

        filterMonitor.scrapeLogsForChanges();
        assertNothingCreated(filterMonitor);
        assertNothingDeleted(filterMonitor);
        assertNothingModified(filterMonitor);

        // Now check other files do get past the filter
        File excluded = new File(folder, "testFileInNestedFolderCreationIsNotNotifiedWhenItDoesNotMatchFilterStillexcluded");
        createFile(excluded);
        File passed = new File(folder, "testFileInNestedFolderCreationIsNotNotifiedWhenItDoesNotMatchFilterincluded");
        createFile(passed);

        filterMonitor.scrapeLogsForChanges();
        assertCreated(filterMonitor, passed);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderModficationIsNotNotifiedWhenFileDoesNotMatchFilter() throws Exception {

        File folder = new File(monitoredFolder, "nestedFolderForFilteredModification");
        createFolder(folder);

        File f = new File(folder, "testFileInNestedFolderModificationIsNotNotifiedWhenItDoesNotMatchFilterexcludedfile");
        createFile(f);
        File excluded = new File(folder, "testFileInNestedFolderModificationIsNotNotifiedWhenItDoesNotMatchFilterStillexcluded");
        createFile(excluded);
        File passed = new File(folder, "testFileInNestedFolderModificationIsNotNotifiedWhenItDoesNotMatchFilterincluded");
        createFile(passed);
        flushNotifications(filterMonitor);

        appendSomething(f);
        filterMonitor.scrapeLogsForChanges();
        assertNothingModified(filterMonitor);
        assertNothingDeleted(filterMonitor);
        assertNothingModified(filterMonitor);

        // Now check other files do get past the filter
        appendSomething(excluded);
        appendSomething(passed);
        filterMonitor.scrapeLogsForChanges();
        assertModified(filterMonitor, passed);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderDeletionIsNotNotifiedWhenFileDoesNotMatchFilter() throws Exception {

        File folder = new File(monitoredFolder, "nestedFolderForFilteredDeletion");
        createFolder(folder);

        File f = new File(folder, "testFileInNestedFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilterexcludedfile");
        createFile(f);
        File excluded = new File(folder, "testFileInNestedFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilterStillexcluded");
        createFile(excluded);
        File passed = new File(folder, "testFileInNestedFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilterincluded");
        createFile(passed);
        flushNotifications(filterMonitor);

        deleteFile(f);
        filterMonitor.scrapeLogsForChanges();
        assertNothingDeleted(filterMonitor);
        assertNothingDeleted(filterMonitor);
        assertNothingDeleted(filterMonitor);

        // Now check other files do get past the filter
        deleteFile(excluded);
        deleteFile(passed);
        filterMonitor.scrapeLogsForChanges();
        assertDeleted(filterMonitor, passed);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderCreationIsNotNotifiedWhenNeitherFolderNorFileMatchFilter() throws Exception {

        File includedFolder = new File(monitoredFolder, "includedNestedFolderForFilteredCreation");
        File excludedFolder = new File(monitoredFolder, "excludedNestedFolderForFilteredCreation");
        createFolder(includedFolder);
        createFolder(excludedFolder);

        filterMonitor.scrapeLogsForChanges();
        // We should get told about only one of the folders
        assertCreated(filterMonitor, includedFolder);

        // Update any marks so we don't see that notification again
        flushNotifications(filterMonitor);

        // We should still be monitoring in both folders
        File iif = new File(includedFolder, "testFileInNestedFolderCreationIsNotNotifiedWhenFolderDoesNotMatchFilterincludedfile");
        createFile(iif);
        File ief = new File(includedFolder, "testFileInNestedFolderCreationIsNotNotifiedWhenFolderDoesNotMatchFilterexcludedfile");
        createFile(ief);

        File eif = new File(excludedFolder, "testFileInNestedFolderCreationIsNotNotifiedWhenFolderDoesNotMatchFilterincludedfile");
        createFile(eif);
        File eef = new File(excludedFolder, "testFileInNestedFolderCreationIsNotNotifiedWhenFolderDoesNotMatchFilterexcludedfile");
        createFile(eef);

        filterMonitor.scrapeLogsForChanges();
        // We should get told about everything whose base name matches the filter
        assertCreated(filterMonitor, iif, eif);
    }

    @Test
    public void testFileInNestedFolderModificationIsNotNotifiedWhenNeitherFolderNorFileMatchFilter() throws Exception {

        File includedFolder = new File(monitoredFolder, "includedNestedFolderForFilteredModification");
        File excludedFolder = new File(monitoredFolder, "excludedNestedFolderForFilteredModification");
        createFolder(includedFolder);
        createFolder(excludedFolder);

        // We should still be monitoring in both folders
        File iif = new File(includedFolder, "testFileInNestedFolderModificationIsNotNotifiedWhenFolderDoesNotMatchFilterincludedfile");
        createFile(iif);
        File ief = new File(includedFolder, "testFileInNestedFolderModificationIsNotNotifiedWhenFolderDoesNotMatchFilterexcludedfile");
        createFile(ief);

        File eif = new File(excludedFolder, "testFileInNestedFolderModificationIsNotNotifiedWhenFolderDoesNotMatchFilterincludedfile");
        createFile(eif);
        File eef = new File(excludedFolder, "testFileInNestedFolderModificationIsNotNotifiedWhenFolderDoesNotMatchFilterexcludedfile");
        createFile(eef);

        flushNotifications(filterMonitor);

        appendSomething(iif);
        appendSomething(ief);
        appendSomething(eif);
        appendSomething(eef);

        filterMonitor.scrapeLogsForChanges();
        // We should get told about everything whose base name matches the filter
        assertModified(filterMonitor, iif, eif);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderDeletionIsNotNotifiedWhenNeitherFolderNorFileMatchFilter() throws Exception {

        File includedFolder = new File(monitoredFolder, "includedNestedFolderForFilteredDeletion");
        File excludedFolder = new File(monitoredFolder, "excludedNestedFolderForFilteredDeletion");
        createFolder(includedFolder);
        createFolder(excludedFolder);

        // We should still be monitoring in both folders
        File iif = new File(includedFolder, "testFileInNestedFolderDeletionIsNotNotifiedWhenFolderDoesNotMatchFilterincludedfile");
        createFile(iif);
        File ief = new File(includedFolder, "testFileInNestedFolderDeletionIsNotNotifiedWhenFolderDoesNotMatchFilterexcludedfile");
        createFile(ief);

        File eif = new File(excludedFolder, "testFileInNestedFolderDeletionIsNotNotifiedWhenFolderDoesNotMatchFilterincludedfile");
        createFile(eif);
        File eef = new File(excludedFolder, "testFileInNestedFolderDeletionIsNotNotifiedWhenFolderDoesNotMatchFilterexcludedfile");
        createFile(eef);

        flushNotifications(filterMonitor);

        deleteFile(iif);
        deleteFile(ief);
        deleteFile(eif);
        deleteFile(eef);

        filterMonitor.scrapeLogsForChanges();
        // We should get told about everything whose base name matches the filter
        assertDeleted(filterMonitor, iif, eif);
    }

    @Test
    public void testFileInNestedFolderCreationIsNotNotifiedForDirectoryTypeFilter() throws Exception {

        File folder = new File(monitoredFolder, "testFileInNestedFolderCreationIsNotNotifiedForDirectoryTypeFilter");
        createFolder(folder);
        flushNotifications(directoryfilterMonitor);

        File childFile = new File(folder, "childFile");
        createFile(childFile);
        File childFolder = new File(folder, "childFolder");
        createFolder(childFolder);

        directoryfilterMonitor.scrapeLogsForChanges();
        // We should get told about every directory
        assertCreated(directoryfilterMonitor, childFolder);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderCreationIsNotNotifiedForFileTypeFilter() throws Exception {

        File folder = new File(monitoredFolder, "testFileInNestedFolderCreationIsNotNotifiedForFileTypeFilter");
        createFolder(folder);

        File childFile = new File(folder, "childFile");
        createFile(childFile);
        File childFolder = new File(folder, "childFolder");
        createFolder(childFolder);

        filefilterMonitor.scrapeLogsForChanges();
        // We should get told about every directory
        assertCreated(filefilterMonitor, childFile);
    }

    @Test
    public void testFileInNestedFolderModificationIsNotNotifiedForDirectoryTypeFilter() throws Exception {

        File folder = new File(monitoredFolder, "testFileInNestedFolderModificationIsNotNotifiedForDirectoryTypeFilter");
        createFolder(folder);

        File childFile = new File(folder, "childFile");
        createFile(childFile);
        File childFolder = new File(folder, "childFolder");
        createFolder(childFolder);

        flushNotifications(directoryfilterMonitor);

        appendSomething(childFile);

        // We don't check timestamps on folders, so these notifications should have no effect
        updateTimestamp(folder);
        updateTimestamp(childFolder);
        // We're including self, so we expect to get notified about the root folder, too 
        updateTimestamp(monitoredFolder);

        directoryfilterMonitor.scrapeLogsForChanges();
        // We should get told about every directory
        assertNothingModified(directoryfilterMonitor);
    }

    @Test
    public void testFileInNestedFolderModificationIsNotifiedForFileTypeFilter() throws Exception {

        File folder = new File(monitoredFolder, "testFileInNestedFolderModificationIsNotNotifiedForFileTypeFilter");
        createFolder(folder);

        File childFile = new File(folder, "childFile");
        createFile(childFile);
        File childFolder = new File(folder, "childFolder");
        createFolder(childFolder);

        flushNotifications(filefilterMonitor);

        appendSomething(childFile);
        updateTimestamp(folder);
        updateTimestamp(childFolder);
        updateTimestamp(monitoredFolder);

        filefilterMonitor.scrapeLogsForChanges();
        // We should get told about every directory
        assertModified(filefilterMonitor, childFile);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderDeletionIsNotifiedForDirectoryTypeFilter() throws Exception {

        File folder = new File(monitoredFolder, "testFileInNestedFolderDeletionIsNotNotifiedForDirectoryTypeFilter");
        // Get rid of any clutter we don't know about in the monitored folder (we want to make sure we notice it being deleted)
        deleteFile(monitoredFolder);
        createFolder(monitoredFolder);
        createFolder(folder);

        File childFile = new File(folder, "childFile");
        createFile(childFile);
        File childFolder = new File(folder, "childFolder");
        createFolder(childFolder);

        flushNotifications(directoryfilterMonitor);

        deleteFile(childFile);
        deleteFile(childFolder);
        deleteFile(folder);
        deleteFile(monitoredFolder);

        directoryfilterMonitor.scrapeLogsForChanges();
        // We should get told about every directory, but not the file
        assertDeleted(directoryfilterMonitor, monitoredFolder, folder, childFolder);
    }

    @Test
    public void testFileInNestedFolderDeletionIsNotifiedForFileTypeFilter() throws Exception {

        File folder = new File(monitoredFolder, "testFileInNestedFolderDeletionIsNotifiedForFileTypeFilter");
        createFolder(folder);

        File childFile = new File(folder, "childFile");
        createFile(childFile);
        File childFolder = new File(folder, "childFolder");
        createFolder(childFolder);

        flushNotifications(filefilterMonitor);

        //    deleteFile(childFile);
        deleteFile(childFolder);
        deleteFile(folder);

        filefilterMonitor.scrapeLogsForChanges();
        // We should get told about every file (but no folders)
        assertDeleted(filefilterMonitor, childFile);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderCreationAndModificationIsNotifiedAsCreation() throws Exception {

        File f = new File(monitoredFolder, "testFileInFolderCreationAndModificationIsNotifiedAsCreation");
        createFile(f);
        appendSomething(f);

        recursiveMonitor.scrapeLogsForChanges();
        assertCreated(recursiveMonitor, f);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderCreationAndDeletionIsNotNotifiedAtAll() throws Exception {

        File f = new File(monitoredFolder, "testFileInFolderCreationAndDeletionIsNotifiedAsCreation");
        createFile(f);
        deleteFile(f);

        recursiveMonitor.scrapeLogsForChanges();
        assertNothingCreated(recursiveMonitor);
        assertNothingDeleted(recursiveMonitor);
        assertNothingModified(recursiveMonitor);
    }

    @Test
    public void testFileInFolderCreationAndThenModificationAndThenDeletionIsNotNotifiedAtAll() throws Exception {

        File f = new File(monitoredFolder, "testFileInFolderCreationAndDeletionIsNotifiedAsCreation");
        createFile(f);
        appendSomething(f);
        deleteFile(f);

        recursiveMonitor.scrapeLogsForChanges();
        assertNothingCreated(recursiveMonitor);
        assertNothingDeleted(recursiveMonitor);
        assertNothingModified(recursiveMonitor);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderCreationIsNotNotifiedWhenItDoesNotMatchFilter() throws Exception {

        File f = new File(monitoredFolder, "testFileInFolderCreationIsNotNotifiedWhenItDoesNotMatchFilterexcludedfile");
        createFile(f);

        filterMonitor.scrapeLogsForChanges();
        assertNothingCreated(filterMonitor);
        assertNothingDeleted(filterMonitor);
        assertNothingModified(filterMonitor);

        // Now check other files do get past the filter
        File excluded = new File(monitoredFolder, "testFileInFolderCreationIsNotNotifiedWhenItDoesNotMatchFilterStillexcluded");
        createFile(excluded);
        File passed = new File(monitoredFolder, "testFileInFolderCreationIsNotNotifiedWhenItDoesNotMatchFilterincluded");
        createFile(passed);

        filterMonitor.scrapeLogsForChanges();
        assertCreated(filterMonitor, passed);
    }

    @Test
    public void testFileInFolderModficationIsNotNotifiedWhenItDoesNotMatchFilter() throws Exception {

        File f = new File(monitoredFolder, "testFileInFolderModificationIsNotNotifiedWhenItDoesNotMatchFilterexcludedfile");
        createFile(f);
        File excluded = new File(monitoredFolder, "testFileInFolderModificationIsNotNotifiedWhenItDoesNotMatchFilterStillexcluded");
        createFile(excluded);
        File passed = new File(monitoredFolder, "testFileInFolderModificationIsNotNotifiedWhenItDoesNotMatchFilterincluded");
        createFile(passed);
        flushNotifications(filterMonitor);

        appendSomething(f);
        filterMonitor.scrapeLogsForChanges();
        assertNothingModified(filterMonitor);
        assertNothingDeleted(filterMonitor);
        assertNothingModified(filterMonitor);

        // Now check other files do get past the filter
        appendSomething(excluded);
        appendSomething(passed);
        filterMonitor.scrapeLogsForChanges();
        assertModified(filterMonitor, passed);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilter() throws Exception {

        File f = new File(monitoredFolder, "testFileInFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilterexcludedfile");
        createFile(f);
        File excluded = new File(monitoredFolder, "testFileInFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilterStillexcluded");
        createFile(excluded);
        File passed = new File(monitoredFolder, "testFileInFolderDeletionIsNotNotifiedWhenItDoesNotMatchFilterincluded");
        createFile(passed);
        flushNotifications(filterMonitor);

        deleteFile(f);
        filterMonitor.scrapeLogsForChanges();
        assertNothingDeleted(filterMonitor);
        assertNothingDeleted(filterMonitor);
        assertNothingDeleted(filterMonitor);

        // Now check other files do get past the filter
        deleteFile(excluded);
        deleteFile(passed);
        filterMonitor.scrapeLogsForChanges();
        assertDeleted(filterMonitor, passed);
    }

    @Test
    public void testMonitoringOfFileAsADirectoryDoesNotProduceNotifications() throws Exception {

        appendSomething(monitoredFile);
        confusedFolderMonitor.scrapeLogsForChanges();
        assertNothingCreated(confusedFolderMonitor);
        assertNothingModified(confusedFolderMonitor);
        assertNothingDeleted(confusedFolderMonitor);

        flushNotifications(confusedFolderMonitor);
        deleteFile(monitoredFile);
        confusedFolderMonitor.scrapeLogsForChanges();
        assertNothingCreated(confusedFolderMonitor);
        assertNothingModified(confusedFolderMonitor);
        assertNothingDeleted(confusedFolderMonitor);

        flushNotifications(confusedFolderMonitor);
        createFile(monitoredFile);
        confusedFolderMonitor.scrapeLogsForChanges();
        assertNothingCreated(confusedFolderMonitor);
        assertNothingModified(confusedFolderMonitor);
        assertNothingDeleted(confusedFolderMonitor);
    }

    @Test
    public void testMonitoringOfDirectoryAsAFileDoesNotProduceNotifications() throws Exception {

        File file = new File(monitoredFolder, "testMonitoringOfDirectoryAsAFileDoesNotProduceNotifications");
        createFile(file);
        confusedFileMonitor.scrapeLogsForChanges();
        assertNothingCreated(confusedFileMonitor);
        assertNothingModified(confusedFileMonitor);
        assertNothingDeleted(confusedFileMonitor);

        flushNotifications(confusedFileMonitor);
        deleteFile(file);
        confusedFileMonitor.scrapeLogsForChanges();
        assertNothingCreated(confusedFileMonitor);
        assertNothingModified(confusedFileMonitor);
        assertNothingDeleted(confusedFileMonitor);

        flushNotifications(confusedFileMonitor);
        createFile(file);
        confusedFileMonitor.scrapeLogsForChanges();
        assertNothingCreated(confusedFileMonitor);
        assertNothingModified(confusedFileMonitor);
        assertNothingDeleted(confusedFileMonitor);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileCreationIsNotifiedWhenMonitoredFileDoesNotInitiallyExist() throws Exception {

        // Don't assume test ordering
        if (nonexistentFile.exists()) {
            deleteFile(nonexistentFile);
            flushNotifications(nonexistentFileMonitor);
        }
        createFile(nonexistentFile);
        nonexistentFileMonitor.scrapeLogsForChanges();
        assertCreated(nonexistentFileMonitor, nonexistentFile);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileModificationIsNotifiedWhenMonitoredFileDoesNotInitiallyExist() throws Exception {
        // Don't assume test ordering
        if (!nonexistentFile.exists()) {
            createFile(nonexistentFile);
            flushNotifications(nonexistentFileMonitor);
        }

        appendSomething(nonexistentFile);
        nonexistentFileMonitor.scrapeLogsForChanges();
        assertModified(nonexistentFileMonitor, nonexistentFile);
    }

    @Test
    public void testFileDeletionIsNotifiedWhenMonitoredFileDoesNotInitiallyExist() throws Exception {
        // Don't assume test ordering
        if (!nonexistentFile.exists()) {
            createFile(nonexistentFile);
            flushNotifications(nonexistentFileMonitor);
        }
        deleteFile(nonexistentFile);
        nonexistentFileMonitor.scrapeLogsForChanges();
        assertDeleted(nonexistentFileMonitor, nonexistentFile);
    }

    @Test
    public void testFolderCreationIsNotifiedWhenMonitoredFolderDoesNotInitiallyExist() throws Exception {

        // Don't assume test ordering
        if (!nonexistentFolder.exists()) {
            createFolder(nonexistentFolder);
            flushNotifications(nonexistentFolderMonitor);
        }
        File file = new File(nonexistentFolder, "testFolderCreationIsNotifiedWhenMonitoredFolderDoesNotInitiallyExist");
        createFile(file);
        nonexistentFolderMonitor.scrapeLogsForChanges();
        assertCreated(nonexistentFolderMonitor, file);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFolderModificationIsNotifiedWhenMonitoredFolderDoesNotInitiallyExist() throws Exception {
        // Don't assume test ordering
        if (!nonexistentFolder.exists()) {
            createFolder(nonexistentFolder);
            flushNotifications(nonexistentFolderMonitor);
        }

        File file = new File(nonexistentFolder, "testFolderModificationIsNotifiedWhenMonitoredFolderDoesNotInitiallyExist");
        createFile(file);
        flushNotifications(nonexistentFolderMonitor);

        appendSomething(file);
        nonexistentFolderMonitor.scrapeLogsForChanges();
        assertModified(nonexistentFolderMonitor, file);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFolderDeletionIsNotifiedWhenMonitoredFolderDoesNotInitiallyExist() throws Exception {
        // Don't assume test ordering
        if (!nonexistentFolder.exists()) {
            createFolder(nonexistentFolder);
            flushNotifications(nonexistentFolderMonitor);
        }

        File file = new File(nonexistentFolder, "testFolderDeletionIsNotifiedWhenMonitoredFolderDoesNotInitiallyExist");
        createFile(file);
        flushNotifications(nonexistentFolderMonitor);

        deleteFile(file);
        nonexistentFolderMonitor.scrapeLogsForChanges();
        assertDeleted(nonexistentFolderMonitor, file);
    }

    @Test
    public void testFileInFolderTimestampChanging() throws Exception {
        // Make a file to delete (and make sure we noticed it)
        File f = new File(monitoredFolder, "testFileInFolderTimestampChanging");
        createFile(f);
        flushNotifications(recursiveMonitor);

        // Make it a lot older than it is (should still be considered modified)
        updateTimestamp(f);

        recursiveMonitor.scrapeLogsForChanges();
        assertModified(recursiveMonitor, f);

    }

    @Test
    public void testFileInFolderDeletionIsNotified() throws Exception {
        // Make a file to delete (and make sure we noticed it)
        File f = new File(monitoredFolder, "testFileInFolderDeletionIsNotified");
        createFile(f);
        flushNotifications(recursiveMonitor);

        // Delete the file: it should be added to the 'deleted' list
        assertTrue("File should be deleted on the filesystem", f.delete());
        recursiveMonitor.scrapeLogsForChanges();
        assertDeleted(recursiveMonitor, f);

    }

    @Test
    public void testAllContentsAreIncludedForFileInFolderDeletion() throws Exception {
        // Make a file to delete (and make sure we noticed it)
        File folder = new File(monitoredFolder, "testFileInFolderDeletionIsNotified");
        createFolder(folder);
        File f = new File(folder, "childFile");
        createFile(f);
        flushNotifications(recursiveMonitor);

        // Delete the folder: it and its contents should be added to the 'deleted' list
        deleteFile(folder);
        recursiveMonitor.scrapeLogsForChanges();
        assertDeleted(recursiveMonitor, f, folder);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testFileInNestedFolderDeletionIsNotified() throws Exception {
        // Make a file to delete (and make sure we noticed it)
        File folder = new File(monitoredFolder, "nestedFolderForDeletion");
        createFolder(folder);
        File f = new File(folder, "testFileInNestedFolderDeletionIsNotified");
        createFile(f);
        flushNotifications(recursiveMonitor);
        flushNotifications(nonRecursiveMonitor);

        // Delete the file: it should be added to the 'deleted' list
        deleteFile(f);
        recursiveMonitor.scrapeLogsForChanges();
        nonRecursiveMonitor.scrapeLogsForChanges();
        assertDeleted(recursiveMonitor, f);
        assertNothingDeleted(nonRecursiveMonitor);

    }

    @Test
    public void testFileInNestedFolderFolderDeletionIsNotified() throws Exception {
        File bfolder = new File(monitoredFolder, "testFileInNestedFolderFolderDeletionIsNotified");
        // Get rid of any clutter we don't know about in the monitored folder (we want to make sure we notice it being deleted)
        // But don't assume test ordering -- don't try to delete if it doesn't exist!
        if (monitoredFolder.exists())
            deleteFile(monitoredFolder);
        createFolder(monitoredFolder);
        createFolder(bfolder);

        File bchildFile = new File(bfolder, "childFile");
        createFile(bchildFile);
        File bchildFolder = new File(bfolder, "childFolder");
        createFolder(bchildFolder);

        flushNotifications(recursiveMonitor);

        deleteFile(bchildFile);
        deleteFile(bchildFolder);
        deleteFile(bfolder);
        deleteFile(monitoredFolder);

        recursiveMonitor.scrapeLogsForChanges();
        // We should get told about every directory
        assertDeleted(recursiveMonitor, bfolder, bchildFolder, bchildFile);

        // Make a file to delete (and make sure we noticed it)
        File folder = new File(monitoredFolder, "testFileInNestedFolderFolderDeletionIsNotified");
        createFolder(folder);
        File childFolder = new File(folder, "testFileInNestedFolderDeletionIsNotified");
        createFile(childFolder);
        flushNotifications(recursiveMonitor);
        flushNotifications(nonRecursiveMonitor);

        // Delete the folder: it and its contents should be added to the 'deleted' list
        deleteFile(childFolder);
        deleteFile(folder);
        recursiveMonitor.scrapeLogsForChanges();
        nonRecursiveMonitor.scrapeLogsForChanges();
        assertDeleted(recursiveMonitor, folder, childFolder);
        assertDeleted(nonRecursiveMonitor, folder);

    }

    @Test
    public void testCreationOfMonitoredFolderIsNotifiedOnlyWhenIgnoreSelfIsTrue() throws Exception {
        // Get rid of anything in the monitored folder so we can make it again
        deleteFile(monitoredFolder);
        flushNotifications(recursiveMonitor);
        flushNotifications(monitorSelfMonitor);

        monitoredFolder.mkdirs();
        recursiveMonitor.scrapeLogsForChanges();
        monitorSelfMonitor.scrapeLogsForChanges();
        assertNothingCreated(recursiveMonitor);
        assertCreated(monitorSelfMonitor, monitoredFolder);

        // Now wait and make sure we don't get told again about the deleted file
        flushNotifications(recursiveMonitor);
        flushNotifications(monitorSelfMonitor);
        recursiveMonitor.scrapeLogsForChanges();
        monitorSelfMonitor.scrapeLogsForChanges();
        assertNothingCreated(recursiveMonitor);
        assertNothingCreated("After an initial (correct) creation report, ", monitorSelfMonitor);

    }

    @Mode(TestMode.FULL)
    @Test
    public void testDeletionOfMonitoredFolderIsNotifiedOnlyWhenIgnoreSelfIsTrue() throws Exception {
        // Get rid of anything in the monitored folder so we can safely delete it (by deleting it)
        deleteFile(monitoredFolder);
        // Now make the actual folder back (so when we delete it we know what notification to expect)
        monitoredFolder.mkdirs();
        flushNotifications(recursiveMonitor);
        flushNotifications(monitorSelfMonitor);

        deleteFile(monitoredFolder);
        // Delete the whole folder: it should be added to the 'deleted' list
        recursiveMonitor.scrapeLogsForChanges();
        monitorSelfMonitor.scrapeLogsForChanges();
        assertNothingDeleted(recursiveMonitor);
        assertDeleted(monitorSelfMonitor, monitoredFolder);

        // Now wait and make sure we don't get told again about the deleted file
        flushNotifications(recursiveMonitor);
        flushNotifications(monitorSelfMonitor);
        recursiveMonitor.scrapeLogsForChanges();
        monitorSelfMonitor.scrapeLogsForChanges();
        assertNothingDeleted(recursiveMonitor);
        assertNothingDeleted("After an initial (correct) deletion report, ", monitorSelfMonitor);

    }
}
