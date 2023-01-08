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

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.junit.Test;

public class OperationIDMergeTest {
    @Test
    public void testRenameConflictingOperationIDs() {
        OpenAPI primaryOpenAPI;

        OpenAPI doc1 = OASFactory.createOpenAPI();
        OpenAPI doc2 = OASFactory.createOpenAPI();

        //setup doc1
        Paths paths = OASFactory.createPaths();
        PathItem pathItem = OASFactory.createPathItem();
        Operation operation = OASFactory.createOperation();
        operation.setOperationId("operationGet");
        pathItem.setGET(operation);
        operation = OASFactory.createOperation();
        operation.setOperationId("operationPost");
        pathItem.setPOST(operation);
        operation = OASFactory.createOperation();
        operation.setOperationId("delete");
        pathItem.setDELETE(operation);
        paths.addPathItem("/users", pathItem);
        doc1.paths(paths);

        //setup doc2
        paths = OASFactory.createPaths();
        pathItem = OASFactory.createPathItem();
        operation = OASFactory.createOperation();
        operation.setOperationId("operationGet");
        pathItem.setGET(operation);
        operation = OASFactory.createOperation();
        operation.setOperationId("operationPost");
        pathItem.setPOST(operation);
        operation = OASFactory.createOperation();
        operation.setOperationId("delete_products");
        pathItem.setDELETE(operation);
        paths.addPathItem("/products", pathItem);
        doc2.paths(paths);

        primaryOpenAPI = TestUtil.merge(doc1, doc2);

        String opId = primaryOpenAPI.getPaths().getPathItem("/users").getGET().getOperationId();
        assertEquals("operationGet", opId);
        opId = primaryOpenAPI.getPaths().getPathItem("/users").getPOST().getOperationId();
        assertEquals("operationPost", opId);
        opId = primaryOpenAPI.getPaths().getPathItem("/users").getDELETE().getOperationId();
        assertEquals("delete", opId);
        opId = primaryOpenAPI.getPaths().getPathItem("/products").getGET().getOperationId();
        assertEquals("operationGet1", opId);
        opId = primaryOpenAPI.getPaths().getPathItem("/products").getPOST().getOperationId();
        assertEquals("operationPost1", opId);
        opId = primaryOpenAPI.getPaths().getPathItem("/products").getDELETE().getOperationId();
        assertEquals("delete_products", opId);

    }
}
