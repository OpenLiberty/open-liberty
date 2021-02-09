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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
public class AbstractNotificationTest {

    protected static final String MESSAGE_LOG = "logs/messages.log";
    private static final String BUNDLE_NAME = "test.filemonitor.bundle";
    private static final String FEATURE_NAME = "monitoringPrintingFeature-1.0";

    // Not thread-safe, but this code is unlikely to run multi-threaded, being a unit test
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    protected static LibertyServer server;
    protected static File monitoredFolder;
    protected static File monitoredFile;
    protected static File nonexistentFolder;
    protected static File nonexistentFile;

    @BeforeClass
    public static void classSetUp() throws Exception {
        server = LibertyServerFactory
                        .getLibertyServer("com.ibm.ws.kernel.filemonitor.server");
        // install our user feature 
        server.installUserBundle(BUNDLE_NAME); // NO HYPHENS! NO ".jar" SUFFIX! 
        server.installUserFeature(FEATURE_NAME); // NO UNDERSCORES! NO ".mf" SUFFIX! 
        server.startServer();

        // Figure out where our test monitors are monitoring
        String foldereyecatcher = "-MONITORED FOLDER-";
        String line = server.waitForStringInLog(foldereyecatcher);
        assertNotNull("Our test feature should report the folder location we're monitoring.", line);
        int index = line.indexOf(foldereyecatcher) + foldereyecatcher.length();
        // We create the file objects but not files on disk
        monitoredFolder = new File(line.substring(index));

        String fileeyecatcher = "-MONITORED FILE-";
        line = server.waitForStringInLog(fileeyecatcher);
        assertNotNull("Our test feature should report the file location we're monitoring.", line);
        index = line.indexOf(fileeyecatcher) + fileeyecatcher.length();
        // We create the file objects but not files on disk
        monitoredFile = new File(line.substring(index));

        String nonexistentfileeyecatcher = "-NONEXISTENT FILE-";
        line = server.waitForStringInLog(nonexistentfileeyecatcher);
        assertNotNull("Our test feature should report the file location we're monitoring.", line);
        index = line.indexOf(nonexistentfileeyecatcher) + nonexistentfileeyecatcher.length();
        // We create the file objects but not files on disk
        nonexistentFile = new File(line.substring(index));

        String nonexistentfoldereyecatcher = "-NONEXISTENT FOLDER-";
        line = server.waitForStringInLog(nonexistentfoldereyecatcher);
        assertNotNull("Our test feature should report the folder location we're monitoring.", line);
        index = line.indexOf(nonexistentfoldereyecatcher) + nonexistentfoldereyecatcher.length();
        // We create the folder objects but not folders on disk
        nonexistentFolder = new File(line.substring(index));

    }

    /**
     * Waits for notifications to catch up after file changes that we're not especially
     * interested in. This method should be used with caution since it resets
     * the mark, so it should only be used when no monitor might want to see
     * the changes that got notified.
     */
    protected int flushNotifications(MonitorReader monitor) throws Exception {
        // Wait for notifications to catch up
        int count = monitor.scrapeLogsForChanges();
        monitor.clear();

        server.setMarkToEndOfLog();

        return count;
    }

    protected void createFolder(File folder) {
        assertTrue("Folder should have been created on the file system", folder.mkdirs());
    }

    /**
     * Creates a file and also asserts we did actually create it
     * (thus avoiding many puzzling notification failures).
     */
    protected void createFile(File f) throws IOException {
        assertTrue("File should have been created on the file system: " + f, f.createNewFile());
    }

    /**
     * Deletes a file or folder and all its contents.
     * If anything doesn't get deleted, an assertion will be failed.
     */
    protected boolean deleteFile(File f) {
        boolean success = false;
        for (int pass = 0; !success && pass < 10; ++pass)
        {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) { // Very minor efficiency hack, avoid last level of recursion
                        // Recurse
                        deleteFile(file);
                    } else {
                        file.delete(); // Failure here will be caught by the later f.delete. Ugly, but that's what we had.
                    }
                }
            }
            success = f.delete();

            if (!success)
            {
                try
                {
                    Thread.sleep(500);
                } catch (InterruptedException e)
                {
                    // that's ok, just try it again
                }
            }
        }
        assertTrue("Should have been able to delete the file " + f, success);
        return success;
    }

    protected void appendSomething(File f) throws FileNotFoundException {
        PrintWriter w = new PrintWriter(new FileOutputStream(f));
        w.println(new Date() + "Append some stuff\n\n");
        w.flush();
        w.close();
    }

    protected boolean updateTimestamp(File f) {
        return f.setLastModified(System.currentTimeMillis() - (30 * 60 * 1000));
    }

    protected void assertNothingDeleted(final MonitorReader monitorReader) {
        assertTrue(
                   "The following files were unexpectedly reported as deleted by the " + monitorReader.name + ":" + stringify(monitorReader.deleted),
                   monitorReader.deleted.isEmpty());
    }

    protected void assertNothingDeleted(String messagePrefix, final MonitorReader monitorReader) {
        assertTrue(messagePrefix +
                   "the following files were unexpectedly reported as deleted by the " + monitorReader.name + ":" + stringify(monitorReader.deleted),
                   monitorReader.deleted.isEmpty());
    }

    protected void assertNothingModified(final MonitorReader monitorReader) {
        assertTrue("The following files were unexpectedly reported as modified by the " + monitorReader.name + ":" + stringify(monitorReader.modified),
                   monitorReader.modified.isEmpty());
    }

    protected void assertNothingCreated(String messagePrefix, final MonitorReader monitorReader) {
        assertTrue(messagePrefix +
                   "the following files were unexpectedly reported as created by the " + monitorReader.name + ":" + stringify(monitorReader.created),
                   monitorReader.created.isEmpty());
    }

    protected void assertNothingCreated(final MonitorReader monitorReader) {
        assertTrue(
                   "The following files were unexpectedly reported as created by the " + monitorReader.name + ":" + stringify(monitorReader.created),
                   monitorReader.created.isEmpty());
    }

    protected void assertModified(final MonitorReader monitorReader, File... files) {

        final Collection<File> collected = new ArrayList<File>();
        for (File file : files) {
            collected.add(file);
        }

        assertNothingCreated(monitorReader);
        assertNothingDeleted(monitorReader);

        assertFalse("A file should have been reported as modified by the " + monitorReader.name + ". \nGave up searching at " + DATE_FORMAT.format(monitorReader.lastSearchTime)
                    + ".", monitorReader.modified.isEmpty());
        assertEqualsOrderless("The wrong file was reported as modified by the " + monitorReader.name, collected, monitorReader.modified);
    }

    protected void assertDeleted(final MonitorReader monitorReader, File... files) {

        final Collection<File> collected = new ArrayList<File>();
        for (File file : files) {
            collected.add(file);
        }

        assertNothingModified(monitorReader);
        assertNothingCreated(monitorReader);

        assertFalse("A file should have been reported as deleted by the " + monitorReader.name + ". \nGave up searching at " + DATE_FORMAT.format(monitorReader.lastSearchTime)
                    + ".", monitorReader.deleted.isEmpty());
        assertEqualsOrderless("The wrong file was reported as deleted by the " + monitorReader.name, collected, monitorReader.deleted);
    }

    protected void assertCreated(final MonitorReader monitorReader, File... files) {

        final Collection<File> collected = new ArrayList<File>();
        for (File file : files) {
            collected.add(file);
        }

        assertNothingModified(monitorReader);
        assertNothingDeleted(monitorReader);

        assertFalse("A file should have been reported as created by the " + monitorReader.name + ". \nGave up searching at " + DATE_FORMAT.format(monitorReader.lastSearchTime)
                    + ".", monitorReader.created.isEmpty());
        assertEqualsOrderless("The wrong file was reported as created by the " + monitorReader.name, collected, monitorReader.created);
    }

    protected void assertEqualsOrderless(String message, File[] expected, Collection<File> actual) {
        assertEqualsOrderless(message, Arrays.asList(expected), actual);
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
        assertEquals("Although we were notified about the correct files, the same file was unexpectedly included multiple times in the list of changed files.",
                     convertToSortedList(expectedList),
                     convertToSortedList(actual));

    }

    private String stringify(Collection<File> list) {
        return Arrays.toString(list.toArray());
    }

    private List<File> convertToSortedList(Collection<File> collection) {
        List<File> list = new ArrayList<File>();
        list.addAll(collection);
        Collections.sort(list);

        return list;
    }

    private Set<File> convertToSet(Collection<File> collection) {
        Set<File> set = new HashSet<File>();
        set.addAll(collection);
        return set;
    }
}
