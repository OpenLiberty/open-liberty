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
package com.ibm.ws.logging.fat.bundle;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class TrWriterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final TraceComponent tc = Tr.register(TrWriterServlet.class, "TRWRITER", "com.ibm.ws.logging.fat.bundle.resources.Messages");
    public static final String LEVEL = "level";
    public static final String MESSAGE_KEY = "msgKey";
    public static final String MESSAGE_PARAMETER = "msgParam";

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int level = 6;
        if (req.getParameter(LEVEL) != null) {
            level = Integer.parseInt(req.getParameter(LEVEL));
        }
        String msgKey = req.getParameter(MESSAGE_KEY);
        String param = req.getParameter(MESSAGE_PARAMETER);

        switch (level) {
            case TrLevelConstants.TRACE_LEVEL_DUMP:
                Tr.dump(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_DEBUG:
                Tr.debug(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_ENTRY_EXIT:
                Tr.entry(tc, msgKey, param);
                Tr.exit(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_EVENT:
                Tr.event(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_CONFIG:
                Tr.audit(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_AUDIT:
                Tr.audit(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_WARNING:
                Tr.warning(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_ERROR:
                Tr.error(tc, msgKey, param);
                break;
            case TrLevelConstants.TRACE_LEVEL_FATAL:
                Tr.fatal(tc, msgKey, param);
                break;
            default:
                Tr.info(tc, msgKey, param);
        }

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.print("<HTML><HEAD><TITLE>TrWriter</TITLE></HEAD>");
        out.print("<BODY>");
        out.print(" level=" + level);
        out.print(" msgKey=" + msgKey);
        out.print(" param=" + param);
        out.println("<BODY BGCOLOR=\"#FFFFEE\">");

    }

}
