/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound.read;

import io.netty.channel.Channel;
import com.ibm.ws.http.netty.pipeline.inbound.read.ReadFlowHandler;

/**
 * This is used  by the {@link ReadFlowHandler} to keep track of the state of read 
 * I/O flow. The state is helpful to determine if and when the read flow handler should
 * invoke the pipeline to read more data from the channel when auto-read is disabled. 
 */
public class FlowState {

    private volatile boolean bodyReadWanted;
    private volatile boolean headRequest;
    private volatile boolean keepAliveAllowed;
    private volatile boolean peerInputShutdown;
    private volatile boolean quiescing;
    private volatile boolean readAgain;
    private volatile boolean readPending;
    private volatile boolean requestConsumed;
    private volatile boolean responseInFlight;
    private volatile boolean stopReading;

    /**
     * FlowState constructor
     */
    public FlowState() {
        this.bodyReadWanted = false;
        this.headRequest = false;
        this.keepAliveAllowed = true;
        this.peerInputShutdown = false;
        this.quiescing = false;
        this.readAgain = false;
        this.readPending = false;
        this.requestConsumed = true;
        this.responseInFlight = false;  
        this.stopReading = false;
    }

    public boolean isBodyReadWanted(){
        return bodyReadWanted;
    }

    /** 
     * @return true if the current request is a HEAD request (no body expected).
     */
    public boolean isHeadRequest() {
        return headRequest;
    }
      
    /**
     * @return true is the connection is allowed to remain open after the current response,
     * based on the response headers and the server Keep-Alive policy. 
     */
    public boolean isKeepAliveAllowed() {
        return keepAliveAllowed;
    }

    /**
     * @return true if the peer has signaled that it is closed and no longer writing data. This is
     * used to determine if the connection sould be closed after the current response is fully processed.
     */
    public boolean isPeerInputShutdown() {
        return peerInputShutdown;
    }

    public boolean isQuiescing(){
        return quiescing;
    }

    public boolean isReadAgain(){
        return readAgain;
    }

    /**
     * @return true if the {@link ReadFlowHandler} has already issued a read that has
     * not yet been marked complete via the channel read complete event.
     */
    public boolean isReadPending() {
        return readPending;
    }

    /**
     * @return true if the current request has been fully consumed (body read to completion)
     * or when no body is expected.
     */
    public boolean isRequestConsumed() {
        return requestConsumed;
    }

    /**
     * @return if a response has been committed and remains true until the final 
     * write completes.
     */
    public boolean isResponseInFlight() {
        return responseInFlight;
    } 

    public boolean stoppedReading() {
        return stopReading;
    }

    public void setBodyReadWanted(boolean bodyReadWanted){
        this.bodyReadWanted = bodyReadWanted;
    }

    public void setHeadRequest(boolean headRequest) {
        this.headRequest = headRequest;
    }

    public void setKeepAliveAllowed(boolean keepAliveAllowed) {
        this.keepAliveAllowed = keepAliveAllowed;
    }

    public void setPeerInputShutdown(boolean peerInputShutdown) {
        this.peerInputShutdown = peerInputShutdown;
    }

    public void setQuiescing(boolean quiescing){
        this.quiescing = quiescing;
    }

    public void setReadAgain(boolean readAgain){
        this.readAgain = readAgain;
    }

    public void setReadPending(boolean readPending) {
        this.readPending = readPending;
    } 

    public void setRequestConsumed(boolean requestConsumed) {
        this.requestConsumed = requestConsumed;
    }

    public void setResponseInFlight(boolean responseInFlight) {
        this.responseInFlight = responseInFlight;
    }


    public void setStopReading(boolean stopReading) {
        this.stopReading = stopReading;
    }
}
