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

    public static int SERVLET_INSTANCES = 3;
    public static int MINUTES_PER_STREAM = 2;

    public static int WAIT_BETWEEN_ITERATIONS_MSEC = 1000;
    public static int PING_INTERVAL = 27;
    public static int WINDOW_UPDATE_INTERVAL = 37;
    public static int DATA_WRITES_PER_MINUTE = 60000 / WAIT_BETWEEN_ITERATIONS_MSEC; // 60000 msec/min divided msec wait interval.

    public static int DATA_ITERATION_LIMIT = DATA_WRITES_PER_MINUTE * MINUTES_PER_STREAM;
    // public static int DATA_ITERATION_LIMIT = 100;

    public static CountDownLatch servletStartCount = new CountDownLatch(SERVLET_INSTANCES);

    private static String sendBackPriority1 = "SEND.BACK.PRIORITY.1";
    private static String sendBackWinUpdate1 = "SEND.BACK.WINDOW.UPDATE.1";
    private static String sendBackPing1 = "SEND.BACK.PING.1";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        int myStreamID = 0;

        synchronized (sync1) {
            myStreamID = (servletInstance * 2) - 1;
            servletInstance++;
        }

        long time = System.currentTimeMillis();
        response.setDateHeader("Date", time);

        response.getWriter().write("ABC123 servlet hc: " + this.hashCode());
        response.getWriter().flush();

        try {
            servletStartCount.countDown();
            servletStartCount.await();
        } catch (InterruptedException e) {
        }

        String s = "FRAME.DATA.";
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
                // send last frame as a separate frame, makes comparison logic in the client easier
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                s = myStreamID + ".LAST.DATA.FRAME";
            }

            if (i == DATA_ITERATION_LIMIT) {
                System.out.println("servlet: myStreamID: " + myStreamID + " writing last data frame");
            }

            response.getWriter().write(s);

            if (i == DATA_ITERATION_LIMIT) {
                System.out.println("servlet: myStreamID: " + myStreamID + " flushing last data frame");
            }

            response.getWriter().flush();

            try {
                Thread.sleep(WAIT_BETWEEN_ITERATIONS_MSEC);
            } catch (InterruptedException e) {
            }

        }

        System.out.println("servlet: myStreamID: " + myStreamID + " closing");

        response.getWriter().close();

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
