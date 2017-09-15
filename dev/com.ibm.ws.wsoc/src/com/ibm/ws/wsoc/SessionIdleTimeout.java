/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.injection.InjectionThings;
import com.ibm.ws.wsoc.util.Utils;

/**
 * Class to support Session maxIdleTimeTimeout
 */
public class SessionIdleTimeout implements Runnable {
    private static final TraceComponent tc = Tr.register(SessionIdleTimeout.class);

    String sessionID = null;
    ScheduledFuture<?> sessionTimeoutFuture = null;
    boolean restartTimer = true;
    WsocConnLink wsocLink;
    long startTime;
    long maxIdleTimeout;

    public SessionIdleTimeout(String sessionID, long maxIdleTimeout, WsocConnLink wsocLink) {
        this.sessionID = sessionID;
        this.maxIdleTimeout = maxIdleTimeout;
        this.wsocLink = wsocLink;
    }

    public void restartIdleTimeout(long maxIdleTimeout) {
        this.maxIdleTimeout = maxIdleTimeout;
        //no action. session timeout is infinite. 
        if (maxIdleTimeout < 1) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Session idle timeout is default which is no time out");
            }
            return;
        }
        //if there is a timer running already, cancel and re-start another timer
        synchronized (sessionID) {
            if (sessionTimeoutFuture != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "restartIdleTimeout to cancel sessionTimeoutFuture");
                }
                sessionTimeoutFuture.cancel(true);
                sessionTimeoutFuture = null;
            }
        }
        //reset read-write flag
        wsocLink.setReadWrite(false);

        //Calculate how often the code should wake up to check if there has been any read/write of messages to/from the server endpoint.
        long readWriteCheckWakeUpTime = calculateReadWriteWakeUpTime();

        //start recording the time to begin session idle timeout
        startTime = System.nanoTime();

        //start the timer to wake up for read/write interval
        synchronized (sessionID) {
            if (restartTimer) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "restartIdleTimeout: schedule new sessionTimeoutFuture");
                }
                sessionTimeoutFuture = ServiceManager.getExecutorService().scheduleAtFixedRate(this, readWriteCheckWakeUpTime, readWriteCheckWakeUpTime,
                                                                                               TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public void run() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Session idle timeout interval check called for session: " + sessionID);
        }
        //read/write time interval occurred. Check if there was any read/write during this period. If yes, re-start the maxIdleTimeOut
        // if no read/write and max idle time has not been reach, then return doing nothing and let the fixed rate scheduler fire again later
        if (wsocLink.isReadWrite()) {
            restartIdleTimeout(maxIdleTimeout);
        } else if (TimeUnit.MILLISECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS) >= maxIdleTimeout) {
            // if no-read write has happened, and idleTimeout period is reached then close after preserving contexts  
            String closeMsg = Tr.formatMessage(tc, "maxidletimeout.closesession", TimeUnit.SECONDS.convert(maxIdleTimeout, TimeUnit.MILLISECONDS));

            //length of close reason can only be 123 UTF-8 encoded character bytes length
            closeMsg = Utils.truncateCloseReason(closeMsg);
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.CLOSED_ABNORMALLY, closeMsg);

            // set the contexts since onClose in the user code may be called.
            InjectionThings it = wsocLink.pushContexts();
            try {
                close(closeReason);
            } finally {
                wsocLink.popContexts(it);
            }
        }
    }

    /*
     * Calculate how often the code should wake up to check if there has been any read/write of messages to/from the server endpoint.
     * If there has been a read/write, the session idle timeout resets and start again.
     * Note that even though WebSocket API says, session idle timeout resets for every msg read/write, for performance reason, instead
     * of restarting idle timeout for every read/write, we chose an interval to wake up and check if read/write has happened to restart
     * the idle timeout.
     */
    private long calculateReadWriteWakeUpTime() {
        //if maxIdleTimeout is less than 60 seconds, then set the read/write wake-up time resolution to 2 seconds.   
        // this means the actual idle time measured will be somewhere between 2 and 4 seconds.
        if (this.maxIdleTimeout <= 60000) {
            return (2000);
        }
        //if maxIdleTimeout is  between 1 -  10 minutes, then check if there is a read/write every 4 seconds
        if (this.maxIdleTimeout >= 60000 && this.maxIdleTimeout <= 600000) {
            return (4000);
        } else { // maxIdleTimeout is more than 10 minutes, then just check every 6 seconds
            return (6000);
        }
    }

    public void cleanup() {
        // cancel the executor request
        // need tolerate that this may be called more than once during shutdown of a session
        synchronized (sessionID) {
            if (sessionTimeoutFuture != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "cleanup: cancel sessionTimeoutFuture");
                }
                sessionTimeoutFuture.cancel(false);
                sessionTimeoutFuture = null;
            }
            restartTimer = false;
        }
    }

    public void close(CloseReason cr) {

        //if there was a read 1)going through websocket code or 2) read from complete callback path, then restart the idle timeout, otherwise close the
        // connection.

        // cut off close loops
        if (wsocLink.checkIfClosingAlready()) {
            synchronized (sessionID) {
                if (sessionTimeoutFuture != null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "close(1): cancel sessionTimeoutFuture");
                    }
                    sessionTimeoutFuture.cancel(false);
                    sessionTimeoutFuture = null;
                }
                restartTimer = false;
            }
            return;
        }

        // close will be done on the error callback (timeout of the current read).
        if (wsocLink.finishReadBeforeIdleClose()) {

            wsocLink.outgoingCloseConnection(cr);

            // if there is a timer running it won't be needed anymore 
            synchronized (sessionID) {
                if (sessionTimeoutFuture != null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "close(2): cancel sessionTimeoutFuture");
                    }
                    sessionTimeoutFuture.cancel(false);
                    sessionTimeoutFuture = null;
                }
                restartTimer = false;
            }
        } else {
            restartIdleTimeout(maxIdleTimeout);
        }
    }
}
