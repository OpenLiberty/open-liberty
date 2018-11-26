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
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.CallbackValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 * Unit test for CaklbacksValidator class
 */
public class CallbacksValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    private final PathItemImpl pathItem = new PathItemImpl();
    private final CallbackValidator validator = CallbackValidator.getInstance();
    private final TestValidationHelper vh = new TestValidationHelper();

    @Test
    public void simpleCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        if (vh.hasEvents())
            Assert.fail("Simple Callback generated invalid error(s):" + vh);
    }

    @Test
    public void complexCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path/{$url}/version/{$method}/root/{$statusCode}/path", pathItem);
        c.addPathItem("http://abc.com/path/{$request.header.token}/version/{$request.query.name}/root", pathItem);
        c.addPathItem("http://abc.com/path/{$response.path.name}/version/{$response.body#/root/end}/root", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        if (vh.hasEvents())
            Assert.fail("Complex Callback generated invalid error:" + vh);
    }

    @Test
    public void emptyCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        Assert.assertEquals("Callback with blank entry must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("The URL template of Callback Object is empty and is not a valid URL"))
            Assert.fail("Callback with blank entry reported an incorrect error:" + vh);
    }

    @Test
    public void missingPathCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", null);
        vh.resetResults();
        validator.validate(vh, context, c);
        Assert.assertEquals("Callback missing a path must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("The Callback Object must contain a valid path item."))
            Assert.fail("Callback with null value path reported an incorrect error:" + vh);
    }

    @Test
    public void invalidUrlCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("[]://abc.com/path", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        Assert.assertEquals("Callback with invalid url must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("The Callback Object must contain a valid URL."))
            Assert.fail("Callback with invalid url reported an incorrect error:" + vh);
    }

    @Test
    public void relativeUrlCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("../../abc.com/path", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        Assert.assertEquals("Callback with relative url must have no error:" + vh, 0, vh.getEventsSize());
    }

    @Test
    public void invalidRuntimeExpressionCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", pathItem);
        c.addPathItem("http://abc.com/path/{$url}/version/{$method}/root/{$statusCodeXXX}/path", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        Assert.assertEquals("Callback with invalid runtime expression must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("The Callback Object must contain a valid runtime expression as defined in the OpenAPI Specification."))
            Assert.fail("Callback with invalid runtime expression reported an incorrect error:" + vh);
    }

    @Test
    public void invalidTemplateCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", pathItem);
        c.addPathItem("http://abc.com/path/{$u{rl}/version", pathItem);
        vh.resetResults();
        validator.validate(vh, context, c);
        Assert.assertEquals("Callback with invalid url template must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("The Callback Object contains invalid substitution variables:"))
            Assert.fail("Callback with invalid url template reported an incorrect error:" + vh);
    }
}
