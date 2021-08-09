/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.synch;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class that contains a thread local that any log handler can use to
 * avoid loops when any tracing or logging in its code path causes an event
 * to be written back to log or trace source.
 */
@Trivial
public class ThreadLocalHandler {

    private static ThreadLocal<Boolean> boolValue = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static Boolean get() {
        return boolValue.get();
    }

    public static void set(Boolean value) {
        boolValue.set(value);
    }

    public static void remove() {
        boolValue.remove();
    }
}
