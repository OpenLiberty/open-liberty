/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.fat.cache;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.microprofile.openapi20.fat.FATSuite;
import io.openliberty.microprofile.openapi20.fat.cache.filter.MyOASFilter;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CacheTest {

    private static final String SERVER_NAME = "ApplicationProcessorServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.repeatReduced(SERVER_NAME);

    @Test
    public void testCacheHit() throws Exception {
        // Deploy app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war").addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check that cache is used and document is not generated
        assertThat(server.findStringsInTrace("Using OpenAPI model loaded from cache"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), is(empty()));
    }

    @Test
    public void testCacheMissAppUpdate() throws Exception {
        // Deploy app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war").addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // Redeploy app
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY, OVERWRITE);

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check that cache is not used
        assertThat(server.findStringsInTrace("Cache out of date because files have changed"), not(empty()));
        assertThat(server.findStringsInTrace("Index out of date because files have changed"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));
    }

    @Test
    public void testCacheMissConfig() throws Exception {
        // Deploy app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war").addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // Update server config
        server.setAdditionalSystemProperties(Collections.singletonMap("MP_OPENAPI_SCAN_DISABLE", "true"));

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check that cache is not used
        assertThat(server.findStringsInTrace("Cache out of date because config is not the same"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
    }

    @Test
    public void testCacheMissIndexOk() throws Exception {
        // Deploy app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war").addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // Update server config
        server.setAdditionalSystemProperties(Collections.singletonMap("MP_OPENAPI_SERVERS", "http://example.com/"));

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check that cache is not used but the index is
        assertThat(server.findStringsInTrace("Cache out of date because config is not the same"), not(empty()));
        assertThat(server.findStringsInTrace("Using Jandex index from cache"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));
    }

    @Test
    public void testFilterAppNotCached() throws Exception {
        // Deploy app with a filter configured
        PropertiesAsset config = new PropertiesAsset().addProperty("mp.openapi.filter", MyOASFilter.class.getName());
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war")
                                   .addPackage(CacheTest.class.getPackage())
                                   .addPackage(MyOASFilter.class.getPackage())
                                   .addAsResource(config, "META-INF/microprofile-config.properties");
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check that cache is not used but the index is
        assertThat(server.findStringsInTrace("Cache not usable because a filter or reader is registered"), not(empty()));
        assertThat(server.findStringsInTrace("Using Jandex index from cache"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));
    }

    @After
    public void cleanup() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.setAdditionalSystemProperties(null);
        }
    }

}
