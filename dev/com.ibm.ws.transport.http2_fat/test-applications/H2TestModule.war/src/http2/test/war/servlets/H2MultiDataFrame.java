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

    // the number of writes at which the servlet will ask for a ping to be sent.
    // set to -1 to not check.
    public int PING_INTERVAL = -1;

    // the number of writes at which the servlet will ask for a new Window Update frame be sent.
    // the DATA frame payload size is about 20, so with a WINDOW_UPDATE_VALUE of 10000, then the interval should be about 500
    // powers of two should be faster to divide with
    // the update interval value is in the client side file of:  com.ibm.ws.http2.test.Constants.java
    private final int WINDOW_UPDATE_INTERVAL = 512;

    // the total number of writes to do per stream. set to -1 to run only time based with variable below
    private final int DATA_ITERATION_LIMIT = 130000; // 400000;

    // total time to take. set to -1 to run only using iteration limit above
    // otherwise set to 1000 * 60 * m, where m is how many minutes to run the test
    private final int DATA_TIME_LIMIT_MSEC = 1000 * 60 * 2;

    private final int WRITES_BETWEEN_SLEEP = 128;
    private final long SLEEP_TIME = 100;

    public static Object sync1 = new Object();
    public static int servletInstance = 1;
    public static long startTime = 0;
    public static long totalStreams = 0;
    public static long rateSum = 0;

    // private static String sendBackPriority1 = "SEND.BACK.PRIORITY.1";
    private static String sendBackWinUpdate1 = "SEND.BACK.WINDOW.UPDATE.1";
    private static String sendBackPing1 = "SEND.BACK.PING.1";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        int myStreamID = 0;
        long streamStartTime = 0;
        int sleepCount = 0;
        long streamTime = 0;

        // these are used for printing out ending statistics
        long divider = 0;
        long rate = 0;

        // because the same object is shared across servlet "instances" - doGet(...) are unique
        int dataIterationLimit = DATA_ITERATION_LIMIT;

        synchronized (sync1) {
            myStreamID = servletInstance;
            servletInstance++;

            if (startTime == 0) {
                // startTime of first servlet to run
                startTime = System.currentTimeMillis();
                System.out.println("this hashcode: " + this.hashCode() + " Start ID: " + myStreamID + " Test started at: " + startTime);
                System.out.println("Writes per Stream: " + dataIterationLimit
                                   + " Time Limit: " + DATA_TIME_LIMIT_MSEC
                                   //+ " Connection Instances: " + STRESS_CONNECTIONS
                                   + " Writes Between Sleep: " + WRITES_BETWEEN_SLEEP
                                   + " Sleep Time (msec): " + SLEEP_TIME);
            } else {
                System.out.println("this hashcode: " + this.hashCode() + " Start ID: " + myStreamID + " at: " + System.currentTimeMillis());
            }
        }

        streamStartTime = System.currentTimeMillis();
        response.setDateHeader("Date", streamStartTime);

        response.getWriter().write("ABC123 servlet hc: " + this.hashCode());
        response.getWriter().flush();

        String s = "FRAME.DATA.";
        for (int i = 1; ((i <= dataIterationLimit) || (dataIterationLimit == -1)); i++) {
            if ((i < dataIterationLimit) || (dataIterationLimit == -1)) {
                if ((i % WINDOW_UPDATE_INTERVAL) == 0) {
                    // don't add to results after the first one
                    if (i == WINDOW_UPDATE_INTERVAL) {
                        s = sendBackWinUpdate1;
                    } else {
                        s = sendBackWinUpdate1 + ".DoNotAdd.";
                    }
                } else if ((PING_INTERVAL != -1) && (i % PING_INTERVAL) == 0) {
                    // don't add to results after the first one
                    if (i == PING_INTERVAL) {
                        s = sendBackPing1;
                    } else {
                        s = sendBackPing1 + ".DoNotAdd.";
                    }
                } else {
                    s = myStreamID + "-" + i + ".DoNotAdd.";
                }
            } else {
                streamTime = System.currentTimeMillis() - streamStartTime;
                // could be 0 very early on
                if (streamTime == 0) {
                    streamTime = 1;
                }
                divider = streamTime - (sleepCount * SLEEP_TIME);
                // just to be paranoid
                if (divider == 0) {
                    divider = 1;
                }

                synchronized (sync1) {
                    totalStreams++;
                    rate = i * 1000 / divider;
                    rateSum += rate;

                    System.out.println("Stats: ID: " + myStreamID + " stream time: " + streamTime / 1000 +
                                       " Stream Rate(w/sec): " + rate +
                                       " Avg Rate Per Stream: " + rateSum / totalStreams);
                }

                // send last frame as a separate frame, makes comparison logic in the client easier
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                s = "LAST.DATA.FRAME";
            }

            try {

                if (s.contains("LAST.DATA.FRAME")) {
                    response.getWriter().write(s);
                    response.getWriter().flush();
                    response.getWriter().close();
                    System.out.println("Closed ID: " + myStreamID + " Close After Writing: " + s + " iterations: " + i);
                    break;
                } else {
                    if (((i % 32768) == 1) && (i != 1)) {
                        // print out every so often.  divide by power of two should be fast if optimized, compare to 1 so we
                        // don't also hit the same interval as the window update or ping
                        System.out.println("ID: " + myStreamID + " Writing: " + s + " streamTime: " + (System.currentTimeMillis() - streamStartTime) / 1000);
                    }
                    response.getWriter().write(s);
                    response.getWriter().flush();
                    if (response.getWriter().checkError()) {
                        System.out.println("Stream has an error, so leaving");
                        break;
                    }

                    // if time is up, then move ahead to writing the last frame and leaving on the next iteration.
                    if ((DATA_TIME_LIMIT_MSEC != -1) &&
                        ((System.currentTimeMillis() - startTime) >= DATA_TIME_LIMIT_MSEC)) {
                        dataIterationLimit = i + 1;
                    }
                }

            } catch (NullPointerException x) {
                System.out.println("NullPointer detected, so leaving.  ID: " + myStreamID);
                StringWriter sw = new StringWriter();
                x.printStackTrace(new PrintWriter(sw));
                String stackTrace = sw.toString();
                System.out.println("\n***StackTrace:: " + stackTrace);
                System.exit(0);
            }

            try {
                if (i % WRITES_BETWEEN_SLEEP == 0) {
                    sleepCount++;
                    Thread.sleep(SLEEP_TIME);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
