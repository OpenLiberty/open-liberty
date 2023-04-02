package com.ibm.ws.objectManager.utils.concurrent.atomic;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @see java.util.concurrent.atomic.AtomicIntegerArray
 */
public interface AtomicIntegerArray {

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicIntegerArray#addAndGet(int,int)
     */
    public abstract int addAndGet(int i, int delta);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicIntegerArray#decrementAndGet(int)
     */
    public abstract int decrementAndGet(int i);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicIntegerArray#get(int)
     */
    public abstract int get(int i);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicIntegerArray#incrementAndGet(int)
     */
    public abstract int incrementAndGet(int i);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicIntegerArray#set(int,int)
     */
    public abstract void set(int i, int newIntegerValue);

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.atomic.AtomicIntegerArray#toString()
     */
    public abstract String toString();
} // interface AtomicIntegerArray.