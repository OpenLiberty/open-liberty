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
package fat.concurrent.spec.app;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Queue of TaskEvent. Events do not show up for polling until they are fully populated.
 */
class TaskEventQueue {
    private final LinkedBlockingQueue<TaskEvent> events = new LinkedBlockingQueue<TaskEvent>();

    void add(TaskEvent event) {
        events.add(event);
        synchronized (this) {
            notifyAll();
        }
    }

    boolean isEmpty() {
        return events.isEmpty();
    }

    /**
     * Wait for the appearance of the next event AND for that event to be fully populated.
     * Then removes the event from the queue and returns it.
     *
     * @param timeout maximum amount of time to wait
     * @param units unit of measurement
     * @return fully populated next TaskEvent.
     * @throws InterruptedException if interrupted while waiting
     */
    TaskEvent poll(long timeout, TimeUnit units) throws InterruptedException {
        long timeoutMillis = units.toMillis(timeout);
        for (long start = System.currentTimeMillis(), elapsedTime = 0; elapsedTime < timeoutMillis; elapsedTime = System.currentTimeMillis() - start) {
            TaskEvent event = events.peek();
            if (event == null)
                synchronized (this) {
                    wait(100);
                }
            else if (event.isPopulated.await(timeoutMillis - elapsedTime, TimeUnit.MILLISECONDS) && events.remove(event))
                return event;
        }
        return null;
    }

    @Override
    public String toString() {
        return events.toString();
    }
}
