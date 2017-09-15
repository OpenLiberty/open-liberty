/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.timedexit.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 *
 */
public class TimedExitThread extends Thread {
    private static class TimedExitException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TimedExitException() {
            super("Server was forced to shutdown by timed exit.");
        }
    }

    private volatile long timeoutMillis = TimeUnit.MILLISECONDS.convert(120, TimeUnit.MINUTES);

    private volatile boolean keepgoing = true;

    private final Object flag = new Object() {};

    public void setTimeout(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public TimedExitThread() {
        super.setName("TimedExitThread");
        setDaemon(true);
    }

    @Override
    public void run() {
        boolean generateFFDC = true;
        // We perform two timed exits: first if the process has been running too
        // long, and second if System.exit has been running too long.
        for (int tries = 0; tries < 2; tries++) {
            long startTimeMillis = System.currentTimeMillis();
            long startTimeNanos = System.nanoTime();
            long endTimeNanos = -1;
            long timeoutMillis = -1;

            synchronized (flag) {
                while (keepgoing) {
                    timeoutMillis = this.timeoutMillis;
                    endTimeNanos = System.nanoTime();
                    long durationNanos = endTimeNanos - startTimeNanos;
                    long durationMillis = TimeUnit.MILLISECONDS.convert(durationNanos, TimeUnit.NANOSECONDS);
                    if (durationMillis >= timeoutMillis) {
                        break;
                    }

                    long waitMillis = timeoutMillis - durationMillis;
                    try {
                        flag.wait(waitMillis);
                    } catch (InterruptedException ie) {
                        // Just repeat the loop
                    }
                }
            }

            // Now exit (if we haven't been cancelled)
            if (keepgoing) {
                boolean canBeGentle = false;

                try {
                    long endTime = System.currentTimeMillis();
                    System.out.println("+-----------------------------------------------------------------------+");
                    System.out.println("| This Liberty server has been " + (tries == 0 ? "running" : "exiting") + " for too long");
                    System.out.println("| (Started at epoch time of " + startTimeMillis + " and nano time of " + startTimeNanos + ")");
                    System.out.println("| (Timed exit time (of " + timeoutMillis + " ms) exceeded at epoch time of " + endTime + " and nano time of " + endTimeNanos + ")");

                    // On IBM JVMs, take a javacore
                    try {
                        Class<?> JavacoreClass = Class.forName("com.ibm.jvm.Dump", true, null);
                        Method javaDump = JavacoreClass.getMethod("JavaDump");
                        Method heapDump = JavacoreClass.getMethod("HeapDump");
                        javaDump.invoke(null);
                        heapDump.invoke(null);
                    } catch (ClassNotFoundException e) {
                        // Ignore - assume we're not on an IBM JVM
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    // How violent do we need to be???
                    ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
                    if (mbean.isObjectMonitorUsageSupported()) {
                        long[] deadlockedThreads = mbean.findDeadlockedThreads();
                        if (deadlockedThreads != null && deadlockedThreads.length != 0) {
                            ThreadInfo[] infos = mbean.getThreadInfo(deadlockedThreads, true, true);
                            System.out.println("| DEADLOCK DETECTED BY THE JVM, thread details:");
                            for (ThreadInfo info : infos) {
                                System.out.println(info);
                                if (info != null) {
                                    System.out.println("Waiting for " + info.getLockName() + " (held by thread ID " + info.getLockOwnerName() + "(id=" + info.getLockOwnerId()
                                                       + ")");
                                    System.out.println("Monitors held by this thread: " + Arrays.toString(info.getLockedMonitors()));
                                    System.out.println("Synchronizers held by this thread: " + Arrays.toString(info.getLockedSynchronizers()));
                                }
                                System.out.println(".............................................................");
                            }
                        } else if (tries < 1) {
                            canBeGentle = true;
                        }
                    }
                } finally {
                    try {
                        if (generateFFDC) {
                            // only do this once.  This is to force a failure in FATs
                            FFDCFilter.processException(new TimedExitException(), TimedExitThread.class.getName(), "run");
                            generateFFDC = false;
                        }
                        if (canBeGentle) {
                            canBeGentle = false; // In case System.out.print has problems!
                            System.out.println("| To attempt the timed exit, System.exit will be called");
                            System.out.println("+-----------------------------------------------------------------------+");
                            canBeGentle = true;
                        } else {
                            System.out.println("| To attempt the timed exit, Runtime.halt will be called");
                            System.out.println("+-----------------------------------------------------------------------+");
                            System.out.flush(); // Need to ensure all output is done BEFORE we vapourise ourselves
                        }
                    } finally {
                        if (canBeGentle) {
                            // System.exit might hang, so attempt that in
                            // another thread and then do another timed exit.
                            try {
                                new Thread(getName() + " System.exit") {
                                    @Override
                                    public void run() {
                                        System.exit(1);
                                    }
                                }.start();
                                continue;
                            } catch (Throwable t) {
                                t.printStackTrace();
                                System.err.flush();
                            }
                        }

                        Runtime.getRuntime().halt(1);
                    }
                }
            }
        }
    }

    public void cancelCountdown() {
        keepgoing = false;
        synchronized (flag) {
            flag.notifyAll();
        }
    }

    // Unit test code - run as Java application from your eclipse workspace
    public static void main(String[] args) {
        final boolean testTimeout = true;
        final boolean testDeadlock = false;
        final boolean testShutdown = true;
        final long timeout = 1000;

        final Object lock1 = new Object();
        final Object lock2 = new Object();

        final Thread thread1 = new Thread() {
            @Override
            public void run() {
                synchronized (lock1) {
                    System.out.println("Thread1 has lock1");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    };
                    synchronized (lock2) {
                        System.out.println("Thread1 has both locks");
                    }
                }
            }
        };
        final Thread thread2 = new Thread() {
            @Override
            public void run() {
                synchronized (lock2) {
                    System.out.println("Thread2 has lock2");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    };
                    synchronized (lock1) {
                        System.out.println("Thread2 has both locks");
                    }
                }
            }
        };
        final Thread shutdownThread = new Thread() {
            @Override
            public void run() {
                System.out.println("Shutdown hook sleeping");
                try {
                    Thread.sleep(timeout * 3);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                } finally {
                    System.out.println("Shutdown hook finished");
                }
            }
        };

        final TimedExitThread tet = new TimedExitThread();
        tet.setTimeout(timeout);

        if (testDeadlock) {
            thread1.start();
            thread2.start();
        }

        if (testShutdown) {
            Runtime.getRuntime().addShutdownHook(shutdownThread);
        }

        tet.start();

        if (testTimeout) {
            System.out.println("main() sleeping");
            try {
                Thread.sleep(timeout * 2);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            } finally {
                System.out.println("main() finished");
            }
        }
    }
}
