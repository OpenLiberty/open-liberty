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

/**
 * A WsTraceHandler receives messages and LogRecords, and logs them.
 */
public interface WsTraceHandler {

    /**
     * Log the given log record.
     * 
     * @param routedMessage The LogRecord along with various message formats.
     */
    void publish(RoutedMessage routedMessage);
}
