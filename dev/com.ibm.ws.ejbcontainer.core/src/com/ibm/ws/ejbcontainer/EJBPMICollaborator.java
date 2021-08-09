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
package com.ibm.ws.ejbcontainer;

public interface EJBPMICollaborator {
    /* F743-9001 - START */
    public final static int CREATE_RT = 14;
    public final static int REMOVE_RT = 15;

    public final static int READ_LOCK_TIME = 36;// Time to obtain a read lock on a Singleton Bean
    public final static int WRITE_LOCK_TIME = 37;// Time to obtain a write lock on a Singleton Bean

    public final static int ASYNC_WAIT_TIME = 39;

    public final static int ASYNC_FUTURE_OBJECT_COUNT = 43;//F1031-28135
    public final static int DISCARDS = 44;//F1031-27071

    /* F743-9001 - END */
    public void beanInstantiated();

    public void beanDestroyed();

    public void beanCreated();

    public void beanRemoved();

    public void beanDiscarded(); //defect 395018

    public long methodPreInvoke(Object key, EJBMethodMetaData mi);

    public void methodPostInvoke(Object key, EJBMethodMetaData mi, long startTime);

    public void objectRetrieve(int size, boolean objectInPool);

    public void objectReturn(int size, boolean objectDiscarded);

    public void poolCreated(int size);

    public void poolDrained(int size, int numObjectsDiscarded);

    public void destroy();

    //For JSR 77

    public long activationTime(); //activation Start

    public void activationTime(long startTime); //activation complete

    public long passivationTime(); //passivation Start

    public void passivationTime(long startTime); //passivation complete

    public long loadTime(); //loading Start

    public void loadTime(long startTime); //loading complete

    public long storeTime(); //storing Start

    public void storeTime(long startTime); //storing complete

    // Message Driven Bean stats methods.
    public void messageDelivered();

    public void messageBackedOut();

    public long waitingForServerSession();

    public void gotServerSession(long startTime);

    public void serverSessionRetrieve(int newInUseCount, int poolSize);

    public void serverSessionReturn(int newInUseCount, int poolSize);

    /* F743-9001 - START */
    /**
     * Returns an opaque cookie that marks the beginning of a timed operation.
     *
     * @param strCounterId one of
     *            <ul>
     *            <li>CREATE_RT
     *            <li>REMOVE_RT
     *            <li>READ_LOCK_TIME
     *            <li>WRITE_LOCK_TIME
     *            <li>ASYNC_WAIT_TIME
     *            </ul>
     * @return an opaque cookie
     * @see #finalTime
     */
    public long initialTime(int strCounterId);

    /**
     * Notification that an event has completed.
     *
     * @param strCounterId a time counter passed to {@link #initialTime}
     * @param startTime an opaque cookie returned by {@link #initialTime}
     * @return a value that can be added to {@link #initialTime} to reduce the
     *         reported duration of another event
     */
    public long finalTime(int strCounterId, long startTime);//648142.1

    public void countCancelledLocks();

    public void asyncQueSizeIncrement();

    public void asyncQueSizeDecrement();

    public void asyncMethodCallCanceled();

    public void asyncFNFFailed();

    /* F743-9001 - END */
    public void discardCount();//F1031-27071

    public void asyncFutureObjectIncrement();//F1031-28135

    public void asyncFutureObjectDecrement();//F1031-28135
}
