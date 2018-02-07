/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.validation.test;

import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OperationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OASValidator;
import com.ibm.ws.microprofile.openapi.impl.validation.OperationValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OperationValidatorTest {

    String key = null;

    @Test
    public void testCorrectOperation() {
        OperationValidator validator = OperationValidator.getInstance();
        OASValidator vh = new OASValidator();

        String pathNameOne = "/my-test-path-one/";
        PathItemImpl pathItemOne = new PathItemImpl();

        OperationImpl pathItemOneGet = new OperationImpl();

        APIResponsesImpl responses = new APIResponsesImpl();
        APIResponseImpl response = new APIResponseImpl();
        responses.addApiResponse("200", response.description("Operation successful"));

        pathItemOneGet.operationId("pathItemOneGetId").responses(responses);
        pathItemOne.setGET(pathItemOneGet);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem(pathNameOne, pathItemOne);

        OpenAPIImpl model = new OpenAPIImpl();
        Context context = new TestValidationContextHelper(model);
        model.setPaths(paths);

        validator.validate(vh, context, key, pathItemOneGet);
        //Assert.assertEquals(0, vh.getEventsSize());
    }
}
