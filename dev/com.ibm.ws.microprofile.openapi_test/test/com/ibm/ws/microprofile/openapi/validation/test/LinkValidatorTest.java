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
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OperationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.links.LinkImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.LinkValidator;
import com.ibm.ws.microprofile.openapi.impl.validation.OperationValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class LinkValidatorTest {

    String key = null;

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Before
    public void setUpPaths() {
        String pathNameOne = "/my-test-path-one";
        PathItemImpl pathItemOne = new PathItemImpl(); //pathItem
        OperationImpl getPathItemOne = new OperationImpl(); //operation
        APIResponsesImpl responsesOne = new APIResponsesImpl(); //adding responses to operation
        APIResponseImpl responseOne = new APIResponseImpl();
        responsesOne.addApiResponse("200", responseOne.description("Operation successful"));
        getPathItemOne.operationId("pathItemOneGetId").responses(responsesOne); //adding op ID and responses to operation
        pathItemOne.setGET(getPathItemOne); //set operation of pathItem

        PathItemImpl pathItemTwo = new PathItemImpl(); //pathItem
        String pathNameTwo = "/my-test-path-two";
        OperationImpl getPathItemTwo = new OperationImpl(); //operation
        APIResponsesImpl responsesTwo = new APIResponsesImpl();
        APIResponseImpl responseTwo = new APIResponseImpl();
        responsesTwo.addApiResponse("400", responseTwo.description("Unable to complete operation"));
        getPathItemTwo.operationId("pathItemTwoGetId").responses(responsesTwo); //adding op ID and responses to operation
        pathItemTwo.setGET(getPathItemTwo); //set operation of pathItem

        PathsImpl paths = new PathsImpl(); //paths
        paths.addPathItem(pathNameOne, pathItemOne);
        paths.addPathItem(pathNameTwo, pathItemTwo);

        model.setPaths(paths);
    }

    @Test
    public void testCorrectLinkWithRef() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl correctLink = new LinkImpl();
        correctLink.setDescription("This is a correct test link");
        String operationRef = "#/paths/my-test-path-two/get";
        correctLink.setOperationRef(operationRef);

        validator.validate(vh, context, key, correctLink);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testCorrectLinkWithId() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl correctLink = new LinkImpl();
        correctLink.setDescription("This is a correct test link");
        String operationId = "pathItemOneGetId";
        correctLink.setOperationId(operationId);

        OperationValidator opValidator = OperationValidator.getInstance();
        opValidator.validate(vh, context, context.getModel().getPaths().get("/my-test-path-one").getGET());

        validator.validate(vh, context, key, correctLink);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullLink() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl link = null;

        validator.validate(vh, context, key, link);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testLinkWithBothRefAndId() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl link = new LinkImpl();
        link.setDescription("This is a link with both OperationRef and operationId defined");
        String operationId = "pathItemOneGetId";
        link.setOperationId(operationId);

        String operationRef = "#/paths/my-test-path-two/get";
        link.setOperationRef(operationRef);

        OperationValidator opValidator = OperationValidator.getInstance();
        opValidator.validate(vh, context, context.getModel().getPaths().get("/my-test-path-one").getGET());

        validator.validate(vh, context, key, link);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Link Object defines \"operationRef\" field and \"operationId\" field"));
    }

    @Test
    public void testLinkWithoutRefOrId() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl link = new LinkImpl();
        link.setDescription("This is a link without operationRef or operationId");

        validator.validate(vh, context, key, link);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Link Object must be identified using either an \"operationRef\" or \"operationId\""));
    }

    @Test
    public void testLinkWithExternalOperationRef() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl link = new LinkImpl();
        link.setDescription("This is a link with external operationRef");
        String operationRef = "http://myserver/paths/my-test-path-two/get";
        link.setOperationRef(operationRef);

        validator.validate(vh, context, key, link);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testLinkWithInvalidRef() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl correctLink = new LinkImpl();
        correctLink.setDescription("This is a link with invalid operationRef lenths");
        String operationRef = "#/paths/my-test-path-two/somethingElse/get";
        correctLink.setOperationRef(operationRef);

        validator.validate(vh, context, key, correctLink);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Link Object specified a relative \"operationRef\" field"));
    }

    @Test
    public void testLinkWithInvalidOperation() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl correctLink = new LinkImpl();
        correctLink.setDescription("This is a link with invalid operation in operationRef");
        String operationRef = "#/paths/my-test-path-two/write";
        correctLink.setOperationRef(operationRef);

        validator.validate(vh, context, key, correctLink);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("Link Object specified a relative \"operationRef\" field"));
    }

    @Test
    public void testLinkWithCorrectId() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl link = new LinkImpl();
        link.setDescription("This is a link with correct operationId defined");
        String operationId = "pathItemOneGetId";
        link.setOperationId(operationId);

        OperationValidator opValidator = OperationValidator.getInstance();
        opValidator.validate(vh, context, context.getModel().getPaths().get("/my-test-path-one").getGET());

        validator.validate(vh, context, key, link);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testLinkWithInvalidId() {
        LinkValidator validator = LinkValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        LinkImpl link = new LinkImpl();
        link.setDescription("This is a link with invalid operationId defined");
        String operationId = "pathItemThreeGetId";
        link.setOperationId(operationId);

        OperationValidator opValidator = OperationValidator.getInstance();
        opValidator.validate(vh, context, context.getModel().getPaths().get("/my-test-path-one").getGET());
        opValidator.validate(vh, context, context.getModel().getPaths().get("/my-test-path-two").getGET());

        validator.validate(vh, context, key, link);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("linkOperationIdInvalid"));
    }
}
