/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal.fat.plugins;

import componenttest.topology.impl.LibertyServer;

/**
 * A test plugin interface that can be used to customize tests based on the
 * JCache provider being used for the test.
 */
public interface TestPlugin {

    /**
     * Provider specific setup to run before a test.
     *
     * @throws Exception
     */
    public void beforeTest() throws Exception;

    /**
     * Whether the cache should exist before the test retrieves the cache the first time.
     *
     * @return True if the cache should exist before the test.
     */
    public boolean cacheShouldExistBeforeTest();

    /**
     * Get the caching provider classname.
     *
     * @return The class name for the caching provider.
     */
    public String getCachingProviderName();

    /**
     * Provider specific setup to run before starting server 1.
     *
     * @param server           The server to setup.
     * @param groupName        The name of the group the server should be a member of (hazelcast).
     * @param authCacheMaxSize The size of the authentication cache.
     * @param authCacheTtlSecs The TTL of the authentication cache.
     * @throws Exception If there was an error setting up server 1.
     */
    public void setupServer1(LibertyServer server, String groupName, Integer authCacheMaxSize, Integer authCacheTtlSecs) throws Exception;

    /**
     * Provider specific setup to run before starting server 2.
     *
     * @param server    The server to setup.
     * @param groupName The name of the group the server should be a member of (hazelcast).
     * @throws Exception If there was an error setting up server 2.
     */
    public void setupServer2(LibertyServer server, String groupName) throws Exception;

    /**
     * Whether to skip any TTL tests.
     *
     * @return True if TTL tests should be skipped.
     */
    public boolean skipTtlTest();
}
