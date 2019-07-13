/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.joblog;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import javax.batch.runtime.JobExecution;

public class JobExecutionLog {
    
    /**
     * The execution id.
     */
    private JobExecution jobExecution;
    
    /**
     * All job log file parts for this JobExecution.
     */
    private List<File> jobLogFiles;
    
    /**
     * A ref to "logs/joblogs/{jobname}/{date}/instance.{instanceId}/execution.{executionId}".  
     * This is used to help resolve relative names for the jobLogFiles.
     */
    private File execLogRootDir;
    
    /**
     * CTOR.
     */
    public JobExecutionLog(JobExecution execution, List<File> jobLogFiles, File rootDir) {
        this.jobExecution = execution;
        this.jobLogFiles = new ArrayList<File>(jobLogFiles); // copy the list.
        this.execLogRootDir = rootDir;
    }
    
    public JobExecution getJobExecution() {
        return jobExecution;
    }
    
    public long getExecutionId() {
        return jobExecution.getExecutionId();
    }
    
    public List<File> getJobLogFiles() {
        return jobLogFiles;
    }
    
    /**
     * @return the joblogs root dir, for resolving the relative names
     *         of job log files.
     */
    public File getExecLogRootDir() {
        return execLogRootDir;
    }

    /**
     * @param relativePath path to joblog part, relative to the job execution's root joblog dir.
     * 
     * @return the joblog file with the given relativePath, or null if the part doesn't exist. 
     */
    public File getPartByRelativePath(String relativePath) {
        
        relativePath = normalizePath(relativePath);
        
        for (File jobLogFile : jobLogFiles) {
            if (normalizePath(getRelativePath(jobLogFile)).equals(relativePath)) {
                return jobLogFile;
            }
        }
        
        return null;
    }
    
    /**
     * @return the relativePath of the given joblog part (relative to the jobexecution's root joblog dir)
     */
    protected String getRelativePath(File jobLogFile) {
        try {
            return stripPrefix( jobLogFile.getCanonicalPath(), getExecLogRootDir().getCanonicalPath() + File.separator);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }
    
    /**
     * @return str, with prefix stripped away from the beginning of it.
     */
    protected static String stripPrefix(String str, String prefix) {
        return (str.startsWith(prefix)) ? str.substring(prefix.length()) : str;
    }
    
    /**
     * @return the path with all file separators normalized to "/".
     */
    protected static String normalizePath(String path) {
        return (path != null) ? path.replaceAll("\\\\", "/") : path;
    }

    /**
     * @return a list of relative paths to all job log parts (relative to the jobexecution's root joblog dir).
     *         Path separator normalized to "/".
     */
    public List<String> getRelativePaths() {
        List<String> retMe  = new ArrayList<String>();
        
        for (File jobLogFile : getJobLogFiles()) {
            retMe.add( getRelativePath( jobLogFile ) );
        }
        
        return retMe;
    }
    
    /**
     * Delete all files associated with this execution from the filesystem.
     * @return true if all files were successfully deleted.
     */
    public boolean purge() {
    	return deleteFileRecursive(execLogRootDir);
    }

    private boolean deleteFileRecursive(final File file) {
        boolean success = true;
        boolean isThere = true;

        boolean isDir = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return file.isDirectory();
            }
        });
        //defect 191586 check if the file exists because file.isDirectory() will return false if it
        //does not exist as well as not being a directory so need a second check.
        if (!isDir) {
            isThere = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return file.exists();
                }
            });
        }

        if (isDir) {
            File[] children = AccessController.doPrivileged(new PrivilegedAction<File[]>() {
                @Override
                public File[] run() {
                    return file.listFiles();
                }
            });
            for (File f : children) {
                success = success && deleteFileRecursive(f);
            }
        }
        //defect 191586: make sure the file exists if it is not a directory.  If it does not exist
        //return true to continue with the purge process.
        if (success && isThere) {
            success = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return file.delete();
                }
            });
        }
        return success;
    }
}