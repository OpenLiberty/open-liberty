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
package com.ibm.ws.jaxws.ejbbasic.client;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * @author Chunqiang (CQ) Tang, ctang@us.ibm.com
 */
public class Util {
    private static final String NEWLINE = System.getProperty("line.separator");
    private static Logger logger = Logger.getLogger("com.ibm.ws.jaxws.ejbbasic.client.Util");

    /**
     * This capability is only available in Java 1.5.
     */
    public static String getTracesOfAllThreads() {
        final StringBuilder buf = new StringBuilder();
        final Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
        final java.util.Iterator<Entry<Thread, StackTraceElement[]>> iterator = map.entrySet().iterator();

        while (iterator.hasNext()) {
            final Entry<Thread, StackTraceElement[]> threadEntry = iterator.next();
            final Thread thread = threadEntry.getKey();
            final StackTraceElement[] trace = threadEntry.getValue();
            if ((trace != null) && (trace.length > 0)) {
                buf.append("\n------ID:");
                buf.append(thread.getId());
                buf.append("---Name:");
                buf.append(thread.getName());
                buf.append("---State:");
                buf.append(thread.getState());
                buf.append("---isDaemon:");
                buf.append(thread.isDaemon());
                buf.append("---Priority:");
                buf.append(thread.getPriority());
                buf.append("---------------------------------------\n");
                for (final StackTraceElement element : trace) {
                    buf.append(element);
                    buf.append("\n");
                }
            }
        }

        buf.append("\n\n");
        return buf.toString();
    }

    public static boolean deleteDir(final String dirName) {
        return deleteDir(new File(dirName));
    }

    /**
     * Recursively delete a directory.
     *
     * @return true if successful.
     **/
    public static boolean deleteDir(final File dir) {
        if (dir.isDirectory()) {
            final String[] children = dir.list();
            for (final String element : children) {
                final boolean success = deleteDir(new File(dir, element));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }

    public static void deleteFilesWithPrefix(final String dirName, final String filePrefix) {
        final File dir = new File(dirName);
        if (!dir.exists()) {
            return;
        }
        if (!dir.isDirectory()) {
            return;
        }

        final String files[] = dir.list();
        if (files == null) {
            return;
        }

        for (final String file2 : files) {
            if (file2.startsWith(filePrefix)) {
                final File file = new File(dirName + File.separator + file2);
                if (file.isDirectory()) {
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
        }
    }

    public static String fileWithPrefixExists(final String dirName, final String filePrefix) {
        final File dir = new File(dirName);
        if (!dir.exists()) {
            return null;
        }
        if (!dir.isDirectory()) {
            return null;
        }

        final String files[] = dir.list();
        if (files == null) {
            return null;
        }
        for (final String file : files) {
            if (file.startsWith(filePrefix)) {
                return file;
            }
        }
        return null;
    }

    public synchronized static void dumpJvmState() {
        logger.info(getTracesOfAllThreads());
        final String pid = System.getProperty("pid");
        if (pid != null) {
            deleteFilesWithPrefix(".", "javacore.");
            try {
                Runtime.getRuntime().exec("kill -3 " + pid);
                while (true) {
                    final String javacore = fileWithPrefixExists(".", "javacore.");
                    if (javacore != null) {
                        long prevLen = -1;
                        while (true) {
                            Thread.sleep(10000);
                            final long newLen = (new File(javacore)).length();
                            if (newLen == prevLen) {
                                return; // File is not growing; dump finished.
                            }
                            prevLen = newLen;
                        }
                    }
                    Thread.sleep(5000);
                }
            } catch (final Exception e) {
                logger.info(e.getMessage());
            }
        }
    }

}
