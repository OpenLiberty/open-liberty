package com.ibm.ws.objectManager.utils.concurrent.atomic;

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
 * @see java.util.concurrent.atomic.AtomicLong
 */
public interface AtomicLong {

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicLong#addAndGet(long)
     */
    public abstract long addAndGet(long delta);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicLong#decrementAndGet()
     */
    public abstract long decrementAndGet();

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicLong#get()
     */
    public abstract long get();

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicLong#incrementAndGet()
     */
    public abstract long incrementAndGet();

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicLong#set(long)
     */
    public abstract void set(long newLongValue);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicLong#toString()
     */
    public abstract String toString();

} // interface AtomicLong.