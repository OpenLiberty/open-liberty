/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

public class ESIProcessor {
    private static final List _running = Collections.synchronizedList(new LinkedList<ESIProcessor>());
    private static final TraceComponent _tc = Tr.register(ESIProcessor.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private String _hostName;
    private int _pid;
    private ESIInputStream _in;
    private ESIOutputStream _out;

    /* added for debug */
    private int _localPort;
    private int _serverPort;
    private int _remotePort;
    private String _localAddr;
    private String _localName;

    private boolean _isAlive;
    private ESIProcessorRequest _curRequest = null;
    private static ESIProcessor protoprocessor = null;

    private ESIProcessorKeepAliveDaemon _keepAliveDaemon = null;
    private static final int KEEP_ALIVE_INTERVAL_IN_MS = 45000; // send initPID every 45 seconds to the plugin

    /**
     * Run until the ESI processor dies. This method is currently called by the ESIInvalidatorServlet.
     * 
     * @param hostName
     *            The name of the machine on which the ESI processor is running.
     * @param in
     *            The input stream from the ESI processor.
     * @param out
     *            The output stream from the ESI processor.
     */
    public static void run(String hostName, InputStream in, OutputStream out) {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "run", hostName);
        ESIProcessor processor = null;
        try {
            processor = new ESIProcessor(hostName, in, out);
            runCommon(processor);
        } catch (IOException ioe) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "An exception was thrown constructing the processor or running runCommon.", ioe);
            }
            // must have gone down
        } catch (Throwable th) {
            FFDCFilter.processException(th, ESIProcessor.class.getName() + ".run()", "69");
            // must have gone down
        } finally {
            if (processor != null)
                _running.remove(processor);
        }
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "run", processor);
    }

    /**
     * Run until the ESI processor dies. This method is currently called by the ESIInvalidatorServlet.
     * 
     * @param hostName
     *            The name of the machine on which the ESI processor is running.
     * @param in
     *            The input stream from the ESI processor.
     * @param out
     *            The output stream from the ESI processor.
     */
    public static void run(HttpServletRequest request, HttpServletResponse response) {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "run", request.getRemoteHost());
        ESIProcessor processor = null;
        try {
            processor = new ESIProcessor(request, response);
            runCommon(processor);
        } catch (IOException ioe) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "An exception was thrown constructing the processor or running runCommon.", ioe);
            }
            // must have gone down
        } catch (Throwable th) {
            FFDCFilter.processException(th, ESIProcessor.class.getName() + ".run()", "104");
            // must have gone down
        } finally {
            if (processor != null)
                _running.remove(processor);
        }
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "run", processor);
    }

    private static void runCommon(ESIProcessor processor) throws IOException {
        _running.add(processor);
        processor.listen();
    }

    /**
     * Return all of the currently running ESI processors.
     * 
     * @return The list of currently running ESI processors.
     */
    public static ESIProcessor[] getRunning() {
        return (ESIProcessor[]) _running.toArray(new ESIProcessor[0]);
    }

    public static synchronized int collectEdgeStats(int _gatherWhat) throws IOException {

        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "collectEdgeStats " + _gatherWhat);

        String edge = null;
        int count = 0;

        if (protoprocessor == null)
            throw new IOException("collectEdgeStats() : Edge inactive.");

        protoprocessor.writeInt(ESIProcessorRequest.MSG_GATHER);
        protoprocessor.writeInt(_gatherWhat);
        protoprocessor.flushWithResponse();

        edge = protoprocessor._in.readUTF(); // get count of response blocks
        count = new Integer(edge).intValue(); // ... as an int

        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "collectEdgeStats() : plugins responded : " + count);

        return count;
    }

    /**
     * Invalidate 'ids' in all of the currently running ESI processors.
     */
    public static void invalidateIds(Iterator ids) {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "invalidateIds");
        ESIProcessor[] processors = getRunning();
        while (ids.hasNext()) {
            Object id = (Object) ids.next();
            if (id instanceof String) { // PK43094
                for (int idx = 0; idx < processors.length; idx++) {
                    try {
                        processors[idx].invalidateId((String) id);
                    } catch (Exception e) {
                        Tr.warning(_tc, "failure invalidating " + id + " in " + processors[idx] + ": " + e.getMessage());
                    }
                }
            }
        }
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "invalidateIds");
    }

    /**
     * Clear the caches in all of the currently running ESI processors.
     */
    public static void clearCaches() {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "clearCaches");
        ESIProcessor[] processors = getRunning();
        for (int idx = 0; idx < processors.length; idx++) {
            try {
                processors[idx].clearCache();
            } catch (Exception e) {
                Tr.warning(_tc, "failure clearing cache in " + processors[idx] + ": " + e.getMessage());
            }
        }
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "clearCaches");
    }

    /**
     * Reset counters in all of the currently running ESI processors.
     */
    public static void resetCounters() {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "resetCounters");
        ESIProcessor[] processors = getRunning();
        for (int idx = 0; idx < processors.length; idx++) {
            try {
                processors[idx].resetCounter();
            } catch (Exception e) {
                Tr.warning(_tc, "failure resetting counters in " + processors[idx] + ": " + e.getMessage());
            }
        }
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "resetCounters");
    }

    public static synchronized ESIProcessorStats[] gather(int gatherWhat) throws IOException {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "gather");
        ESIProcessorStats[] stats;

        ESIProcessor[] processors = getRunning();
        stats = new ESIProcessorStats[processors.length];
        // Send a request to all processors
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "submitting requests to " + stats.length + " ESI processors");
        for (int idx = 0; idx < stats.length; idx++) {
            stats[idx] = new ESIProcessorStats(processors[idx], gatherWhat);
            processors[idx].submit(stats[idx]);
        }
        // Await completion of all requests
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "waiting for responses");
        int successCount = 0;
        for (int idx = 0; idx < stats.length; idx++) {
            try {
                if (processors[idx].isAlive()) {
                    if (_tc.isDebugEnabled())
                        Tr.debug(_tc, "waiting for response from " + processors[idx].toString());
                    stats[idx].awaitCompletion();
                    successCount++;
                } else {
                    if (_tc.isDebugEnabled())
                        Tr.debug(_tc, "processor is dead; not waiting for response from " + processors[idx].toString());
                    stats[idx] = null;
                }
            } catch (Throwable th) {
                stats[idx] = null;
                // th.printStackTrace(System.out);
            }
        }
        if (stats.length > successCount) {
            ESIProcessorStats[] successStats = new ESIProcessorStats[successCount];
            successCount = 0;
            for (int idx = 0; idx < stats.length; idx++) {
                if (stats[idx] != null) {
                    successStats[successCount++] = stats[idx];
                }
            }
            stats = successStats;
        }
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "gather");
        return stats;
    }

    /**
     * Constructor for ESI (Edge Side Include) processor "on the edge", e.g. those ESI processors which are running in
     * the webserver plugin.
     * 
     * @param hostName
     *            The name of the machine on which the ESI processor is running.
     * @param in
     *            The input stream from the ESI processor.
     * @param out
     *            The output stream from the ESI processor. (@param in - on z ignored, non-z is servlet InputStream.) @PQ91098A
     *            (@param out - on z ignored, non-z is servlet OutputStream.) @PQ91098A
     */
    protected ESIProcessor(HttpServletRequest request, HttpServletResponse response) throws IOException {
        _serverPort = request.getServerPort();
        _remotePort = request.getRemotePort();
        _localPort = request.getLocalPort();
        _localAddr = request.getLocalAddr();
        _localName = request.getLocalName();

        initalizeProcessor(request.getRemoteHost(), new ESIInputStream(request.getInputStream()), new ESIOutputStream(response.getOutputStream()));
    }

    protected ESIProcessor(String hostName, InputStream in, OutputStream out) throws IOException {
        initalizeProcessor(hostName, in, out);
    }

    private void initalizeProcessor(String hostName, InputStream in, OutputStream out) throws IOException {

        _hostName = hostName;

        _in = new ESIInputStream(in);
        _out = new ESIOutputStream(out);
        _isAlive = true;
        _keepAliveDaemon = new ESIProcessorKeepAliveDaemon(KEEP_ALIVE_INTERVAL_IN_MS);
        _keepAliveDaemon.setESIProcessor(this);
        _keepAliveDaemon.start();

        initPID();

        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "constructed " + this);
    }

    public boolean isAlive() {
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "is alive: " + this);
        return _isAlive;
    }

    public synchronized void markDead() {
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "mark dead: " + this);
        _isAlive = false;
        if (_keepAliveDaemon != null)
            _keepAliveDaemon.stop();
        notify();
    }

    /**
     * Return the host name of the ESI server.
     * 
     * @return The host name on which the ESI server is running.
     */
    public String getHostName() {
        return _hostName;
    }

    /**
     * Return the PID of the ESI processor.
     * 
     * @return The PID of the ESI processor.
     */
    public int getPID() {
        return _pid;
    }

    public synchronized void invalidateId(String id) throws IOException {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "invalidateId '" + id + "' in " + this);
        writeInt(ESIProcessorRequest.MSG_INVALIDATE_ID);
        writeString(id);
        flush();
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "invalidateId '" + id + "' in " + this);
    }

    public synchronized void clearCache() throws IOException {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "clearCache in " + this);
        writeInt(ESIProcessorRequest.MSG_CLEAR_CACHE);
        flush();
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "clearCache in " + this);
    }

    public synchronized void resetCounter() throws IOException {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "resetCounter in " + this);
        writeInt(ESIProcessorRequest.MSG_RESET_COUNTERS);
        flush();
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "resetCounter in " + this);
    }

    public synchronized void submit(ESIProcessorRequest request) throws IOException {
        _curRequest = request;
        notify();

        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "submit gather in " + this);
    }

    /**
     * Listen and handle new requests.
     * 
     * @return none
     */
    private synchronized void listen() throws IOException {
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "begin listening to " + this);
        while (_isAlive) {
            if (_curRequest != null) {
                try {
                    _curRequest.handle();
                    _curRequest.markCompleted(null);
                } catch (Throwable throwable) {
                    _curRequest.markCompleted(throwable);
                }
                _curRequest = null;
            }
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "end listening to " + this);
    }

    /**
     * Initialize the PID. Sends a request to the ESI processor and awaits the reply.
     * 
     * @return none
     */
    synchronized void initPID() throws IOException {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "initPID from " + this);
        writeInt(ESIProcessorRequest.MSG_GET_PID);
        flush();
        _pid = readInt();
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "initPID from " + this);
    }

    public int available() throws IOException {
        return _in.available();
    }

    public int readInt() throws IOException {
        try {

            int val = _in.readInt();
            if (_tc.isDebugEnabled())
                Tr.debug(_tc, "readInt " + val);
            return val;
        } catch (IOException ioe) {
            markDead();
            throw ioe;
        }
    }

    public String readString() throws IOException {
        try {
            String val = _in.readUTF();
            if (_tc.isDebugEnabled())
                Tr.debug(_tc, "readString " + val);
            return val;
        } catch (IOException ioe) {
            markDead();
            throw ioe;
        }
    }

    public void writeInt(int val) throws IOException {
        try {
            _out.writeInt(val);
            if (_tc.isDebugEnabled())
                Tr.debug(_tc, "writeInt " + val);
        } catch (IOException ioe) {
            markDead();
            throw ioe;
        }
    }

    public void writeString(String str) throws IOException {
        try {
            _out.writeUTF(str);
            if (_tc.isDebugEnabled())
                Tr.debug(_tc, "writeString " + str);
        } catch (IOException ioe) {
            markDead();
            throw ioe;
        }
    }

    public void flush() throws IOException {
        try {
            _out.flush();
        } catch (IOException ioe) {
            markDead();
            throw ioe;
        }
    }

    public void flushWithResponse() throws IOException {
        try {
            _in = _out.flushWithResponse();
        } catch (IOException ioe) {
            markDead();
            throw ioe;
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("ESIProcessor[");
        buffer.append("_hostName = ").append(_hostName);
        buffer.append(" _isAlive = ").append(_isAlive);
        buffer.append(" _localAddr = ").append(_localAddr);
        buffer.append(" _localName = ").append(_localName);
        buffer.append(" _localPort = ").append(_localPort);
        buffer.append(" _pid = ").append(_pid);
        buffer.append(" _remotePort = ").append(_remotePort);
        buffer.append(" _serverPort = ").append(_serverPort);
        buffer.append("]");
        return buffer.toString();
    }
}

abstract class ESIProcessorRequest {

    // Msg types
    protected static final int MSG_GET_PID = 1;
    protected static final int MSG_GATHER = 2;
    protected static final int MSG_RESET_COUNTERS = 3;
    protected static final int MSG_CLEAR_CACHE = 4;
    protected static final int MSG_INVALIDATE_ID = 5;
    protected static final int MSG_END = 6;

    // Gather message modifiers
    protected static final int CACHE_HITS = 7;
    protected static final int CACHE_MISSES_BY_URL = 8;
    protected static final int CACHE_MISSES_BY_ID = 9;
    protected static final int CACHE_EXPIRES = 10;
    protected static final int CACHE_PURGES = 11;
    protected static final int CACHE_ENTRY = 12;

    protected static final int CACHE_COUNTS = (1 << CACHE_HITS) | (1 << CACHE_MISSES_BY_URL) | (1 << CACHE_MISSES_BY_ID) | (1 << CACHE_EXPIRES)
            | (1 << CACHE_PURGES);
    protected static final int GATHER_ALL = CACHE_COUNTS | (1 << CACHE_ENTRY);

    private static final TraceComponent _tc = Tr.register(ESIProcessorRequest.class, "WebSphere Dynamic Cache",
            "com.ibm.ws.cache.servlet.ESIProcessorRequest"); // @PQ91098C

    protected ESIProcessor _processor;
    protected Throwable _throwable;
    protected boolean _completed = false;

    public ESIProcessorRequest(ESIProcessor processor) {
        _processor = processor;
        _throwable = null;
    }

    public abstract void handle() throws IOException;

    public synchronized void markCompleted(Throwable throwable) {
        if (_tc.isDebugEnabled())
            Tr.debug(_tc, "markCompleted " + this);
        _completed = true;
        _throwable = throwable;
        notify();
    }

    public synchronized void awaitCompletion() throws Throwable {
        if (_tc.isEntryEnabled())
            Tr.entry(_tc, "awaitCompletion " + this);
        while (!_completed) {
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
        if (_throwable != null)
            throw _throwable;
        if (_tc.isEntryEnabled())
            Tr.exit(_tc, "awaitCompletion " + this);
    }

    protected int readInt() throws IOException {
        return _processor.readInt();
    }

    protected String readString() throws IOException {
        return _processor.readString();
    }

    protected void writeInt(int val) throws IOException {
        _processor.writeInt(val);
    }

    protected void writeString(String str) throws IOException {
        _processor.writeString(str);
    }

    protected void flush() throws IOException {
        _processor.flush();
    }

    protected int available() throws IOException {
        return _processor.available();
    }

    protected void flushWithResponse() throws IOException {
        _processor.flushWithResponse();
    }

}
