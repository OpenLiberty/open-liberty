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
package componenttest.topology.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Pipes data from a PrintStream (usually System.out or System.err)
 * to java.util.logging
 */
public class JULPipe extends PrintStream {

    private final Logger log;

    public JULPipe() {
        this(System.out, Logger.GLOBAL_LOGGER_NAME);
    }

    /**
     * @param original The PrintStream to wrap and pipe to Log4j
     * @param name     The name of the logger to use
     */
    public JULPipe(PrintStream original, String name) {
        super(original);
        log = Logger.getLogger(name);
    }

    @Override
    public void write(byte[] b) throws IOException {
        String msg = new String(b);
        if (msg != null && msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 2);
        log.info(msg);
    }

    @Override
    public void println() {
        log.info("\n");
    }

    @Override
    public void println(char x) {
        log.info("" + x);
    }

    @Override
    public void println(boolean x) {
        log.info("" + x);
    }

    @Override
    public void println(char[] x) {
        log.info(Arrays.toString(x));
    }

    @Override
    public void println(double x) {
        log.info("" + x);
    }

    @Override
    public void println(float x) {
        log.info("" + x);
    }

    @Override
    public void println(int x) {
        log.info("" + x);
    }

    @Override
    public void println(long x) {
        log.info("" + x);
    }

    @Override
    public void println(Object x) {
        log.info("" + x);
    }

    @Override
    public void println(String x) {
        log.info("" + x);
    }

}
