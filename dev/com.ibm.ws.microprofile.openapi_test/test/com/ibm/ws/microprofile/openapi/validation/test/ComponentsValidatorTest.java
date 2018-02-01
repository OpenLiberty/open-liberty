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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.ComponentsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import com.ibm.ws.microprofile.openapi.impl.validation.ComponentsValidator;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationContextHelper;
import com.ibm.ws.microprofile.openapi.test.utils.TestValidationHelper;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class ComponentsValidatorTest {

    OpenAPIImpl model = new OpenAPIImpl();
    Context context = new TestValidationContextHelper(model);

    @Test
    public void testComponentsCorrect() {

        ComponentsValidator validator = ComponentsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("testKey", new SecuritySchemeImpl());
        components.setSecuritySchemes(securitySchemes);

        Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        parameters.put("Test_key", new ParameterImpl());
        parameters.put("test-key", new ParameterImpl());
        components.setParameters(parameters);

        Map<String, APIResponse> responses = new HashMap<String, APIResponse>();
        responses.put("test.my.key", new APIResponseImpl());

        validator.validate(vh, context, null, components);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testOneInvalidKeyInComponent() {

        ComponentsValidator validator = ComponentsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("testKey", new SecuritySchemeImpl());
        components.setSecuritySchemes(securitySchemes);

        Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        parameters.put("Test_key", new ParameterImpl());
        parameters.put("_11?78", new ParameterImpl());
        components.setParameters(parameters);

        Map<String, APIResponse> responses = new HashMap<String, APIResponse>();
        responses.put("test.my.key", new APIResponseImpl());

        validator.validate(vh, context, null, components);
        Assert.assertEquals(1, vh.getEventsSize());

        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("must be a regular expression"));
    }

    @Test
    public void testMultipleInvalidKeysInComponent() {

        ComponentsValidator validator = ComponentsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("", new SecuritySchemeImpl());
        components.setSecuritySchemes(securitySchemes);

        Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        parameters.put("Test_key", new ParameterImpl());
        parameters.put("_11?78", new ParameterImpl());
        components.setParameters(parameters);

        Map<String, APIResponse> responses = new HashMap<String, APIResponse>();
        responses.put(".test/I'm.invalid", new APIResponseImpl());

        validator.validate(vh, context, null, components);
        Assert.assertEquals(1, vh.getEventsSize());

        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("must be a regular expression"));
    }

    @Test
    public void testNullComponents() {

        ComponentsValidator validator = ComponentsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ComponentsImpl components = new ComponentsImpl();

        validator.validate(vh, context, null, components);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullKey() {

        ComponentsValidator validator = ComponentsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put(null, new SecuritySchemeImpl());
        components.setSecuritySchemes(securitySchemes);

        validator.validate(vh, context, null, components);
        Assert.assertEquals(1, vh.getEventsSize());
    }

}
