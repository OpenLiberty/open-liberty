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

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

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
         * Connect to the Hazelcast cache and insert a value.
         * 
         * The cluster name must be unique from what we are using for our JCache testing, otherwise
         * the configuration from the JCache testing cluster will be deserialized by Hazelcast and
         * the javax.cache JCache classes will result in ClassNotFoundExceptions.
         */
        Config config = new Config();
        config.setClusterName("ProviderInAppServletTest");
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        IMap<Object, Object> distributedMap = instance.getMap(methodName);
        distributedMap.put(KEY, VALUE);

        /*
         * Verify we can retrieve the value.
         */
        if (VALUE.equals(distributedMap.get(KEY))) {
            System.out.print("ProviderInAppServlet." + methodName + ": value matches");
            response.setStatus(200);
        } else {
            System.out.print("ProviderInAppServlet." + methodName + ": value does not match");
        }
    }
}
