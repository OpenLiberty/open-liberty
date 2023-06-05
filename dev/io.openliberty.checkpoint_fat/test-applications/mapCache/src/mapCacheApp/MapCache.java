/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package mapCacheApp;

import java.io.IOException;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.cache.DistributedMap;
import com.ibm.websphere.cache.EntryInfo;

@ApplicationScoped
@WebServlet(urlPatterns = "/servlet")
public class MapCache extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    @ConfigProperty(name = "useInactivityParm")
    private Boolean useInactivityParm;

    @Resource(lookup = "services/cache/distributedmap")
    private DistributedMap dmap;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String key = request.getParameter("key");
        if (!dmap.containsKey(key)) {
            response.getWriter().append("Key [" + key + "] not in cache");
        } else {
            response.getWriter().append(dmap.get(key).toString());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (useInactivityParm) {
            dmap.put("key", "value",
                     1, // priority
                     20, // timeToLive (seconds)
                     4, // inactivityTime (seconds)
                     EntryInfo.NOT_SHARED, null);
        } else {
            dmap.put("key", "value",
                     1, //priority
                     8, // timeToLive
                     EntryInfo.NOT_SHARED, null);
        }
        System.out.println("MapCache init() called. useInactivityParm: " + useInactivityParm);
    }

}