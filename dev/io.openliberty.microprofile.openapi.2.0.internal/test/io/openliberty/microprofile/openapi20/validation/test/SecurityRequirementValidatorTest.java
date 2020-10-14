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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.In;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type;
import org.junit.Assert;
import org.junit.Test;

import io.openliberty.microprofile.openapi20.test.utils.TestValidationContextHelper;
import io.openliberty.microprofile.openapi20.test.utils.TestValidationHelper;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.SecurityRequirementValidator;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.security.OAuthFlowsImpl;
import io.smallrye.openapi.api.models.security.SecurityRequirementImpl;
import io.smallrye.openapi.api.models.security.SecuritySchemeImpl;

/**
 *
 */
public class SecurityRequirementValidatorTest {

    /*
     * Test Cases
     *
     * Negative:
     *
     * 1. SecurityRequirement object is empty
     * 2. context.getModel().getComponents() is null
     * 3. A key of the SecurityRequirement map is not in the SecurityScheme
     * 4. A key of the SecurityRequirements map exists in the SecurityScheme but has no value
     * 5. Type of SecurityScheme is null
     * 6. Type of SecurityScheme of the field name is oauth2 or openIdConnect and the value is null or empty
     * 7. Type of SecurityScheme of the field name is apiKey or http and the value is not null or empty
     *
     *
     * Positive:
     *
     * 1. All fields of the SecurityRequirement object are set correctly
     */

    String key;
    Type oauth = Type.OAUTH2;
    Type apiKey = Type.APIKEY;
    In in = In.COOKIE;
    OAuthFlowsImpl flows = new OAuthFlowsImpl();

    //Positive test, SecurityRequirement model set up correctly, validator is expected to validate successfully, no events in the helper
    @Test
    public void testPositiveSecurityRequirement() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();

        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        //Set required fields of SecurityRequirement object for positive test case
        List<String> scope = Arrays.asList("write:pets", "read:pets");
        securityRequirement.addScheme("petStore_auth", scope);
        securityRequirement.addScheme("api_key");

        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("petStore_auth", petStore_auth);
        securitySchemes.put("api_key", api_key);

        components.setSecuritySchemes(securitySchemes);

        model.setComponents(components);

        Context context = new TestValidationContextHelper(model);

        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(0, vh.getEventsSize());

    }

    //OpenAPI model is set up correctly, but the SecurityRequirement object is empty
    @Test
    public void testEmptySecurityRequirement() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();

        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("petStore_auth", petStore_auth);
        securitySchemes.put("api_key", api_key);

        components.setSecuritySchemes(securitySchemes);

        model.setComponents(components);

        Context context = new TestValidationContextHelper(model);

        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(1, vh.getEventsSize());

    }

    //OpenAPI model with null components, but the SecurityRequirement model set up correctly
    @Test
    public void testNullComponent() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();
        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        //Set required fields of SecurityRequirement object for positive test case
        List<String> scope = Arrays.asList("write:pets", "read:pets");
        securityRequirement.addScheme("petStore_auth", scope);
        securityRequirement.addScheme("api_key");

        OpenAPIImpl model = new OpenAPIImpl();

        //Set model but not components because we are testing for empty component case
        Context context = new TestValidationContextHelper(model);

        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(2, vh.getEventsSize());

    }

    //One of the SecurityScheme objects in SecurityRequirement is not declared in the components of the OpenAPI model
    @Test
    public void testKeyNotAScheme() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();

        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        //Set required fields of SecurityRequirement object for positive test case
        List<String> scope = Arrays.asList("write:pets", "read:pets");
        securityRequirement.addScheme("petStore_auth", scope);
        securityRequirement.addScheme("api_key");

        //Set the model
        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("petStore_auth", petStore_auth);

        components.setSecuritySchemes(securitySchemes);

        model.setComponents(components);

        Context context = new TestValidationContextHelper(model);

        //Validate
        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(1, vh.getEventsSize());

    }

    //One of the SecurityScheme objects declared in the OpenAPI model has a null value
    @Test
    public void testNullKey() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();

        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        //Set name and value fields of SecurityRequirement object
        List<String> scope = Arrays.asList("write:pets", "read:pets");
        securityRequirement.addScheme("petStore_auth", scope);
        securityRequirement.addScheme("api_key");

        //Set the model
        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("petStore_auth", null);
        securitySchemes.put("api_key", api_key);

        components.setSecuritySchemes(securitySchemes);

        model.setComponents(components);

        Context context = new TestValidationContextHelper(model);

        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(1, vh.getEventsSize());

    }

    //An oauth2 type SecurityScheme has a null value in the SecurityRequirement (REQUIRED to be a list of scope names)
    @Test
    public void testNullValue() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();

        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        //Set required fields of SecurityRequirement object for positive test case
        securityRequirement.addScheme("petStore_auth");
        securityRequirement.addScheme("api_key");

        //Set model
        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("petStore_auth", petStore_auth);
        securitySchemes.put("api_key", api_key);

        components.setSecuritySchemes(securitySchemes);

        model.setComponents(components);

        Context context = new TestValidationContextHelper(model);

        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(1, vh.getEventsSize());

    }

    //An apiKey type SecurityScheme in the SecurityRequirement is REQUIRED to have an empty array for value but has a non-empty list of scopes for value
    @Test
    public void testValueNotNull() {

        SecurityRequirementImpl securityRequirement = new SecurityRequirementImpl();
        //Declare and initialize SecurityScheme objects
        SecuritySchemeImpl petStore_auth = new SecuritySchemeImpl();
        SecuritySchemeImpl api_key = new SecuritySchemeImpl();

        //Set petStore_auth required fields
        petStore_auth.setType(oauth);
        petStore_auth.setFlows(flows);

        //Set api_key required fields
        api_key.setType(apiKey);
        api_key.setName("api_key");
        api_key.setIn(in);

        //Set required fields of SecurityRequirement object for positive test case
        List<String> scope = Arrays.asList("write:pets", "read:pets");
        securityRequirement.addScheme("petStore_auth", scope);
        securityRequirement.addScheme("api_key", scope);

        OpenAPIImpl model = new OpenAPIImpl();

        ComponentsImpl components = new ComponentsImpl();

        Map<String, SecurityScheme> securitySchemes = new HashMap<String, SecurityScheme>();
        securitySchemes.put("petStore_auth", petStore_auth);
        securitySchemes.put("api_key", api_key);

        components.setSecuritySchemes(securitySchemes);

        model.setComponents(components);

        Context context = new TestValidationContextHelper(model);

        TestValidationHelper vh = new TestValidationHelper();
        SecurityRequirementValidator validator = SecurityRequirementValidator.getInstance();
        validator.validate(vh, context, key, securityRequirement);

        Assert.assertEquals(1, vh.getEventsSize());

    }

}
