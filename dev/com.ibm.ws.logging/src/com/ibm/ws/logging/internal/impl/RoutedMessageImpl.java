/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.util.logging.LogRecord;

import com.ibm.ws.logging.RoutedMessage;

/**
 * Wrapper around a LogRecord plus its various message formats.
 */
public class RoutedMessageImpl implements RoutedMessage {

    private final String formattedMsg;
    private final String formattedVerboseMsg;
    private final String messageLogFormat;

    private final LogRecord logRecord;

    public RoutedMessageImpl(String formattedMsg,
                             String formattedVerboseMsg,
                             String messageLogFormat,
                             LogRecord logRecord) {
        this.formattedMsg = formattedMsg;
        this.formattedVerboseMsg = formattedVerboseMsg;
        this.messageLogFormat = messageLogFormat;
        this.logRecord = logRecord;
    }

    @Override
    public String getFormattedMsg() {
        return formattedMsg;
    }

    @Override
    public String getFormattedVerboseMsg() {
        return formattedVerboseMsg;
    }

    @Override
    public String getMessageLogFormat() {
        return messageLogFormat;
    }

    @Override
    public LogRecord getLogRecord() {
        return logRecord;
    }
}