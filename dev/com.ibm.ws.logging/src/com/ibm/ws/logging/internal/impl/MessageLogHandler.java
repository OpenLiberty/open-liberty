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
import com.ibm.ws.logging.data.GenericData;
import com.ibm.ws.logging.internal.impl.BaseTraceService.TraceWriter;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

/**
 *
 */
public class MessageLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    private TraceWriter traceWriterOriginal;

    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.MessageLogHandler";

    public MessageLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
    }

    public void setFileLogHolder(TraceWriter trw) {
        traceWriterOriginal = trw;
    }

    @Override
    public void setWriter(Object writer) {
        this.traceWriterOriginal = (TraceWriter) writer;
    }

    @Override
    public void synchronousWrite(Object event) {
        TraceWriter traceWriter = traceWriterOriginal;
        if (traceWriter == null)
            return;

        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        String evensourcetType = getSourceTypeFromDataObject(event);
        String messageOutput = null;
        if (event instanceof GenericData) {
            GenericData genData = (GenericData) event;
            if (genData.getJsonMessage() == null) {
                genData.setJsonMessage((String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH));
            }
            messageOutput = genData.getJsonMessage();

        }
        if (messageOutput == null) {
            messageOutput = (String) formatEvent(evensourcetType, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH);
        }
        if (messageOutput != null) {
            traceWriter.writeRecord(messageOutput);
        }
    }

}
