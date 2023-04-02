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
 * Message routing service. Routes messages to sundry logging streams.
 */
public interface MessageRouter {

    /**
     * Route the given message.
     * 
     * @param msg The fully formatted message.
     * @param logRecord The LogRecord associated with the message.
     * 
     * @return true if the message may be logged normally by the caller,
     *         (in addition to whatever logging was performed under this
     *         method), if desired.
     */
    public boolean route(String msg, LogRecord logRecord);
}