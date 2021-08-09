/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;

public class WebErrorHandler {

    private static final String ERROR_PAGE_HTML_TITLE = "HTTP Error Message";

    private static final TraceComponent tc = Tr.register(WebErrorHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public WebErrorHandler() {
    }

    public void writeErrorHtml(HttpServletResponse response, String errorHeader, String errorMessage) {
        try {
            PrintWriter out = response.getWriter();
            out.println(getErrorPageHtml(errorHeader, errorMessage));
            out.flush();
        } catch (IOException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to obtain writer for response to write exception: " + e);
            }
        }
    }

    public String getErrorPageHtml(String errorHeader, String errorMessage) {
        StringBuffer sb = new StringBuffer();
        sb.append("<html>");
        sb.append(getErrorPageHead());
        sb.append(getErrorPageBody(errorHeader, errorMessage));
        sb.append("</html>");
        return sb.toString();
    }

    String getErrorPageHead() {
        StringBuffer sb = new StringBuffer();
        sb.append("<head>");
        sb.append("<meta http-equiv=\"Pragma\" content=\"no-cache\"/>");
        sb.append("<title>");
        sb.append(ERROR_PAGE_HTML_TITLE);
        sb.append("</title>");
        sb.append(createCssContentString());
        sb.append("</head>");
        return sb.toString();
    }

    /**
     * Creates the CSS content string to be used to format page.
     *
     * @return
     */
    String createCssContentString() {
        StringBuilder css = new StringBuilder();
        css.append("<style>");

        // body
        css.append("body {");
        css.append("background-color: #152935;");
        css.append("font-family: serif;");
        css.append("margin: 0;");
        css.append("}\n");
        // #top, #bottom
        css.append("#top, #bottom {");
        css.append("padding: 20px;");
        css.append("}\n");
        css.append("");
        // #top-middle, #bottom-middle
        css.append("#top-middle, #bottom-middle {");
        css.append("background-color: #001428;");
        css.append("padding: 10px;");
        css.append("}\n");
        css.append("");
        // .container
        css.append(".container {");
        css.append("background-color: white;");
        css.append("padding: 20px 50px;");
        css.append("}\n");
        // .error
        css.append(".error {");
        css.append("color: red;");
        css.append("font-weight: bold;");
        css.append("}\n");

        css.append("</style>");
        return css.toString();
    }

    String getErrorPageBody(String errorHeader, String errorMessage) {
        StringBuffer sb = new StringBuffer();
        sb.append("<body>");
        sb.append("<div class=\"wrapper\"></div>");
        sb.append("<div id=\"top\"></div>");
        sb.append("<div id=\"top-middle\"></div>");
        sb.append("<div class=\"container\">");
        sb.append("<h1>");
        sb.append(errorHeader);
        sb.append("</h1>");
        sb.append("<span class=\"error\">");
        sb.append("Error: " + errorMessage);
        sb.append("</span>");
        sb.append("</div>");
        sb.append("<div id=\"bottom-middle\"></div>");
        sb.append("<div id=\"bottom\"></div>");
        sb.append("</div>");
        sb.append("</body>");
        return sb.toString();
    }

}
