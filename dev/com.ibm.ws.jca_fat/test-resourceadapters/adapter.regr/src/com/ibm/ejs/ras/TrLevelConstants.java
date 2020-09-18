/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

import java.util.logging.Level;

/**
 *
 */
public class TrLevelConstants {

    // Mapping of trace level strings to distinct level groups. The index of the
    // group in the array will be used as an index to the levels array to
    // retrieve
    // the logging Level
    String[][] traceLevels = { { "all", "dump" }, // = 0 0,1
                               { "finest", "debug" }, // = 1 2,3
                               { "finer", "entryExit" }, // = 2 4,5
                               { "fine", "event" }, // = 3 6,7
                               { "detail" }, // = 4 8
                               { "config" }, // = 5 9
                               { "info" }, // = 6 10
                               { "audit" }, // = 7 11
                               { "warning" }, // = 8 12
                               { "severe", "error" }, // = 9 13,14
                               { "fatal" }, // = 10 15
                               { "off" } // = 11 16
    };

    int TRACE_LEVEL_DUMP = 0;
    static int TRACE_LEVEL_DEBUG = 1;
    static int TRACE_LEVEL_ENTRY_EXIT = 2;
    static int TRACE_LEVEL_EVENT = 3;
    int TRACE_LEVEL_DETAIL = 4;
    int TRACE_LEVEL_CONFIG = 5;
    int TRACE_LEVEL_INFO = 6;
    int TRACE_LEVEL_AUDIT = 7;
    int TRACE_LEVEL_WARNING = 8;
    int TRACE_LEVEL_ERROR = 9;
    int TRACE_LEVEL_FATAL = 10;
    int TRACE_LEVEL_OFF = 11;

    int SPEC_TRACE_LEVEL_OFF = 16;

    String TRACE_ENABLED = "enabled", TRACE_DISABLED = "disabled",
                    TRACE_ON = "on", TRACE_OFF = "off";

    // An array of distinct trace levels. The trace specification is converted
    // into a
    // distinct index using the traceLevels array. That index is used to
    // retrieve the actual
    // trace Level from this array.
    Level[] levels = { Level.ALL, // all, dump
                       Level.FINEST, // finest, debug
                       Level.FINER, // finer, entryexit
                       Level.FINE, // fine, event
                       Level.CONFIG, // config
                       Level.INFO, // info
                       Level.WARNING, // warning
                       Level.SEVERE, // severe, error
                       Level.OFF }; // off
}
