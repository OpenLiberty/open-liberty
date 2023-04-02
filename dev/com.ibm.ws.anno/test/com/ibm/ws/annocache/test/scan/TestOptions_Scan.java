/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

package com.ibm.ws.annocache.test.scan;

import com.ibm.wsspi.annocache.classsource.ClassSource_Options;

public class TestOptions_Scan {

    public static final boolean DO_USE_JANDEX = true;
    public static final boolean DO_NOT_USE_JANDEX = false;

    public static final int SCAN_THREADS_UNBOUNDED = ClassSource_Options.SCAN_THREADS_UNBOUNDED;
    public static final int SCAN_THREADS_MAX = ClassSource_Options.SCAN_THREADS_MAX;
    public static final int SCAN_THREADS_DEFAULT = ClassSource_Options.SCAN_THREADS_DEFAULT_VALUE;

    public TestOptions_Scan(boolean useJandex, int scanThreads) {
        this.useJandex = useJandex;
        this.scanThreads = scanThreads;
    }

    public final boolean useJandex;
    public final int scanThreads;
}
