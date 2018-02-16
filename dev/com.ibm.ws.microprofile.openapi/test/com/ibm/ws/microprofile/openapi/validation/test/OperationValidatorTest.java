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

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OperationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.OperationValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OperationValidatorTest {

    String key = null;

    @Test
    public void testCorrectOperation() {
        OperationValidator validator = OperationValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        String pathNameOne = "/my-test-path-one/";
        PathItemImpl pathItemOne = new PathItemImpl();

        OperationImpl getPathItemOne = new OperationImpl();

        APIResponsesImpl responses = new APIResponsesImpl();
        APIResponseImpl response = new APIResponseImpl();
        responses.addApiResponse("200", response.description("Operation successful"));

        getPathItemOne.operationId("pathItemOneGetId").responses(responses);
        pathItemOne.setGET(getPathItemOne);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem(pathNameOne, pathItemOne);

        OpenAPIImpl model = new OpenAPIImpl();
        Context context = new TestValidationContextHelper(model);
        model.setPaths(paths);

        validator.validate(vh, context, key, getPathItemOne);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullOperation() {
        OperationValidator validator = OperationValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        OpenAPIImpl model = new OpenAPIImpl();
        Context context = new TestValidationContextHelper(model);

        OperationImpl nullOperation = null;

        validator.validate(vh, context, key, nullOperation);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testOperationWithNoResponses() {
        OperationValidator validator = OperationValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        String pathNameOne = "/my-test-path-one/";

        PathItemImpl pathItemOne = new PathItemImpl();
        OperationImpl getPathItemOne = new OperationImpl();
        getPathItemOne.operationId("pathItemOneGetId");
        pathItemOne.setGET(getPathItemOne);

        PathsImpl paths = new PathsImpl();
        paths.addPathItem(pathNameOne, pathItemOne);

        OpenAPIImpl model = new OpenAPIImpl();
        Context context = new TestValidationContextHelper(model);
        model.setPaths(paths);

        validator.validate(vh, context, key, getPathItemOne);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Required \"responses\" field is missing or is set to an invalid value"));
    }

    @Test
    public void testOperationWithNonUniqueIds() {
        OperationValidator validator = OperationValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        String pathNameOne = "/my-test-path-one/";
        PathItemImpl pathItemOne = new PathItemImpl(); //pathItem
        OperationImpl getPathItemOne = new OperationImpl(); //operation
        APIResponsesImpl responsesOne = new APIResponsesImpl(); //adding responses to operation
        APIResponseImpl responseOne = new APIResponseImpl();
        responsesOne.addApiResponse("200", responseOne.description("Operation successful"));
        getPathItemOne.operationId("pathItemOneGetId").responses(responsesOne); //adding op ID and responses to operation
        pathItemOne.setGET(getPathItemOne); //set operation of pathItem

        PathItemImpl pathItemTwo = new PathItemImpl(); //pathItem
        String pathNameTwo = "/my-test-path-two/";
        OperationImpl getPathItemTwo = new OperationImpl(); //operation
        APIResponsesImpl responsesTwo = new APIResponsesImpl();
        APIResponseImpl responseTwo = new APIResponseImpl();
        responsesTwo.addApiResponse("200", responseTwo.description("Good to go"));
        getPathItemTwo.operationId("pathItemOneGetId").responses(responsesTwo); //adding op ID and responses to operation
        pathItemTwo.setGET(getPathItemTwo); //set operation of pathItem

        PathsImpl paths = new PathsImpl(); //paths
        paths.addPathItem(pathNameOne, pathItemOne);
        paths.addPathItem(pathNameTwo, pathItemTwo);

        OpenAPIImpl model = new OpenAPIImpl();
        Context context = new TestValidationContextHelper(model);
        model.setPaths(paths);

        validator.validate(vh, context, key, getPathItemOne);
        validator.validate(vh, context, key, getPathItemTwo);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("More than one Operation Objects with \"pathItemOneGetId\" value for \"operationId\" field was found."));
    }
}
