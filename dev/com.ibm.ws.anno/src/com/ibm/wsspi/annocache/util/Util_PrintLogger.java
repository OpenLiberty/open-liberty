/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.util;

import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface Util_PrintLogger {

    Logger getLogger();
    PrintWriter getWriter();

    boolean isLoggable(Level level);

    void logp(Level level, String className, String methodName, String message);
    void logp(Level level, String className, String methodName, String message, Object... parms);
}
