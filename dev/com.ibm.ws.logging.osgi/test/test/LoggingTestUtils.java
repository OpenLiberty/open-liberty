/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.kernel.boot.logging.WsLogManager;
import com.ibm.ws.logging.internal.WsLogger;
import com.ibm.ws.logging.internal.impl.LoggingConstants;

/**
 *
 */
public class LoggingTestUtils {
    public static final void ensureLogManager() {
        // bootProps.get(..) checks initProps, then system props
        System.setProperty("java.util.logging.manager", "com.ibm.ws.kernel.boot.logging.WsLogManager");
        WsLogManager.setWsLogger(WsLogger.class);
    }

    /**
     * Recursively clean all children of the given file
     * 
     * @param tmp
     */
    public static void deepClean(File tmp) {
        if (tmp == null)
            return;

        if (tmp.isDirectory()) {
            File[] files = tmp.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory())
                        deepClean(file);

                    boolean deleted = file.delete();

                    if (!deleted) {
                        String msg = "Unable to delete test file: " + file.getAbsolutePath();
                        throw new IllegalStateException(msg);
                    }
                }
            }
        }

        if (tmp.exists()) {
            boolean deleted = tmp.delete();

            if (!deleted) {
                String msg = "Unable to delete test file: " + tmp.getAbsolutePath();
                throw new IllegalStateException(msg);
            }
        }
    }

    /**
     * FOR DEBUGGING: Dump information about all configured loggers. This can
     * get pretty long!
     */
    public static final void dumpAllLoggers() {
        LogManager lm = LogManager.getLogManager();

        Enumeration<String> list = lm.getLoggerNames();
        while (list.hasMoreElements()) {
            Logger lg = lm.getLogger(list.nextElement());
            dumpLogger(lg);
        }
    }

    /**
     * FOR DEBUGGING: Dump information about selected logger, including listing
     * all configured handlers.
     */
    public static final void dumpLogger(Logger log) {
        System.out.println("Logger: " + log);
        System.out.println("\t\tname: " + log.getName());
        System.out.println("\t\tlevel: " + log.getLevel());
        System.out.println("\t\tfilter: " + log.getFilter());
        System.out.println("\t\tparent: " + log.getParent());

        for (Handler handler : log.getHandlers()) {
            System.out.println("\t\tHandler: " + handler.toString());
            System.out.println("\t\t\tlevel: " + handler.getLevel());
            System.out.println("\t\t\tfilter: " + handler.getFilter());
            System.out.println("\t\t\tformatter: " + handler.getFormatter());
        }
    }

    public static String readFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder("");

        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append(LoggingConstants.nl);
        }

        reader.close();

        return sb.toString();
    }

    public static void setTraceSpec(String trace) {
        try {
            Method m = TrConfigurator.class.getDeclaredMethod("setTraceSpec", String.class);
            m.setAccessible(true);
            m.invoke(null, trace);
        } catch (Exception e) {
            Error error = new AssertionFailedError("Unable to set trace spce");
            error.initCause(e);
            throw error;
        }
    }
}
