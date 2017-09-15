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
package com.ibm.wsspi.kernel.filemonitor;

import java.io.File;
import java.util.Collection;

/**
 * Components that want to monitor the file system for changes should implement
 * the FileMonitor interface and register that implementation in the service registry.
 * <p>
 * The properties associated with the registered service specify what resources
 * are monitored and with what frequency. Valid service properties are listed
 * as constants below with descriptive javadoc.
 */
public interface FileMonitor {

    /**
     * <h4>Service property</h4>
     * 
     * The value should be a Collection<String>, indicating what
     * paths should be monitored. Elements of the collection will
     * only be monitored while they do not exist, or exist as files.
     */
    String MONITOR_FILES = "monitor.files";

    /**
     * <h4>Service property</h4>
     * 
     * The value should be a Collection<String>, indicating what
     * paths should be monitored. Elements of the collection will
     * only be monitored while they do not exist, or exist as
     * directories.
     */
    String MONITOR_DIRECTORIES = "monitor.directories";

    /**
     * <h4>Service property</h4>
     * 
     * Poll/Scanning interval as a long with a time unit suffix:
     * <ul>
     * <li>ms - milliseconds</li>
     * <li>s - seconds</li>
     * <li>m - minutes</li>
     * <li>h - hours</li>
     * </ul>
     * e.g. 2ms or 5s.
     * 
     * This is not a compound interval (e.g. 5s2ms will not work)
     */
    String MONITOR_INTERVAL = "monitor.interval";

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property should be a boolean: if true, a the monitor will receive
     * notifications for individual files in the directory tree that have changed.
     * If false, the monitor will only receive notification for the immediate children of the
     * specified directory.
     * 
     * <p>
     * For example, given monitored directory A:
     * <ul><li>In all cases, the monitor will be notified if A/file is created, changed, or deleted.
     * <li>When working with A/directory, if
     * <ul><li>monitor.recurse is true, the monitor will be notified when A/directory/file has been created, changed, or deleted.
     * <li>monitor.recurse is false, the monitor will only be notified when A/directory is added or removed.
     * </ul>
     * </ul>
     */
    String MONITOR_RECURSE = "monitor.recurse";

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property should be a boolean:
     * If true, the monitor will additionally notify for the creation/deletion of any directories being monitored.
     * If false, the monitor will report only on any files monitored, and the content of any monitored directories.
     * 
     * <p>
     * For example, given monitored directory A:
     * <ul><li>In all cases, the monitor will be notified if A/file is created, changed, or deleted.
     * <li>When working with A, if
     * <ul><li>monitor.includeself is true, the monitor will be notified when A has been created, changed, or deleted.
     * <li>monitor.includeself is false, the monitor will not be notified if A is created, changed, or deleted.
     * </ul>
     * </ul>
     */
    String MONITOR_INCLUDE_SELF = "monitor.includeself";

    /**
     * <h4>Service property</h4>
     * 
     * The value associated with this property is a collection<String>,
     * where each string is either a regex match of names, or a simple string: "files" for
     * only watching files, and "directories" for only monitoring directories.
     * 
     * <p>
     * This applies only to monitoring directories. Given monitored directory A:
     * <ul>
     * <li>"\\.xml" will monitor A/*.xml,
     * <li>{@value #MONITOR_FILTER_DIRECTORIES_ONLY} will only look for subdirectories of A
     * <li>{@value #MONITOR_FILTER_FILES_ONLY} will only look for files contained in A
     */
    String MONITOR_FILTER = "monitor.filter";

    /**
     * <h4>Property value</h4> Special filter string limiting the monitor to directories only.
     * 
     * @see #MONITOR_FILTER
     */
    String MONITOR_FILTER_DIRECTORIES_ONLY = "directories";

    /**
     * <h4>Property value</h4> Special filter string limiting the monitor to files only
     * 
     * @see #MONITOR_FILTER
     */
    String MONITOR_FILTER_FILES_ONLY = "files";

    /**
     * <h4>Service property</h4>
     * 
     * The value of this property determines how the file monitoring is to be performed.
     * Acceptable values are as follows:
     * <ul>
     * <li>{@value #MONITOR_TYPE_TIMED} will monitor the files and/or directories at regular intervals</li>
     * <li>{@value #MONITOR_TYPE_EXTERNAL} will defer monitoring to an external agent</li>
     * </ul>
     * <em>({@value #MONITOR_TYPE_TIMED} is the default setting.)</em>
     */
    String MONITOR_TYPE = "monitor.type";

    /** <h4>Property value</h4> Monitor at regular intervals. */
    String MONITOR_TYPE_TIMED = "timed";

    /** <h4>Property value</h4> Defer monitoring to an external agent. */
    String MONITOR_TYPE_EXTERNAL = "external";

    /**
     * Called with the result of a scan of specified resources.
     * Only resources that existed at the time of the scan will
     * be included.
     * 
     * @param baseline Collection of files which match the
     *            specified filters that were discovered during the scan.
     * @see #MONITOR_BASELINE
     */
    void onBaseline(Collection<File> baseline);

    /**
     * Called by the monitor service when a scheduled scan completes with
     * changes (indicated by the presence of files in the corresponding collection).
     * 
     * @param createdFiles A collection of files that were created since the last scan.
     * @param modifiedFiles A collection of files that were modified since the last scan.
     * @param deletedFiles A collection of files that were deleted since the last scan.
     */
    void onChange(Collection<File> createdFiles, Collection<File> modifiedFiles, Collection<File> deletedFiles);
}
