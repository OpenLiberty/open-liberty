/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.logging;

import java.util.Locale;

/**
 * An extension to java.util.logging.LogRecord.
 * 
 * This interface is implemented by LogRecords cut from the WLP logging code.
 */
public interface LogRecordExt {

    /**
     * Retrieve the formatted message for this LogRecord from the given Locale,
     * using the resource bundle, message, and parameters associated with this
     * LogRecord.
     * 
     * Note: this method assumes the LogRecord message (as returned by getMessage())
     * is a key that references a message in the resource bundle.
     * 
     * @param locale The desired Locale.
     * 
     * @return The formatted message for this LogRecord in the given Locale.
     */
    public String getFormattedMessage(Locale locale);
}
