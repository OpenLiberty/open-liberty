/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.filemonitor.internal;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 * Update monitor used to monitor the contents of a directory.
 * <ul>
 * <li> {@link FileMonitor#MONITOR_FILTER} property is used to filter
 * the list of updated files using a regular expression.
 * <li> {@link FileMonitor#MONITOR_RECURSE} property is used to
 * specify whether or not files should be monitored recursively.
 * </ul>
 * 
 * <p>
 * This monitor is only used for files that exist: if the file does not exist
 * when monitoring is started, it will be watched by a {@link ResourceUpdateMonitor} until it does.
 * <p>
 * NOT THREAD SAFE: Calling/using class must ensure that only one operation (scan/init) is
 * active on the monitored file at a time.
 * 
 * @see FileMonitor
 */
public class DirectoryUpdateMonitor extends UpdateMonitor {

    private final String fileFilter;
    private final boolean filesOnly;
    private final boolean directoriesOnly;
    private final Pattern fileNameRegex;

    private static final class FileInfo {
        final boolean isFile;
        final long lastModified;
        final long size;

        FileInfo(boolean isFile, long lastModified, long size) {
            this.isFile = isFile;
            this.lastModified = lastModified;
            this.size = size;
        }

        boolean hasChanged(boolean newIsFile, long newLastModified, long newSize) {
            if (isFile != newIsFile)
                return true;

            if (isFile) {
                return newLastModified != lastModified
                       || newSize != size;
            }
            return false;
        }

        @Override
        public String toString() {
            return lastModified + ":" + size + (isFile ? ":f" : ":d");
        }
    }

    // This map will be replaced on first scan
    private LinkedHashMap<String, FileInfo> inMemoryScanResults = null;

    protected DirectoryUpdateMonitor(File monitoredFile, MonitorType type, String filter) {
        super(monitoredFile, type);

        fileFilter = filter;

        // Check for special filter values here: ignored in hashCode     
        filesOnly = FileMonitor.MONITOR_FILTER_FILES_ONLY.equals(fileFilter);
        directoriesOnly = filesOnly ? false : FileMonitor.MONITOR_FILTER_DIRECTORIES_ONLY.equals(fileFilter);
        fileNameRegex = (filesOnly || directoriesOnly || filter == null) ? null : Pattern.compile(fileFilter);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * The init method creates an initial baseline for the contents
     * of the directory according to the filter and recurse settings.
     * This will involve reading previously cached information, if present, to
     * identify resources changed since the last time the directory was
     * scanned.
     */
    @Override
    public void init(Collection<File> baseline) {
        performScan(baseline, null, null);
    }

    /**
     * Called to destroy an update monitor: this method should be
     * called when a resource should no longer be monitored, or when the
     * parameters (recurse/filter) have changed.
     */
    @Override
    public void destroy() {
        inMemoryScanResults = null;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[type=" + type
               + ",filter=" + fileFilter
               + ",file=" + monitoredFile
               + "]";
    }

    /**
     * Why not check .isFile()? Because certain types of files, like external
     * links (which are things used on zOS to point from the HFS into a DATASET),
     * are neither files nor directories. For the purposes of this class,
     * we want to consider these external links as files (so they can be detected
     * and loaded by the <library> that uses this <fileset>).
     * 
     * We also check that the file exists, because fortunately external links will
     * return true for exists(), while broken sym links will return false.
     */
    private static boolean isFile(File f) {
        return (f.exists() && !f.isDirectory());
    }

    /** {@inheritDoc} */
    @Override
    public void scanForUpdates(Collection<File> created, Collection<File> modified, Collection<File> deleted) {
        performScan(created, modified, deleted);
    }

    /** {@inheritDoc} */
    protected void performScan(Collection<File> created, Collection<File> modified, Collection<File> deleted) {

        final LinkedHashMap<String, FileInfo> prevScanResult = inMemoryScanResults;
        final LinkedHashMap<String, FileInfo> newScanResult = new LinkedHashMap<String, FileInfo>();

        // Check that directory exists
        if (!monitoredFile.isDirectory()) {
            // Directory does not exist or was removed
            // If we had/knew about files before, then all of the files were deleted.. 
            if (prevScanResult != null && !prevScanResult.isEmpty() && deleted != null) {
                for (Map.Entry<String, FileInfo> entry : prevScanResult.entrySet()) {
                    File f = new File(entry.getKey());

                    // Simplify: Make sure only stuff that matches gets
                    // into the cache, and then you can be indiscriminate about what
                    // gets removed.
                    deleted.add(f);
                }
                // Add the monitored directory itself to the results.. (if required)
                if (isIncludeSelf()) {
                    deleted.add(monitoredFile);
                }
            }
        } else {
            // Directory exists: check only nested resources that match.
            scanDirectory(prevScanResult, newScanResult, monitoredFile, created, modified);

            // Add the monitored directory itself to the results.. (if required)
            if (isIncludeSelf()) {
                boolean isFile = isFile(monitoredFile);
                if (matches(monitoredFile, isFile)) {
                    scanFile(prevScanResult, newScanResult, monitoredFile, created, modified, isFile);
                }
            }

            // Any remaining in the previous result have been deleted.
            // Notify based on filter settings
            if (deleted != null && prevScanResult != null) {
                for (Map.Entry<String, FileInfo> entry : prevScanResult.entrySet()) {
                    File f = new File(entry.getKey());

                    // Simplify: Make sure only stuff that matches gets into the cache, 
                    // and then you know whatever is leftover in the cache was deleted
                    // (because you know it matches.. )
                    deleted.add(f);
                }
            }
        }

        // replace the referenced map with the new scan result
        inMemoryScanResults = newScanResult;
    }

    /**
     * Scan a given file (remove from cacheMap as "seen", test for match against filter for
     * addition to created/modified list, add new file attributes to the new map).
     * <p>
     * Assumes only one scan is active at a time.
     * 
     * @param cacheMap The map of file information from the previous scan
     * @param newMap The map of information for files/directories found in this scan
     * @param f A file or directory to inspect for changes
     * @param created A list containing files that were not found in the previous scan
     * @param modified A list containing files whose attributes have changed since the previous scan
     * @param isFile true if this is a file
     */
    private void scanFile(LinkedHashMap<String, FileInfo> cacheMap, LinkedHashMap<String, FileInfo> newMap, File f, Collection<File> created, Collection<File> modified,
                          final boolean isFile) {
        String key = f.getAbsolutePath();

        // !REMOVE! we saw it in this scan, it's still here.
        // Removing items we've seen from the cacheMap allows us to identify deleted resources: 
        // they will be the only items left in the cacheMap when we've completed the scan.

        FileInfo cachedValue = null;
        if (cacheMap != null) {
            cachedValue = cacheMap.remove(key);
        }

        long fileModified = f.lastModified();
        long fileSize = f.length();

        final FileInfo newValue;
        if (cachedValue == null) {
            newValue = new FileInfo(isFile, fileModified, fileSize);
            // If we have no cached value, add the file to the list of created resources
            addToList(created, f);
        } else if (cachedValue.hasChanged(isFile, fileModified, fileSize)) {
            newValue = new FileInfo(isFile, fileModified, fileSize);
            // If the modified time or file size have changed since we last looked, 
            // add the file to the list of modified resources
            addToList(modified, f);
        } else {
            newValue = cachedValue;
        }

        // set the new value in the map
        newMap.put(key, newValue);
    }

    /**
     * Scan the "current" directory: recursive method.
     * <p>
     * Assumes only one scan is active at a time.
     * 
     * @param cacheMap The map of file information from the previous scan
     * @param newMap The map of information for files/directories found in this scan
     * @param currentDir The current directory
     * @param created A list containing files that were not found in the previous scan
     * @param modified A list containing files whose attributes have changed since the previous scan
     */
    private void scanDirectory(LinkedHashMap<String, FileInfo> cacheMap, LinkedHashMap<String, FileInfo> newMap, File currentDir, Collection<File> created,
                               Collection<File> modified) {
        File[] children = currentDir.listFiles();
        if (children != null) {
            for (File child : children) {
                // depth first if we're recursing
                if (isRecursing() && child.isDirectory()) {
                    scanDirectory(cacheMap, newMap, child, created, modified);
                }

                boolean isFile = isFile(child);
                if (matches(child, isFile)) {
                    scanFile(cacheMap, newMap, child, created, modified, isFile);
                }
            }
        }
    }

    protected boolean isRecursing() {
        return (type == MonitorType.DIRECTORY_RECURSE || type == MonitorType.DIRECTORY_RECURSE_SELF);
    }

    protected boolean isIncludeSelf() {
        return type == MonitorType.DIRECTORY_SELF || type == MonitorType.DIRECTORY_RECURSE_SELF;
    }

    /**
     * Check to see if this is a resource we're monitoring based
     * on the filter configuration
     * 
     * @param f File (or directory) to match against configured file filters
     * @param isFile true if this is a file (instead of a directory). This is looked
     *            up once and passed around.
     * @return true if no filter is configured, or if the file matches the filter
     */
    protected boolean matches(File f, boolean isFile) {
        if (fileFilter != null) {
            if (isFile && directoriesOnly) {
                return false;
            } else if (!isFile && filesOnly) {
                return false;
            } else if (fileNameRegex != null) {
                Matcher m = fileNameRegex.matcher(f.getName());
                if (!m.matches()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            if (((DirectoryUpdateMonitor) obj).fileFilter.equals(this.fileFilter)) {
                return true;
            }
        }
        return false;
    }

}
