/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.providerinapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;

/**
 * A servlet that will run tests with a JCache provider to ensure there is no regression.
 * This doesn't necessarily mean the tests will be run using JCache.
 */
public class ProviderInAppServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Config config = new Config("TestInstance");
            Hazelcast.getOrCreateHazelcastInstance(config);
        } catch (Throwable t) {
            Throwable cause = t;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            /*
             * We expect the root cause is a ClassNotFoundException due to the Hazlecast driver finding
             * the javax.cache classes in the thread context ClassLoader, but not in the app ClassLoader.
             *
             * When we fix the class loading issue, we should do a put and get to the cache to ensure we
             * can communicate with the cache.
             */
            if (cause instanceof ClassNotFoundException && "javax.cache.CacheException".equals(cause.getMessage())) {
                response.setStatus(200);
            } else {
                throw t;
            }
        }
    }
}
