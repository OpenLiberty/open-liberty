/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi40.internal.services.validation.test;

import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.hasError;
import static io.openliberty.microprofile.openapi20.test.utils.ValidationResultMatcher.successful;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.internal.services.OASValidationResult;
import io.openliberty.microprofile.openapi20.internal.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi40.internal.validation.Reference31Validator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
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
public class Reference31ValidatorTest {

    String key;
    OpenAPIImpl model;
    Context context;
    Reference31Validator validator;
    TestValidationHelper vh;

    private OpenAPIImpl setModel() {

        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();
        model.setComponents(components);

        SchemaImpl schema = new SchemaImpl();
        Map<String, Schema> schemas = new HashMap<>();
        schemas.put("testSchema", schema);
        components.setSchemas(schemas);
        schema.setTitle("testSchema");
        schema.setDescription("Basic schema for testing");
        schema.setFormat("string");

        APIResponseImpl response = new APIResponseImpl();
        Map<String, APIResponse> responses = new HashMap<>();
        responses.put("testResponse", response);
        components.setResponses(responses);
        response.setDescription("A simple test response.");

        ParameterImpl parameter = new ParameterImpl();
        Map<String, Parameter> parameters = new HashMap<>();
        parameters.put("testParameter", parameter);
        components.setParameters(parameters);
        parameter.setName("Accept");
        parameter.setIn(In.HEADER);

        ExampleImpl example = new ExampleImpl();
        Map<String, Example> examples = new HashMap<>();
        examples.put("testExample", example);
        components.setExamples(examples);
        example.setSummary("A test example");
        example.setDescription("A simple example for testing");

        RequestBodyImpl requestBody = new RequestBodyImpl();
        Map<String, RequestBody> requestBodies = new HashMap<>();
        requestBodies.put("testRequestBody", requestBody);
        components.setRequestBodies(requestBodies);

        HeaderImpl header = new HeaderImpl();
        Map<String, Header> headers = new HashMap<>();
        headers.put("testHeader", header);
        components.setHeaders(headers);

        SecuritySchemeImpl securityScheme = new SecuritySchemeImpl();
        Map<String, SecurityScheme> securitySchemes = new HashMap<>();
        securitySchemes.put("testSecurityScheme", securityScheme);
        components.setSecuritySchemes(securitySchemes);

        LinkImpl link = new LinkImpl();
        Map<String, Link> links = new HashMap<>();
        links.put("testLink", link);
        components.setLinks(links);

        PathItemImpl pathItem = new PathItemImpl();
        Map<String, PathItem> pathItems = new HashMap<>();
        pathItems.put("testPathItem", pathItem);
        components.setPathItems(pathItems);

        return model;

    }

    @Before
    public void setUp() {
        model = setModel();
        context = new TestValidationContextHelper(model);
        vh = new TestValidationHelper();
        validator = Reference31Validator.getInstance();
    }

    @Test
    public void testNullComponents() {
        String ref = "#/components/schemas/Pet";

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testExternalFileRef() {
        String ref = "Pet.yaml";

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        vh.resetResults();

        ref = "Pet.json";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        vh.resetResults();

        ref = "Pet.yml";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testExternalFileWithEmbeddedRef() {
        String ref = "definitions.yaml#/Pet";

        validator.validate(vh, context, ref);

        assertThat(vh.getResult(), successful());

        vh.resetResults();

        ref = "definitions.json#/Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        vh.resetResults();

        ref = "definitions.yml#/Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testExtensionRef() {
        String ref = "application/x-www-form-urlencoded";

        validator.validate(vh, context, ref);

        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testHttpLinkRef() {

        String ref = "http://foo.bar#/examples/zip-example";

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testInvalidRef() {

        String ref = "#/invalidRef/schemas/testSchema";

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // In 3.1, we don't validate references we don't recognize

        vh.resetResults();

        ref = "#/components/schemas/Pet/Cat";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref)); // #/components/schemas/Pet is not present

        vh.resetResults();

        ref = "#/components/schemas/testSchema/Cat";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // #/components/schemas/testSchema is present and we don't know how to validate under that

        vh.resetResults();

        ref = "#/components/components/schemas/testSchema";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref)); // #/components/components is not present

        vh.resetResults();

        ref = "";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), isNullRef()); // We do report a blank reference

        vh.resetResults();

        ref = null;
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), isNullRef()); // We do report a null reference

        vh.resetResults();

        ref = " ";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), isNullRef()); // We do report a blank reference
        vh.resetResults();

        ref = "#";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // In 3.1, we don't validate references we don't recognize

        vh.resetResults();

        ref = "#/";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // In 3.1, we don't validate references we don't recognize

        vh.resetResults();

        ref = "#/components/Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref)); // Illegal component type

        vh.resetResults();

        ref = "#/components//Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref)); // Illegal component type

        vh.resetResults();

        ref = "#/components/invalid/Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref)); // Illegal component type

        vh.resetResults();

        ref = "#/components/x-invalid/Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // Don't validate within an extension component type

        vh.resetResults();

        ref = "#invalid/components/schemas/Pet";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // In 3.1, we don't validate references we don't recognize

        vh.resetResults();

        ref = "#/components/schemas";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful()); // This resolves to an object under components, so there's no validation error even though it's weird

        vh.resetResults();

        ref = "#/components/schemas/schemas";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref)); // This is just a reference to a schema named "schemas" which is missing
    }

    @Test
    public void testExternalFileWithLink() {
        String ref = "http://foo.bar#/examples/address-example.json";

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testExternalFileWithSecureLink() {
        String ref = "https://foo.bar#/examples/address-example.json";

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testNullSchema() {
        String ref = "#/components/schemas/Pet";

        Components component = model.getComponents();
        component.setSchemas(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullResponses() {
        String ref = "#/components/responses/Pet";

        Components component = model.getComponents();
        component.setResponses(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullParameters() {
        String ref = "#/components/parameters/Pet";

        Components component = model.getComponents();
        component.setParameters(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullExamples() {
        String ref = "#/components/examples/Pet";

        Components component = model.getComponents();
        component.setExamples(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullRequestBodies() {
        String ref = "#/components/requestBodies/Pet";

        Components component = model.getComponents();
        component.setRequestBodies(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullHeaders() {
        String ref = "#/components/headers/Pet";

        Components component = model.getComponents();
        component.setHeaders(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullSecuritySchemes() {
        String ref = "#/components/securitySchemes/Pet";

        Components component = model.getComponents();
        component.setSecuritySchemes(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNullLinks() {
        String ref = "#/components/links/Pet";

        Components component = model.getComponents();
        component.setLinks(null);
        model.setComponents(component);
        context = new TestValidationContextHelper(model);

        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testValidReferences() {
        String ref = "#/components/schemas/testSchema";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/responses/testResponse";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/parameters/testParameter";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/examples/testExample";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/requestBodies/testRequestBody";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/headers/testHeader";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/securitySchemes/testSecurityScheme";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());

        ref = "#/components/links/testLink";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), successful());
    }

    @Test
    public void testInvalidReferences() {
        String ref = "#/components/schemas/testInvalidSchema";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/responses/testInvalidResponse";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/parameters/testInvalidParameter";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/examples/testInvalidExample";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/requestBodies/testInvalidRequestBody";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/headers/testInvalidHeader";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/securitySchemes/testInvalidSecurityScheme";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));

        vh.resetResults();

        ref = "#/components/links/testInvalidLink";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), targetMissing(ref));
    }

    @Test
    public void testNonUri() {
        String ref = "http://\n/#/components/schemas/testSchema";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), notValidUri(ref));

        vh.resetResults();
        ref = "http://{}/#/components/schemas/testSchema";
        validator.validate(vh, context, ref);
        assertThat(vh.getResult(), notValidUri(ref));
    }

    private Matcher<OASValidationResult> targetMissing(String ref) {
        return hasError("The \"" + ref + "\" reference value is not defined within the Components Object");
    }

    private Matcher<OASValidationResult> notValidUri(String ref) {
        return hasError("The \"" + ref + "\" value is not a valid URI");
    }

    private Matcher<OASValidationResult> isNullRef() {
        return hasError("The reference value is null");
    }

}
