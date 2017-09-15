/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejs.ras;

import java.util.logging.LogRecord;

import com.ibm.websphere.ras.DataFormatHelper;

public class RasHelper {
    // @start_class_string_prolog@
    public static final String $sccsid = "@(#) 1.3 SERV1/ws/code/ras.lite/src/com/ibm/ejs/ras/RasHelper.java, WAS.ras.lite, WAS80.SERV1, kk1041.02 07/08/30 15:32:50 [10/22/10 01:28:54]";

    // @end_class_string_prolog@

    public static boolean isServer() {
        return false;
    }

    public static String getThreadId() {
        return DataFormatHelper.getThreadId();
    }

    public static String getThreadId(LogRecord logRecord) {
        return DataFormatHelper.getThreadId();
    }

    public static String getVersionId() {
        return "";
    }

    public static String getServerName() {
        return "";
    }

    public static String getProcessId() {
        return "";
    }

    public final static String throwableToString(Throwable t) {
        return DataFormatHelper.throwableToString(t);
    }
}

// End of file
