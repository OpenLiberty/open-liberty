/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.util.List;

import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.ws.logging.collector.Formatter;
import com.ibm.ws.logging.internal.impl.BaseTraceService.SystemLogHolder;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 *
 */
public class ConsoleLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.ConsoleLogHandler";
    private SystemLogHolder sysLogHolderOriginal;

    public ConsoleLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    @Override
    public void synchronousWrite(Object event) {
        SystemLogHolder sysLogHolder = sysLogHolderOriginal;
        if (sysLogHolder == null) {
            return;
        }

        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        String evensourcetType = getSourceTypeFromDataObject(event);
        String messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        synchronized (this) {
            sysLogHolder.getOriginalStream().println(messageOutput);
        }
    }

    @Override
    public void setWriter(Object writer) {
        this.sysLogHolderOriginal = (SystemLogHolder) writer;
    }
}
