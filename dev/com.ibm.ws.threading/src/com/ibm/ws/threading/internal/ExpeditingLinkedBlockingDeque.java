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
package com.ibm.ws.threading.internal;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * TODO Experiment with this to see if it is superior to existing BoundedBuffer and alternative BlockingQueue implementations.
 * Extension of LinkedBlockingDeque which supports expedite when a QueueItem is added/offered/put with isExpedited=true.
 * When supplied to ThreadPoolExecutor as a BlockingQueue, we can control whether or not task items are expedited
 * by having them implement QueueItem.
 *
 * @param <T>
 */
public class ExpeditingLinkedBlockingDeque<T> extends LinkedBlockingDeque<T> {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean add(T item) {
        if (item instanceof QueueItem && ((QueueItem) item).isExpedited())
            addFirst(item);
        else
            addLast(item);
        return true;
    }

    @Override
    public boolean offer(T item) {
        if (item instanceof QueueItem && ((QueueItem) item).isExpedited())
            return offerFirst(item);
        else
            return offerLast(item);
    }

    @Override
    public boolean offer(T item, long timeout, TimeUnit unit) throws InterruptedException {
        if (item instanceof QueueItem && ((QueueItem) item).isExpedited())
            return offerFirst(item, timeout, unit);
        else
            return offerLast(item, timeout, unit);
    }

    @Override
    public void put(T item) throws InterruptedException {
        if (item instanceof QueueItem && ((QueueItem) item).isExpedited())
            putFirst(item);
        else
            putLast(item);
    }
}