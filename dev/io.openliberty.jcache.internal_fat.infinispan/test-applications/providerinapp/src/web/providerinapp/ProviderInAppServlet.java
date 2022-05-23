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

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;

/**
 * A servlet that will run tests with a JCache provider to ensure there is no regression.
 * This doesn't necessarily mean the tests will be run using JCache.
 */
public class ProviderInAppServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String KEY = "key1";
    private static final String VALUE = "value1";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*
         * Connect to the remote Infinispan server.
         */
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.uri(System.getProperty("infinispan.client.hotrod.uri")).security().authentication().realm("default").saslMechanism("DIGEST-MD5");
        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build(), true);

        /*
         * Get the remote cache.
         */
        RemoteCache<String, String> cache = cacheManager.administration().createCache("test", DefaultTemplate.DIST_ASYNC);

        /*
         * Add a key and retrieve it back out of the cache.
         */
        cache.put(KEY, VALUE);
        if (VALUE.equals(cache.get(KEY))) {
            response.setStatus(200);
        } else {
            System.out.print("ProviderInAppServlet: value not does not match");
            response.setStatus(500);
        }
    }
}
