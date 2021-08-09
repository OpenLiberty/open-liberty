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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CacheTest {

    @Server("ApplicationProcessorServer")
    public static LibertyServer server;

    @Test
    public void testCacheHit() throws Exception {
        // Deploy app
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war").addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
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

    @After
    public void cleanup() throws Exception {
        try {
            server.stopServer();
        } finally {
            server.setAdditionalSystemProperties(null);
        }
    }

}
