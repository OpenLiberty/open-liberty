/*******************************************************************************
 * Copyright (c) 2001, 2023 IBM Corporation and others.
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
package com.ibm.ejs.j2c;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This is a utility class used by the PoolManager.
 */
public abstract class TaskTimer extends Thread {
    protected PoolManager pm = null;

    /**
     * Create a new TaskTimer.
     */

    protected TaskTimer(PoolManager value) {
        super();
        pm = value;
        final TaskTimer finalThis = this;
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                finalThis.setDaemon(true);
                return null;
            }
        });

        start();
    }
}