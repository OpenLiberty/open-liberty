/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.hpel.impl;

import java.io.File;
import java.io.FileFilter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.logging.hpel.reader.GenericFile;
import com.ibm.ws.logging.hpel.LogRepositoryBrowser;
import com.ibm.ws.logging.object.hpel.RepositoryPointerImpl;

/**
 * Implementation of the browser over log files in a file system and sub-processes log
 * files in sub-directories.
 */
public class LogRepositoryBrowserImpl extends LogRepositoryBaseImpl implements LogRepositoryBrowser {
    private final String pid;
    private final String label;
    private long timestamp;
    private final String[] ids;

    /**
     * Construct browser of files in the specified directory and associated with the
     * specified id path.
     * 
     * @param directory location containing repository files
     * @param ids array of strings leading to this repository.
     */
    public LogRepositoryBrowserImpl(File directory, String[] ids) {
        super(directory);
        this.timestamp = parseTimeStamp(directory.getName());
        this.pid = parseProcessID(directory.getName());
        this.label = parseLabel(directory.getName());
        this.ids = ids;
    }

    private File getChild(String name) {
        File result;
        if (getLocation() instanceof GenericFile) {
            result = ((GenericFile) getLocation()).getChild(name);
        } else {
            result = new File(getLocation(), name);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.LogFileBrowser#find(com.ibm.ws.logging.hpel.impl.RepositoryPointerImpl)
     */
    @Override
    public File findFile(RepositoryPointerImpl location) {
        // Assume that instanceIds are good and check fileId only.
        File result = getChild(location.getFileId());
        return isFile(result) ? result : null;
    }

    /**
     * @param result
     * @return
     */
    private boolean isFile(final File result) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return result.isFile();
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.LogFileBrowser#findNext(com.ibm.ws.logging.hpel.impl.RepositoryPointerImpl, long)
     */
    @Override
    public File findNext(RepositoryPointerImpl location, long timelimit) {
        return findNext(getChild(location.getFileId()), timelimit);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.LogRepositoryBrowser#findByMillis(long)
     */
    @Override
    public File findByMillis(long timestamp) {
        File[] files = listFiles(filter);

        File result = null;
        long max = Long.MIN_VALUE;
        for (File file : files) {
            long time = getLogFileTimestamp(file);
            // Select file with a maximum time stamp which is smaller than 'timestamp'.
            if (max < time && time <= timestamp) {
                max = time;
                result = file;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.LogFileBrowser#findNext(java.io.File, long)
     */
    @Override
    public File findNext(File current, long timelimit) {
        File[] files = listFiles(filter);
        File result = null;

        long min = Long.MAX_VALUE;
        long cur = current == null ? Long.MIN_VALUE : getLogFileTimestamp(current);
        for (File file : files) {
            long time = getLogFileTimestamp(file);
            // Select file with a smallest time stamp bigger than time stamp of the 'current' file.
            if (cur < time && time < min) {
                min = time;
                result = file;
            }
        }
        // return 'null' if found file has time stamp outside of the time limit.
        return timelimit > 0 && timelimit < min ? null : result;
    }

    @Override
    public File findPrev(File current, long timelimit) {
        long cur = current == null ? (timelimit < 0 ? Long.MAX_VALUE : timelimit) : getLogFileTimestamp(current);
        // return 'null' if reference file has time stamp outside of the time limit.
        if (timelimit >= 0 && cur < timelimit) {
            return null;
        }
        File[] files = listFiles(filter);
        File result = null;

        long max = Long.MIN_VALUE;
        for (File file : files) {
            long time = getLogFileTimestamp(file);
            // Select file with a smallest time stamp bigger than time stamp of the 'current' file.
            if (cur > time && time > max) {
                max = time;
                result = file;
            }
        }

        return result;
    }

    @Override
    public int count(File first, File last) {
        File[] files = listFiles(filter);
        // Short cut for the case of counting all files in the repository
        if (first == null && last == null) {
            return files.length;
        }

        int count = 0;
        long min = first == null ? Long.MIN_VALUE : getLogFileTimestamp(first);
        long max = last == null ? Long.MAX_VALUE : getLogFileTimestamp(last);
        for (File file : files) {
            long time = getLogFileTimestamp(file);
            // Select file with a smallest time stamp bigger than time stamp of the 'current' file.
            if (min <= time && time <= max) {
                count++;
            }
        }

        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.logging.hpel.MainLogRepositoryBrowser#getSubProcesses(com.ibm.ws.logging.hpel.LogFileBrowser)
     */
    @Override
    public Map<String, LogRepositoryBrowser> getSubProcesses() {
        HashMap<String, LogRepositoryBrowser> result = new HashMap<String, LogRepositoryBrowser>();
        for (File file : listFiles(subprocFilter)) {
            String id = getSubProcessId(file);
            String[] newIds = Arrays.copyOf(ids, ids.length + 1);
            newIds[ids.length] = id;
            result.put(id, new LogRepositoryBrowserImpl(file, newIds));
        }
        return result;
    }

    /**
     * @param subprocFilter
     * @return
     */
    private File[] listFiles(final FileFilter subprocFilter) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return getLocation().listFiles(subprocFilter);
            }
        });
    }

    private String getSubProcessId(File file) {
        return file.getName();
    }

    @Override
    public String[] getIds() {
        return ids;
    }

    @Override
    public String getProcessId() {
        return pid;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public long getTimestamp() {
        // If we didn't have timestamp before take it from the first file in the directory.
        if (timestamp < 0) {
            timestamp = getLogFileTimestamp(findNext((File) null, -1));
        }
        return timestamp;
    }

}
