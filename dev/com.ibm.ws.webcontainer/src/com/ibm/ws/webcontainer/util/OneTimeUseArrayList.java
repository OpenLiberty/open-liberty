/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * This subclass of ArrayList will only allow elements to be added to
 * it until an iterator is created for it.  At that point, the list
 * becomes a doorstop.  The add method will simply return false.
 */
@SuppressWarnings("serial")
public class OneTimeUseArrayList {


    private boolean enabled = true;
    
    private ArrayList<Future<?>> runnables = new ArrayList<Future<?>>();

    public synchronized boolean add(ExecutorService es, Runnable r) {
        if (enabled) {
            return runnables.add(es.submit(r));
        }
        return false;
    }

    public synchronized Iterator<Future<?>> iterator() {
        enabled = false;
        return runnables.iterator();
    }

    public synchronized ListIterator<Future<?>> listIterator() {
        enabled = false;
        return runnables.listIterator();
    }

    public synchronized ListIterator<Future<?>> listIterator(int index) {
        enabled = false;
        return runnables.listIterator(index);
    }
    
    public int size() {
        return runnables.size();
    }
    
    public Future<?> get(int index) {
        return runnables.get(index);
    }
}
