/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2016
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.logging.source;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.logging.Incident;
import com.ibm.wsspi.logging.IncidentForwarder;

/**
 *
 */
public class FFDCSource implements Source {

    private static final TraceComponent tc = Tr.register(FFDCSource.class);

    public final static int CALLER_DETAILS_CHARS_MAX_SIZE = 2048;
    public final static String NEWLINE_CHARS = System.getProperty("line.separator");

    private final String sourceName = "com.ibm.ws.logging.source.ffdcsource";
    private final String location = "memory";

    private BufferManager bufferMgr = null;
    private IncidentHandler incidentHandler;

    protected synchronized void activate(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceName() {
        return sourceName;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocation() {
        return location;
    }

    /** {@inheritDoc} */
    @Override
    public void setBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting buffer manager " + this);
        }
        this.bufferMgr = bufferMgr;
        startSource();
    }

    /** {@inheritDoc} */
    @Override
    public void unsetBufferManager(BufferManager bufferMgr) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Un-setting buffer manager " + this);
        }
        //Indication that the buffer will no longer be available
        stopSource();
        this.bufferMgr = null;
    }

    /**
     *
     */
    private void startSource() {
        incidentHandler = new IncidentHandler();
        FFDC.registerIncidentForwarder(incidentHandler);
    }

    /**
     *
     */
    private void stopSource() {
        FFDC.deregisterIncidentForwarder(incidentHandler);
        incidentHandler = null;
    }

    private class IncidentHandler implements IncidentForwarder {

        private final AtomicLong seq = new AtomicLong();

        /** {@inheritDoc} */
        @Override
        public void process(Incident in, Throwable th) {
            long timeStamp = in.getTimeStamp();
            long dateOfFirstOccurrence = in.getDateOfFirstOccurrence().getTime();
            int count = in.getCount();
            //Condition to prevent adding ffdc event for the same failure
            //to the bufferMgr multiple times
            //TODO: Need to evaluate the need for the timeStamp (timeStamp == dateOfFirstOccurrence) check is required or not
            if (count == 1) {
                String className = in.getSourceId();
                String label = in.getLabel();
                String exceptionName = in.getExceptionName();
                String probeID = in.getProbeId();
                String sourceID = in.getSourceId();
                int threadID = (int) in.getThreadId();
                String stackTrace = getStackTraceAsString(th);
                String callerDetails = getCallerDetails(in);
                String message = th.getMessage();
                String sequence = timeStamp + "_" + String.format("%013X", seq.incrementAndGet());

                FFDCData data = new FFDCData(exceptionName, message, className, timeStamp, dateOfFirstOccurrence, count, label, probeID, sourceID, threadID, stackTrace, callerDetails, sequence);
                bufferMgr.add(data);
            }
        }

        private String getCallerDetails(Incident in) {
            String details = null;;

            String dump = in.getIntrospectedCallerDump();
            if (dump != null) {
                int firstLineEnd = dump.indexOf(NEWLINE_CHARS);
                int start = firstLineEnd + NEWLINE_CHARS.length(); // skip 1st line, ie 'Dump of callerThis'

                int len = dump.length() - start;
                if (len > CALLER_DETAILS_CHARS_MAX_SIZE) {
                    // remove 3 chars, and add ...(3 dots), when more chars than max size
                    int end = (start + (CALLER_DETAILS_CHARS_MAX_SIZE - 3));
                    details = dump.substring(start, end) + "...";
                } else {
                    details = dump.substring(start);
                }
            }
            return details;
        }

        private String getStackTraceAsString(Throwable th) {
            StringWriter strBuf = new StringWriter();
            PrintWriter writer = new PrintWriter(strBuf);
            th.printStackTrace(writer);
            writer.close();
            strBuf.flush();
            return strBuf.toString();
        }

    }

}
