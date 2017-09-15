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
package com.ibm.ws.kernel.boot.internal;

public enum KernelStartLevel {
    /** WAS Server start levels: 0 (stopped), and 1 have special meaning w/ OSGi */
    OSGI_INIT(1),
    BOOTSTRAP(2),
    KERNEL_CONFIG(3),
    KERNEL(4),
    KERNEL_LATE(6),
    FEATURE_PREPARE(7),
    ACTIVE(20),
    // LIBERTY_BOOT is a special start-level used as internal marker for boot jars
    LIBERTY_BOOT(123456789);

    final int startLevel;

    KernelStartLevel(int level) {
        startLevel = level;
    }

    public int getLevel() {
        return startLevel;
    }
}