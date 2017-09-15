package com.ibm.ws.objectManager.utils.concurrent.locks;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Native JVM implementation.
 */
public class ReentrantReadWriteLockImpl
                implements ReentrantReadWriteLock {
    java.util.concurrent.locks.ReentrantReadWriteLock readWriteLock;
    ReadLock readLock;
    WriteLock writeLock;

    public ReentrantReadWriteLockImpl() {
        readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
        readLock = new ReadLock(readWriteLock);
        writeLock = new WriteLock(readWriteLock);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.concurrent.locks.ReentrantReadWriteLock#readLock()
     */
    public Lock readLock() {
        return readLock;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.utils.concurrent.locks.ReentrantReadWriteLock#writeLock()
     */
    public Lock writeLock() {
        return writeLock;
    }

    public class ReadLock implements Lock {
        java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock javaReadLock;

        private ReadLock(java.util.concurrent.locks.ReentrantReadWriteLock readWriteLock) {
            javaReadLock = readWriteLock.readLock();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.utils.concurrent.locks.ReadLock#lock()
         */
        public final void lock() {
            javaReadLock.lock();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.utils.concurrent.locks.ReadLock#lockInterruptibly()
         */
        public final void lockInterruptibly()
                        throws InterruptedException {
            javaReadLock.lockInterruptibly();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.utils.concurrent.locks.ReadLock#unlock()
         */
        public final void unlock() {
            javaReadLock.unlock();
        }
    } // inner class ReadLock.

    public class WriteLock implements Lock {
        java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock javaWriteLock;

        private WriteLock(java.util.concurrent.locks.ReentrantReadWriteLock readWriteLock) {
            javaWriteLock = readWriteLock.writeLock();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.utils.concurrent.locks.WriteLock#lock()
         */
        public final void lock() {
            javaWriteLock.lock();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.utils.concurrent.locks.WriteLock#lockInterruptibly()
         */
        public final void lockInterruptibly()
                        throws InterruptedException {
            javaWriteLock.lockInterruptibly();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.objectManager.utils.concurrent.locks.WriteLock#unlock()
         */
        public final void unlock() {
            javaWriteLock.unlock();
        }
    } // inner class WriteLock.
} // class ReentrantReadWriteLock.