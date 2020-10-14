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

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.ReferenceValidator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.examples.ExampleImpl;
import io.smallrye.openapi.api.models.headers.HeaderImpl;
import io.smallrye.openapi.api.models.links.LinkImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.parameters.ParameterImpl;
import io.smallrye.openapi.api.models.parameters.RequestBodyImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.security.SecuritySchemeImpl;

/**
 *
 */
public class ReferenceValidatorTest {

    String key;
    OpenAPIImpl model;
    Context context;
    ReferenceValidator validator;
    TestValidationHelper vh;

    private OpenAPIImpl setModel() {

        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();
        model.setComponents(components);

        SchemaImpl schema = new SchemaImpl();
        Map<String, Schema> schemas = new HashMap<String, Schema>();
        schemas.put("testSchema", schema);
        components.setSchemas(schemas);
        schema.setTitle("testSchema");
        schema.setDescription("Basic schema for testing");
        schema.setFormat("string");

        APIResponseImpl response = new APIResponseImpl();
        Map<String, APIResponse> responses = new HashMap<String, APIResponse>();
        responses.put("testResponse", response);
        components.setResponses(responses);
        response.setDescription("A simple test response.");

        ParameterImpl parameter = new ParameterImpl();
        Map<String, Parameter> parameters = new HashMap<String, Parameter>();
        parameters.put("testParameter", parameter);
        components.setParameters(parameters);
        parameter.setName("Accept");
        parameter.setIn(In.HEADER);

        ExampleImpl example = new ExampleImpl();
        Map<String, Example> examples = new HashMap<String, Example>();
        examples.put("testExample", example);
        components.setExamples(examples);
        example.setSummary("A test example");
        example.setDescription("A simple example for testing");

        RequestBodyImpl requestBody = new RequestBodyImpl();
        Map<String, RequestBody> requestBodies = new HashMap<String, RequestBody>();
        requestBodies.put("testRequestBody", requestBody);
        components.setRequestBodies(requestBodies);

        HeaderImpl header = new HeaderImpl();
        Map<String, Header> headers = new HashMap<String, Header>();
        headers.put("testHeader", header);
        components.setHeaders(headers);

        SecuritySchemeImpl securityScheme = new SecuritySchemeImpl();
        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("testSecurityScheme", securityScheme);
        components.setSecuritySchemes(securitySchemes);

        LinkImpl link = new LinkImpl();
        Map<String, Link> links = new HashMap<String, Link>();
        links.put("testLink", link);
        components.setLinks(links);

        return model;

    }

    @Before
    public void setUp() {
        model = setModel();
        context = new TestValidationContextHelper(model);
        vh = new TestValidationHelper();
        validator = ReferenceValidator.getInstance();
    }

    @Test
    public void testNullComponents() {
        String $ref = "#/components/schemas/Pet";

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testExternalFileRef() {
        String $ref = "Pet.yaml";

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        vh.resetResults();

        $ref = "Pet.json";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        vh.resetResults();

        $ref = "Pet.yml";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExternalFileWithEmbeddedRef() {
        String $ref = "definitions.yaml#/Pet";

        validator.validate(vh, context, key, $ref);

        Assert.assertEquals(0, vh.getEventsSize());

        vh.resetResults();

        $ref = "definitions.json#/Pet";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        vh.resetResults();

        $ref = "definitions.yml#/Pet";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExtensionRef() {
        String $ref = "application/x-www-form-urlencoded";

        validator.validate(vh, context, key, $ref);

        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testHttpLinkRef() {

        String $ref = "http://foo.bar#/examples/zip-example";

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testInvalidRef() {

        String $ref = "#/invalidRef/schemas/testSchema";

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/schemas/Pet/Cat";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/components/schemas/testSchema";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = null;
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = " ";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/Pet";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components//Pet";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#invalid/components/schemas/Pet";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/schemas";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/schemas/schemas";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());
    }

    @Test
    public void testExternalFileWithLink() {
        String $ref = "http://foo.bar#/examples/address-example.json";

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testExternalFileWithSecureLink() {
        String $ref = "https://foo.bar#/examples/address-example.json";

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testNullSchema() {
        String $ref = "#/components/schemas/Pet";

        Components component = model.getComponents();
        component.setSchemas(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullResponses() {
        String $ref = "#/components/responses/Pet";

        Components component = model.getComponents();
        component.setResponses(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullParameters() {
        String $ref = "#/components/parameters/Pet";

        Components component = model.getComponents();
        component.setParameters(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullExamples() {
        String $ref = "#/components/examples/Pet";

        Components component = model.getComponents();
        component.setExamples(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullRequestBodies() {
        String $ref = "#/components/requestBodies/Pet";

        Components component = model.getComponents();
        component.setRequestBodies(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullHeaders() {
        String $ref = "#/components/headers/Pet";

        Components component = model.getComponents();
        component.setHeaders(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullSecuritySchemes() {
        String $ref = "#/components/securitySchemes/Pet";

        Components component = model.getComponents();
        component.setSecuritySchemes(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testNullLinks() {
        String $ref = "#/components/links/Pet";

        Components component = model.getComponents();
        component.setLinks(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

    }

    @Test
    public void testValidReferences() {
        String $ref = "#/components/schemas/testSchema";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/responses/testResponse";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/parameters/testParameter";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/examples/testExample";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/requestBodies/testRequestBody";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/headers/testHeader";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/securitySchemes/testSecurityScheme";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());

        $ref = "#/components/links/testLink";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(0, vh.getEventsSize());
    }

    @Test
    public void testInvalidReferences() {
        String $ref = "#/components/schemas/testInvalidSchema";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/responses/testInvalidResponse";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/parameters/testInvalidParameter";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/examples/testInvalidExample";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/requestBodies/testInvalidRequestBody";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/headers/testInvalidHeader";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/securitySchemes/testInvalidSecurityScheme";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());

        vh.resetResults();

        $ref = "#/components/links/testInvalidLink";
        validator.validate(vh, context, key, $ref);
        Assert.assertEquals(1, vh.getEventsSize());
    }

}
