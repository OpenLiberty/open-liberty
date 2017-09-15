/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * This subclass of {@link ReferenceQueue} exists solely to cast the return types
 * to a more specific subtype of {@link Reference}.
 * 
 * @param <V> the 'value' or referent type
 * @param <R> the reference type to use
 */

@SuppressWarnings("unchecked")
public class RefQueue<V, R extends Reference<V>> extends ReferenceQueue<V> {
    @Override
    public R poll() {
        return (R) super.poll();
    }

    @Override
    public R remove() throws InterruptedException {
        return (R) super.remove();
    }

    @Override
    public R remove(long timeout) throws InterruptedException {
        return (R) super.remove(timeout);
    }
}
