/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ras;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.logging.internal.impl.BaseTraceService;
import com.ibm.ws.logging.internal.impl.LogProviderConfigImpl;
import com.ibm.ws.logging.internal.impl.RoutedMessageImpl;

/**
 *
 */
public class CapturedOutputHolder extends BaseTraceService {

    public static final String nl = System.getProperty("line.separator");

    /**
     * A marker for whether we're running command-line or in Eclipse. We want to
     * handle whether we suppress streams differently in those two cases.
     * Eclipse seems to put some of its OSGi configuration data onto
     * the classpath, so take that as the test.
     */
    private static final boolean IS_RUNNING_IN_ECLIPSE = System.getProperty("java.class.path").contains("eclipse/configuration/org.eclipse.osgi/bundles");

    static final PrintStream out = System.out;
    static final PrintStream err = System.err;

    private boolean captureEnabled = false;

    private ByteArrayOutputStream capturedSystemErr = null;
    private ByteArrayOutputStream capturedSystemOut = null;
    private final DelegateTrWriter messageDelegate = new DelegateTrWriter();
    private final DelegateTrWriter traceDelegate = new DelegateTrWriter();

    public CapturedOutputHolder() {
        super();
        /*
         * CaputuredOutputHolder is created frequently for Junits.
         * The Parent class BaseTraceSerivce creates a Timer object
         * that will remove the earlyMessageQueue and earlyTraceQueue.
         *
         * Due to the frequency of Junit executions that create new
         * CapturedOutputHolders, this can lead to a scenario with multiple
         * Timer objects which can cause an OOM during builds.
         */
        earlyMessageTraceKiller_Timer.cancel();
        earlyMessageTraceKiller_Timer = null;
    }

    /**
     * This is not to be used by the SharedOutputManager! This is an override
     * of the BaseTraceService method that captures system streams.
     *
     * We aren't changing behavior here: just checking what's what.
     */
    @Override
    protected void captureSystemStreams() {
        PrintStream sysOut = System.out;
        PrintStream sysErr = System.err;

        PrintStream trSysOut = systemOut.getOriginalStream();
        PrintStream trSysErr = systemErr.getOriginalStream();

        if (sysOut != trSysOut) {
            throw new ConcurrentModificationException("Someone else has reset or cached System.out. Current System.out value: " + System.out + " and current original stream: "
                                                      + systemOut.getOriginalStream());
        }
        if (sysErr != trSysErr) {
            throw new ConcurrentModificationException("Someone else has reset or cached System.err");
        }

        super.captureSystemStreams();
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized void initializeWriters(LogProviderConfigImpl config) {
        super.initializeWriters(config);

        // wrap the writers with something we can use for delegation/capture
        messageDelegate.init(messagesLog);
        messagesLog = messageDelegate;

        traceDelegate.init(traceLog);

        String fileName = config.getTraceFileName();
        if (fileName.equals("stdout")) {
            traceLog = systemOut;
        } else {
            traceLog = traceDelegate;
        }
    }

    //Overwritten for old BaseTraceService behaviour for echo
    @Override
    public void echo(SystemLogHolder holder, LogRecord logRecord) {
        TraceWriter detailLog = traceLog;

        // Tee to messages.log (always)
        String message = formatter.messageLogFormat(logRecord, logRecord.getMessage());
        messagesLog.writeRecord(message);

        invokeMessageRouters(new RoutedMessageImpl(logRecord.getMessage(), logRecord.getMessage(), message, logRecord));

        if (detailLog == systemOut) {
            // preserve System.out vs. System.err
            publishTraceLogRecord(holder, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
        } else {
            if (copySystemStreams) {
                // Tee to console.log if we are copying System.out and System.err to system streams.
                writeFilteredStreamOutput(holder, logRecord);
            }

            if (TraceComponent.isAnyTracingEnabled()) {
                publishTraceLogRecord(detailLog, logRecord, NULL_ID, NULL_FORMATTED_MSG, NULL_FORMATTED_MSG);
            }
        }
    }

    //Overwritten for old BaseTraceService behaviour for publishTraceLogRecord
    @Override
    protected void publishTraceLogRecord(TraceWriter detailLog, LogRecord logRecord, Object id, String formattedMsg, String formattedVerboseMsg) {
        if (formattedVerboseMsg == null) {
            formattedVerboseMsg = formatter.formatVerboseMessage(logRecord, formattedMsg, false);
        }
        Level level = logRecord.getLevel();
        int levelVal = level.intValue();

        String traceDetail = formatter.traceLogFormat(logRecord, id, formattedMsg, formattedVerboseMsg);
        invokeTraceRouters(new RoutedMessageImpl(formattedMsg, formattedVerboseMsg, traceDetail, logRecord));
        if (traceDetail.contains("x.com.ibm")) {
            return;
        }
        if (detailLog == systemOut || detailLog == systemErr) {
            if (levelVal == WsLevel.ERROR.intValue() || levelVal == WsLevel.SEVERE.intValue()) {
                writeStreamOutput(systemErr, traceDetail, false);
            } else {
                writeStreamOutput((SystemLogHolder) detailLog, traceDetail, false);
            }
        } else {
            detailLog.writeRecord(traceDetail);
        }
    }

    /**
     * This overrides the BaseTraceService method that writes output to the
     * system streams. If the output is for a captured stream, we shunt the output
     * over to that stream instead.
     * <p>
     * We're relying on the BaseTraceService to manage set/restore of system streams.
     * The SharedOutputManager makes sure that Tr as a whole is started/stopped...
     * (in the event restoreStreams() is missed.
     */
    @Override
    protected synchronized void writeStreamOutput(SystemLogHolder holder, String txt, boolean rawStream) {
        if (holder == systemErr && capturedSystemErr != null) {
            // clear the hushed stream if there was an error using it
            capturedSystemErr = writeCapturedData(holder, txt, capturedSystemErr);
        } else if (holder == systemOut && capturedSystemOut != null) {
            // clear the hushed stream if there was an error using it
            capturedSystemOut = writeCapturedData(holder, txt, capturedSystemOut);
        } else {
            super.writeStreamOutput(holder, txt, rawStream);
        }
    }

    /**
     * Write the text from the string (and the text for a newline) into the captured
     * byte array. If all is well, return the hushed stream for the next write.
     * If there is an exception writing to the stream, return null to disable subsequent
     * writes.
     *
     * @param holder         LogHolder : system err or system out
     * @param txt            Text to be logged
     * @param capturedStream stream holding captured data
     * @return capturedStream if all is well, null if an exception occurred writing to the stream
     */
    private synchronized ByteArrayOutputStream writeCapturedData(SystemLogHolder holder, String txt, ByteArrayOutputStream capturedStream) {
        try {
            capturedStream.write(txt.getBytes());

            // If we're running in an Eclipse GUI, tee the output for usability
            if (IS_RUNNING_IN_ECLIPSE) {
                if (holder == systemErr && err != null) {
                    err.println(txt);
                } else if (out != null) {
                    out.println(txt);
                }
            }
            capturedStream.write(nl.getBytes());
            return capturedStream;
        } catch (IOException ioe) {
            super.writeStreamOutput(systemErr, "Unable to write/contain captured trace data", true);
            super.writeStreamOutput(holder, txt, false);
            return null;
        }
    }

    /**
     *
     */
    public synchronized void captureStreams() {
        if (!captureEnabled) {
            captureEnabled = true;
            resetStreams();
        }
    }

    /**
     *
     */
    public synchronized void resetStreams() {
        if (captureEnabled) {
            capturedSystemErr = new ByteArrayOutputStream(1024);
            capturedSystemOut = new ByteArrayOutputStream(1024);
            traceDelegate.createStream();
            messageDelegate.createStream();
        }

    }

    /**
     *
     */
    @Override
    public synchronized void stop() {
        if (captureEnabled) {
            captureEnabled = false;
            capturedSystemErr = null;
            capturedSystemOut = null;
            traceDelegate.deleteStream();
            messageDelegate.deleteStream();
        }
        super.stop();
    }

    /**
     *
     */
    public synchronized void copySystemStreams() {
        if (captureEnabled) {
            systemOut.getOriginalStream().println(capturedSystemOut.toString());
            systemErr.getOriginalStream().println(capturedSystemErr.toString());
        }
    }

    /**
     *
     */
    public void copyTraceStream() {
        if (captureEnabled) {
            systemOut.getOriginalStream().println("-----------------------------------------------------------");
            systemOut.getOriginalStream().println("SharedOutputManager captured the following trace output: ");
            systemOut.getOriginalStream().println(traceDelegate.toString());
            systemOut.getOriginalStream().println("-----------------------------------------------------------");
        }
    }

    /**
     *
     */
    public void copyMessageStream() {
        if (captureEnabled) {
            systemOut.getOriginalStream().println("-----------------------------------------------------------");
            systemOut.getOriginalStream().println("SharedOutputManager captured the following message output: ");
            systemOut.getOriginalStream().println(messageDelegate.toString());
            systemOut.getOriginalStream().println("-----------------------------------------------------------");
        }
    }

    /** Return captured output as a string (e.g. for contains() checking ) */
    public synchronized String getCapturedOut() {
        if (captureEnabled) {
            return capturedSystemOut.toString();
        }
        return "";
    }

    /** Return captured error outout as a string (e.g. for contains() checking ) */
    public synchronized String getCapturedErr() {
        if (captureEnabled) {
            return capturedSystemErr.toString();
        }
        return "";
    }

    /**
     * @return
     */
    public synchronized String getCapturedTrace() {
        return traceDelegate.toString();
    }

    /**
     * @return
     */
    public synchronized String getCapturedMessages() {
        return messageDelegate.toString();
    }

    /**
     *
     */
    public void dumpStreams() {
        if (captureEnabled) {
            systemOut.getOriginalStream().println("SharedOutputManager captured the following: ");
            printStreamOutput("System.out", capturedSystemOut.toString());
            printStreamOutput("System.err", capturedSystemErr.toString());
            printStreamOutput("Messages", messageDelegate.toString());
            printStreamOutput("Trace", traceDelegate.toString());
        }
    }

    private void printStreamOutput(String name, String s) {
        systemOut.getOriginalStream().println("-- " + name + " -------------------------------------------------");
        if (!s.isEmpty()) {
            systemOut.getOriginalStream().println(s);
        }
    }

    class DelegateTrWriter implements TraceWriter {

        TraceWriter originalWriter;
        ByteArrayOutputStream capturedStream;

        public void init(TraceWriter writer) {
            originalWriter = writer;
        }

        public synchronized void createStream() {
            capturedStream = new ByteArrayOutputStream(1024);
        }

        public synchronized void deleteStream() {
            capturedStream = null;
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void writeRecord(String record) {
            originalWriter.writeRecord(record);
            if (capturedStream != null) {
                capturedStream = writeCapturedData(record, capturedStream);
            }
        }

        private synchronized ByteArrayOutputStream writeCapturedData(String txt, ByteArrayOutputStream capturedStream) {
            if (txt == null || txt.isEmpty())
                return capturedStream;

            try {
                capturedStream.write(txt.getBytes());
                capturedStream.write(nl.getBytes());
                return capturedStream;
            } catch (IOException ioe) {
                originalWriter.writeRecord("Unable to write/contain captured data");
                originalWriter.writeRecord(txt);
                return null;
            }
        }

        /** {@inheritDoc} */
        @Override
        public synchronized void close() throws IOException {
            if (originalWriter != null) {
                originalWriter.close();
            }
        }

        @Override
        public String toString() {
            if (capturedStream != null) {
                return capturedStream.toString();
            }
            return "";
        }
    }
}
