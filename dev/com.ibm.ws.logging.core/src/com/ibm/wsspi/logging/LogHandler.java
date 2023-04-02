/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package com.ibm.wsspi.logging;

import java.util.logging.LogRecord;

/**
 * A LogHandler receives messages and LogRecords, and logs them.
 */
public interface LogHandler {

    /**
     * Log the given log record.
     * 
     * @param msg The fully formatted message, derived from the given LogRecord.
     * @param logRecord The LogRecord.
     */
    void publish(String msg, LogRecord logRecord);
}
