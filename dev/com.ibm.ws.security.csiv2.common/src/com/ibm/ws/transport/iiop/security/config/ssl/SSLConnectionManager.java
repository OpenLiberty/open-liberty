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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.ssl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * @version $Revision: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class SSLConnectionManager {
    private static final Set listeners = new HashSet();
    private static long nextId = 0;

    public static void register(SSLConnectionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static void unregister(SSLConnectionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public synchronized static long allocateId() {
        return nextId++;
    }

    public static void fireOpen(long connectionId) {
        Set copy = null;

        synchronized (listeners) {
            copy = new HashSet(listeners);
        }

        for (Iterator iter = copy.iterator(); iter.hasNext();) {
            ((SSLConnectionListener) iter.next()).open(connectionId);
        }
    }

    public static void fireClose(long connectionId) {
        Set copy = null;

        synchronized (listeners) {
            copy = new HashSet(listeners);
        }

        for (Iterator iter = copy.iterator(); iter.hasNext();) {
            ((SSLConnectionListener) iter.next()).close(connectionId);
        }
    }
}
