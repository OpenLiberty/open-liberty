/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.test.merge.parts;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.OpenAPIProvider;

public class ServersMergeTest {

    @Test
    public void testMergingServers() {

        OpenAPI primaryOpenAPI;

        OpenAPI doc1 = OASFactory.createOpenAPI();
        doc1.addServer(OASFactory.createServer().url("http://common"));
        doc1.addServer(OASFactory.createServer().url("http://abc"));
        doc1.addServer(OASFactory.createServer().url("http://xyz"));

        Paths docPaths = OASFactory.createPaths();
        docPaths.addPathItem("/users", OASFactory.createPathItem().addServer(OASFactory.createServer().url("http://custom")));
        docPaths.addPathItem("/events", OASFactory.createPathItem());
        docPaths.addPathItem("/status", OASFactory.createPathItem());
        doc1.setPaths(docPaths);

        OpenAPIProvider provider1 = TestUtil.createProvider(doc1);

        OpenAPI doc2 = OASFactory.createOpenAPI();
        doc2.addServer(OASFactory.createServer().url("http://common"));
        doc2.addServer(OASFactory.createServer().url("http://abc2"));
        doc2.addServer(OASFactory.createServer().url("http://xyz2"));

        docPaths = OASFactory.createPaths();
        docPaths.addPathItem("/billing", OASFactory.createPathItem());
        docPaths.addPathItem("/inventory", OASFactory.createPathItem());
        docPaths.addPathItem("/store", OASFactory.createPathItem().addServer(OASFactory.createServer().url("http://custom")));
        doc2.setPaths(docPaths);

        OpenAPIProvider provider2 = TestUtil.createProvider(doc2);

        OpenAPI doc3 = OASFactory.createOpenAPI();
        doc3.addServer(OASFactory.createServer().url("/basepath"));

        docPaths = OASFactory.createPaths();
        docPaths.addPathItem("/feed", OASFactory.createPathItem());
        docPaths.addPathItem("/timeline", OASFactory.createPathItem());
        docPaths.addPathItem("/news", OASFactory.createPathItem().addServer(OASFactory.createServer().url("http://custom/basepath")));
        doc3.setPaths(docPaths);

        OpenAPIProvider provider3 = TestUtil.createProvider(doc3, "/basepath");

        List<Server> doc1Servers = doc1.getServers();
        List<Server> doc2Servers = doc2.getServers();

        primaryOpenAPI = TestUtil.merge(Arrays.asList(provider1));
        Paths paths = primaryOpenAPI.getPaths();

        validatePathServers(paths.getPathItem("/users"), Arrays.asList(OASFactory.createServer().url("http://custom")));
        validatePathServers(paths.getPathItem("/events"), null);
        validatePathServers(paths.getPathItem("/status"), null);

        primaryOpenAPI = TestUtil.merge(Arrays.asList(provider1, provider2));
        paths = primaryOpenAPI.getPaths();

        validatePathServers(paths.getPathItem("/users"), Arrays.asList(OASFactory.createServer().url("http://custom")));
        validatePathServers(paths.getPathItem("/events"), doc1Servers);
        validatePathServers(paths.getPathItem("/status"), doc1Servers);
        validatePathServers(paths.getPathItem("/store"), Arrays.asList(OASFactory.createServer().url("http://custom")));
        validatePathServers(paths.getPathItem("/inventory"), doc2Servers);
        validatePathServers(paths.getPathItem("/billing"), doc2Servers);

        Assert.assertNull("Servers should be null", primaryOpenAPI.getServers());

        primaryOpenAPI = TestUtil.merge(Arrays.asList(provider1, provider2, provider3));
        paths = primaryOpenAPI.getPaths();

        validatePathServersEmpty(paths.getPathItem("/basepath/feed"));
        validatePathServersEmpty(paths.getPathItem("/basepath/timeline"));
        validatePathServers(paths.getPathItem("/basepath/news"), Arrays.asList(OASFactory.createServer().url("http://custom")));

        Assert.assertNull("Servers should be null", primaryOpenAPI.getServers());
    }

    private void validatePathServers(PathItem pathItem, List<Server> expectedServers) {
        Assert.assertNotNull(pathItem);
        if (expectedServers == null) {
            Assert.assertNull(pathItem.getServers());
        } else {
            Assert.assertNotNull(pathItem.getServers());
            Set<String> actualServerUrls = pathItem.getServers().stream().map(Server::getUrl).collect(Collectors.toSet());
            Set<String> expectedServerUrls = expectedServers.stream().map(Server::getUrl).collect(Collectors.toSet());
            Assert.assertEquals(expectedServerUrls, actualServerUrls);
        }
    }

    private void validatePathServersEmpty(PathItem pathItem) {
        Assert.assertNotNull(pathItem);
        Assert.assertNull(pathItem.getServers());
    }

}
