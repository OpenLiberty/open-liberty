/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package samesite.filters;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * The SameSiteAddCookieSetCookieHeaderServlet is also invoked for the same request and will print out
 * the Set-Cookie headers.
 *
 * Add a Cookie to the HttpServletResponse.
 *
 * Call the setHeader and addHeader methods on the HttpServletResponse for the Set-Cookie header.
 *
 */
@WebFilter("/TestAddCookieSetCookieHeader")
public class SameSiteAddCookieSetCookieHeaderFilter implements Filter {

    private ServletContext servletContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter invoked >****************");
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        boolean testSameSiteEmptyValue = Boolean.valueOf(request.getParameter("testEmptySameSiteValue"));
        boolean testSameSiteIncorrectValue = Boolean.valueOf(request.getParameter("testIncorrectSameSiteValue"));
        boolean testSameSiteAddCookieFirst = Boolean.valueOf(request.getParameter("testSameSiteAddCookieFirst"));
        boolean testSameSiteDuplicateSameSiteValue = Boolean.valueOf(request.getParameter("testDuplicateSameSiteValue"));

        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter testSameSiteEmptyValue: " + testSameSiteEmptyValue + " >****************");
        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter testSameSiteIncorrectValue: " + testSameSiteIncorrectValue + " >****************");
        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter testSameSiteAddCookieFirst: " + testSameSiteAddCookieFirst + " >****************");

        if (httpResponse.containsHeader("Set-Cookie")) {
            servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter Set-Cookie exists >****************");
            ArrayList<String> headers = new ArrayList<String>(httpResponse.getHeaders("Set-Cookie"));
            for (String header : headers) {
                servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter Set-Cookie header: " + header);
            }
        }

        if (testSameSiteAddCookieFirst) {
            // Create and add a Cookie to the Response
            Cookie cookie = new Cookie("AddTestCookie", "AddTestCookie");
            httpResponse.addCookie(cookie);

            // We will call setHeader before addHeader so we can test both methods.
            // If there is already a Set-Cookie header in the response we'll overwrite it but for
            // testing purposes this should not be an issue.
            if (!testSameSiteEmptyValue && !testSameSiteIncorrectValue) {
                httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None");
                httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None");
            }
        } else if (testSameSiteEmptyValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite");
        } else if (testSameSiteIncorrectValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect");
        } else if (testSameSiteDuplicateSameSiteValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Lax; SameSite=None");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None");
        } else {
            // Add the header then the cookie
            // We will call setHeader before addHeader so we can test both methods.
            // If there is already a Set-Cookie header in the response we'll overwrite it but for
            // testing purposes this should not be an issue.
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None");
        }

        // Create and add a Cookie to the Response
        if (!testSameSiteAddCookieFirst) {
            Cookie cookie = new Cookie("AddTestCookieAfterSetHeader", "AddTestCookieAfterSetHeader");
            httpResponse.addCookie(cookie);
        }

        chain.doFilter(request, response);

        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter doFilter after chain.doFilter invoked >****************");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter init invoked >****************");
    }

    @Override
    public void destroy() {
        servletContext.log("***********< SameSiteAddCookieSetCookieHeaderFilter destroy invoked >****************");
    }

}
