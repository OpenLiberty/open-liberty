/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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

import com.ibm.wsspi.session.IStore;
import com.ibm.wsspi.session.ITimer;

/**
 * @author dettlaff
 * 
 *         default invalidator that does not depend on websphere AlarmManager -
 *         uses java.util.Timer
 */
public class SessionInvalidator implements ITimer {

    private int _invalInterval = 60; // default to 1 minute
    private long _delay = 0; // default is 0
    private Timer _timer;
    private InvalidationTask _invalTask;

    /**
     * Method setStorageInterval
     * <p>
     * 
     * @param interval
     * @see com.ibm.wsspi.session.IStorer#setStorageInterval(int)
     */
    public void start(IStore store, int interval) {
        synchronized (this) {
            _invalInterval = interval;
            _timer = new Timer(true);
            _invalTask = new InvalidationTask(store);
            _timer.schedule(_invalTask, _delay * 1000, _invalInterval * 1000);
        }
    }

    public void stop() {
        _timer.cancel();
    }
    
    //PM74718
    public void setDelay(long invalStart) {
        _delay = invalStart;
    }

    protected class InvalidationTask extends TimerTask {

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
