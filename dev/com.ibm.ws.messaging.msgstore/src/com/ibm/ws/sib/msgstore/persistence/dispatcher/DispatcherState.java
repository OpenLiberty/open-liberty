/* ==============================================================================
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ==============================================================================
 */
package com.ibm.ws.sib.msgstore.persistence.dispatcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.ibm.ws.sib.msgstore.persistence.dispatcher.StateUtils.StateUpdater;

final class DispatcherState {
    static final StateUpdater<DispatcherState> updaterForStart = state -> state.startRequested().running(true);
    static final StateUpdater<DispatcherState> updaterForStopped = state -> state.running(false);
    static final StateUpdater<DispatcherState> updaterForErrorOccurred = DispatcherState::addThreadWriteError;
    static final StateUpdater<DispatcherState> updaterForErrorCleared = DispatcherState::clearThreadWriteError;

    public static final class StopRequesterInfo extends Throwable {
        private static final long serialVersionUID = 1L;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd_HH.mm.ss.SSS_Z");

        public StopRequesterInfo(Throwable requester) {
            super("Stop requested at " + DATE_FORMATTER.format(OffsetDateTime.now()), requester);
        }
    }

    static StateUpdater<DispatcherState> getUpdaterForStopRequested(final Throwable requester) {
        return state -> state.isRunning ? state.stopRequested(new StopRequesterInfo(requester)) : state;
    }

    // Flag set to indicate whether dispatcher is running.
    final boolean isRunning;
    // Flag set to indicate that the dispatcher should stop.
    // This is caused by calling the {@link Dispatcher#stop()} method.
    final boolean isStopRequested;
    // Count of the number of worker threads experiencing write errors.
    final int threadWriteErrors;
    final Throwable requester;

    DispatcherState() {
        this(false, false, 0, null);
    }

    private DispatcherState(boolean running, boolean stopRequested, int threadWriteErrors, Throwable requester) {
        this.isRunning = running;
        this.isStopRequested = stopRequested;
        this.threadWriteErrors = threadWriteErrors;
        this.requester = requester;
    }

    private DispatcherState running(final boolean running) {
        return (running == isRunning) ? this : new DispatcherState(running, isStopRequested, threadWriteErrors, requester);
    }

    DispatcherState startRequested() {
        return isStopRequested ? new DispatcherState(isRunning, false, threadWriteErrors, null) : this;
    }

    private DispatcherState stopRequested(Throwable requester) {
        return isStopRequested ? this : new DispatcherState(isRunning, true, threadWriteErrors, requester);
    }

    private DispatcherState addThreadWriteError() {
        return new DispatcherState(isRunning, isStopRequested, (threadWriteErrors + 1), requester);
    }

    private DispatcherState clearThreadWriteError() {
        return (0 >= threadWriteErrors) ? this : new DispatcherState(isRunning, isStopRequested, (threadWriteErrors - 1), requester);
    }

    boolean isHealthy() {
        return isRunning && !isStopRequested && (0 == threadWriteErrors);
    }

    String desc() {
        StringBuilder sb = new StringBuilder();
        if (isStopRequested) sb.append(" (STOP REQUESTED)");
        if (!isRunning) sb.append(" (STOPPED)");
        if (0 < threadWriteErrors) sb.append(" (ERROR (").append(threadWriteErrors).append("))");
        if (null != requester) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            requester.printStackTrace(pw.printf("%n"));
            sb.append(sw);
        }
        return sb.toString();
    }
}
