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
package com.ibm.ws.product.utility;

public interface CommandConsole {
    /**
     * Determines if the Console is currently available.
     * 
     * @return true if the Console is available, false otherwise.
     */
    public boolean isInputStreamAvailable();

    /**
     * Reads text from the input String, prompting with the given String.
     * The values entered on the console are masked (not echoed back to
     * the console).
     * 
     * @param prompt
     * @return String read from input.
     */
    public String readMaskedText(String prompt);

    /**
     * Reads text from the input String, prompting with the given String.
     * The values entered on the console are NOT masked
     * 
     * @param prompt
     * @return String read from input.
     */
    public String readText(String prompt);

    /**
     * Print message to standard output stream
     * 
     * @param message
     */
    public void printInfoMessage(String message);

    /**
     * Print message to standard output stream with line separator
     * 
     * @param message
     */
    public void printlnInfoMessage(String message);

    /**
     * Print message to standard error output stream
     * 
     * @param errorMessage
     */
    public void printErrorMessage(String errorMessage);

    /**
     * Print message to standard error output stream with line separator
     * 
     * @param errorMessage
     */
    public void printlnErrorMessage(String errorMessage);
}
