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
package com.ibm.ws.kernel.filemonitor.monitor.test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

public class FileMonitorRegistrationHandler {

    private final long monitorInterval = 150;

    public void activate(ComponentContext cc) throws IOException {
        System.out.println("FAT test code is registering FileMonitor implementations.");

        File tmpFile = File.createTempFile(FileMonitorRegistrationHandler.class.getName(), "file");

        // Make a temporary folder to monitor
        File tmpFolder = File.createTempFile(FileMonitorRegistrationHandler.class.getName(), "folder");
        tmpFolder.delete();
        tmpFolder.mkdirs();

        File nonExistentFile = File.createTempFile(FileMonitorRegistrationHandler.class.getName(), "nonexistentfile");
        nonExistentFile.delete();

        // Make a temporary folder to monitor
        File nonExistentFolder = File.createTempFile(FileMonitorRegistrationHandler.class.getName(), "nonexistentfolder");
        nonExistentFolder.delete();

        // We need to make baseline files before we register
        File baseline = new File(tmpFolder, "baseline");
        baseline.createNewFile();
        File baseline1 = new File(tmpFolder, "baseline1");
        baseline1.createNewFile();
        File nestedFolder = new File(tmpFolder, "nestedBaselineFolder");
        nestedFolder.mkdirs();
        File nestedFile = new File(nestedFolder, "nestedBaselineFile");
        nestedFile.createNewFile();

        // This output is important to the FAT test
        System.out.println("-MONITORED FOLDER-" + tmpFolder.getAbsolutePath());
        System.out.println("-MONITORED FILE-" + tmpFile.getAbsolutePath());
        System.out.println("-NONEXISTENT FOLDER-" + nonExistentFolder.getAbsolutePath());
        System.out.println("-NONEXISTENT FILE-" + nonExistentFile.getAbsolutePath());

        BundleContext bundleContext = cc.getBundleContext();

        Collection<String> folderSet = new HashSet<String>();
        folderSet.add(tmpFolder.getAbsolutePath());

        Collection<String> fileSet = new HashSet<String>();
        fileSet.add(tmpFile.getAbsolutePath());

        Collection<String> nonExistentFolderSet = new HashSet<String>();
        nonExistentFolderSet.add(nonExistentFolder.getAbsolutePath());

        Collection<String> nonExistentFileSet = new HashSet<String>();
        nonExistentFileSet.add(nonExistentFile.getAbsolutePath());

        registerRecursiveMonitor(bundleContext, "-RECURSIVETESTMONITOROUTPUT-", folderSet);
        registerRecursiveMonitor(bundleContext, "-NONEXISTENTFOLDERTESTMONITOROUTPUT-", nonExistentFolderSet);
        // Register something monitoring a directory, where there's actually a file in that spot
        registerRecursiveMonitor(bundleContext, "-RECURSIVEBUTACTUALLYMONITORINGFILETESTMONITOROUTPUT-", fileSet);

        // Also register a monitor with filters
        registerRegexFilteredFileMonitor(bundleContext, folderSet);
        registerFileFilteredFileMonitor(bundleContext, folderSet);
        registerDirectoryFilteredFileMonitor(bundleContext, folderSet);

        // Also register a non-recursive monitor
        registerNonRecursiveMonitor(bundleContext, folderSet);

        // Also register a monitor which isn't watching its own folder
        registerMonitorSelfMonitor(bundleContext, folderSet);
        registerNonRecursiveMonitorSelfMonitor(bundleContext, folderSet);

        // Also register a monitor watching a file
        registerFileMonitor(bundleContext, "-FILETESTMONITOROUTPUT-", fileSet);
        registerFileMonitor(bundleContext, "-FILENONEXISTENTTESTMONITOROUTPUT-", nonExistentFileSet);
        // Register something monitoring a directory, where there's actually a file in that spot
        registerFileMonitor(bundleContext, "-FILEBUTACTUALLYMONITORINGDIRECTORYTESTMONITOROUTPUT-", folderSet);

        // Register an externally driven monitor watching a folder
        registerManualMonitor(bundleContext, folderSet);

    }

    /**
     * A monitor which really is of file-type to monitor just files (not folders).
     */
    private void registerFileMonitor(BundleContext bundleContext, String fileEyecatcher, Collection<String> fileSet) {
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_FILES, fileSet);
        fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(fileEyecatcher), fileMonitorProps);
    }

    private void registerMonitorSelfMonitor(BundleContext bundleContext, Collection<String> folderSet) {
        String monitorSelfEyecatcher = "-MONITORSELFTESTMONITOROUTPUT-";
        final Hashtable<String, Object> monitorSelfFileMonitorProps = new Hashtable<String, Object>();
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, folderSet);
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, true);
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_RECURSE, true);
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(monitorSelfEyecatcher), monitorSelfFileMonitorProps);
    }

    private void registerNonRecursiveMonitorSelfMonitor(BundleContext bundleContext, Collection<String> folderSet) {
        String monitorSelfEyecatcher = "-NONRECURSEMONITORSELFTESTMONITOROUTPUT-";
        final Hashtable<String, Object> monitorSelfFileMonitorProps = new Hashtable<String, Object>();
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, folderSet);
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, true);
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_RECURSE, false);
        monitorSelfFileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(monitorSelfEyecatcher), monitorSelfFileMonitorProps);
    }

    private void registerNonRecursiveMonitor(BundleContext bundleContext, Collection<String> folderSet) {
        // Avoid overlap with the eyecatcher for the recursive monitor to avoid regexp horribleness
        String nonRecursiveEyecatcher = "-NONRECURSINGTESTMONITOROUTPUT-";
        final Hashtable<String, Object> nonRecursiveFileMonitorProps = new Hashtable<String, Object>();
        nonRecursiveFileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, folderSet);
        nonRecursiveFileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, false);
        nonRecursiveFileMonitorProps.put(FileMonitor.MONITOR_RECURSE, false);
        nonRecursiveFileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(nonRecursiveEyecatcher), nonRecursiveFileMonitorProps);
    }

    /**
     * A monitor which monitors a folder recursively (but doesn't include itself).
     */
    private void registerRecursiveMonitor(BundleContext bundleContext, String recursiveEyecatcher, Collection<String> fileSet) {
        final Hashtable<String, Object> recursiveFileMonitorProps = new Hashtable<String, Object>();
        recursiveFileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, fileSet);
        recursiveFileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, false);
        recursiveFileMonitorProps.put(FileMonitor.MONITOR_RECURSE, true);
        recursiveFileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(recursiveEyecatcher), recursiveFileMonitorProps);
    }

    private void registerRegexFilteredFileMonitor(BundleContext bundleContext, Collection<String> fileSet) {
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, fileSet);
        fileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, false);
        fileMonitorProps.put(FileMonitor.MONITOR_RECURSE, true);
        fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        // Use the middle of the file's name as a filter
        String filter = ".*include.*";
        fileMonitorProps.put(FileMonitor.MONITOR_FILTER, filter);
        String eyecatcher = "-FILTEREDTESTMONITOROUTPUT-";
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(eyecatcher), fileMonitorProps);
    }

    private void registerFileFilteredFileMonitor(BundleContext bundleContext, Collection<String> fileSet) {
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, fileSet);
        fileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, false);
        fileMonitorProps.put(FileMonitor.MONITOR_RECURSE, true);
        fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        fileMonitorProps.put(FileMonitor.MONITOR_FILTER, FileMonitor.MONITOR_FILTER_FILES_ONLY);
        String eyecatcher = "-FILEFILTERTESTMONITOROUTPUT-";
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(eyecatcher), fileMonitorProps);
    }

    private void registerDirectoryFilteredFileMonitor(BundleContext bundleContext, Collection<String> fileSet) {
        final Hashtable<String, Object> fileMonitorProps = new Hashtable<String, Object>();
        fileMonitorProps.put(FileMonitor.MONITOR_DIRECTORIES, fileSet);
        fileMonitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, true);
        fileMonitorProps.put(FileMonitor.MONITOR_RECURSE, true);
        fileMonitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        fileMonitorProps.put(FileMonitor.MONITOR_FILTER, FileMonitor.MONITOR_FILTER_DIRECTORIES_ONLY);
        String eyecatcher = "-DIRECTORYFILTERTESTMONITOROUTPUT-";
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(eyecatcher), fileMonitorProps);
    }

    private void registerManualMonitor(BundleContext bundleContext, Collection<String> fileSet) {
        final Hashtable<String, Object> monitorProps = new Hashtable<String, Object>();
        monitorProps.put(FileMonitor.MONITOR_DIRECTORIES, fileSet);
        monitorProps.put(FileMonitor.MONITOR_INCLUDE_SELF, true);
        monitorProps.put(FileMonitor.MONITOR_RECURSE, true);
        monitorProps.put(FileMonitor.MONITOR_INTERVAL, monitorInterval);
        monitorProps.put(FileMonitor.MONITOR_TYPE, FileMonitor.MONITOR_TYPE_EXTERNAL);

        String eyecatcher = "-MANUALMONITOROUTPUT-";
        bundleContext.registerService(FileMonitor.class, new FileMonitorPrintingImplementation(eyecatcher), monitorProps);
    }

}
