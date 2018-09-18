/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package http2.test.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class
 */
@WebServlet("/H2MultiDataFrame")
public class H2MultiDataFrame extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static Object sync1 = new Object();
    public static int servletInstance = 1;

    public static int SERVLET_INSTANCES = 3; // Parallel H2 Connections
    public static int MINUTES_PER_STREAM = 2;

    public static int WAIT_BETWEEN_ITERATIONS_MSEC = 1000;
    public static int PING_INTERVAL = 27;
    public static int WINDOW_UPDATE_INTERVAL = 37; // make this large for long running stress tests, based on DATA_ITERATION_LIMIT
    public static int DATA_WRITES_PER_MINUTE = 60000 / WAIT_BETWEEN_ITERATIONS_MSEC; // 60000 msec/min divided msec wait interval.

    public static int DATA_ITERATION_LIMIT = DATA_WRITES_PER_MINUTE * MINUTES_PER_STREAM;
    // public static int DATA_ITERATION_LIMIT = 50000; // use this and make it large for long running stress tests;

    public static long startTime = 0;
    public static long totalStreams = 0;
    public static long rateSum = 0;

    public static CountDownLatch servletStartCount = new CountDownLatch(SERVLET_INSTANCES);

    private static String sendBackPriority1 = "SEND.BACK.PRIORITY.1";
    private static String sendBackWinUpdate1 = "SEND.BACK.WINDOW.UPDATE.1";
    private static String sendBackPing1 = "SEND.BACK.PING.1";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String s = "FRAME.DATA.";
        int myStreamID = 0;
        long streamStartTime = 0;
        int sleepCount = 0;
        int WRITES_BETWEEN_SLEEP = 100;
        long SLEEP_TIME = 100;
        long elapsedTime = 0;
        long streamTime = 0;

        // these are used for printing out ending statistics
        long divider = 0;
        long rate = 0;

        synchronized (sync1) {

            // servlet can't deduce stream id, since a new connection could have been made for this instance.
            // So stream ID is really just the absolute number of this stream, NOT stream per connection.
            // myStreamID = (servletInstance * 2) - 1;
            myStreamID = servletInstance;

            servletInstance++;
            if (startTime == 0) {
                // startTime of first servlet to run
                startTime = System.currentTimeMillis();
                System.out.println("Start ID: " + myStreamID + " Test started at: " + startTime);
                System.out.println("Writes per Stream: " + DATA_ITERATION_LIMIT
                                   + " Connection Instances: " + SERVLET_INSTANCES
                                   + " Writes Between Sleep: " + WRITES_BETWEEN_SLEEP
                                   + " Sleep Time (msec): " + SLEEP_TIME);
            } else {
                System.out.println("Start ID: " + myStreamID + " at: " + System.currentTimeMillis());
            }
        }

        streamStartTime = System.currentTimeMillis();

        long time = System.currentTimeMillis();
        response.setDateHeader("Date", time);

        response.getWriter().write("ABC123 servlet hc: " + this.hashCode());
        response.getWriter().flush();

        try {
            servletStartCount.countDown();
            servletStartCount.await();
        } catch (InterruptedException e) {
        }

        for (int i = 1; i <= DATA_ITERATION_LIMIT; i++) {
            if (i < DATA_ITERATION_LIMIT) {
                if ((i % PING_INTERVAL) == 0) {
                    s = sendBackPing1;
                } else if ((i % WINDOW_UPDATE_INTERVAL) == 0) {
                    s = sendBackWinUpdate1;
                } else {
                    s = myStreamID + "-" + i + ".DoNotAdd.";
                }
            } else {
                elapsedTime = System.currentTimeMillis() - startTime;
                streamTime = System.currentTimeMillis() - streamStartTime;
                // could be 0 very early on
                if (elapsedTime == 0) {
                    elapsedTime = 1;
                }
                if (streamTime == 0) {
                    streamTime = 1;
                }
                divider = streamTime - (sleepCount * SLEEP_TIME);

                synchronized (sync1) {

                    if (divider > 0) {
                        totalStreams++;
                        rate = i * 1000 / divider;
                        rateSum += rate;

                        System.out.println("Done: ID: " + myStreamID +
                                           " Stream Rate(writes/sec): " + rate +
                                           " Avg Rate Per Stream: " + rateSum / totalStreams);
                    } else {
                        System.out.println("Finished ID: " + myStreamID + " Write #: " + i);
                    }
                }

                // send last frame as a separate frame, makes comparison logic in the client easier
                try {
                    Thread.sleep(4500);
                } catch (InterruptedException e) {
                }
                s = "LAST.DATA.FRAME";
            }

            try {
                if ((i % 1000) == 0) {
                    // print out every 1000 times we are writing data
                    System.out.println("ID: " + myStreamID + " Writing: " + s);
                }
                response.getWriter().write(s);
                response.getWriter().flush();
                if (response.getWriter().checkError()) {
                    System.out.println("Stream has an error, so leaving");
                    break;
                }

            } catch (NullPointerException x) {
                System.out.println("NullPointer detected, so leaving.  ID: " + myStreamID);

                StringWriter sw = new StringWriter();
                x.printStackTrace(new PrintWriter(sw));
                String stackTrace = sw.toString();
                System.out.println("\n***StackTrace:: " + stackTrace);

                System.exit(0);
            }
            if (response.getWriter().checkError()) {
                System.out.println("Stream has an error, so leaving. ID: " + myStreamID);
                break;
            }

            try {
                if (i >= DATA_ITERATION_LIMIT) {
                    // sleep before closing, to make sure that data makes it to the other side before closing
                    Thread.sleep(4000);
                } else {
                    if (i % WRITES_BETWEEN_SLEEP == 0) {
                        sleepCount++;
                        Thread.sleep(SLEEP_TIME);
                    } else {
                        Thread.yield();
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        System.out.println("Stream is closing. ID: " + myStreamID);
        response.getWriter().close();

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
