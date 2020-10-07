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
import javax.servlet.http.HttpServletResponse;

/**
 * The SameSiteSetCookieServlet is also invoked for the same request and will print out
 * the Set-Cookie headers.
 *
 * Call the setHeader and addHeader methods on the HttpServletResponse for the Set-Cookie header.
 *
 */
@WebFilter("/TestSetCookie")
public class SameSiteSetCookieFilter implements Filter {

    private ServletContext servletContext;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        servletContext.log("***********< SameSiteSetCookieFilter doFilter invoked >****************");
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        boolean testSameSiteEmptyValue = Boolean.valueOf(request.getParameter("testEmptySameSiteValue"));
        boolean testSameSiteIncorrectValue = Boolean.valueOf(request.getParameter("testIncorrectSameSiteValue"));
        boolean testSameSiteDuplicateSameSiteValue = Boolean.valueOf(request.getParameter("testDuplicateSameSiteValue"));
        boolean testSameSiteConfigSetAddHeaderValue = Boolean.valueOf(request.getParameter("testSameSiteConfigSetAddHeader"));

        servletContext.log("***********< SameSiteSetCookieFilter doFilter testSameSiteEmptyValue: " + testSameSiteEmptyValue + " >****************");
        servletContext.log("***********< SameSiteSetCookieFilter doFilter testSameSiteIncorrectValue: " + testSameSiteIncorrectValue + " >****************");

        if (httpResponse.containsHeader("Set-Cookie")) {
            servletContext.log("***********< SameSiteSetCookieFilter doFilter Set-Cookie exists >****************");
            ArrayList<String> headers = new ArrayList<String>(httpResponse.getHeaders("Set-Cookie"));
            for (String header : headers) {
                servletContext.log("***********< SameSiteSetCookieFilter doFilter Set-Cookie header: " + header);
            }
        }

        servletContext.log("***********< SameSiteSetCookieFilter doFilter Set-Cookie exists >****************");
        ArrayList<String> headers = new ArrayList<String>(httpResponse.getHeaders("Set-Cookie"));
        for (String header : headers) {
            servletContext.log("***********< SameSiteSetCookieFilter doFilter Set-Cookie header: " + header);
        }

        if (testSameSiteEmptyValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite");
        } else if (testSameSiteIncorrectValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Incorrect");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Incorrect");
        } else if (testSameSiteDuplicateSameSiteValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=Lax; SameSite=None");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=Lax; SameSite=None");
        } else if (testSameSiteConfigSetAddHeaderValue) {
            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader");
        } else {
            // We will call setHeader before addHeader so we can test both methods.
            // If there is already a Set-Cookie header in the response we will overwrite it but for
            // testing purposes this should not be an issue.

            httpResponse.setHeader("Set-Cookie", "MySameSiteCookieNameSetHeader=MySameSiteCookieValueSetHeader; Secure; SameSite=None");
            httpResponse.addHeader("Set-Cookie", "MySameSiteCookieNameAddHeader=MySameSiteCookieValueAddHeader; Secure; SameSite=None");

        }

        chain.doFilter(request, response);

        servletContext.log("***********< SameSiteSetCookieFilter doFilter after chain.doFilter invoked >****************");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        servletContext = filterConfig.getServletContext();
        servletContext.log("***********< SameSiteSetCookieFilter init invoked >****************");
    }

    @Override
    public void destroy() {
        servletContext.log("***********< SameSiteSetCookieFilter destroy invoked >****************");
    }

}
