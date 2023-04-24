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
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.cache.DistributedMap;

@WebServlet(urlPatterns = "/servlet")
@ApplicationScoped
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

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        System.out.println("on start");
        if (useInactivityParm) {
            dmap.put("key", "value", 1, 5, 5, 1, null);
        } else {
            dmap.put("key", "value", 1, 10, 1, null);
        }
    }

}