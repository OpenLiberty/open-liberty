/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package accessor.servlets;

import java.io.IOException;
import java.util.function.Consumer;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSession.Accessor;

/**
 * Test HttpSession.Accessor getAccessor() , accept()
 */
@WebServlet(urlPatterns = {"/TestHttpSessionAccessor/*"}, name = "HttpSessionAccessor")
public class HttpSessionAccessor extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = HttpSessionAccessor.class.getName();

    private static final String SESN8602E = "SESN8602E: The session is invalid";
    private static final String SESN8603E = "SESN8603E: The session Id is invalid";

    //Common
    HttpServletRequest request;
    HttpServletResponse response;
    StringBuilder responseSB;
    ServletOutputStream sos;

    long access_lastAccessedTime = 0;

    public HttpSessionAccessor() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGENTER("doGET");

        request = req;
        response = resp;
        responseSB = new StringBuilder();
        sos = response.getOutputStream();

        switch (request.getHeader("runTest")) {
            case "testSessionAccessorLastAccessedTime" : testSessionAccessorLastAccessedTime(); break;
            case "testSessionAccessorInvalidSession" : testSessionAccessorInvalidSession(); break;
            case "testSessionAccessorInvalidSessionId" : testSessionAccessorInvalidSessionId(); break;
        }

        if (!responseSB.isEmpty()) {
            sos.println(responseSB.toString());
        }

        LOGEXIT("doGET");
    }

    /*
     * access HttpSession.Accessor which update automatically the _lastAccessedTime.
     */
    private void testSessionAccessorLastAccessedTime() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        long lastAccessedTime_1 = 0, lastAccessedTime_2 = 0;

        LOGENTER(method);

        Consumer<HttpSession> httpSession = new Consumer<HttpSession> () {
            @Override
            public void accept(HttpSession session) {
                //it can be an empty accept() block here.  The _lastAccessedTime will be updated regardless.
                LOG("   accept() ...");
                HttpSessionAccessor.this.access_lastAccessedTime = session.getLastAccessedTime();
            }
        };

        HttpSession session = request.getSession(true);
        lastAccessedTime_1 = session.getLastAccessedTime();

        //at creation, _lastAccessedTime = _currentAccessedTime
        LOG(" 0. Last accessed time [" + session.getLastAccessedTime() +"]");

        Accessor accessor = session.getAccessor();
        try {
            //first time, _lastAccessedTime = _currentAccessedTime; currentAccessedTime is updated ; so _lastAccessedTime does not actually change.
            LOG(" session access");
            accessor.access(httpSession);
            LOG(" 1. Last accessed time [" + access_lastAccessedTime +"]");

            //subsequently, _lastAccessedTime = _currentAccessedTime; currentAccessedTime is updated ;
            LOG(" session access");
            accessor.access(httpSession);
            LOG(" 2. Last accessed time [" + (lastAccessedTime_2 = access_lastAccessedTime) +"]");
        }
        catch (Exception e) {
            LOG(method + " There is an exception [" + e+ "]");
            responseSB.append(" Exception [" + e + "]");
        }

        if (lastAccessedTime_2 > lastAccessedTime_1) {
            responseSB.append(method + " [PASS] Updated lastAccessedTime.");
        }
        else {
            responseSB.append(method + " [FAIL] Cannot update lastAccessedTime; old [" +lastAccessedTime_1+ "] new ["+lastAccessedTime_2 +"]");
        }

        LOG(" Sending response [" + responseSB.toString() + "]");
        LOGEXIT(method);
    }

    /*
     * access HttpSession.Accessor
     * invalidate the session
     * access HttpSession.Accessor again to cause IllegalStateException due to session is invalidated.
     *
     *  The app reports back to client PASS/FAIL.
     */
    private void testSessionAccessorInvalidSession() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        Consumer<HttpSession> httpSession = new Consumer<HttpSession> () {
            @Override
            public void accept(HttpSession session) {
                LOG("   accept() ...");
                LOG("   session: [" +session + "]");
            }
        };

        HttpSession session = request.getSession(true);
        Accessor accessor = session.getAccessor();
        try {
            LOG(" First session access");
            accessor.access(httpSession);

            LOG(" Invalidate the session");
            session.invalidate();

            LOG(" Second session access, expecting ISE. ");
            accessor.access(httpSession);
        }
        catch (IllegalStateException ise) {
            LOG(" invalidated session ... catch ISE");
            if (ise.getMessage().contains(SESN8602E)) {
                responseSB.append(method + " [PASS] Found ["+ SESN8602E +"]");
            }
            else {
                responseSB.append(method + " [FAIL] Excepted ["+ SESN8602E +"] ; Found [" + ise.getMessage() + "]");
            }
        }

        //Do not log the responseSB which has the SESN code; otherwise test framework will fail the test
        LOGEXIT(method);
    }

    /*
     * access HttpSession.Accessor
     * change session ID
     * access HttpSession.Accessor again to cause IllegalStateException due to session ID has changed.
     *
     *  The app reports back to client PASS/FAIL.
     */
    private void testSessionAccessorInvalidSessionId() throws IOException{
        String method = new Object(){}.getClass().getEnclosingMethod().getName();
        LOGENTER(method);

        Consumer<HttpSession> httpSession = new Consumer<HttpSession> () {
            @Override
            public void accept(HttpSession session) {
                LOG("   accept() ...");
                LOG("   session.getId() [" +session.getId() + "]");
            }
        };

        HttpSession session = request.getSession(true);
        Accessor accessor = session.getAccessor();

        try {
            LOG(" First session access");
            accessor.access(httpSession);

            LOG(" Changed session ID [" + request.changeSessionId() + "]");

            LOG(" Second session access, expecting ISE.");
            accessor.access(httpSession);
        }
        catch (IllegalStateException ise) {
            LOG(" invalid session ID ... catch ISE");
            if (ise.getMessage().contains(SESN8603E)) {
                responseSB.append(method + " [PASS] Found ["+ SESN8603E +"]");
            }
            else {
                responseSB.append(method + " [FAIL] Expected ["+ SESN8603E +"] ; Found [" + ise.getMessage() + "]");
            }
        }

        //Do not log the responseSB which has the SESN code; otherwise test framework will fail the test due to WARNING/ERROR found
        LOGEXIT(method);
    }

    //#########################################################
    private void LOG(String s) {
        System.out.println(CLASS_NAME + " " + s);
    }

    private void LOGENTER(String method) {
        LOG(">>>>>>>>>>>>>>>> TESTING [" + method + "] ENTER >>>>>>>>>>>>>>>>");
    }

    private void LOGEXIT(String method) {
        LOG("<<<<<<<<<<<<<<<<<< TESTING [" + method + "] EXIT <<<<<<<<<<<<<<<<<<");
    }
}
