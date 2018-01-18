/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.ffdc.source;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDC;
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.data.KeyValuePair;
import com.ibm.ws.logging.data.Pair;
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

    private final String sourceName = "com.ibm.ws.logging.ffdc.source.ffdcsource";
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

            KeyValuePair timeStamp = new KeyValuePair("ibm_datetime", Long.toString(in.getTimeStamp()), KeyValuePair.ValueTypes.NUMBER);
            KeyValuePair dateOfFirstOccurrence = new KeyValuePair("dateOfFirstOccurence", Long.toString(in.getDateOfFirstOccurrence().getTime()), KeyValuePair.ValueTypes.NUMBER);
            int countVal = in.getCount();
            KeyValuePair count = new KeyValuePair("count", Integer.toString(countVal), KeyValuePair.ValueTypes.NUMBER);
            //Condition to prevent adding ffdc event for the same failure
            //to the bufferMgr multiple times
            //TODO: Need to evaluate the need for the timeStamp (timeStamp == dateOfFirstOccurrence) check is required or not
            if (countVal == 1) {
                KeyValuePair className = new KeyValuePair("ibm_className", in.getSourceId(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair label = new KeyValuePair("label", in.getLabel(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair exceptionName = new KeyValuePair("ibm_exceptionName", in.getExceptionName(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair probeID = new KeyValuePair("ibm_probeID", in.getProbeId(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair sourceID = new KeyValuePair("sourceID", in.getSourceId(), KeyValuePair.ValueTypes.STRING);
                KeyValuePair threadID = new KeyValuePair("ibm_threadId", Integer.toString((int) in.getThreadId()), KeyValuePair.ValueTypes.NUMBER);
                KeyValuePair stackTrace = new KeyValuePair("ibm_stackTrace", getStackTraceAsString(th), KeyValuePair.ValueTypes.STRING);
                KeyValuePair callerDetails = new KeyValuePair("ibm_objectDetails", getCallerDetails(in), KeyValuePair.ValueTypes.STRING);
                KeyValuePair message = new KeyValuePair("message", th.getMessage(), KeyValuePair.ValueTypes.STRING);

                String sequenceVal = timeStamp + "_" + String.format("%013X", seq.incrementAndGet());
                KeyValuePair sequence = new KeyValuePair("ibm_sequence", sequenceVal, KeyValuePair.ValueTypes.STRING);

                GenericData genData = new GenericData();
                ArrayList<Pair> pairs = genData.getPairs();

                pairs.add(timeStamp);
                pairs.add(dateOfFirstOccurrence);
                pairs.add(count);
                pairs.add(className);
                pairs.add(label);
                pairs.add(exceptionName);
                pairs.add(probeID);
                pairs.add(sourceID);
                pairs.add(threadID);
                pairs.add(stackTrace);
                pairs.add(callerDetails);
                pairs.add(message);
                pairs.add(sequence);

                genData.setSourceType(sourceName);

                bufferMgr.add(genData);
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
