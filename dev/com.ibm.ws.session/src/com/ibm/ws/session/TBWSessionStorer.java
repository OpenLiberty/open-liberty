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
package com.ibm.ws.session;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.IStorer;
import com.ibm.wsspi.session.ITimer;

public class TBWSessionStorer implements IStorer, ITimer {

    protected static class TimeBasedWriteTask extends TimerTask {
        private final IStore store;

        public TimeBasedWriteTask(IStore store) {
            this.store = store;
        }

        public void run() {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, "TimeBasedWriteTask", "run", "running TBW for " + this.store.getId());
            }
            this.store.runTimeBasedWrites();
        }
    }

    private final Timer timer;

    /**
     * Timer started during constructor instead of start method (same as tWAS)
     * 
     * @param store
     * @param interval
     */
    public TBWSessionStorer(IStore store, int interval) {
        long attemptToWriteInterval = interval * 1000; // input is seconds, we need milliseconds
        TimeBasedWriteTask invalTask = new TimeBasedWriteTask(store);
        this.timer = new Timer(true);
        this.timer.schedule(invalTask, 0, attemptToWriteInterval);
    }

    @Override
    public void storeSession(ISession session) {
        // cache last access times to be written before invalidator thread runs
        synchronized (session) {
            session.flush(true);
        }
    }

    @Override
    public void storeSession(ISession session, boolean usesCookies) {
        this.storeSession(session);
    }

    @Override
    public void setStorageInterval(int interval) {
        // never used; consider removing this method from the interface
    }

    @Override
    public void start(IStore store, int interval) {
        // timer started during constructor instead of start method (same as tWAS)
    }

    @Override
    public void stop() {
        this.timer.cancel();
    }

}
