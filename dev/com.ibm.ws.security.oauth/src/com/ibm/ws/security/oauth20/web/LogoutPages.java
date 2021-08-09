/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.lang.LocalesModifier;

/**
 *Convenience class for supplying localized default logout page and logout error page.
 */
public class LogoutPages {
    private static TraceComponent tc = Tr.register(LogoutPages.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static String logoutHtml = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">" +
            "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
            "<title>#TITLE#</title></head><body>#BODY#</body></html>";

    /**
     * return a logout page for the appropriate locales
     * @param locales
     * @return
     */
    String getDefaultLogoutPage(Enumeration<Locale> locales) {
        String logoutTitle = Tr.formatMessage(tc, LocalesModifier.getPrimaryLocale(locales), "LOGOUT_PAGE_TITLE");
        String logoutMessage = Tr.formatMessage(tc, LocalesModifier.getPrimaryLocale(locales), "LOGOUT_PAGE_BODY"); // logout successful
        String logoutString = logoutHtml.replace("#TITLE#", logoutTitle).replace("#BODY#", logoutMessage);
        return logoutString;
    }

    String getDefaultLogoutErrorPage(Enumeration<Locale> locales) {
        String logoutTitle = Tr.formatMessage(tc, LocalesModifier.getPrimaryLocale(locales), "LOGOUT_ERROR_PAGE_TITLE");
        String logoutMessage = Tr.formatMessage(tc, LocalesModifier.getPrimaryLocale(locales), "LOGOUT_ERROR_PAGE_BODY"); // An exception occurred during logout
        String logoutString = logoutHtml.replace("#TITLE#", logoutTitle).replace("#BODY#", logoutMessage);
        return logoutString;
    }

    void sendDefaultLogoutPage(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.getOutputStream().print(getDefaultLogoutPage(req.getLocales()));
        } catch (IOException e) {
            // ffdc
        }
    }

    void sendDefaultErrorPage(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.getOutputStream().print(getDefaultLogoutErrorPage(req.getLocales()));
            // todo: should the response code be something special here?
        } catch (IOException e) {
            // ffdc
        }
    }
}
