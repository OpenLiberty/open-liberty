/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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

package com.ibm.ws.logging.object.hpel;

import java.util.logging.LogRecord;

import com.ibm.ws.logging.internal.WsLogRecord;

public class HpelLogRecordFactory {

    /**
     * If logRecord is convertible to WsLogRecord then convert and return
     * Otherwise return null
     * 
     * @param logRecord
     * @return WsLogRecord
     */
    public static WsLogRecord getWsLogRecordIfConvertible(LogRecord logRecord) {

        if (logRecord instanceof WsLogRecord)
            return (WsLogRecord) logRecord;

        return null;
    }
}
