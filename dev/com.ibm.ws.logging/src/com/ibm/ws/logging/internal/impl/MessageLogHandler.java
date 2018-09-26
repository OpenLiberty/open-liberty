/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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

public class MessageLogHandler extends JsonLogHandler implements SynchronousHandler, Formatter {

    private TraceWriter traceWriterOriginal;

    public static final String COMPONENT_NAME = "com.ibm.ws.logging.internal.impl.MessageLogHandler";

    private String format = LoggingConstants.DEFAULT_MESSAGE_FORMAT;
    private BaseTraceFormatter basicFormatter = null;

    public MessageLogHandler(String serverName, String wlpUserDir, List<String> sourcesList) {
        super(serverName, wlpUserDir, sourcesList);
    }

    @Override
    public String getHandlerName() {
        return COMPONENT_NAME;
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
        String currFormat = format;
        /*
         * Given an 'object' we must determine what type of log event it originates from.
         * Knowing that it is a *Data object, we can figure what type of source it is.
         */
        GenericData genData = null;
        if (event instanceof GenericData) {
            genData = (GenericData) event;
        } else {
            throw new IllegalArgumentException("event not an instance of GenericData");
        }

        String eventSourceName = getSourceNameFromDataObject(genData);

        String messageOutput = null;
        if (currFormat.equals(LoggingConstants.JSON_FORMAT) || !eventSourceName.equals(CollectorConstants.MESSAGES_SOURCE)) {
            if (genData.getJsonMessage() == null) {
                genData.setJsonMessage((String) formatEvent(eventSourceName, CollectorConstants.MEMORY, event, null, MAXFIELDLENGTH));
            }
            messageOutput = genData.getJsonMessage();

        } else if (currFormat.equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT) && basicFormatter != null) {
            messageOutput = basicFormatter.messageLogFormat(genData);

        }
        if (messageOutput != null && traceWriter != null) {
            traceWriter.writeRecord(messageOutput);
        }

    }

    /**
     * Set BaseTraceFormatter passed from BaseTraceService
     * This formatter is used to format the BASIC log events
     * that pass through
     *
     * @param formatter the BaseTraceFormatter to use
     */
    public void setBasicFormatter(BaseTraceFormatter formatter) {
        this.basicFormatter = formatter;
    }

    /**
     * The format to set (i.e. BASIC or JSON)
     *
     * @param format the format to set (i.e. BASIC or JSON)
     */
    public void setFormat(String format) {
        this.format = format;
    }

}
