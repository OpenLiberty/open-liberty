/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.test.scan;

import com.ibm.wsspi.anno.classsource.ClassSource_Options;

public class TestOptions_Scan {

    public static final boolean DO_USE_JANDEX = true;
    public static final boolean DO_NOT_USE_JANDEX = false;
    
    public static final boolean DO_USE_JANDEX_FULL = true;
    public static final boolean DO_NOT_USE_JANDEX_FULL = false;

    public static final int SCAN_THREADS_UNBOUNDED = ClassSource_Options.SCAN_THREADS_UNBOUNDED;
    public static final int SCAN_THREADS_MAX = ClassSource_Options.SCAN_THREADS_MAX;
    public static final int SCAN_THREADS_DEFAULT = ClassSource_Options.SCAN_THREADS_DEFAULT_VALUE;

    public TestOptions_Scan(boolean useJandex, boolean useJandexFull, int scanThreads) {
        this.useJandex = useJandex;
        this.useJandexFull = useJandexFull;

        this.scanThreads = scanThreads;
    }

    public final boolean useJandex;
    public final boolean useJandexFull;

    public final int scanThreads;
}
