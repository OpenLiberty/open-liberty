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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.junit.Assert;
import org.junit.Test;

public class SecurityMergeTest {

    @Test
    public void testMergingServers() {

        OpenAPI primaryOpenAPI;

        OpenAPI doc1 = OASFactory.createOpenAPI();
        doc1.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("api-key", new ArrayList<>()));
        doc1.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("oauth", new ArrayList<>()));

        Paths docPaths = OASFactory.createPaths();
        docPaths.addPathItem("/users", createPathItem());
        docPaths.addPathItem("/status", createPathItem());
        doc1.setPaths(docPaths);

        OpenAPI doc2 = OASFactory.createOpenAPI();
        doc2.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("api-key", new ArrayList<>()));
        doc2.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("basic", new ArrayList<>()));

        docPaths = OASFactory.createPaths();
        docPaths.addPathItem("/events", createPathItem());
        docPaths.addPathItem("/airlines", createPathItem());
        doc2.setPaths(docPaths);
        doc2.setPaths(docPaths);

        List<SecurityRequirement> doc1Security = doc1.getSecurity();
        List<SecurityRequirement> doc2Security = doc2.getSecurity();

        primaryOpenAPI = TestUtil.merge(doc1);
        Paths paths = primaryOpenAPI.getPaths();

        validatePathServers(paths.getPathItem("/users"), null);
        validatePathServers(paths.getPathItem("/status"), null);

        primaryOpenAPI = TestUtil.merge(doc1, doc2);
        paths = primaryOpenAPI.getPaths();

        validatePathServers(paths.getPathItem("/users"), doc1Security);
        validatePathServers(paths.getPathItem("/status"), doc1Security);
        validatePathServers(paths.getPathItem("/events"), doc2Security);
        validatePathServers(paths.getPathItem("/airlines"), doc2Security);

        Assert.assertNull("Servers should be null", primaryOpenAPI.getServers());
    }

    private PathItem createPathItem() {
        PathItem pathItem = OASFactory.createPathItem();
        pathItem.setGET(OASFactory.createOperation());
        pathItem.setPOST(OASFactory.createOperation().addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("my-token", new ArrayList<>())));
        pathItem.setPUT(OASFactory.createOperation());
        pathItem.setDELETE(OASFactory.createOperation());
        pathItem.setHEAD(OASFactory.createOperation());
        pathItem.setPATCH(OASFactory.createOperation());
        pathItem.setTRACE(OASFactory.createOperation());
        pathItem.setOPTIONS(OASFactory.createOperation());
        return pathItem;
    }

    private void validatePathServers(PathItem pathItem, List<SecurityRequirement> expectedSecRequirements) {
        Assert.assertNotNull(pathItem);
        validateOperationRequirements(pathItem.getGET(), expectedSecRequirements);
        validateOperationRequirements(pathItem.getPOST(), Arrays.asList(OASFactory.createSecurityRequirement().addScheme("my-token", new ArrayList<>())));
        validateOperationRequirements(pathItem.getPUT(), expectedSecRequirements);
        validateOperationRequirements(pathItem.getDELETE(), expectedSecRequirements);
        validateOperationRequirements(pathItem.getPATCH(), expectedSecRequirements);
        validateOperationRequirements(pathItem.getHEAD(), expectedSecRequirements);
        validateOperationRequirements(pathItem.getTRACE(), expectedSecRequirements);
        validateOperationRequirements(pathItem.getOPTIONS(), expectedSecRequirements);
    }

    private void validateOperationRequirements(Operation operation, List<SecurityRequirement> expectedSecRequirements) {
        Assert.assertNotNull(operation);
        List<SecurityRequirement> actualSecurity = operation.getSecurity();
        if (expectedSecRequirements == null) {
            Assert.assertNull(actualSecurity);
        } else {
            Assert.assertNotNull(actualSecurity);
            Set<SecurityRequirement> actualSecReq = new HashSet<>(operation.getSecurity());
            Set<SecurityRequirement> expectedSecReq = new HashSet<>(expectedSecRequirements);
            Assert.assertEquals(expectedSecReq, actualSecReq);
        }
    }

}
