/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

/**
 * Encapsulation of a single specific AIO channel group.
 */
public class AsyncChannelGroup {

    /** IO handle for this group */
    private ResultHandler resultHandler = null;
    /** completion port for this object */
    private long completionPort = 0;
    /** Name for this group */
    private String myGroupName = null;

    private static IAsyncProvider provider = AsyncLibrary.getInstance();

    // private static final TraceComponent tc = Tr.register(
    // AsyncChannelGroup.class,
    // TCPChannelMessageConstants.TCP_TRACE_NAME,
    // TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     * 
     * @param name
     * @throws AsyncException
     */
    public AsyncChannelGroup(String name) throws AsyncException {

        this.completionPort = provider.getNewCompletionPort();
        this.myGroupName = name;
        this.resultHandler = new ResultHandler(name, this.completionPort);
    }

    /**
     * @return ResultHandler for this channel group
     */
    protected ResultHandler getResultHandler() {
        return this.resultHandler;
    }

    /**
     * Activate the AIO channel group.
     */
    public void activate() {
        this.resultHandler.activate();
    }

    /**
     * @return completionPort for this channel group
     */
    public long getCompletionPort() {
        return this.completionPort;
    }

    /**
     * Query the name of this work group.
     * 
     * @return String
     */
    protected String getGroupName() {
        return this.myGroupName;
    }

    /**
     * Trigger the debug printing of the current statistics.
     */
    public void dumpStatistics() {
        this.resultHandler.dumpStatistics();
    }

}