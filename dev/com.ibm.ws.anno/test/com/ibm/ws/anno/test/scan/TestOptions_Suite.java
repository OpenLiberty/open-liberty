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

import com.ibm.ws.anno.targets.delta.TargetsDelta;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Options;

public class TestOptions_Suite {

    public static final boolean USE_JANDEX = true;
    public static final boolean USE_JANDEX_FULL = true;

    public static final int SCAN_THREADS_ONE = 1;
    public static final int SCAN_THREADS_DEFAULT = ClassSource_Options.SCAN_THREADS_DEFAULT_VALUE;

    public static final int WRITE_THREADS_ONE = 1;
    public static final int WRITE_THREADS_DEFAULT = TargetCache_Options.WRITE_THREADS_DEFAULT;

    public static final String SINGLE_WRITE_SUFFIX = "SW";
    public static final String MULTI_WRITE_SUFFIX = "MW";
    public static final String SINGLE_WRITE_ASYNC_SUFFIX = "SWA";
    public static final String MULTI_WRITE_ASYNC_SUFFIX = "MWA";

    public static final boolean DO_CLEAN_STORAGE = true;

    public static final boolean DO_IGNORE_MISSING_PACKAGES = TargetsDelta.DO_IGNORE_REMOVED_PACKAGES;
    public static final boolean DO_IGNORE_MISSING_INTERFACES = TargetsDelta.DO_IGNORE_REMOVED_INTERFACES;

    // Definition: String title, String description,
    // Scan Options: boolean useJandex, boolean useJandexFull, int scanThreads,
    // Cache Options: (boolean enabled), String storageSuffix, boolean readOnly, boolean alwaysValid, int writeThreads 

    // "Single", "Single Threaded Scan"
    // "SingleJandex", "Single Threaded Scan Using Jandex Reads"
    // "SingleJandexFull", "Single Threaded Scan Using Jandex Full Reads"

    // "Multi", "Multi Threaded Scan"
    // "MultiJandex", "Multi Threaded Scan Using Jandex Reads"
    // "MultiJandexFull", "Multi Threaded Scan Using Jandex Full Reads"

    // "SingleWrite", "Single Threaded Scan With Initial Write"
    // "SingleRead", "Single Threaded Scan With Read"    

    // "MultiWrite", "Multi Threaded Scan With Initial Write"
    // "MultiRead", "Multi Threaded Scan With Read"

    // "SingleWriteAsync", "Single Threaded Scan with Asynchronous Initial Write"
    // "MultiWriteAsync", "Multi Threaded Scan with Asynchronous Initial Write"

    public static TestOptions SINGLE_OPTIONS =
        new TestOptions(
            "Single", "Single Threaded Scan",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_ONE,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions SINGLE_JANDEX_OPTIONS =
        new TestOptions(
            "SingleJandex", "Single Threaded Scan Using Jandex Reads",
            USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_ONE,
            DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_JANDEX_FULL_OPTIONS =
        new TestOptions(
            "SingleJandexFull", "Single Threaded Scan Using Jandex Full Reads",
            USE_JANDEX, USE_JANDEX_FULL, SCAN_THREADS_ONE,
            DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions MULTI_OPTIONS =
        new TestOptions(
            "Multi", "Multi Threaded Scan",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_DEFAULT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_JANDEX_OPTIONS =
        new TestOptions(
            "MultiJandex", "Multi Threaded Scan Using Jandex Reads",
            USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_DEFAULT,
            DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_JANDEX_FULL_OPTIONS =
        new TestOptions(
            "MultiJandexFull", "Multi Threaded Scan Using Jandex Full Reads",
            USE_JANDEX, USE_JANDEX_FULL, SCAN_THREADS_DEFAULT,
            DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_WRITE_OPTIONS =
        new TestOptions(
            "SingleWrite", "Single Threaded Scan With Initial Write",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_ONE,
            SINGLE_WRITE_SUFFIX, DO_CLEAN_STORAGE, WRITE_THREADS_ONE,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions SINGLE_READ_OPTIONS =
        new TestOptions(
            "SingleRead", "Single Threaded Scan With Read",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_ONE,
            SINGLE_WRITE_SUFFIX, !DO_CLEAN_STORAGE, WRITE_THREADS_ONE,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions MULTI_WRITE_OPTIONS =
        new TestOptions(
            "MultiWrite", "Multi Threaded Scan With Initial Write",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_DEFAULT,
            MULTI_WRITE_SUFFIX, DO_CLEAN_STORAGE, WRITE_THREADS_ONE,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_READ_OPTIONS =
        new TestOptions("MultiRead", "Multi Threaded Scan With Read",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_DEFAULT,
            MULTI_WRITE_SUFFIX, !DO_CLEAN_STORAGE, WRITE_THREADS_ONE,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_WRITE_ASYNC_OPTIONS =
        new TestOptions(
            "SingleWriteAsync", "Single Threaded Scan with Asynchronous Initial Write",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_ONE,
            SINGLE_WRITE_ASYNC_SUFFIX, DO_CLEAN_STORAGE, WRITE_THREADS_DEFAULT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_WRITE_ASYNC_OPTIONS =
        new TestOptions(
            "MultiWriteAsync", "Multi Threaded Scan with Asynchronous Initial Write",
            !USE_JANDEX, !USE_JANDEX_FULL, SCAN_THREADS_ONE,
            MULTI_WRITE_ASYNC_SUFFIX, DO_CLEAN_STORAGE, WRITE_THREADS_DEFAULT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
}
