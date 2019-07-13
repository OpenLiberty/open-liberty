/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.utils;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TaskIO {
    
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
        return new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS Z]").format( new Date() );
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
