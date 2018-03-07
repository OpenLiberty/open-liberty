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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class
 */
@WebServlet("/H2PriorityWindowUpdate1")
public class H2PriorityWindowUpdate1 extends HttpServlet {

    private static final long serialVersionUID = 1L;

    int SEND_DATA_LIMIT = 100;
    String sBoringData = ".DoNotAdd.";
    String sPriortyResponse = "SEND.BACK.PRIORITY.1";
    String sWinUpdateResponse = "SEND.BACK.WINDOW.UPDATE.1";

    int SEND_DATA_LIMIT_PING = 100;
    String sPingResponse = "SEND.BACK.PING.1";

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String q = request.getQueryString();

        System.out.println("Query String is: " + q);
        if (q != null) {
            if ((q.indexOf("testName") != -1) && (q.indexOf("Ping1") != -1)) {
                testPing1(request, response);
                return;
            }
        }

        long time = System.currentTimeMillis();
        response.setDateHeader("Date", time);

        response.getWriter().write("ABC123");
        response.getWriter().flush();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        String s = "FRAME.DATA.";
        int j = 0;

        for (int i = 2; i <= SEND_DATA_LIMIT; i++, j++) {
            if (i == SEND_DATA_LIMIT) {
                // try to send last frame as a separate frame, makes comparison logic in the client easier
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                s = "LAST.DATA.FRAME";
            } else if (j == 3) {
                s = sPriortyResponse;
            } else if (j == 7) {
                j = 0;
                s = sWinUpdateResponse;
            } else {
                s = sBoringData + "." + i + "...";
                // yield/sleep every ten frames to let the client-write-side/server-read-side get some cycles
                if ((i % 10) == 0) {
                    Thread.yield();
                }
            }

            response.getWriter().write(s);
            response.getWriter().flush();
        }

        response.getWriter().close();

    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    public void testPing1(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long time = System.currentTimeMillis();
        response.setDateHeader("Date", time);

        response.getWriter().write("ABC123");
        response.getWriter().flush();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        String s = "FRAME.DATA.";
        int j = 0;
        for (int i = 2; i <= SEND_DATA_LIMIT_PING; i++, j++) {
            if (i == SEND_DATA_LIMIT_PING) {
                // try to send last frame as a separate frame, makes comparison logic in the client easier
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
                s = "LAST.DATA.FRAME";
            } else if (j == 3) {
                j = 0;
                s = sPingResponse;
            } else {
                s = sBoringData + "." + i + "...";
                // yield/sleep every ten frames to let the client-write-side/server-read-side get some cycles
                if ((i % 10) == 0) {
                    Thread.yield();
                }
            }

            response.getWriter().write(s);
            response.getWriter().flush();
        }

        response.getWriter().close();

    }

}
