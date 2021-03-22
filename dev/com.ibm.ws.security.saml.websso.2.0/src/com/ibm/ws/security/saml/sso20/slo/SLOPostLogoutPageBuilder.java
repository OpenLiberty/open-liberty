/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.slo;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.web.WebUtils;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;

public class SLOPostLogoutPageBuilder {

    private static TraceComponent tc = Tr.register(SLOPostLogoutPageBuilder.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private HttpServletRequest request = null;
    private BasicMessageContext<?, ?> messageContext = null;
    private SLOMessageContextUtils msgContextUtils = null;

    public SLOPostLogoutPageBuilder(HttpServletRequest request, BasicMessageContext<?, ?> msgCtx) {
        this.request = request;
        this.messageContext = msgCtx;
        this.msgContextUtils = new SLOMessageContextUtils(messageContext);
    }

    public void writeDefaultLogoutPage(HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        writer.println(getPostLogoutPageHtml());
        writer.close();
    }

    String getPostLogoutPageHtml() {
        String html = "<!DOCTYPE html>\n";
        html += "<html " + getHtmlLang() + ">\n";
        html += createHtmlHead() + "\n";
        html += createHtmlBody() + "\n";
        html += "</html>";
        return html;
    }

    String getHtmlLang() {
        if (request != null) {
            return "lang=\"" + request.getLocale() + "\"";
        }
        return "";
    }

    String createHtmlHead() {
        String html = "<head>\n";
        html += "<meta charset=\"utf-8\">\n";
        html += "<title>" + WebUtils.htmlEncode(getHtmlTitle()) + "</title>\n";
        html += "</head>";
        return html;
    }

    String getHtmlTitle() {
        return Tr.formatMessage(tc, request.getLocales(), "POST_LOGOUT_PAGE_TITLE");
    }

    String createHtmlBody() {
        String html = "<body>\n";
        html += getBodyText() + "\n";
        html += "</body>";
        return html;
    }

    String getBodyText() {
        if (isSuccessStatusCode(msgContextUtils.getSloStatusCode())) {
            return Tr.formatMessage(tc, request.getLocales(), "POST_LOGOUT_PAGE_SUCCESS_TEXT");
        } else {
            return Tr.formatMessage(tc, request.getLocales(), "POST_LOGOUT_PAGE_FAILURE_TEXT");
        }
    }

    boolean isSuccessStatusCode(String statusCodeValue) {
        if (statusCodeValue == null) {
            return false;
        }
        if (statusCodeValue.equals(SLOMessageContextUtils.LOGOUT_STATUS_CODE_SUCCESS)) {
            return true;
        }
        return false;
    }

}
