/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.ffdc.FFDCConfigurator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.logging.internal.NLSConstants;
import com.ibm.ws.logging.internal.impl.LoggingConstants.FFDCSummaryPolicy;
import com.ibm.wsspi.logging.IncidentForwarder;
import com.ibm.wsspi.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.logprovider.FFDCFilterService;
import com.ibm.wsspi.logprovider.LogProviderConfig;

/**
 * Simple FFDC service instance that only allows one output file for FFDC
 * incident logging.
 */
public class BaseFFDCService implements FFDCFilterService {
    /** The trace component used by this class */
    private static final TraceComponent tc = Tr.register(BaseFFDCService.class, NLSConstants.GROUP, NLSConstants.FFDC_NLS);

    /** Map of incidents, in order of insertion to the map */
    private final Map<IncidentImpl.Key, IncidentImpl> incidents = new LinkedHashMap<IncidentImpl.Key, IncidentImpl>();

    private volatile File ffdcLogDirectory = null;
    private volatile File summaryFile;

    /** Concurrent access by synchronizing "this" (BaseFFDCService). */
    final FileLogSet summaryLogSet = new FileLogSet(false);

    /** Concurrent access by synchronizing ffdcLogSet. */
    final FileLogSet ffdcLogSet = new FileLogSet(false);

    /**
     * The time in nanoseconds since the last time the table was dumped.
     */
    private long lastTimeOfDump = Long.MIN_VALUE; // We use MIN_VALUE as the initial value since System.nanoTime can be negative

    /**
     * The number of exceptions which have been processed since the last time
     * the hash table contents were dumped.
     */
    private int numberOfEntiesProcessed = 0;

    private FFDCSummaryPolicy ffdcSummaryPolicy = FFDCSummaryPolicy.DEFAULT;

    /**
     * The value in nanoseconds of the minimum amount of time which will elapse
     * between the dump of the hash table.
     */
    private static final long lowWaterTime = (1L * 60L * 1000000000L); // 1 minute

    /**
     * The number of exceptions which need to be processed before the hash table
     * is dumped.
     */
    private static final int normalDumpThreshold = 10;

    /**
     * The maximum amount of time (in nanoseconds) before the hash table is dumped.
     */
    private static final long highWaterTime = (5L * 60L * 1000000000L); // 5 minutes

    /**
     * Initialize the FFDC service based on the provided configuration
     */
    @Override
    public synchronized void init(LogProviderConfig config) {
        ffdcSummaryPolicy = ((LogProviderConfigImpl) config).getFfdcSummaryPolicy();
        update(config);
    }

    /**
     * Update the FFDC service based on the provided configuration
     */
    @Override
    public synchronized void update(LogProviderConfig config) {
        // Get the configured log location, and derive the ffdc location from it
        File location = config.getLogDirectory();
        if (location == null) {
            location = new File(".");
        }
        location = new File(location, FFDCConfigurator.FFDC_DIR);

        ffdcLogDirectory = location;

        int maxFiles = config.getMaxFiles();

        // Creation and rolling of files should be done as the server
        Object token = ThreadIdentityManager.runAsServer();
        try {
            summaryLogSet.update(location,
                                 FFDCConfigurator.FFDC_SUMMARY_FILE_NAME,
                                 FFDCConfigurator.FFDC_EXTENSION,
                                 maxFiles);
            synchronized (ffdcLogSet) {
                ffdcLogSet.update(location,
                                  FFDCConfigurator.FFDC_FILE_NAME,
                                  FFDCConfigurator.FFDC_EXTENSION,
                                  0 /* unlimited */);
            }
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    @Override
    public File getFFDCLogLocation() {
        return ffdcLogDirectory;
    }

    /**
     * Stop this FFDC service instance and free up any resources currently used.
     */
    @Override
    public synchronized void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stopping the basic FFDC service");
        }
        logSummary(true);
    }

    /**
     * Process an exception
     * 
     * @param th
     *            The exception to be processed
     * @param sourceId
     *            The source id of the reporting code
     * @param probeId
     *            The probe of the reporting code
     */
    @Override
    public void processException(Throwable th, String sourceId, String probeId) {
        log(sourceId, probeId, th, null, null);
    }

    /**
     * Process an exception
     * 
     * @param th
     *            The exception to be processed
     * @param sourceId
     *            The source id of the reporting code
     * @param probeId
     *            The probe of the reporting code
     * @param callerThis
     *            The instance of the reporting code
     */

    @Override
    public void processException(Throwable th, String sourceId, String probeId, Object callerThis) {
        log(sourceId, probeId, th, callerThis, null);
    }

    /**
     * Process an exception
     * 
     * @param th
     *            The exception to be processed
     * @param sourceId
     *            The source id of the reporting code
     * @param probeId
     *            The probe of the reporting code
     * @param objectArray
     *            An array of additional interesting objects
     */
    @Override
    public void processException(Throwable th, String sourceId, String probeId, Object[] objectArray) {
        log(sourceId, probeId, th, null, objectArray);
    }

    /**
     * Process an exception
     * 
     * @param th
     *            The exception to be processed
     * @param sourceId
     *            The source id of the reporting code
     * @param probeId
     *            The probe of the reporting code
     * @param callerThis
     *            The instance of the reporting code
     * @param objectArray
     *            An array of additional interesting objects
     */
    @Override
    public void processException(Throwable th, String sourceId, String probeId, Object callerThis, Object[] objectArray) {
        log(sourceId, probeId, th, callerThis, objectArray);
    }

    /**
     * Log a problem to the global incident stream (creating it if necessary
     * 
     * @param txt
     *            A description of the incident (the name of the exception)
     * @param sourceId
     *            The source id of the reporting code
     * @param probeId
     *            The probe id of the reporting code
     * @param th
     *            The exception
     * @param callerThis
     *            The instance of the reporting code (null if no specific
     *            instance)
     * @param objectArray
     *            Additional interesting object (null if there aren't any)
     */
    @FFDCIgnore(PrivilegedActionException.class)
    private void log(String sourceId, String probeId, Throwable th, Object callerThis, Object[] objectArray) {
        IncidentImpl incident = getIncident(sourceId, probeId, th, callerThis, objectArray);
        incident.log(th, callerThis, objectArray);
        if (System.getSecurityManager() == null) {
            logSummary(ffdcSummaryPolicy == FFDCSummaryPolicy.IMMEDIATE);
        } else {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    logSummary(ffdcSummaryPolicy == FFDCSummaryPolicy.IMMEDIATE);
                    return null;
                }
            });
        }

        for (IncidentForwarder forwarder : FFDC.getIncidentForwarders()) {
            forwarder.process(incident, th);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "FFDC exception: " + th);
    }

    protected IncidentImpl getIncident(String sourceId, String probeId, Throwable th, Object callerThis, Object[] objectArray) {
        IncidentImpl.Key key = new IncidentImpl.Key(sourceId, probeId, th, callerThis, objectArray);
        IncidentImpl incident = null;
        synchronized (incidents) {
            incident = incidents.get(key);
            if (incident == null) {
                incident = new IncidentImpl(ffdcLogSet, key);
                incidents.put(key, incident);
            }
        }
        return incident;
    }

    /**
     * Update the log summary-- triggered by the logging of an exception/incident
     */
    private void logSummary(boolean force) {
        long currentTime = System.nanoTime();

        //PM39875 - summary table updates should not be dependent on a new unique incident occurring
        if (force || dumpAlgorithm(currentTime)) {

            List<IncidentImpl> incidentCopies;
            synchronized (incidents) {
                incidentCopies = new ArrayList<IncidentImpl>(incidents.values());
            }

            logSummary(incidentCopies);
        }
    }

    /**
     * Update the log summary. Triggered by logging of an exception/incident
     * via {@link #logSummary(boolean)}, or by daily log rolling {@link #rollLogs()}
     * 
     * @param incidents Thread-safe list that should be used to write summary contents
     */
    private void logSummary(final List<IncidentImpl> incidents) {
        if (incidents.isEmpty())
            return;

        // summaryFile can be replaced so hold on to a local copy
        File outFile = summaryFile;

        if (outFile == null) {
            outFile = summaryFile = createNewSummaryFile();
        }

        // outFile can still be null, the create above can still fail
        if (outFile != null) {
            synchronized (outFile) {
                OutputStream os = null;
                try {
                    os = createSummaryStream(outFile);
                    new IncidentSummaryLogger().logIncidentSummary(os, incidents);
                } catch (FileNotFoundException e) {
                    // This is FFDC not being able to find itself... we're in bad
                    // shape if this doesn't work.. 
                    e.printStackTrace();
                } catch (IOException e) {
                } finally {
                    LoggingFileUtils.tryToClose(os);
                }
            }
        }
    }

    /*
     * PM39875 - use dumpAlgorithm from legacy FFDC (See com.ibm.ws.ffdc.FFDCFilter in WAS61 release) to determine
     * when a summary table should be dumped.
     */
    synchronized private boolean dumpAlgorithm(long currentTime) {
        boolean dumpTable = false;

        numberOfEntiesProcessed++; // Increment the number of entries updated in the table

        if ((lastTimeOfDump == Long.MIN_VALUE) // Note we check this since if currentTime is zero or more, the line below will be false due to arthimetic overflow
            || (currentTime - lastTimeOfDump > highWaterTime)) {
            // Dump the content of the hash regardless of the
            // number of entries which have been seen.
            dumpTable = true;
        } else {
            if ((numberOfEntiesProcessed > normalDumpThreshold) &&
                (currentTime - lastTimeOfDump > lowWaterTime)) {
                dumpTable = true;
            }
        }

        if (dumpTable == true) {
            lastTimeOfDump = currentTime;
            numberOfEntiesProcessed = 0;
        }
        return dumpTable;
    }

    /**
     * {@inheritDoc}
     * 
     * This method is called in response to a scheduled trigger.
     * A new summary log file will be created for the next summary period.
     * 
     * @see FFDCJanitor
     */
    @Override
    public void rollLogs() {
        summaryFile = null;

        List<IncidentImpl> incidentCopies;
        synchronized (incidents) {
            incidentCopies = new ArrayList<IncidentImpl>(incidents.values());
        }

        int overage = incidentCopies.size() - 500;
        if (overage > 0) {
            // we have more than 500 incidents: we need to remove least-recently-seen
            // incidents until we're back to 500. We do this daily to prevent unchecked growth
            // in the number of incidents we remember
            List<IncidentImpl> lastSeenIncidents = new ArrayList<IncidentImpl>(incidentCopies);

            // sort the incidents by when they were last seen, rather than when they were added
            Collections.sort(lastSeenIncidents, new Comparator<IncidentImpl>() {
                @Override
                public int compare(IncidentImpl o1, IncidentImpl o2) {
                    // static method on double does the same as Long.compareTo, and we avoid
                    // object allocation or auto-boxing, etc.
                    return Double.compare(o1.getTimeStamp(), o2.getTimeStamp());
                }
            });

            // For each item we're over 500, remove one from the front of the list (least recently seen)
            synchronized (incidents) {
                for (Iterator<IncidentImpl> i = lastSeenIncidents.iterator(); i.hasNext() && overage > 0; overage--) {
                    IncidentImpl impl = i.next();
                    i.remove();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "FFDC cleanup -- removing " + impl.key);
                    }
                    // remove the incident from the map, and clean it up (remove the associated files)
                    incidents.remove(impl.key);
                    impl.cleanup();
                }
            }
        }

        for (IncidentImpl incident : incidentCopies) {
            incident.roll();
        }

        logSummary(incidentCopies);
    }

    synchronized File createNewSummaryFile() {
        File newFile;
        // Creation and rolling of files should be done as the server
        Object token = ThreadIdentityManager.runAsServer();
        try {
            newFile = LoggingFileUtils.createNewFile(summaryLogSet);
        } finally {
            ThreadIdentityManager.reset(token);
        }

        return newFile;
    }

    /**
     * This wrapper method for creating streams should be called while
     * synchronized on the targetFile..
     * 
     * @param targetFile
     * @return
     * @throws IOException
     */
    private OutputStream createSummaryStream(File targetFile) throws IOException {
        if (targetFile == null)
            return null;

        TextFileOutputStreamFactory factory = FFDCConfigurator.getFileOutputStreamFactory();
        OutputStream newStream = null;

        // Creation and rolling of files should be done as the server
        Object token = ThreadIdentityManager.runAsServer();
        try {
            newStream = factory.createOutputStream(targetFile);
        } finally {
            ThreadIdentityManager.reset(token);
        }

        return newStream;
    }
}
