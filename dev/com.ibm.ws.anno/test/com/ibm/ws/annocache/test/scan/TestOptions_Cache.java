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

import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;

public class TestOptions_Cache {
	public static final boolean DO_CLEAN_STORAGE = true;
	public static final boolean IS_READ_ONLY = true;
    public static final boolean IS_ALWAYS_VALID = true;

    public static final int WRITE_THREADS_UNBOUNDED = TargetCache_Options.WRITE_THREADS_UNBOUNDED;
    public static final int WRITE_THREADS_MAX = TargetCache_Options.WRITE_THREADS_MAX;
    public static final int WRITE_THREADS_DEFAULT = TargetCache_Options.WRITE_THREADS_DEFAULT;

    public static final boolean DO_USE_JANDEX_FORMAT = true;

    public TestOptions_Cache(
        String storageSuffix,
        boolean cleanStorage,

        boolean readOnly,
        boolean alwaysValid,

        int writeThreads,

        boolean useJandexFormat,
        boolean useBinaryFormat) {

        this.storageSuffix = storageSuffix;
        this.cleanStorage = cleanStorage;

        this.readOnly = readOnly;
        this.alwaysValid = alwaysValid;

        this.writeThreads = writeThreads;

        this.useJandexFormat = useJandexFormat;
        this.useBinaryFormat = useBinaryFormat;
    }

    public final String storageSuffix;
    public final boolean cleanStorage;

    public final boolean readOnly;
    public final boolean alwaysValid;

    public final int writeThreads;

    public final boolean useJandexFormat;
    public final boolean useBinaryFormat;
}
