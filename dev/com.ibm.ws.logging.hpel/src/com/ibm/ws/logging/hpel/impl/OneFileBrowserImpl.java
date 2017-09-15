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
package com.ibm.ws.logging.hpel.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.logging.hpel.reader.RepositoryLogRecord;
import com.ibm.websphere.logging.hpel.reader.RepositoryPointer;
import com.ibm.websphere.logging.hpel.reader.ServerInstanceLogRecordList;
import com.ibm.ws.logging.hpel.LogRecordSerializer;
import com.ibm.ws.logging.hpel.LogRepositoryBrowser;
import com.ibm.ws.logging.hpel.impl.LogRecordBrowser.IInternalRecordFilter;
import com.ibm.ws.logging.object.hpel.RepositoryLogRecordImpl;
import com.ibm.ws.logging.object.hpel.RepositoryPointerImpl;

/**
 * Implementation of the {@link LogRepositoryBrowser} containing just one file in the repository.
 */
public class OneFileBrowserImpl implements LogRepositoryBrowser {

    private final File repositoryFile;
    private final long timestamp;
    private final String processId;
    private final String label;
    private final static Map<String, LogRepositoryBrowser> emptyMap = new HashMap<String, LogRepositoryBrowser>();

    /*
     * Filter which accept all records.
     */
    private static class AllAcceptVerifier implements IInternalRecordFilter {
        @Override
        public boolean filterAccepts(LogRecordSerializer formatter, DataInputStream reader, RepositoryLogRecordImpl nextRecord) throws IOException {
            formatter.deserializeLogHead(nextRecord, reader);
            formatter.deserializeLogRecord(nextRecord, reader);
            return true;
        }

    }

    public OneFileBrowserImpl(File repositoryLocation) throws IllegalArgumentException {
        if (!isFile(repositoryLocation)) {
            throw new IllegalArgumentException("Specified location is not a file.");
        }
        OneLogFileRecordIterator it = new OneLogFileRecordIterator(repositoryLocation, -1, new AllAcceptVerifier()) {
            @Override
            protected RepositoryPointer getPointer(File file, long position) {
                return new RepositoryPointerImpl(getIds(), file.getName(), position);
            }
        };
        if (it.header == null) {
            it.close();
            throw new IllegalArgumentException("Specified file does not contains HPEL header.");
        }
        this.processId = it.header.getProperty(ServerInstanceLogRecordList.HEADER_PROCESSID);
        if ("Y".equalsIgnoreCase(it.header.getProperty(ServerInstanceLogRecordList.HEADER_ISZOS, "N"))) {
            this.label = it.header.getProperty(ServerInstanceLogRecordList.HEADER_JOBNAME, "name") + "_" +
                         it.header.getProperty(ServerInstanceLogRecordList.HEADER_JOBID, "id");
        } else {
            this.label = it.header.getProperty(ServerInstanceLogRecordList.HEADER_SERVER_NAME, "server");
        }
        RepositoryLogRecord first = it.next();
        this.timestamp = first == null ? -1L : first.getMillis();
        this.repositoryFile = repositoryLocation;
    }

    /**
     * @param repositoryLocation
     * @return
     */
    private boolean isFile(final File repositoryLocation) {
        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                return repositoryLocation.isFile();
            }
        });
    }

    @Override
    public int count(File first, File last) {
        return 1;
    }

    @Override
    public File findByMillis(long timestamp) {
        return timestamp < this.timestamp ? null : repositoryFile;
    }

    @Override
    public File findFile(RepositoryPointerImpl location) {
        return location.getFileId().equals(repositoryFile.getName()) ? repositoryFile : null;
    }

    @Override
    public File findNext(RepositoryPointerImpl location, long timelimit) {
        return null;
    }

    @Override
    public File findNext(File current, long timelimit) {
        return current == null ? repositoryFile : null;
    }

    @Override
    public File findPrev(File current, long timelimit) {
        return current == null ? repositoryFile : null;
    }

    @Override
    public String[] getIds() {
        return new String[0];
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    @Override
    public Map<String, LogRepositoryBrowser> getSubProcesses() {
        return emptyMap;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public long getLogFileTimestamp(File file) {
        return repositoryFile.equals(file) ? timestamp : -1L;
    }

}
