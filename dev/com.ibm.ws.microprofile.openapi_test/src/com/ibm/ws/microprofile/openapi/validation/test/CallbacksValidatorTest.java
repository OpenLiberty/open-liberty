/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.openapi.validation.test;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.impl.validation.CallbackValidator;

/**
 * Unit test for CaklbacksValidator class
 */
public class CallbacksValidatorTest {

    private final PathItemImpl pathItem = new PathItemImpl();
    private final CallbackValidator validator = CallbackValidator.getInstance();
    private final TestValidationHelper vh = new TestValidationHelper();

    @Test
    public void simpleCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", pathItem);
        vh.resetResults();
        validator.validate(vh, null, c);
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
        validator.validate(vh, null, c);
        if (vh.hasEvents())
            Assert.fail("Complex Callback generated invalid error:" + vh);
    }

    @Test
    public void emptyCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("", pathItem);
        vh.resetResults();
        validator.validate(vh, null, c);
        Assert.assertEquals("Callback with blank entry must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("URL template is empty and is not a valid URL"))
            Assert.fail("Callback with blank entry reported an incorrect error:" + vh);
    }

    @Test
    public void missingPathCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", null);
        vh.resetResults();
        validator.validate(vh, null, c);
        Assert.assertEquals("Callback missing a path must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("The path item value associated with key"))
            Assert.fail("Callback with null value path reported an incorrect error:" + vh);
    }

    @Test
    public void invalidUrlCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("h://abc.com/path", pathItem);
        vh.resetResults();
        validator.validate(vh, null, c);
        Assert.assertEquals("Callback with invalid url must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("must contain a valid URL"))
            Assert.fail("Callback with invalid url reported an incorrect error:" + vh);
    }

    @Test
    public void invalidRuntimeExpressionCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", pathItem);
        c.addPathItem("http://abc.com/path/{$url}/version/{$method}/root/{$statusCodeXXX}/path", pathItem);
        vh.resetResults();
        validator.validate(vh, null, c);
        Assert.assertEquals("Callback with invalid runtime expression must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("statusCodeXXX"))
            Assert.fail("Callback with invalid runtime expression reported an incorrect error:" + vh);
    }

    @Test
    public void invalidTemplateCallBack() {
        CallbackImpl c = new CallbackImpl();
        c.addPathItem("http://abc.com/path", pathItem);
        c.addPathItem("http://abc.com/path/{$u{rl}/version", pathItem);
        vh.resetResults();
        validator.validate(vh, null, c);
        Assert.assertEquals("Callback with invalid url template must have one error:" + vh, 1, vh.getEventsSize());
        String message = vh.getResult().getEvents().get(0).message;
        if (!message.contains("{$u{rl}"))
            Assert.fail("Callback with invalid url template reported an incorrect error:" + vh);
    }
}
