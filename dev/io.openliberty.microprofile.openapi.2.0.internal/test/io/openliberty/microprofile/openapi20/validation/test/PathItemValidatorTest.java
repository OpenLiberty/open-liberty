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
package io.openliberty.microprofile.openapi20.validation.test;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.PathItemValidator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.parameters.ParameterImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;

/**
 *
 */
public class PathItemValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testCorrectPathItem() {

        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a correctly set pathItem for testing of PathItemValidator.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        OperationImpl getOp = new OperationImpl();
        APIResponsesImpl getResponses = new APIResponsesImpl();
        APIResponseImpl getResponse = new APIResponseImpl();
        getResponses.addAPIResponse("200", getResponse.description("Information retrieved successfully"));

        ParameterImpl getOpParamHeader = new ParameterImpl();
        getOpParamHeader.in(In.HEADER).name("Authorization");

        ParameterImpl getOpParamQuery = new ParameterImpl();
        getOpParamQuery.in(In.QUERY).name("id");

        ParameterImpl getOpParamCookie = new ParameterImpl();
        getOpParamCookie.in(In.COOKIE).name("client");

        List<Parameter> opParams = new ArrayList<Parameter>();
        opParams.add(getOpParamHeader);
        opParams.add(getOpParamQuery);
        opParams.add(getOpParamCookie);

        getOp.responses(getResponses).parameters(opParams);
        pathItem.GET(getOp);

        OperationImpl postOp = new OperationImpl();
        APIResponsesImpl postResponses = new APIResponsesImpl();
        APIResponseImpl postResponse = new APIResponseImpl();
        postResponses.addAPIResponse("200", postResponse.description("Information updated successfully"));

        ParameterImpl postOpParamHeader = new ParameterImpl();
        postOpParamHeader.in(In.HEADER).name("Accept");

        ParameterImpl postOpParamQuery = new ParameterImpl();
        postOpParamQuery.in(In.QUERY).name("id");

        ParameterImpl postOpParamCookie = new ParameterImpl();
        postOpParamCookie.in(In.COOKIE).name("sessionId");

        List<Parameter> postOpParams = new ArrayList<Parameter>();
        postOpParams.add(postOpParamHeader);
        postOpParams.add(postOpParamQuery);
        postOpParams.add(postOpParamCookie);

        postOp.responses(postResponses).parameters(postOpParams);
        pathItem.POST(postOp);

        OperationImpl deleteOp = new OperationImpl();
        APIResponsesImpl deleteResponses = new APIResponsesImpl();
        APIResponseImpl deleteResponse = new APIResponseImpl();
        deleteResponses.addAPIResponse("200", deleteResponse.description("Information deleted successfully"));

        ParameterImpl deleteOpParamHeader = new ParameterImpl();
        deleteOpParamHeader.in(In.HEADER).name("Content-Type");

        ParameterImpl deleteOpParamQuery = new ParameterImpl();
        deleteOpParamQuery.in(In.QUERY).name("username");

        ParameterImpl deleteOpParamCookie = new ParameterImpl();
        deleteOpParamCookie.in(In.COOKIE).name("id");

        List<Parameter> deleteOpParams = new ArrayList<Parameter>();
        deleteOpParams.add(deleteOpParamHeader);
        deleteOpParams.add(deleteOpParamQuery);
        deleteOpParams.add(deleteOpParamCookie);

        deleteOp.responses(deleteResponses).parameters(deleteOpParams);
        pathItem.DELETE(deleteOp);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullPathItem() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = null;

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullRefInPathItem() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.ref(null);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExternalRefInPathItem() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.ref("http://test-this-ref.com");

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testInternalRefInPathItem() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.ref("#/paths/username/get");

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testPathItemWithVar() {
        String key = "{$request.query.callbackUrl}/data";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testPathItemWithPathParamAndRequiredFalse() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with path parameters and 'required' set to false.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.required(false);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testPathItemWithDuplicateParams() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with duplicate parameters.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPathOne = new ParameterImpl();
        pathParamPathOne.in(In.PATH).name("username");
        pathParamPathOne.setRequired(true);

        ParameterImpl pathParamPathTwo = new ParameterImpl();
        pathParamPathTwo.in(In.PATH).name("username");
        pathParamPathTwo.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPathOne);
        pathParams.add(pathParamPathTwo);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testPathItemWithInvalidPathStrOne() {
        String key = "username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with invalid path string.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The format of the \"username}\" path is invalid"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The Path Item Object must contain a valid path. The \"username}\" path defines \"[username]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithInvalidPathStrTwo() {
        String key = "{username";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with invalid path string.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The format of the \"{username\" path is invalid"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The Path Item Object must contain a valid path. The \"{username\" path defines \"[username]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithInvalidPathStrThree() {
        String key = "}username{";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with invalid path string.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The format of the \"}username{\" path is invalid"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The Path Item Object must contain a valid path. The \"}username{\" path defines \"[username]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithInvalidPathStrFour() {
        String key = "{}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with invalid path string.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The format of the \"{}\" path is invalid"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The Path Item Object must contain a valid path. The \"{}\" path defines \"[username]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithInvalidPathStrFive() {
        String key = "{us{ername}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with invalid path string.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The format of the \"{us{ername}\" path is invalid"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The Path Item Object must contain a valid path. The \"{us{ername}\" path defines \"[username]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithInvalidPathStrSix() {
        String key = "{username/}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with invalid path string.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPath = new ParameterImpl();
        pathParamPath.in(In.PATH).name("username");
        pathParamPath.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPath);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(2, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The format of the \"{username/}\" path is invalid"));
        Assert.assertTrue(vh.getResult().getEvents().get(1).message.contains("The Path Item Object must contain a valid path. The \"{username/}\" path defines \"[username]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithDeclaredMultiplePathParam() {
        String key = "{username}/{id}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with multiple declared path parameters.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPathOne = new ParameterImpl();
        pathParamPathOne.in(In.PATH).name("username");
        pathParamPathOne.setRequired(true);

        ParameterImpl pathParamPathTwo = new ParameterImpl();
        pathParamPathTwo.in(In.PATH).name("id");
        pathParamPathTwo.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPathOne);
        pathParams.add(pathParamPathTwo);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testPathItemWithUndeclaredPathParam() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with undeclared path parameter.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPathOne = new ParameterImpl();
        pathParamPathOne.in(In.PATH).name("username");
        pathParamPathOne.setRequired(true);

        ParameterImpl pathParamPathTwo = new ParameterImpl();
        pathParamPathTwo.in(In.PATH).name("id");
        pathParamPathTwo.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPathOne);
        pathParams.add(pathParamPathTwo);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The \"{username}\" path defines \"[id]\" path parameter that is not declared"));
    }

    @Test
    public void testPathItemWithMultipleUndeclaredPathParam() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with multiple undeclared path parameters.");

        ParameterImpl pathParamHeader = new ParameterImpl();
        pathParamHeader.in(In.HEADER).name("token");

        ParameterImpl pathParamQuery = new ParameterImpl();
        pathParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPathOne = new ParameterImpl();
        pathParamPathOne.in(In.PATH).name("username");
        pathParamPathOne.setRequired(true);

        ParameterImpl pathParamPathTwo = new ParameterImpl();
        pathParamPathTwo.in(In.PATH).name("id");
        pathParamPathTwo.setRequired(true);

        ParameterImpl pathParamPathThree = new ParameterImpl();
        pathParamPathThree.in(In.PATH).name("accountNumber");
        pathParamPathThree.setRequired(true);

        ParameterImpl pathParamCookie = new ParameterImpl();
        pathParamCookie.in(In.COOKIE).name("status");

        List<Parameter> pathParams = new ArrayList<Parameter>();
        pathParams.add(pathParamHeader);
        pathParams.add(pathParamQuery);
        pathParams.add(pathParamPathOne);
        pathParams.add(pathParamPathTwo);
        pathParams.add(pathParamPathThree);
        pathParams.add(pathParamCookie);
        pathItem.parameters(pathParams);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The Path Item Object must contain a valid path. The \"{username}\" path defines \"2\" path parameters that are not declared: \"[id, accountNumber]\""));
    }

    @Test
    public void testPathItemWithOperationPathParamAndRequiredFalse() {
        String key = "{username}";

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with operation path parameter and 'required' field set to false.");

        OperationImpl getOp = new OperationImpl();
        APIResponsesImpl getResponses = new APIResponsesImpl();
        APIResponseImpl getResponse = new APIResponseImpl();
        getResponses.addAPIResponse("200", getResponse.description("Information retrieved successfully"));

        ParameterImpl getOpParamHeader = new ParameterImpl();
        getOpParamHeader.in(In.HEADER).name("Authorization");

        ParameterImpl getOpParamQuery = new ParameterImpl();
        getOpParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPathOne = new ParameterImpl();
        pathParamPathOne.in(In.PATH).name("username").required(false);
        
        ParameterImpl getOpParamCookie = new ParameterImpl();
        getOpParamCookie.in(In.COOKIE).name("client");

        List<Parameter> opParams = new ArrayList<Parameter>();
        opParams.add(getOpParamHeader);
        opParams.add(getOpParamQuery);
        opParams.add(getOpParamCookie);
        opParams.add(pathParamPathOne);

        getOp.responses(getResponses).parameters(opParams);
        pathItem.GET(getOp);

        OperationImpl postOp = new OperationImpl();
        APIResponsesImpl postResponses = new APIResponsesImpl();
        APIResponseImpl postResponse = new APIResponseImpl();
        postResponses.addAPIResponse("200", postResponse.description("Information updated successfully"));

        ParameterImpl postOpParamHeader = new ParameterImpl();
        postOpParamHeader.in(In.HEADER).name("Accept");

        ParameterImpl postOpParamQuery = new ParameterImpl();
        postOpParamQuery.in(In.QUERY).name("id");

        ParameterImpl pathParamPathTwo = new ParameterImpl();
        pathParamPathTwo.in(In.PATH).name("username");
        pathParamPathTwo.setRequired(true);

        ParameterImpl postOpParamCookie = new ParameterImpl();
        postOpParamCookie.in(In.COOKIE).name("sessionId");

        List<Parameter> postOpParams = new ArrayList<Parameter>();
        postOpParams.add(postOpParamHeader);
        postOpParams.add(postOpParamQuery);
        postOpParams.add(postOpParamCookie);
        postOpParams.add(pathParamPathTwo);

        postOp.responses(postResponses).parameters(postOpParams);
        pathItem.POST(postOp);

        OperationImpl deleteOp = new OperationImpl();
        APIResponsesImpl deleteResponses = new APIResponsesImpl();
        APIResponseImpl deleteResponse = new APIResponseImpl();
        deleteResponses.addAPIResponse("200", deleteResponse.description("Information deleted successfully"));

        ParameterImpl deleteOpParamHeader = new ParameterImpl();
        deleteOpParamHeader.in(In.HEADER).name("Content-Type");

        ParameterImpl deleteOpParamQuery = new ParameterImpl();
        deleteOpParamQuery.in(In.QUERY).name("username");

        ParameterImpl pathParamPathThree = new ParameterImpl();
        pathParamPathThree.in(In.PATH).name("username");
        pathParamPathThree.setRequired(true);

        ParameterImpl deleteOpParamCookie = new ParameterImpl();
        deleteOpParamCookie.in(In.COOKIE).name("id");

        List<Parameter> deleteOpParams = new ArrayList<Parameter>();
        deleteOpParams.add(deleteOpParamHeader);
        deleteOpParams.add(deleteOpParamQuery);
        deleteOpParams.add(deleteOpParamCookie);
        deleteOpParams.add(pathParamPathThree);

        deleteOp.responses(deleteResponses).parameters(deleteOpParams);
        pathItem.DELETE(deleteOp);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testPathItemRefParameter() {
        String key = "/mypath/{username}";

        ParameterImpl pathParam = new ParameterImpl();
        pathParam.in(In.PATH).name("username").required(true);

        Components component = new ComponentsImpl();
        component.addParameter("refUsername", pathParam);

        OpenAPIImpl model = new OpenAPIImpl();
        model.setComponents(component);
        Context context = new TestValidationContextHelper(model);

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with ref parameter");

        ParameterImpl refParam = new ParameterImpl();
        refParam.ref("refUsername");
        pathItem.addParameter(refParam);

        OperationImpl postOp = new OperationImpl();
        APIResponsesImpl postResponses = new APIResponsesImpl();
        APIResponseImpl postResponse = new APIResponseImpl();
        postResponses.addAPIResponse("200", postResponse.description("Information updated successfully"));

        postOp.setResponses(postResponses);
        pathItem.setPOST(postOp);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testPathItemRefParameterInOperation() {
        String key = "/mypath/{username}";

        ParameterImpl pathParam = new ParameterImpl();
        pathParam.in(In.PATH).name("username").required(true);

        Components component = new ComponentsImpl();
        component.addParameter("refUsername", pathParam);

        OpenAPIImpl model = new OpenAPIImpl();
        model.setComponents(component);
        Context context = new TestValidationContextHelper(model);

        PathItemValidator validator = PathItemValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        PathItemImpl pathItem = new PathItemImpl();
        pathItem.setSummary("This is a pathItem with ref parameter");

        OperationImpl postOp = new OperationImpl();
        APIResponsesImpl postResponses = new APIResponsesImpl();
        APIResponseImpl postResponse = new APIResponseImpl();
        postResponses.addAPIResponse("200", postResponse.description("Information updated successfully"));
        postOp.setResponses(postResponses);

        ParameterImpl refParam = new ParameterImpl();
        refParam.ref("refUsername");
        postOp.addParameter(refParam);

        pathItem.setPOST(postOp);

        validator.validate(vh, context, key, pathItem);
        Assert.assertEquals(0, vh.getEventsSize());
    }

}
