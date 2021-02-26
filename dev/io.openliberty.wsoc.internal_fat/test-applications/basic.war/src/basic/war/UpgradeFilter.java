/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package basic.war;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;

import com.ibm.websphere.wsoc.WsWsocServerContainer;

/**
 * Servlet Filter implementation class UpgradeFilter
 */
@WebFilter("/UpgradeFilter")
public class UpgradeFilter implements Filter {

    public UpgradeFilter() {

    }

    @Override
    public void destroy() {

    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        Map<String, String> themap = new HashMap<String, String>(5);
        themap.put("TEST1", "TEST1");
        themap.put("TEST2", "TEST2");


        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        ServerContainer container = (ServerContainer) req.getServletContext().getAttribute("javax.websocket.server.ServerContainer");
        if (container instanceof WsWsocServerContainer) {
            WsWsocServerContainer ws = (WsWsocServerContainer) container;
            ws.doUpgrade(req, resp, new ServletUpgradePathEndpointConfig(), themap);
            // Our response should be committed by now, but we'll try to change it here for one of our tests...
            if (resp.getStatus() != 101) {
                resp.setStatus(500);
            }

        }
        // We are handling the request, so stop the chain...
    }

    @Override
    public void init(FilterConfig fConfig) throws ServletException {

    }

}
