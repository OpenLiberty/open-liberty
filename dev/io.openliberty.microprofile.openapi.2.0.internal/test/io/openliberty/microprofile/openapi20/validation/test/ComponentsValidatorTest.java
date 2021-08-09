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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.ComponentsValidator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.parameters.ParameterImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.security.SecuritySchemeImpl;

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

        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("match the regular expression as defined by the OpenAPI Specification"));
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
        Assert.assertEquals(2, vh.getEventsSize());

        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("match the regular expression as defined by the OpenAPI Specification"));
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
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The map contains an invalid key. A map should not have empty or null keys"));
    }

    @Test
    public void testNullMapValue() {

        ComponentsValidator validator = ComponentsValidator.getInstance();
        TestValidationHelper vh = new TestValidationHelper();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("mySecurity", null);
        components.setSecuritySchemes(securitySchemes);

        validator.validate(vh, context, null, components);
        Assert.assertEquals(1, vh.getEventsSize());
        Assert.assertTrue(vh.getResult().getEvents().get(0).message.contains("The map specifies an invalid value for the \"mySecurity\" key. A map should not have null values"));
    }

}
