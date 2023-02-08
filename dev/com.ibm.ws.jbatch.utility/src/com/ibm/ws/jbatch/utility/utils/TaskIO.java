/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.jbatch.utility.utils;

import java.io.PrintStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TaskIO {
    
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("'['uuuu/MM/dd HH:mm:ss.SSS Z']'");

    /**
     * STDOUT/STDOUT/STDERR streams.
     */
    private ConsoleWrapper stdin;
    private PrintStream stdout;
    private PrintStream stderr;
    
    /**
     * CTOR.
     */
    public TaskIO(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr) {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public ConsoleWrapper getStdin() {
        return stdin;
    }

    public PrintStream getStdout() {
        return stdout;
    }
    
    /**
     * @param message a message to issue to stdout.
     */
    public void info(String message) {
        stdout.println(getTimestamp() + " " + message);
    }
    
    /**
     * @return a timestamp string for messages
     */
    public static String getTimestamp() {
        return dateTimeFormatter.format(ZonedDateTime.now());
    }

    /**
     * Prompt for masked input (e.g. a password).
     * 
     * @return the input
     */
    public String promptForMaskedInput(String prompt) {
        return getStdin().readMaskedText(prompt);
    }
}
