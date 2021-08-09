/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.concurrent.web;

import java.lang.Thread.State;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runnable that collects information about the thread on which it runs.
 */
class ThreadInfoRunnable implements Runnable {
    final LinkedBlockingQueue<ClassLoader> threadContextClassLoaderQ = new LinkedBlockingQueue<ClassLoader>();
    final LinkedBlockingQueue<ThreadGroup> threadGroupQ = new LinkedBlockingQueue<ThreadGroup>();
    final LinkedBlockingQueue<Boolean> threadInterruptedQ = new LinkedBlockingQueue<Boolean>();
    final LinkedBlockingQueue<Boolean> threadIsDaemonQ = new LinkedBlockingQueue<Boolean>();
    final LinkedBlockingQueue<String> threadNameQ = new LinkedBlockingQueue<String>();
    final LinkedBlockingQueue<Integer> threadPriorityQ = new LinkedBlockingQueue<Integer>();
    final LinkedBlockingQueue<State> threadStateQ = new LinkedBlockingQueue<State>();

    /**
     * Collect information about the thread on which this runs.
     */
    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        threadContextClassLoaderQ.add(thread.getContextClassLoader());
        threadGroupQ.add(thread.getThreadGroup());
        threadInterruptedQ.add(thread.isInterrupted());
        threadIsDaemonQ.add(thread.isDaemon());
        threadPriorityQ.add(thread.getPriority());
        threadNameQ.add(thread.getName());
        threadStateQ.add(thread.getState());
    }
}