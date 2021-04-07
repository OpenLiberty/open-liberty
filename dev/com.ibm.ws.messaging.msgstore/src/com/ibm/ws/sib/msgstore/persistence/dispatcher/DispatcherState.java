package com.ibm.ws.sib.msgstore.persistence.dispatcher;

import com.ibm.ws.sib.msgstore.persistence.dispatcher.StateUtils.StateUpdater;

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

final class DispatcherState {
    static final StateUpdater<DispatcherState> updaterForStart = new StateUpdater<DispatcherState>() {
        public DispatcherState update(DispatcherState currentState) {
            return currentState.stopRequested(false).running(true);
        }
    };
    static final StateUpdater<DispatcherState> updaterForStopRequested = new StateUpdater<DispatcherState>() {
        public DispatcherState update(DispatcherState currentState) {
            if (false == currentState.isRunning) return currentState;
            return currentState.stopRequested(true);
        }
    };
    static final StateUpdater<DispatcherState> updaterForStopped = new StateUpdater<DispatcherState>() {
        public DispatcherState update(DispatcherState currentState) {
            return currentState.running(false);
        }
    };
    static final StateUpdater<DispatcherState> updaterForErrorOccurred = new StateUpdater<DispatcherState>() {
        public DispatcherState update(DispatcherState currentState) {
            return currentState.addThreadWriteError();
        }
    };
    static final StateUpdater<DispatcherState> updaterForErrorCleared = new StateUpdater<DispatcherState>() {
        public DispatcherState update(DispatcherState currentState) {
            return currentState.clearThreadWriteError();
        }
    };

    // Flag set to indicate whether dispatcher is running.
    final boolean isRunning;
    // Flag set to indicate that the dispatcher should stop.
    // This is caused by calling the {@link Dispatcher#stop()} method.
    final boolean isStopRequested;
    // Count of the number of worker threads experiencing write errors.
    final int threadWriteErrors;

    DispatcherState() {
        this(false, false, 0);
    }

    private DispatcherState(boolean running, boolean stopRequested, int threadWriteErrors) {
        this.isRunning = running;
        this.isStopRequested = stopRequested;
        this.threadWriteErrors = threadWriteErrors;
    }

    DispatcherState running(final boolean running) {
        return (running == isRunning) ? this : new DispatcherState(running, this.isStopRequested, this.threadWriteErrors);
    }

    DispatcherState stopRequested(final boolean stopRequested) {
        return (stopRequested == isStopRequested) ? this : new DispatcherState(this.isRunning, stopRequested, this.threadWriteErrors);
    }

    DispatcherState addThreadWriteError() {
        return new DispatcherState(isRunning, isStopRequested, (threadWriteErrors + 1));
    }

    DispatcherState clearThreadWriteError() {
        return (0 >= threadWriteErrors) ? this : new DispatcherState(isRunning, isStopRequested, (threadWriteErrors - 1));
    }

    boolean isHealthy() {
        return isRunning && !isStopRequested && (0 == threadWriteErrors);
    }

    String desc() {
        String s = "";
        if (isStopRequested) s+= " (STOP REQUESTED)";
        if (!isRunning) s+= " (STOPPED)";
        if (0 < threadWriteErrors) s+= " (ERROR)";
        return s;
    }
}
