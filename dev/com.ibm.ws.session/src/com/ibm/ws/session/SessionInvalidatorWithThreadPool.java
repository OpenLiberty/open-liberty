/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.webcontainer.httpsession.SessionMgrComponentImpl;
import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.ITimer;

/*This class is similar to SessionInvalidator but instead uses ScheduledExecutorService to control thread pool
 */

public class SessionInvalidatorWithThreadPool implements ITimer{

    private int _invalInterval = 60; // default to 1 minute
    private long _delay = 0; // default is 0
    private InvalidationTask _invalTask;
    private ScheduledExecutorService _scheduler; 
    private ScheduledFuture<?> _result;

 
    public void start(IStore store, int interval) {
        _invalInterval = interval;
        
        /*Get reference to ScheduledExecutorService from SessionMgrComponentImpl*/
        _scheduler = SessionMgrComponentImpl.INSTANCE.get().getScheduledExecutorService();
        
        _invalTask = new InvalidationTask(store);
        
        /*schedule periodic invalidation task and store in ScheduledFuture object*/
        _result=_scheduler.scheduleWithFixedDelay(_invalTask, _delay * 1000, _invalInterval * 1000, TimeUnit.MILLISECONDS);
        
    }

    public void stop() {
        /*allows the current running task to continue to completion but stops any further tasks from running*/
        _result.cancel(false);
    }
    
    public void setDelay(long invalStart) {
        _delay = invalStart;
    }
 
    protected static class InvalidationTask implements Runnable {

        /*
         * (non-Javadoc)
         * 
         * @see java.util.TimerTask#run()
         */
        IStore _store;
        
        public InvalidationTask(IStore store) {
            _store = store;
        }

        public void run() {

            _store.runInvalidation();
         
        }
    }    

    
}
