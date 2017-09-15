/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.exceptions;

import java.sql.DataTruncation;

/**
 * This class overrides java.sql.DataTruncation to allow a message to be specified.
 */
public class WSDataTruncation extends DataTruncation
{
    private static final long serialVersionUID = -2697519459662430640L;
    /** The exception message. */
    String message;

    /**
     * Construct a data truncation error.
     * 
     * @param message the exception message.
     * @param index the index of the parameter or column value.
     * @param isParameter indicates if the truncated value was a parameter (not a column).
     * @param isRead indicates if a read operation was truncated (vs a write operation)
     * @param dataSize the original size of the data.
     * @param transferSize the size after truncation.
     */
    public WSDataTruncation(String message, int index, boolean isParameter, boolean isRead, int dataSize, int transferSize) {
        super(index, isParameter, isRead, dataSize, transferSize);
        this.message = message;
    }

    /**
     * @return the exception message.
     */
    @Override
    public String getMessage() {
        return message;
    }
}
