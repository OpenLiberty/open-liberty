/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.logging;

import java.util.logging.LogRecord;

/**
 * Encapsulates a LogRecord and its various message formats.
 */
public interface RoutedMessage {

    /**
     * TODO
     */
    public String getFormattedMsg();

    /**
     * 
     */
    public String getFormattedVerboseMsg();

    /**
     * 
     */
    public String getMessageLogFormat();

    /**
     * 
     */
    public LogRecord getLogRecord();
}
