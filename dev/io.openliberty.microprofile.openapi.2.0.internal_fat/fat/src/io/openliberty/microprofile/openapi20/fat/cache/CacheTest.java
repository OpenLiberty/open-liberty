/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat.cache;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.OVERWRITE;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.stream.Stream;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.PropertiesAsset;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
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

    private static final String SERVER_NAME = "OpenAPITestServer";

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

    @Test
    @Mode(TestMode.EXPERIMENTAL) // Too slow and low-risk of breaking to be worth running regularly
    @ExpectedFFDC("java.io.FileNotFoundException")
    public void testCacheReadWarning() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war")
                                   .addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // replace cached model with a directory to provoke cacheRead error
        Path cacheDir = getCacheDir("cacheTest");
        Path modelFile = cacheDir.resolve("model");
        Files.delete(modelFile);
        Files.createDirectory(modelFile);

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check the warning is emitted and cache is not used
        assertThat(server.findStringsInLogs("CWWKO1688W.*cacheTest.*FileNotFoundException"), not(empty()));
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // Stop server, expecting error
        server.stopServer("CWWKO1688W");
    }

    @Test
    @Mode(TestMode.EXPERIMENTAL) // Too slow and low-risk of breaking to be worth running regularly
    @ExpectedFFDC("java.io.IOException")
    public void testCacheWriteWarning() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cacheTest.war")
                                   .addPackage(CacheTest.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        // start server
        server.startServer();

        // check that document is generated and cache written
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInTrace("Cache entry written"), not(empty()));

        // stop server without archiving it
        server.stopServer(false);

        // replace cache directory with a file to provoke cacheWrite error
        Path cacheDir = getCacheDir("cacheTest");
        Files.walkFileTree(cacheDir, RECURSIVE_DELETE);
        Files.createFile(cacheDir); // Create an empty file

        // start server without clean (since that would clear the cache)
        server.startServer(false);

        // check the cache is not used and the warning is emitted when writing the new cache
        assertThat(server.findStringsInTrace("Building Jandex index"), not(empty()));
        assertThat(server.findStringsInTrace("Generating OpenAPI model"), not(empty()));
        assertThat(server.findStringsInLogs("CWWKO1689W.*cacheTest.*IOException"), not(empty()));

        // Stop server, expecting error
        server.stopServer("CWWKO1689W");
    }

    private static final FileVisitor<Path> RECURSIVE_DELETE = new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    };

    private Path getCacheDir(String appName) throws IOException {
        // cache dir is in workarea/org.eclipse.osgi/*/data/cache/appName
        Path workarea = FileSystems.getDefault().getPath(server.getOsgiWorkAreaRoot());
        try (Stream<Path> s = Files.list(workarea)) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.resolve("data/cache/" + appName))
                    .filter(Files::exists)
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Failed to find cache directory for " + appName + " in " + workarea));
        }
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
