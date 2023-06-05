/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

/**
 * Base servlet which the JASPI test servlets extend.
 */
public abstract class FlexibleBaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String servletName;
    protected List<BaseServletStep> mySteps = new ArrayList<BaseServletStep>();
    protected BaseServletStep myErrorStep = new WriteErrorStep();
    private static Logger log = Logger.getLogger(FlexibleBaseServlet.class.getName());

    FlexibleBaseServlet(String servletName) {
        this.servletName = servletName;
    }

    protected void updateServletName(String servletName) {
        this.servletName = servletName;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest("GET", req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest("POST", req, resp);
    }

    private void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest("CUSTOM", req, resp);
    }

    /**
     * Common logic to handle any of the various requests this servlet supports.
     * The actual business logic can be customized by overriding performTask.
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void handleRequest(String type, HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter writer = resp.getWriter();
        writer.println("ServletName: " + servletName);
        writer.println("Request type: " + type);

        StringBuffer sb = new StringBuffer();
        try {
            performTask(type, req, resp, sb);
        } catch (Throwable t) {
            t.printStackTrace(writer);
        }

        writer.write(sb.toString());
    }

    /**
     * Default action for the servlet if not overridden.
     *
     * @param req
     * @param resp
     * @param writer
     * @throws ServletException
     * @throws IOException
     */
    protected void performTask(String type,
                               HttpServletRequest req,
                               HttpServletResponse resp, StringBuffer sb) throws Exception {
        performCustomTasks(type, req, resp, sb);
    }

    class BaseServletParms {
        private String myType;
        private HttpServletRequest myRequest;
        private HttpServletResponse myResponse;
        private StringBuffer myBuffer;
        private Throwable myError;

        public String getType() {
            return myType;
        }

        public void setType(String type) {
            myType = type;
        }

        public HttpServletRequest getRequest() {
            return myRequest;
        }

        public void setRequest(HttpServletRequest req) {
            myRequest = req;
        }

        public HttpServletResponse getResponse() {
            return myResponse;
        }

        public void setResponse(HttpServletResponse res) {
            myResponse = res;
        }

        public StringBuffer getBuffer() {
            return myBuffer;
        }

        public void setBuffer(StringBuffer sb) {
            myBuffer = sb;
        }

        public Subject getSubject() throws Exception {
            return fetchSubject();
        }

        public Throwable getError() {
            return myError;
        }

        public void setError(Throwable t) {
            myError = t;
        }
    }

    static interface BaseServletStep {
        void invoke(BaseServletParms p) throws Exception;
    }

    class WriteRequestBasicsStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) {
            writeLine(p.getBuffer(), "getAuthType: " + p.getRequest().getAuthType());
            writeLine(p.getBuffer(), "getRequestURL: " + p.getRequest().getRequestURL().toString());
            if (p.getRequest().getQueryString() != null)
                writeLine(p.getBuffer(), "Query string: " + p.getRequest().getQueryString().toString());
        }
    }

    class WritePrincipalStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            writeLine(p.getBuffer(), "getRemoteUser: " + p.getRequest().getRemoteUser());
            writeLine(p.getBuffer(), "getUserPrincipal: " + p.getRequest().getUserPrincipal());

            if (p.getRequest().getUserPrincipal() != null) {
                writeLine(p.getBuffer(), "getUserPrincipal().getName(): "
                                         + p.getRequest().getUserPrincipal().getName());
            }
        }
    }

    class WriteErrorStep implements BaseServletStep {

        @Override
        public void invoke(BaseServletParms p) throws Exception {
            if (p.getError() != null) {
                if (p.getError() instanceof NoClassDefFoundError) {
                    // For OSGI App testing (EBA file), we expect this exception for all
                    // packages that are not public
                    writeLine(p.getBuffer(), "NoClassDefFoundError for SubjectManager: " + p.getError());

                } else {
                    p.getError().printStackTrace();
                }
            }

        }

    }

    protected void performCustomTasks(String type, HttpServletRequest req, HttpServletResponse res,
                                      StringBuffer sb) throws Exception {
        if (mySteps.isEmpty())
            throw new IllegalArgumentException("No steps specified for test flow");

        BaseServletParms parms = new BaseServletParms();
        parms.setType(type);
        parms.setRequest(req);
        parms.setResponse(res);
        parms.setBuffer(sb);
        try {
            for (BaseServletStep step : mySteps) {

                step.invoke(parms);
            }
        } catch (Throwable t) {
            parms.setError(t);
            myErrorStep.invoke(parms);
        }
    }

    private Subject fetchSubject() throws WSSecurityException {
        // Get the CallerSubject
        Subject callerSubject = WSSubject.getCallerSubject();
        return callerSubject;
    }

    /**
     * "Writes" the msg out to the client and System.out. This actually appends the msg
     * and a line delimiters to the running StringBuffer. This is necessary
     * because if too much data is written to the PrintWriter before the
     * logic is done, a flush() may get called and lock out changes to the
     * response.
     *
     * @param sb  Running StringBuffer
     * @param msg Message to write
     */
    void writeLine(StringBuffer sb, String msg) {
        sb.append(msg + "\n");
        log.info(msg);
    }
}
