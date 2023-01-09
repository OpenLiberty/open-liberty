/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
        final String methodName = "doPost";
        System.out.print("ProviderInAppServlet." + methodName + ": starting test...");

        /*
         * Default the test to fail
         */
        response.setStatus(500);

        /*
         * Connect to the remote Infinispan server and insert a value into the cache.
         */
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.uri(System.getProperty("infinispan.client.hotrod.uri")).security().authentication().realm("default").saslMechanism("DIGEST-MD5");
        RemoteCacheManager cacheManager = new RemoteCacheManager(builder.build(), true);
        RemoteCache<String, String> cache = cacheManager.administration().createCache("test", DefaultTemplate.DIST_ASYNC);
        cache.put(KEY, VALUE);

        /*
         * Verify we can retrieve the value.
         */
        if (VALUE.equals(cache.get(KEY))) {
            System.out.print("ProviderInAppServlet: value matches");
            response.setStatus(200);
        } else {
            System.out.print("ProviderInAppServlet: value does not match");
        }
    }
}
