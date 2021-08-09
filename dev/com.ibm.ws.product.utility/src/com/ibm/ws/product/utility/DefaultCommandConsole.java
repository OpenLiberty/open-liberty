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

import java.io.Console;
import java.io.IOError;
import java.io.PrintStream;
import java.text.MessageFormat;

public class DefaultCommandConsole implements CommandConsole {

    private final Console console;

    private final PrintStream stderr;

    private final PrintStream stdout;

    public DefaultCommandConsole(Console console, PrintStream stdout, PrintStream stderr) {
        this.console = console;
        this.stderr = stderr;
        this.stdout = stdout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInputStreamAvailable() {
        return console != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readMaskedText(String prompt) {
        if (!isInputStreamAvailable()) {
            stderr.println(CommandConstants.PRODUCT_MESSAGES.getString("ERROR_INPUT_CONSOLE_NOT_AVAILABLE"));
            return null;
        }
        try {
            char[] in = console.readPassword(prompt);
            if (in == null) {
                return null;
            } else {
                return String.valueOf(in);
            }
        } catch (IOError e) {
            stderr.println(MessageFormat.format(CommandConstants.PRODUCT_MESSAGES.getString("ERROR_UNABLE_READ_FROM_CONSOLE"), e.getMessage()));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String readText(String prompt) {
        if (!isInputStreamAvailable()) {
            stderr.println(CommandConstants.PRODUCT_MESSAGES.getString("ERROR_INPUT_CONSOLE_NOT_AVAILABLE"));
            return null;
        }
        try {
            return console.readLine(prompt);
        } catch (IOError e) {
            stderr.println(MessageFormat.format(CommandConstants.PRODUCT_MESSAGES.getString("ERROR_UNABLE_READ_FROM_CONSOLE"), e.getMessage()));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printInfoMessage(String message) {
        stdout.print(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printlnInfoMessage(String message) {
        stdout.println(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printErrorMessage(String errorMessage) {
        stderr.print(errorMessage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printlnErrorMessage(String errorMessage) {
        stderr.println(errorMessage);
    }
}
