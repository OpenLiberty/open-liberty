/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.test.scan;

import com.ibm.ws.annocache.targets.delta.TargetsDelta;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;

public class TestOptions_Suite {

    public static final boolean USE_JANDEX = true;

    public static final int SCAN_THREADS_MULTI = 8;
    public static final int SCAN_THREADS_DEFAULT = ClassSource_Options.SCAN_THREADS_DEFAULT_VALUE;

    public static final boolean ALWAYS_VALID = true;
    public static final boolean ALWAYS_VALID_DEFAULT = !ALWAYS_VALID;
    
    public static final boolean READ_ONLY = true;
    public static final boolean READ_ONLY_DEFAULT = !READ_ONLY;

    public static final int WRITE_THREADS_MULTI = 8;
    public static final int WRITE_THREADS_DEFAULT = TargetCache_Options.WRITE_THREADS_DEFAULT;

    public static final boolean DO_USE_JANDEX_FORMAT = true;
    public static final boolean DO_USE_BINARY_FORMAT = true;

    public static final String SINGLE_WRITE_SUFFIX = "SW";
    public static final String MULTI_WRITE_SUFFIX = "MW";
    public static final String SINGLE_WRITE_ASYNC_SUFFIX = "SWA";
    public static final String MULTI_WRITE_ASYNC_SUFFIX = "MWA";

    public static final String SINGLE_WRITE_BINARY_FORMAT_SUFFIX = "SWBF";
    public static final String SINGLE_WRITE_JANDEX_FORMAT_SUFFIX = "SWJF";

    public static final boolean DO_CLEAN_STORAGE = true;

    public static final boolean DO_IGNORE_MISSING_PACKAGES =
        TargetsDelta.DO_IGNORE_REMOVED_PACKAGES;
    public static final boolean DO_IGNORE_MISSING_INTERFACES =
        TargetsDelta.DO_IGNORE_REMOVED_INTERFACES;

    // Definition:
    //   String title, String description,
    //
    // Scan Options:
    //   boolean useJandex, int scanThreads,
    //
    // [ Cache Options:
    //     String storageSuffix, boolean cleanStorage,
    //     int writeThreads,
    //     boolean omitJandexWrites, boolean separateContainers ]
    //
    // Test Options:
    //   boolean ignoreMissingPackages, boolean ignoreMissingInferfaces,

    public static TestOptions SINGLE_OPTIONS =
        new TestOptions(
            "Single", "Single Threaded Scan",
            !USE_JANDEX, SCAN_THREADS_DEFAULT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions SINGLE_JANDEX_OPTIONS =
        new TestOptions(
            "SingleJandex", "Single Threaded Scan Using Jandex Reads",
            USE_JANDEX, SCAN_THREADS_DEFAULT,
            !DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions MULTI_OPTIONS =
        new TestOptions(
            "Multi", "Multi Threaded Scan",
            !USE_JANDEX, SCAN_THREADS_MULTI,
            !DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_JANDEX_OPTIONS =
        new TestOptions(
            "MultiJandex", "Multi Threaded Scan Using Jandex Reads",
            USE_JANDEX, SCAN_THREADS_MULTI,
            !DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_WRITE_OPTIONS =
        new TestOptions(
            "SingleWrite", "Single Threaded Scan with Initial Write",
            !USE_JANDEX, SCAN_THREADS_DEFAULT,
            SINGLE_WRITE_SUFFIX, DO_CLEAN_STORAGE,
            READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
            !DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions SINGLE_READ_OPTIONS =
        new TestOptions(
            "SingleRead", "Single Threaded Scan with Read",
            !USE_JANDEX, SCAN_THREADS_DEFAULT,
            SINGLE_WRITE_SUFFIX, !DO_CLEAN_STORAGE,
            READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
            !DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions MULTI_WRITE_OPTIONS =
        new TestOptions(
            "MultiWrite", "Multi Threaded Scan with Initial Write",
            !USE_JANDEX, SCAN_THREADS_MULTI,
            MULTI_WRITE_SUFFIX, DO_CLEAN_STORAGE,
            READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
            !DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_READ_OPTIONS =
        new TestOptions("MultiRead", "Multi Threaded Scan with Read",
            !USE_JANDEX, SCAN_THREADS_MULTI,
            MULTI_WRITE_SUFFIX, !DO_CLEAN_STORAGE,
            READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
            !DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_WRITE_ASYNC_OPTIONS =
        new TestOptions(
            "SingleWriteAsync", "Single Threaded Scan with Asynchronous Initial Write",
            !USE_JANDEX, SCAN_THREADS_DEFAULT,
            SINGLE_WRITE_ASYNC_SUFFIX, DO_CLEAN_STORAGE,
            READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
            !DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    public static TestOptions MULTI_WRITE_ASYNC_OPTIONS =
        new TestOptions(
            "MultiWriteAsync", "Multi Threaded Scan with Asynchronous Initial Write",
            !USE_JANDEX, SCAN_THREADS_MULTI,
            MULTI_WRITE_ASYNC_SUFFIX, DO_CLEAN_STORAGE,
            READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_MULTI,
            !DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
            !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_WRITE_JANDEX_FORMAT_OPTIONS =
            new TestOptions(
                "SingleWriteUsingJandex", "Write using Jandex Format",
                !USE_JANDEX, SCAN_THREADS_DEFAULT,
                SINGLE_WRITE_JANDEX_FORMAT_SUFFIX, DO_CLEAN_STORAGE,
                READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
                DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
                !DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_READ_JANDEX_FORMAT_OPTIONS =
            new TestOptions(
                "SingleReadUsingJandex", "Read using Jandex Format",
                !USE_JANDEX, SCAN_THREADS_DEFAULT,
                SINGLE_WRITE_JANDEX_FORMAT_SUFFIX, !DO_CLEAN_STORAGE,
                READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
                DO_USE_JANDEX_FORMAT, !DO_USE_BINARY_FORMAT,
                !DO_IGNORE_MISSING_PACKAGES, DO_IGNORE_MISSING_INTERFACES);
    
    public static TestOptions SINGLE_WRITE_BINARY_FORMAT_OPTIONS =
            new TestOptions(
                "SingleWriteUsingBinary", "Write using Binary Format",
                !USE_JANDEX, SCAN_THREADS_DEFAULT,
                SINGLE_WRITE_BINARY_FORMAT_SUFFIX, DO_CLEAN_STORAGE,
                READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
                !DO_USE_JANDEX_FORMAT, DO_USE_BINARY_FORMAT, 
                !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);

    public static TestOptions SINGLE_READ_BINARY_FORMAT_OPTIONS =
            new TestOptions(
                "SingleReadUsingBinary", "Read using Binary Format",
                !USE_JANDEX, SCAN_THREADS_DEFAULT,
                SINGLE_WRITE_BINARY_FORMAT_SUFFIX, !DO_CLEAN_STORAGE,
                READ_ONLY_DEFAULT, ALWAYS_VALID_DEFAULT, WRITE_THREADS_DEFAULT,
                !DO_USE_JANDEX_FORMAT, DO_USE_BINARY_FORMAT,
                !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);
    
    public static TestOptions SINGLE_READ_BINARY_FORMAT_VALID_OPTIONS =
            new TestOptions(
                "SingleReadUsingBinaryValid", "Read using Binary Format, Always Valid",
                !USE_JANDEX, SCAN_THREADS_DEFAULT,
                SINGLE_WRITE_BINARY_FORMAT_SUFFIX, !DO_CLEAN_STORAGE,
                READ_ONLY_DEFAULT, ALWAYS_VALID, WRITE_THREADS_DEFAULT,
                !DO_USE_JANDEX_FORMAT, DO_USE_BINARY_FORMAT,
                !DO_IGNORE_MISSING_PACKAGES, !DO_IGNORE_MISSING_INTERFACES);    
}
