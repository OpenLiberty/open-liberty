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
 * @see java.util.concurrent.atomic.AtomicInteger
 */
public interface AtomicInteger {

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicInteger#addAndGet(int)
     */
    public abstract int addAndGet(int delta);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicInteger#decrementAndGet()
     */
    public abstract int decrementAndGet();

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicInteger#get()
     */
    public abstract int get();

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicInteger#incrementAndGet()
     */
    public abstract int incrementAndGet();

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicInteger#set(int)
     */
    public abstract void set(int newIntegerValue);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicInteger#toString()
     */
    public abstract String toString();
} // interface AtomicInteger.