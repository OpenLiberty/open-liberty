/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.logging.internal.NLSConstants;
import com.ibm.wsspi.logging.Incident;

public class IncidentImpl implements Incident {
    private static final TraceComponent tc = Tr.register(IncidentImpl.class, NLSConstants.GROUP, NLSConstants.FFDC_NLS);

    static class Key implements Comparable<Key> {
        final String sourceId;
        final String probeId;
        final String exceptionName;
        final long threadId;

        public Key(String sourceId, String probeId, Throwable th) {
            this.sourceId = sourceId;
            this.probeId = probeId;
            if (th == null) {
                exceptionName = String.valueOf((String) null);
            } else {
                exceptionName = th.getClass().getName();
            }
            this.threadId = Thread.currentThread().getId();
        }

        @Override
        public int compareTo(Key other) {
            final int cSourceId = sourceId.compareTo(other.sourceId);
            if (cSourceId != 0)
                return cSourceId;

            final int cProbeId = probeId.compareTo(other.probeId);
            if (cProbeId != 0)
                return cProbeId;

            final int cExpName = exceptionName.compareTo(other.exceptionName);
            if (cExpName != 0)
                return cExpName;

            return 0;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((exceptionName == null) ? 0 : exceptionName.hashCode());
            result = prime * result + ((probeId == null) ? 0 : probeId.hashCode());
            result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
            return result;
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            return compareTo((Key) obj) == 0;
        }

        @Override
        public String toString() {
            return exceptionName + ' ' + sourceId + ' ' + probeId;
        }
    }

    static class IncidentFile {
        private final int exceptionMsgHash;
        private volatile boolean refreshLog = false;
        private File incidentFile = null;

        /**
         * @param hashCode
         * @param newFile
         */
        public IncidentFile(int hashCode) {
            this.exceptionMsgHash = hashCode;
        }

        public synchronized File getFile() {
            return incidentFile;
        }

        /**
         * Replace the file and mark as not-for-refresh (until the daily task runs that
         * will mark this as eligible for replacement again)
         *
         * @param file New file
         * @return Old file (may be null)
         */
        public synchronized File setFile(File file) {
            File oldFile = incidentFile;
            incidentFile = file;
            refreshLog = false;

            // return the old value
            return oldFile;
        }

        public void enableReplace() {
            refreshLog = true;
        }
    }

    final FileLogSet fileLogSet;
    final Key key;
    private final Date firstOccurrence;
    private String label;

    private volatile long timeStamp;
    private final AtomicInteger count = new AtomicInteger(0);

    private int dailyWaterMark = 0;

    /**
     * List of files associated with this key. Oldest will be first
     * newest will be last. We keep at most 10 unique exception messages per
     * source/probe/exception, to accommodate cases where the processing path
     * looks the same (same stack trace), but where the exception message contains
     * information that can't be easily obtained otherwise, and which makes a significant
     * difference in the actual problem to be diagnosed.
     */
    private final List<IncidentFile> incidentFiles;

    public IncidentImpl(FileLogSet fileLogSet, final Key key) {
        this.fileLogSet = fileLogSet;
        this.key = key;
        timeStamp = System.currentTimeMillis();
        firstOccurrence = new java.util.Date(timeStamp);
        incidentFiles = new ArrayList<IncidentFile>();
    }

    /**
     * @param th
     * @param callerThis
     * @param objectArray
     */
    public void log(Throwable th, Object callerThis, Object[] objectArray) {
        boolean logThis = true;
        File newFile = null;
        File oldFile = null;
        IncidentFile incident = null;

        int exMsgHash = th == null ? 0 : th.toString().hashCode();

        synchronized (this) {
            timeStamp = System.currentTimeMillis();

            // increment the total number of incidents (always)
            count.incrementAndGet();
            dailyWaterMark++;

            if (dailyWaterMark > 10) {
                // We've exceeded the max per day: no logging.
                logThis = false;
            } else {
                // If we haven't gone beyond our "max unique instances" for today,
                // see if we should make a unique log for this one...

                // See if we have an incident for this exact message (using the hash of the message text)
                // we have at most 10 items to look at
                for (Iterator<IncidentFile> i = incidentFiles.iterator(); i.hasNext();) {
                    IncidentFile oldIncident = i.next();

                    if (oldIncident.exceptionMsgHash == exMsgHash) {
                        incident = oldIncident;

                        // this is an exact match. move to the end of the list (newest)
                        i.remove();
                        incidentFiles.add(incident);

                        logThis = oldIncident.refreshLog;
                        break;
                    }
                }

                // if we didn't find a match..
                if (incident == null) {
                    // we'll create a new incident for this one.
                    logThis = true;

                    // create and add the new incident
                    incident = new IncidentFile(exMsgHash);
                    incidentFiles.add(incident);
                }

                // remove the oldest one if we've exceeded 10...
                if (incidentFiles.size() > 10) {
                    IncidentFile oldIncident = incidentFiles.remove(0);
                    oldFile = oldIncident.setFile(null);
                }
            }
        }

        // If we cleaned up an old entry, delete that file.
        if (oldFile != null) {
            oldFile.delete();
        }

        // If we should log a new incident...
        if (logThis && incident != null) {
            IncidentStreamImpl iStream = null;
            try {
                iStream = new IncidentStreamImpl(fileLogSet);
            } catch (Exception e) {
                // darn. Prevent the exception logging the error from percolating upward
            }

            if (iStream != null) {
                try {
                    newFile = iStream.getFile();
                    new IncidentLogger().logIncident(iStream, this, th, callerThis, objectArray);
                } catch (Throwable e) {
                    iStream.printStackTrace(e);
                } finally {
                    LoggingFileUtils.tryToClose(iStream);

                    oldFile = incident.setFile(newFile);
                    if (oldFile != null) {
                        oldFile.delete();
                    }
                }
            }

            if (tc.isInfoEnabled() && newFile != null) {
                final String txt = th + " " + key.sourceId + " " + key.probeId;
                Tr.info(tc, "lwas.FFDCIncidentEmitted", txt, newFile.getName());
            }
        }
    }

    /**
     * @return
     */
    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * @return
     */
    @Override
    public int getCount() {
        return count.get();
    }

    /**
     * @return
     */
    @Override
    public Date getDateOfFirstOccurrence() {
        return firstOccurrence;
    }

    /**
     * @return
     */
    @Override
    public String getExceptionName() {
        return key.exceptionName;
    }

    /**
     * @return
     */
    @Override
    public String getSourceId() {
        return key.sourceId;
    }

    /**
     * @return
     */
    @Override
    public String getProbeId() {
        return key.probeId;
    }

    /**
     * @return
     */
    @Override
    public long getThreadId() {
        return key.threadId;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    @Override
    public String getIntrospectedCallerDump() {
        return "";
    }

    /**
     * Allow incident files to be replaced so they stay current with
     * log files: called by {@link BaseFFDCService#rollLogs()}
     */
    public synchronized void roll() {
        dailyWaterMark = 0;
        for (IncidentFile inF : incidentFiles) {
            inF.enableReplace();
        }
    }

    /**
     * @return
     */
    public synchronized List<IncidentFile> getFiles() {
        return new ArrayList<IncidentFile>(incidentFiles);
    }

    /**
     *
     */
    public void cleanup() {
        List<IncidentFile> files = getFiles();

        // Working with log files must occur using the server's identity. This is a no-op in most cases.
        Object token = ThreadIdentityManager.runAsServer();
        try {
            for (IncidentFile ifile : files) {
                File f = ifile.getFile();
                if (f != null)
                    LoggingFileUtils.deleteFile(ifile.getFile());
            }
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * @param i
     * @return
     */
    public String formatSummaryEntry(int index) {
        DateFormat dateFormatter = BaseTraceFormatter.useIsoDateFormat ? new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ") : getDateFormatter();
        String dateString = "";
        String firstDateString = ""; //d375467
        java.util.Date date = new java.util.Date(timeStamp);

        try {
            dateString = dateFormatter.format(date);
            firstDateString = dateFormatter.format(firstOccurrence); //d375467
        } catch (Throwable th) {
            // We can't FFDC from here, but we should know if something happened.
            th.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();

        @SuppressWarnings("resource")
        Formatter formatter = new Formatter(sb, Locale.US);

        formatter.format("%6d %6d %27s %27s",
                         index,
                         count.get(),
                         firstDateString,
                         dateString);

        sb.append(' ');
        sb.append(key.exceptionName);
        sb.append(' ');
        sb.append(key.sourceId);
        sb.append(' ');
        sb.append(key.probeId);

        for (Iterator<IncidentFile> i = getFiles().iterator(); i.hasNext();) {
            sb.append(LoggingConstants.nl);
            File f = i.next().getFile();
            if (f != null)
                formatter.format("%6s %70s %s", " ", "-", f.getAbsolutePath());
        }

        return sb.toString();
    }

    protected DateFormat getDateFormatter() {
        return DateFormatProvider.getDateFormat();
    }
}
