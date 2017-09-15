/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util;

public interface ThreadPoolListener {
    public void threadPoolCreated(ThreadPool tp);

    public void threadCreated(ThreadPool tp, int poolSize);

    public void threadStarted(ThreadPool tp, int activeThreads, int maxThreads);

    public void threadReturned(ThreadPool tp, int activeThreads, int maxThreads);

    public void threadDestroyed(ThreadPool tp, int poolSize);
}
